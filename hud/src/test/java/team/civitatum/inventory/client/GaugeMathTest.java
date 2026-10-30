package team.civitatum.inventory.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Values of other mods' getters brought to a bar from 0 to 1. */
class GaugeMathTest {
    @Test
    void pixelsOfTheModsBarAreDividedByItsLength() {
        assertEquals(0.5, GaugeMath.fraction(12, 24));
    }

    @Test
    void valueIsClampedToTheBar() {
        assertEquals(1, GaugeMath.fraction(82, 41));
        assertEquals(0, GaugeMath.fraction(-3, 41));
    }

    @Test
    void flagCountsAsFullOrEmpty() {
        assertEquals(1, GaugeMath.fraction(true, 1));
        assertEquals(0, GaugeMath.fraction(false, 1));
    }

    @Test
    void floatProgressKeepsItsFraction() {
        assertEquals(0.25, GaugeMath.fraction(0.25F, 1), 1e-6);
    }

    @Test
    void anythingElseIsAnEmptyBar() {
        assertEquals(0, GaugeMath.fraction("lit", 1));
        assertEquals(0, GaugeMath.fraction(null, 1));
    }

    @Test
    void ratioDividesTimeDoneByTimeNeeded() {
        assertEquals(0.25, GaugeMath.ratio(30, 120));
    }

    @Test
    void ratioIsEmptyWhileNothingIsNeeded() {
        assertEquals(0, GaugeMath.ratio(30, 0));
    }
}
