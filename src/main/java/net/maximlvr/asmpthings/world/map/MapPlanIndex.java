package net.maximlvr.asmpthings.world.map;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/** Disk-backed, ordered per-chunk streams. Heap usage depends on chunk count, not voxel/fragment count. */
public final class MapPlanIndex implements AutoCloseable {
    private static final int MAX_CHUNKS = 1_000_000;
    private static final int PAGE_BYTES = 2048 * MapOperation.BYTES;
    private final Path directory;
    private final FileChannel data;
    private final long[] keys, offsets;
    private final List<MapPlanReader.Material> materials;
    private final long inputOperations;

    public static final class Progress {
        public volatile String phase = "Lecture du JSON";
        public volatile long operations, written, totalReferences;
        public String describe() {
            return phase + " : " + operations + " opérations lues"
                    + (totalReferences > 0 ? ", index " + written + "/" + totalReferences : "");
        }
    }

    private MapPlanIndex(Path directory, long[] keys, long[] offsets, List<MapPlanReader.Material> materials,
                         long inputOperations) throws IOException {
        this.directory = directory;
        this.keys = keys;
        this.offsets = offsets;
        this.materials = materials;
        this.inputOperations = inputOperations;
        this.data = FileChannel.open(directory.resolve("chunks.bin"), StandardOpenOption.READ);
    }

    public static MapPlanIndex build(Reader json, Path cacheRoot, int minY, int maxY,
                                     BooleanSupplier cancelled, Progress progress) throws IOException {
        Files.createDirectories(cacheRoot);
        Path directory = Files.createTempDirectory(cacheRoot, "plan-");
        Path rawFile = directory.resolve("operations.bin"), indexedFile = directory.resolve("chunks.bin");
        boolean success = false;
        try {
            Long2IntOpenHashMap counts = new Long2IntOpenHashMap();
            List<MapPlanReader.Material> materials;
            try (FileChannel raw = FileChannel.open(rawFile, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                ByteBuffer page = ByteBuffer.allocate(PAGE_BYTES);
                materials = new MapPlanReader(json, minY, maxY, cancelled, (operation, material) -> {
                    if (page.remaining() < MapOperation.BYTES) flush(raw, page);
                    operation.write(page);
                    int minX = operation.x() >> 4, minZ = operation.z() >> 4;
                    int maxX = material.template() ? minX : (operation.endX() - 1) >> 4;
                    int maxZ = material.template() ? minZ : (operation.endZ() - 1) >> 4;
                    if ((long) (maxX - minX + 1) * (maxZ - minZ + 1) > MAX_CHUNKS)
                        throw new IOException("Un remplissage couvre plus de " + MAX_CHUNKS + " chunks");
                    for (int z = minZ; z <= maxZ; z++) for (int x = minX; x <= maxX; x++) {
                        long key = key(x, z);
                        int previous = counts.addTo(key, 1);
                        if (previous == Integer.MAX_VALUE) throw new IOException("Trop d'opérations dans un chunk");
                        if (counts.size() > MAX_CHUNKS) throw new IOException("Plan de plus de " + MAX_CHUNKS + " chunks");
                    }
                    progress.operations++;
                }).read();
                flush(raw, page);
            }
            checkCancelled(cancelled);
            progress.phase = "Indexation par chunk";
            long[] keys = counts.keySet().toLongArray();
            Arrays.sort(keys); // key encoding orders Z, then X, including negative coordinates.
            long[] offsets = new long[keys.length + 1];
            for (int i = 0; i < keys.length; i++) {
                offsets[i + 1] = Math.addExact(offsets[i], Math.multiplyExact((long) counts.get(keys[i]), MapOperation.BYTES));
                counts.put(keys[i], i); // Reuse the primitive map as key -> sorted index.
            }
            long bytes = offsets[keys.length];
            progress.totalReferences = bytes / MapOperation.BYTES;
            if (Files.getFileStore(directory).getUsableSpace() < bytes + 64L * 1024 * 1024)
                throw new IOException("Espace disque insuffisant pour l'index (" + bytes / (1024 * 1024) + " Mio nécessaires)");
            long[] positions = offsets.clone();
            ByteBuffer record = ByteBuffer.allocate(MapOperation.BYTES);
            try (FileChannel source = FileChannel.open(rawFile, StandardOpenOption.READ);
                 FileChannel target = FileChannel.open(indexedFile, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                if (bytes > 0) target.write(ByteBuffer.wrap(new byte[]{0}), bytes - 1);
                Cursor cursor = new Cursor(source, 0, source.size());
                MapOperation operation;
                while ((operation = cursor.next()) != null) {
                    checkCancelled(cancelled);
                    boolean template = materials.get(operation.material()).template();
                    int minX = operation.x() >> 4, minZ = operation.z() >> 4;
                    int maxX = template ? minX : (operation.endX() - 1) >> 4;
                    int maxZ = template ? minZ : (operation.endZ() - 1) >> 4;
                    for (int z = minZ; z <= maxZ; z++) for (int x = minX; x <= maxX; x++) {
                        checkCancelled(cancelled);
                        MapOperation clipped = template ? operation : operation.clipToChunk(x, z);
                        int chunkIndex = counts.get(key(x, z));
                        record.clear();
                        clipped.write(record);
                        record.flip();
                        long position = positions[chunkIndex];
                        while (record.hasRemaining()) position += target.write(record, position);
                        positions[chunkIndex] = position;
                        progress.written++;
                    }
                }
                for (int i = 0; i < keys.length; i++)
                    if (positions[i] != offsets[i + 1]) throw new IOException("Index incomplet pour le chunk " + i);
                progress.phase = "Finalisation de l'index";
                target.force(false);
            }
            Files.delete(rawFile);
            checkCancelled(cancelled);
            MapPlanIndex result = new MapPlanIndex(directory, keys, offsets, materials, progress.operations);
            success = true;
            return result;
        } finally {
            if (!success) {
                Files.deleteIfExists(rawFile);
                Files.deleteIfExists(indexedFile);
                Files.deleteIfExists(directory);
            }
        }
    }

    private static void flush(FileChannel output, ByteBuffer page) throws IOException {
        page.flip();
        while (page.hasRemaining()) output.write(page);
        page.clear();
    }

    private static void checkCancelled(BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) throw new CancellationException("Indexation arrêtée");
    }

    // Signed long comparison sorts signed Z first, then signed X encoded as an unsigned low word.
    public static long key(int x, int z) { return (long) z << 32 | (x ^ Integer.MIN_VALUE) & 0xffffffffL; }
    public static int chunkX(long key) { return (int) key ^ Integer.MIN_VALUE; }
    public static int chunkZ(long key) { return (int) (key >> 32); }
    public int chunkCount() { return keys.length; }
    public long chunkKey(int index) { return keys[index]; }
    public int findChunk(int x, int z) { return Arrays.binarySearch(keys, key(x, z)); }
    public long operationCount() { return inputOperations; }
    public long referenceCount() { return offsets[keys.length] / MapOperation.BYTES; }
    public List<MapPlanReader.Material> materials() { return materials; }
    public Cursor openChunk(int index) { return new Cursor(data, offsets[index], offsets[index + 1]); }

    /** A single page, even if one chunk has millions of overlapping operations. */
    public static final class Cursor {
        private final FileChannel input;
        private final long end;
        private long position;
        private final ByteBuffer page = ByteBuffer.allocate(PAGE_BYTES);
        Cursor(FileChannel input, long start, long end) {
            this.input = input;
            this.position = start;
            this.end = end;
            page.limit(0);
        }
        public MapOperation next() throws IOException {
            if (!page.hasRemaining()) {
                if (position == end) return null;
                page.clear();
                page.limit((int) Math.min(page.capacity(), end - position));
                int expected = page.remaining();
                while (page.hasRemaining()) {
                    int read = input.read(page, position + page.position());
                    if (read < 0) throw new EOFException("Index de carte tronqué");
                }
                position += expected;
                page.flip();
            }
            return MapOperation.read(page);
        }
    }

    @Override public void close() throws IOException {
        data.close();
        Files.deleteIfExists(directory.resolve("chunks.bin"));
        Files.deleteIfExists(directory);
    }
}
