package team.civitatum.inventory.client;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Xaero's minimap in the frame of the inventory windows; Xaero's own frame is switched off by
 * {@code MinimapFrameMixin}. Xaero's module is the square map plus a margin of 9 of its own pixels on each side
 * ({@code MinimapSession.getConfiguredWidth() = map size / 2 + 18}), where its frame used to be drawn.
 */
public final class MinimapFrame {
    /** Xaero's margin around the map, in its own pixels. */
    static final int XAERO_MARGIN = 9;
    /**
     * The frame is drawn around the map, not over it: {@link MorrowindSkin#frame} paints 2 px (outline and gold) inside
     * its left and top edges and 2 px (outline and shadow) inside its right and bottom edges.
     */
    static final int OUTSET = 2;

    private MinimapFrame() {
    }

    /**
     * @param moduleWidth     the module's width in GUI pixels
     * @param configuredWidth the same width in Xaero's own pixels, so their ratio scales the margin to GUI pixels
     */
    public static void draw(GuiGraphics graphics, int moduleX, int moduleY, int moduleWidth, int configuredWidth) {
        double margin = margin(moduleWidth, configuredWidth);
        int size = size(moduleWidth, margin);
        int x = start(moduleX, margin);
        int y = start(moduleY, margin);
        MinimapBounds.set(x, y, size);
        MorrowindSkin.frame(graphics, x, y, size, size, false);
    }

    static double margin(int moduleWidth, int configuredWidth) {
        return (double) XAERO_MARGIN * moduleWidth / configuredWidth;
    }

    static int start(int moduleStart, double margin) {
        return (int) Math.round(moduleStart + margin) - OUTSET;
    }

    static int size(int moduleWidth, double margin) {
        return (int) Math.round(moduleWidth - 2 * margin) + 2 * OUTSET;
    }
}
