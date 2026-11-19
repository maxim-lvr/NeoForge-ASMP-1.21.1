package net.maximlvr.asmpthings.world.map;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

/** An unsplit fill (or one NBT placement). Dimensions are exclusive lengths, never voxel arrays. */
public record MapOperation(int material, int rotation, int x, int y, int z, int sizeX, int sizeY, int sizeZ) {
    public static final int BYTES = 7 * Integer.BYTES;

    public MapOperation {
        if (material < 0 || material >= 1 << 30 || rotation < 0 || rotation > 3
                || sizeX <= 0 || sizeY <= 0 || sizeZ <= 0)
            throw new IllegalArgumentException("Dimensions ou matériau invalides");
        Math.addExact(x, sizeX);
        Math.addExact(y, sizeY);
        Math.addExact(z, sizeZ);
    }

    public long volume() { return Math.multiplyExact(Math.multiplyExact((long) sizeX, sizeY), sizeZ); }
    public int endX() { return x + sizeX; }
    public int endY() { return y + sizeY; }
    public int endZ() { return z + sizeZ; }

    public MapOperation clipToChunk(int chunkX, int chunkZ) {
        int minX = Math.max(x, chunkX * 16), minZ = Math.max(z, chunkZ * 16);
        int maxX = Math.min(endX(), chunkX * 16 + 16), maxZ = Math.min(endZ(), chunkZ * 16 + 16);
        return minX < maxX && minZ < maxZ
                ? new MapOperation(material, rotation, minX, y, minZ, maxX - minX, sizeY, maxZ - minZ) : null;
    }

    /** Lazily split only the current operation. At most one fragment is retained by the caller. */
    public Iterator<MapOperation> sections() {
        return new Iterator<>() {
            int nextX = x, nextY = y, nextZ = z;
            @Override public boolean hasNext() { return nextX < endX(); }
            @Override public MapOperation next() {
                if (!hasNext()) throw new NoSuchElementException();
                int sx = Math.min(endX() - nextX, 16 - (nextX & 15));
                int sy = Math.min(endY() - nextY, 16 - (nextY & 15));
                int sz = Math.min(endZ() - nextZ, 16 - (nextZ & 15));
                MapOperation result = new MapOperation(material, rotation, nextX, nextY, nextZ, sx, sy, sz);
                nextZ += sz;
                if (nextZ == endZ()) {
                    nextZ = z;
                    nextY += sy;
                    if (nextY == endY()) { nextY = y; nextX += sx; }
                }
                return result;
            }
        };
    }

    public void write(ByteBuffer target) {
        target.putInt(material | rotation << 30).putInt(x).putInt(y).putInt(z)
                .putInt(sizeX).putInt(sizeY).putInt(sizeZ);
    }

    public static MapOperation read(ByteBuffer source) {
        int header = source.getInt();
        return new MapOperation(header & 0x3fffffff, header >>> 30, source.getInt(), source.getInt(), source.getInt(),
                source.getInt(), source.getInt(), source.getInt());
    }
}
