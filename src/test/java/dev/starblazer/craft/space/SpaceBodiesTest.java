package dev.starblazer.craft.space;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpaceBodiesTest {
    @Test void surfaceAndCraters() {
        assertEquals(6296,SpaceBodies.moonSurface(512,0),1e-9);
        assertTrue(Double.isNaN(SpaceBodies.moonSurface(608,0)));
        assertTrue(Double.isNaN(SpaceBodies.moonSurface(Double.NaN,0)));
        assertTrue(SpaceBodies.craterDepth(20,-15)>4);
    }
    @Test void descentWalkingJumpAndUnderside() {
        var hit=SpaceBodies.contact(512,6500,0,512,6290,0,-50);
        assertNotNull(hit); assertEquals(6296,hit.y(),1e-9);
        assertNotNull(SpaceBodies.contact(512,6296,0,512.1,6295.9,0,-.01));
        assertNull(SpaceBodies.contact(512,6296,0,512,6297,0,.4));
        assertNull(SpaceBodies.contact(512,6100,0,512,6090,0,-1));
        assertNull(SpaceBodies.contact(1000,6500,0,1000,6000,0,-10));
    }
    @Test void moonGravityAndVacuum() {
        assertEquals(1.0/6,SpaceBodies.gravity(512,6296,0,0),1e-9);
        assertEquals(0,SpaceBodies.gravity(512,6600,0,0));
        assertEquals(1,SpaceBodies.gravity(0,70,0,1));
        assertEquals(0,SpaceBodies.gravity(1000,6000,1000,0));
    }
}
