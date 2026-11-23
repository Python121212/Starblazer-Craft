package dev.starblazer.craft;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import dev.starblazer.craft.SelectionBounds.Point;

class SelectionBoundsTest {
    @Test void inclusiveAndReversed() {
        assertEquals(1, SelectionBounds.volume(new Point(0,0,0), new Point(0,0,0)));
        assertEquals(27, SelectionBounds.volume(new Point(1,1,1), new Point(-1,-1,-1)));
        assertTrue(SelectionBounds.contains(new Point(3,4,5), new Point(-3,-4,-5), new Point(-3,4,0)));
        assertFalse(SelectionBounds.contains(new Point(0,0,0), new Point(1,1,1), new Point(2,0,0)));
    }
    @Test void limitAndOverflow() {
        assertEquals(4096, SelectionBounds.volume(new Point(0,0,0), new Point(15,15,15)));
        assertTrue(SelectionBounds.volume(new Point(0,0,0), new Point(16,15,15)) > 4096);
        assertTrue(SelectionBounds.volume(new Point(Integer.MIN_VALUE,0,0), new Point(Integer.MAX_VALUE,0,0)) > 4096);
    }
}
