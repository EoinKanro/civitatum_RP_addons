package team.civitatum.inventory.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The frame wraps Xaero's map: the module is the map plus 9 of Xaero's pixels on each side. */
class MinimapFrameTest {
    @Test
    void frameSurroundsTheMapAtOneToOneScale() {
        // map of 100 px: configured width 118, drawn 118 GUI px wide at x = 200
        double margin = MinimapFrame.margin(118, 118);
        assertEquals(9.0, margin);
        assertEquals(200 + 9 - 2, MinimapFrame.start(200, margin));
        assertEquals(100 + 4, MinimapFrame.size(118, margin));
    }

    @Test
    void marginScalesWithTheModule() {
        // the same module drawn at half size: 59 GUI px, 4.5 px margin, 50 px map
        double margin = MinimapFrame.margin(59, 118);
        assertEquals(4.5, margin);
        assertEquals(50 + 4, MinimapFrame.size(59, margin));
    }
}
