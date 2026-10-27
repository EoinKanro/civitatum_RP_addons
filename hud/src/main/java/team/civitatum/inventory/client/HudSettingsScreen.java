package team.civitatum.inventory.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** The mod's config screen in the mod list; every change is saved at once. */
public final class HudSettingsScreen extends Screen {
    private static final int BUTTON_WIDTH = 200;
    /** Button height plus a gap. */
    private static final int ROW = 24;
    private static final int TEXT = 0xFFE8D9B7;
    private final Screen parent;

    public HudSettingsScreen(Screen parent) {
        super(CivitatumInventoryScreen.tr("settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = (width - BUTTON_WIDTH) / 2, y = height / 2 - ROW;
        var style = addRenderableWidget(new MorrowindButton(styleLabel(), b -> {
            var preferences = InventoryPreferences.get();
            preferences.hotbarStyle = preferences.hotbarStyle.next();
            preferences.save();
            b.setMessage(styleLabel());
        }, BUTTON_WIDTH));
        style.setPosition(x, y);
        var done = addRenderableWidget(new MorrowindButton(CommonComponents.GUI_DONE, b -> onClose(), BUTTON_WIDTH));
        done.setPosition(x, y + 2 * ROW);
    }

    private static Component styleLabel() {
        return CivitatumInventoryScreen.tr("hotbar", InventoryPreferences.get().hotbarStyle.label());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float tick) {
        super.render(g, mx, my, tick);
        g.drawCenteredString(font, title, width / 2, height / 2 - 2 * ROW, TEXT);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
