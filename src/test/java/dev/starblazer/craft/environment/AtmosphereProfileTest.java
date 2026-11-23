package dev.starblazer.craft.environment;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AtmosphereProfileTest {
    @Test void boundaries() {
        assertEquals(1, AtmosphereProfile.sample(320).density());
        assertEquals(.85, AtmosphereProfile.sample(800).density(), 1e-12);
        assertEquals(.12, AtmosphereProfile.sample(2000).density(), 1e-12);
        assertEquals(.005, AtmosphereProfile.sample(3500).density(), 1e-12);
        var space = AtmosphereProfile.sample(5000);
        assertEquals(AtmosphereProfile.Layer.SPACE, space.layer());
        assertEquals(0, space.density()); assertEquals(0, space.gravity());
        assertEquals(1, space.stars());
    }
    @Test void boundedMonotonicAndContinuous() {
        double density=1, gravity=1, oxygen=1, lift=1;
        for (int y=-64; y<=5100; y++) {
            var a=AtmosphereProfile.sample(y);
            assertTrue(a.density()>=0 && a.density()<=density + 1e-12);
            assertTrue(a.gravity()>=0 && a.gravity()<=gravity + 1e-12);
            assertTrue(a.oxygen()>=0 && a.oxygen()<=oxygen + 1e-12);
            assertTrue(a.elytraLift()>=0 && a.elytraLift()<=lift + 1e-12);
            density=a.density(); gravity=a.gravity(); oxygen=a.oxygen(); lift=a.elytraLift();
        }
        for (double y : new double[]{320,800,1200,1500,2000,3500,5000}) {
            assertEquals(AtmosphereProfile.sample(y-.001).density(), AtmosphereProfile.sample(y+.001).density(), .00001);
        }
    }
    @Test void elytraAndOxygen() {
        assertEquals(1, AtmosphereProfile.sample(1200).elytraLift());
        assertEquals(.5, AtmosphereProfile.sample(1350).elytraLift(), 1e-12);
        assertEquals(0, AtmosphereProfile.sample(1500).elytraLift());
        assertEquals(1, AtmosphereProfile.sample(1500).oxygen());
        assertEquals(0, AtmosphereProfile.sample(2000).oxygen());
    }
    @Test void velocityDependentEffects() {
        assertEquals(0, AtmosphereProfile.windStrength(5000, 100));
        assertTrue(AtmosphereProfile.windStrength(1000, 2) > AtmosphereProfile.windStrength(1000, 1));
        assertTrue(AtmosphereProfile.reentryStrength(2200, 5, -2)>AtmosphereProfile.reentryStrength(2200, 1, -1));
        assertEquals(0, AtmosphereProfile.reentryStrength(2200, 5, 1));
        assertEquals(0, AtmosphereProfile.reentryStrength(5000, 100, -10));
        assertThrows(IllegalArgumentException.class, () -> AtmosphereProfile.sample(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> AtmosphereProfile.windStrength(1000, -1));
    }
}
