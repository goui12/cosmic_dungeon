package net.goui.cosmicdungeon.achievement.d1;

/** Bounded incremental traversal without allocating a list of room blocks. */
public final class RoomScanCursor {
    public record Point(int x, int y, int z) {}
    private final int minX, minY, minZ;
    private final long width, height, volume;
    private long index;
    public RoomScanCursor(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, long maximumVolume) {
        if (maxX < minX || maxY < minY || maxZ < minZ) throw new IllegalArgumentException("Inverted room bounds");
        this.minX = minX; this.minY = minY; this.minZ = minZ;
        width = (long)maxX - minX + 1; height = (long)maxY - minY + 1;
        try { volume = Math.multiplyExact(Math.multiplyExact(width, height), (long)maxZ - minZ + 1); }
        catch (ArithmeticException overflow) { throw new IllegalArgumentException("Room volume overflow", overflow); }
        if (volume > maximumVolume) throw new IllegalArgumentException("Room exceeds configured scan volume: " + volume);
    }
    public boolean hasNext() { return index < volume; }
    public Point next() {
        if (!hasNext()) throw new IllegalStateException("Room scan complete");
        long n = index++;
        return new Point((int)(minX + n % width), (int)(minY + n / width % height), (int)(minZ + n / (width * height)));
    }
    public long volume() { return volume; }
}
