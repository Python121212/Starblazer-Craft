package dev.starblazer.craft;

/** Pure integer geometry, independent of Minecraft for inexpensive CI tests. */
public final class SelectionBounds {
    public static final long MAX_VOLUME = 4096;
    public static final int MAX_EXTRAS = 256;
    private SelectionBounds() {}

    public record Point(int x, int y, int z) {}

    public static long volume(Point a, Point b) {
        long x = Math.abs((long) a.x - b.x) + 1;
        long y = Math.abs((long) a.y - b.y) + 1;
        long z = Math.abs((long) a.z - b.z) + 1;
        // Saturate before multiplying, including deliberately corrupt saved coordinates.
        if (x > MAX_VOLUME || y > MAX_VOLUME || z > MAX_VOLUME) return MAX_VOLUME + 1;
        return Math.min(MAX_VOLUME + 1, x * y * z);
    }

    public static boolean contains(Point a, Point b, Point p) {
        return p.x >= Math.min(a.x, b.x) && p.x <= Math.max(a.x, b.x)
            && p.y >= Math.min(a.y, b.y) && p.y <= Math.max(a.y, b.y)
            && p.z >= Math.min(a.z, b.z) && p.z <= Math.max(a.z, b.z);
    }
}
