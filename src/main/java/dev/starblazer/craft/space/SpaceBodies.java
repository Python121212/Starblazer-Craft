package dev.starblazer.craft.space;

/** Small, fixed, same-coordinate prototype, not a procedural solar system. */
public final class SpaceBodies {
    public static final double MOON_X = 512, MOON_Y = 6200, MOON_Z = 0, MOON_RADIUS = 96;
    public static final double EARTH_Y = -2048, EARTH_RADIUS = 2048;
    private SpaceBodies() {}
    public record Contact(double x, double y, double z) {}

    public static double craterDepth(double x, double z) {
        return crater(x-20,z+15,18,5) + crater(x+28,z-24,12,3) + crater(x+30,z+35,9,2);
    }
    private static double crater(double x,double z,double radius,double depth) {
        double t=1-(x*x+z*z)/(radius*radius);
        return t>0 ? t*t*depth : 0;
    }
    public static double moonSurface(double x, double z) {
        double dx=x-MOON_X, dz=z-MOON_Z;
        double r2=dx*dx+dz*dz;
        if (!Double.isFinite(r2) || r2>=MOON_RADIUS*MOON_RADIUS) return Double.NaN;
        return MOON_Y+Math.sqrt(MOON_RADIUS*MOON_RADIUS-r2)*(1-craterDepth(dx,dz)/MOON_RADIUS);
    }
    public static double gravity(double x,double y,double z,double earthGravity) {
        double surface=moonSurface(x,z);
        if (!Double.isFinite(surface) || y<surface-1 || y>surface+192) return earthGravity;
        double height=Math.max(0,y-surface);
        double t=Math.max(0,Math.min(1,(height-32)/160));
        return (1-t*t*(3-2*t))/6;
    }
    /** Sweep from above, then support walking on the upper hemisphere. No underside collision. */
    public static Contact contact(double px,double py,double pz,double x,double y,double z,double vy) {
        if (!Double.isFinite(py) || !Double.isFinite(y) || !Double.isFinite(vy) || vy>0) return null;
        double surface=moonSurface(x,z), oldSurface=moonSurface(px,pz);
        if (!Double.isFinite(surface) || y>surface+.001) return null;
        boolean above=Double.isFinite(oldSurface) ? py>=oldSurface-.15 : py>=MOON_Y+MOON_RADIUS;
        if (!above) return null;
        // Do not pull players uphill through a steep wall; keep the previous horizontal position.
        if (Double.isFinite(oldSurface) && surface>py+.6) return new Contact(px,oldSurface,pz);
        return new Contact(x,surface,z);
    }
}
