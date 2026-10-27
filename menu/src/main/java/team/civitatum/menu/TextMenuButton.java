package team.civitatum.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** A frameless text button; the label shrinks to fit the button width. */
public final class TextMenuButton extends AbstractButton {
    private static final int INACTIVE = 0xFF777777;
    private static final int HOVERED = 0xFFFFDF95;
    private static final int NORMAL = 0xFFFFFFFF;
    /** Free space kept around the label: 4 px on each side. */
    private static final int PADDING = 8;
    /** Height of a line of the vanilla font. */
    private static final float LINE_HEIGHT = 8.0F;

    private final Runnable action;
    private final float textScale;

    public TextMenuButton(int x, int y, int width, int height, Component label, float scale, Runnable action) {
        super(x, y, width, height, label);
        this.action = action;
        this.textScale = scale;
    }

    @Override
    public void onPress() {
        action.run();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        int color = !active ? INACTIVE : isHoveredOrFocused() ? HOVERED : NORMAL;
        int labelWidth = font.width(getMessage());
        float fittedScale = Math.min(textScale, (float) Math.max(1, width - PADDING) / Math.max(1, labelWidth));
        graphics.pose().pushPose();
        graphics.pose().translate(getX() + width / 2.0F, getY() + (height - LINE_HEIGHT * fittedScale) / 2.0F, 0.0F);
        graphics.pose().scale(fittedScale, fittedScale, 1.0F);
        graphics.drawString(font, getMessage(), -labelWidth / 2, 0, color, true);
        graphics.pose().popPose();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
