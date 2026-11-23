package dev.starblazer.craft.environment;

/** Game-scale atmosphere; no Minecraft classes, world scans or allocations per block. */
public final class AtmosphereProfile {
    public static final double SPACE_ALTITUDE = 5000.0;
    /** Compatibility alias: a boundary, no longer a flight ceiling. */
    @Deprecated public static final double MAX_ALTITUDE = SPACE_ALTITUDE;
    public static final double HYPOXIA_START = 1500.0;
    private AtmosphereProfile() {}

    public enum Layer { LOWER_ATMOSPHERE, UPPER_ATMOSPHERE, TRANSITION, NEAR_VACUUM, SPACE }
    public record Sample(Layer layer, double density, double gravity, double elytraLift,
                         double oxygen, double darkness, double stars) {}

    private static double smooth(double a, double b, double y) {
        double t = Math.max(0, Math.min(1, (y - a) / (b - a)));
        return t * t * (3 - 2 * t);
    }
    private static double mix(double a, double b, double t) { return a + (b - a) * t; }
    public static Sample sample(double y) {
        if (!Double.isFinite(y)) throw new IllegalArgumentException("Altitude must be finite");
        Layer layer = y < 800 ? Layer.LOWER_ATMOSPHERE : y < 2000 ? Layer.UPPER_ATMOSPHERE
            : y < 3500 ? Layer.TRANSITION : y < SPACE_ALTITUDE ? Layer.NEAR_VACUUM : Layer.SPACE;
        double density = y < 800 ? mix(1, .85, smooth(320, 800, y))
            : y < 2000 ? mix(.85, .12, smooth(800, 2000, y))
            : y < 3500 ? mix(.12, .005, smooth(2000, 3500, y))
            : mix(.005, 0, smooth(3500, SPACE_ALTITUDE, y));
        return new Sample(layer, density, 1 - smooth(3500, SPACE_ALTITUDE, y),
            1 - smooth(1200, 1500, y), 1 - smooth(HYPOXIA_START, 2000, y),
            smooth(320, SPACE_ALTITUDE, y), smooth(2000, 3500, y));
    }

    public static float advanceExposure(float previous, Sample air) {
        if (!Float.isFinite(previous) || previous < 0) previous = 0;
        if (air.oxygen() >= 1) return 0;
        return Math.min(200, previous + (float) (1 - air.oxygen()));
    }

    /** Relative wind strength and heating are bounded visual inputs, not damage physics. */
    public static double windStrength(double y, double speed) {
        checkSpeed(speed);
        return Math.min(1, sample(y).density() * speed * speed / 4);
    }
    public static double reentryStrength(double y, double speed, double verticalSpeed) {
        checkSpeed(speed);
        if (!Double.isFinite(verticalSpeed)) throw new IllegalArgumentException("Invalid vertical speed");
        if (verticalSpeed >= 0 || y < 800) return 0;
        return Math.min(1, sample(y).density() * speed * speed * speed / 32);
    }
    private static void checkSpeed(double speed) {
        if (!Double.isFinite(speed) || speed < 0) throw new IllegalArgumentException("Invalid speed");
    }
}
