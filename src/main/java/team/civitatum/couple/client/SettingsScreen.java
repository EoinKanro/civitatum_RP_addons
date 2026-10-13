package team.civitatum.couple.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class SettingsScreen extends Screen {
    private final Screen parent;
    public SettingsScreen(Screen parent) { super(Component.translatable("ui.civitatum_couple.settings_title")); this.parent = parent; }
    @Override protected void init() {
        int x = width / 2 - 120, y = Math.max(46, height / 2 - 54);
        addToggle(x, y, "carry_requests", CoupleClientConfig.CARRY_REQUESTS);
        addToggle(x, y + 26, "kiss_requests", CoupleClientConfig.KISS_REQUESTS);
        addToggle(x, y + 52, "sound", CoupleClientConfig.SOUND);
        addToggle(x, y + 78, "particles", CoupleClientConfig.PARTICLES);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(x, y + 112, 240, 20).build());
    }
    private void addToggle(int x, int y, String key, ModConfigSpec.BooleanValue option) {
        addRenderableWidget(Button.builder(label(key, option.get()), b -> {
            option.set(!option.get()); CoupleClientConfig.SPEC.save();
            b.setMessage(label(key, option.get())); ClientState.sendPreferences();
        }).bounds(x, y, 240, 20).build());
    }
    private Component label(String key, boolean value) {
        return Component.translatable("setting.civitatum_couple." + key).append(": ").append(Component.translatable(value ? "options.on" : "options.off"));
    }
    @Override public void render(GuiGraphics g, int x, int y, float delta) {
        g.pose().pushPose();g.pose().translate(0,0,400);
        g.fill(0, 0, width, height, 0xE016181F);
        g.drawCenteredString(font, title, width / 2, 20, 0xFFF1E5D7);
        g.drawCenteredString(font, Component.translatable("ui.civitatum_couple.settings_hint"), width / 2, 34, 0xFFA9A3A0);
        super.render(g, x, y, delta);
        g.flush();g.pose().popPose();
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { minecraft.setScreen(parent); }
}
