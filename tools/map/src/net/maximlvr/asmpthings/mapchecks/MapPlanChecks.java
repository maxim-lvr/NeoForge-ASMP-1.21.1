package net.maximlvr.asmpthings.mapchecks;

import net.maximlvr.asmpthings.world.map.MapOperation;
import net.maximlvr.asmpthings.world.map.MapPlanIndex;
import net.maximlvr.asmpthings.world.map.MapPlanReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CancellationException;

public final class MapPlanChecks {
    public static void main(String[] args) throws Exception {
        if (args.length > 0) { actualMap(Path.of(args[0])); return; }
        projection();
        largeFill();
        malformedAndCancelled();
        System.out.println("Carte : ordre des remplissages, limites de chunks/sections, coordonnées négatives, NBT, annulation et nettoyage OK.");
    }

    private static void projection() throws Exception {
        String json = """
                {"zone":{"pos":[-17,-4,-17],"size":[50,32,50]},"runs":[
                  {"block":"minecraft:air","pos":[-17,-10,-17],"size":[50,50,50]},
                  {"block":"minecraft:deepslate","pos":[-16,0,-16],"size":[32,3,32]},
                  {"block":"minecraft:water","pos":[15,1,0],"size":[3,1,2]},
                  {"block":"minecraft:ladder[facing=north]","pos":[16,3,0],"size":[1,3,1]},
                  {"kind":"base","block":"minecraft:stone","pos":[0,0,0],"size":[1,1,1]},
                  {"block":"minecraft:air","pos":[1,0,0],"size":[1,1,1]},
                  {"name":"room_1","pos":[48,0,48],"size":[32,8,32],"rotation_y":270}
                ]}
                """;
        List<MapOperation> original = new ArrayList<>();
        List<MapPlanReader.Material> materials = new MapPlanReader(new StringReader(json), -4, 28, () -> false,
                (operation, material) -> original.add(operation)).read();
        Path root = Files.createTempDirectory("asmp-map-check-");
        try (MapPlanIndex index = MapPlanIndex.build(new StringReader(json), root, -4, 28, () -> false, new MapPlanIndex.Progress())) {
            int[] expected = new int[50 * 32 * 50], actual = new int[expected.length];
            Arrays.fill(expected, -1);
            Arrays.fill(actual, -1);
            for (MapOperation operation : original) if (!materials.get(operation.material()).template()) paint(expected, operation);
            long previous = Long.MIN_VALUE;
            int templates = 0;
            for (int i = 0; i < index.chunkCount(); i++) {
                long key = index.chunkKey(i);
                require(i == 0 || key > previous, "Tri de chunks incorrect");
                require(index.findChunk(MapPlanIndex.chunkX(key), MapPlanIndex.chunkZ(key)) == i, "Recherche de chunk incorrecte");
                previous = key;
                var cursor = index.openChunk(i);
                MapOperation operation;
                while ((operation = cursor.next()) != null) {
                    if (index.materials().get(operation.material()).template()) {
                        templates++;
                        require(operation.rotation() == 3 && operation.sizeX() == 32, "NBT dupliqué ou tronqué");
                        continue;
                    }
                    require(operation.x() >> 4 == MapPlanIndex.chunkX(key) && (operation.endX() - 1) >> 4 == MapPlanIndex.chunkX(key), "Remplissage hors de son chunk X");
                    require(operation.z() >> 4 == MapPlanIndex.chunkZ(key) && (operation.endZ() - 1) >> 4 == MapPlanIndex.chunkZ(key), "Remplissage hors de son chunk Z");
                    long volume = 0;
                    var pieces = operation.sections();
                    while (pieces.hasNext()) {
                        MapOperation piece = pieces.next();
                        require(piece.x() >> 4 == (piece.endX() - 1) >> 4 && piece.y() >> 4 == (piece.endY() - 1) >> 4
                                && piece.z() >> 4 == (piece.endZ() - 1) >> 4, "Fragment traverse une section");
                        volume += piece.volume();
                        paint(actual, piece);
                    }
                    require(volume == operation.volume(), "Perte de blocs au découpage");
                }
            }
            require(templates == 1, "Un NBT doit être posé une fois à son ancre");
            require(Arrays.equals(expected, actual), "Le placement par chunk ne respecte pas l'ordre des runs");
        }
        require(empty(root), "Cache non supprimé");
        Files.delete(root);
    }

    private static void paint(int[] blocks, MapOperation operation) {
        for (int x = operation.x(); x < operation.endX(); x++) for (int y = operation.y(); y < operation.endY(); y++)
            for (int z = operation.z(); z < operation.endZ(); z++) blocks[((x + 17) * 32 + y + 4) * 50 + z + 17] = operation.material();
    }

    private static void largeFill() throws Exception {
        Path root = Files.createTempDirectory("asmp-map-large-fill-");
        String json = "[{\"block\":\"minecraft:air\",\"pos\":[0,-64,0],\"size\":[128,384,128]}]";
        try (MapPlanIndex index = MapPlanIndex.build(new StringReader(json), root, -64, 320, () -> false, new MapPlanIndex.Progress())) {
            require(index.operationCount() == 1 && index.referenceCount() == 64, "Le gros remplissage a été développé par section dans l'index");
            long volume = 0;
            for (int i = 0; i < index.chunkCount(); i++) {
                var cursor = index.openChunk(i);
                MapOperation operation = cursor.next();
                require(operation.sizeY() == 384 && cursor.next() == null, "La hauteur doit rester compacte");
                volume += operation.volume();
            }
            require(volume == 128L * 384 * 128, "Volume non conservé");
        }
        Files.delete(root);
    }

    private static void malformedAndCancelled() throws Exception {
        Path root = Files.createTempDirectory("asmp-map-cancel-");
        String valid = "{\"block\":\"minecraft:air\",\"pos\":[0,-64,0],\"size\":[32,384,32]}";
        MapPlanIndex.Progress progress = new MapPlanIndex.Progress();
        try {
            MapPlanIndex.build(new StringReader("[" + valid + "," + valid + "]"), root, -64, 320, () -> progress.operations >= 1, progress);
            throw new AssertionError("Annulation ignorée");
        } catch (CancellationException expected) {}
        require(empty(root), "Fichiers temporaires conservés après annulation");
        try {
            MapPlanIndex.build(new StringReader("[" + valid + ", {\"block\":\"minecraft:air\",\"pos\":[0,0,0],\"size\":[2147483647,5,5]}]"),
                    root, -64, 320, () -> false, new MapPlanIndex.Progress());
            throw new AssertionError("Dimensions hors limites acceptées");
        } catch (IOException expected) {}
        require(empty(root), "Fichiers temporaires conservés après erreur JSON");
        Files.delete(root);
    }

    private static void actualMap(Path path) throws Exception {
        long start = System.nanoTime();
        Path root = Files.createTempDirectory("asmp-real-map-check-");
        MapPlanIndex.Progress progress = new MapPlanIndex.Progress();
        try (Reader reader = Files.newBufferedReader(path);
             MapPlanIndex index = MapPlanIndex.build(reader, root, -64, 320, () -> false, progress)) {
            System.out.println("Plan réel indexé : " + index.operationCount() + " opérations, " + index.referenceCount()
                    + " références, " + index.chunkCount() + " chunks, " + index.materials().size() + " matériaux.");
            int[][] samples = {{0, 0}, {1, 1}, {300, 300}, {624, 624}};
            Map<Long, List<MapOperation>> expected = new HashMap<>();
            for (int[] sample : samples) expected.put(MapPlanIndex.key(sample[0], sample[1]), new ArrayList<>());
            try (Reader secondRead = Files.newBufferedReader(path)) {
                new MapPlanReader(secondRead, -64, 320, () -> false, (operation, material) -> {
                    for (int[] sample : samples) {
                        if (material.template()) {
                            if (operation.x() >> 4 == sample[0] && operation.z() >> 4 == sample[1])
                                expected.get(MapPlanIndex.key(sample[0], sample[1])).add(operation);
                        } else {
                            MapOperation clipped = operation.clipToChunk(sample[0], sample[1]);
                            if (clipped != null) expected.get(MapPlanIndex.key(sample[0], sample[1])).add(clipped);
                        }
                    }
                }).read();
            }
            for (int[] sample : samples) {
                int number = index.findChunk(sample[0], sample[1]);
                List<MapOperation> wanted = expected.get(MapPlanIndex.key(sample[0], sample[1]));
                require(number >= 0 || wanted.isEmpty(), "Chunk réel absent de l'index");
                if (number < 0) continue;
                List<MapOperation> actual = new ArrayList<>();
                var cursor = index.openChunk(number);
                MapOperation operation;
                while ((operation = cursor.next()) != null) actual.add(operation);
                require(wanted.equals(actual), "Ordre ou contenu incorrect du chunk " + Arrays.toString(sample));
            }
            System.out.println("Échantillons du plan réel identiques à la lecture d'origine. Durée : "
                    + (System.nanoTime() - start) / 1_000_000 + " ms, heap maximal : " + Runtime.getRuntime().maxMemory() / (1024 * 1024) + " Mio.");
        }
        require(empty(root), "Index réel non supprimé");
        Files.delete(root);
    }

    private static boolean empty(Path directory) throws IOException {
        try (var entries = Files.list(directory)) { return entries.findAny().isEmpty(); }
    }
    private static void require(boolean test, String message) { if (!test) throw new AssertionError(message); }
}
