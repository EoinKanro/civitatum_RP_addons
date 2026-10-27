package team.civitatum.inventory.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Original tiled textures; panel alpha 102/255 = 40% opacity / 60% transparency. */
public final class MorrowindSkin {
    public static final int PANEL_ALPHA = 102;
    private static final ResourceLocation STONE =
        ResourceLocation.fromNamespaceAndPath("civitatum_hud", "textures/gui/stone.png");
    private static final int TILE = 32;
    private static final int OUTLINE = 0xFF17130E, SHADOW = 0xFF54422C, GOLD = 0xFFB29A6B, LIT_GOLD = 0xFFE2C18A;

    public static void body(GuiGraphics g, int x, int y, int w, int h) {
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(1, 1, 1, (float) ((1 - InventoryPreferences.get().transparency) * 255 / PANEL_ALPHA));
        for (int dy = 0; dy < h; dy += TILE) {
            for (int dx = 0; dx < w; dx += TILE) {
                g.blit(STONE, x + dx, y + dy, 0, 0, Math.min(TILE, w - dx), Math.min(TILE, h - dy), TILE, TILE);
            }
        }
        g.flush();
        g.setColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }

    public static void frame(GuiGraphics g, int x, int y, int w, int h, boolean lit) {
        int gold = lit ? LIT_GOLD : GOLD;
        g.fill(x, y, x + w, y + 1, OUTLINE);
        g.fill(x, y + h - 1, x + w, y + h, OUTLINE);
        g.fill(x, y, x + 1, y + h, OUTLINE);
        g.fill(x + w - 1, y, x + w, y + h, OUTLINE);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, gold);
        g.fill(x + 1, y + 1, x + 2, y + h - 1, gold);
        g.fill(x + 2, y + h - 2, x + w - 1, y + h - 1, SHADOW);
        g.fill(x + w - 2, y + 2, x + w - 1, y + h - 1, SHADOW);
    }

    /**
     * {@link #frame} as a ring ({@link FrameRing}) inscribed in the square {@code size} wide.
     * Pixels of one colour in a row are drawn as one fill.
     */
    public static void circleFrame(GuiGraphics g, int x, int y, int size, boolean lit) {
        int[] colors = {0, OUTLINE, lit ? LIT_GOLD : GOLD, SHADOW};
        double radius = size / 2.0;
        for (int row = 0; row < size; row++) {
            int runStart = 0;
            int runPart = FrameRing.NONE;
            for (int column = 0; column <= size; column++) {
                int part = column < size
                    ? FrameRing.at(column + 0.5 - radius, row + 0.5 - radius, radius) : FrameRing.NONE;
                if (part != runPart) {
                    if (runPart != FrameRing.NONE) {
                        g.fill(x + runStart, y + row, x + column, y + row + 1, colors[runPart]);
                    }
                    runStart = column;
                    runPart = part;
                }
            }
        }
    }
}
