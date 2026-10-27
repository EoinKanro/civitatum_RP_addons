package team.civitatum.inventory.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public final class CivitatumHud {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("civitatum_hud", "status");
    private static final ResourceLocation ARS_MANA = ResourceLocation.fromNamespaceAndPath("ars_nouveau", "mana_hud");
    private static final String APPLESKIN = "appleskin";
    /** Full food bar, as in vanilla. */
    private static final int MAX_FOOD = 20;
    private static final int FOOD_COLOR = 0xFF718D48, FOOD_PREVIEW = 0xFFB9D98A;
    private static final int BAR_HEIGHT = 9;
    private static final int SLOT = 24;

    public static boolean active() {
        var mc = Minecraft.getInstance();
        return InventoryPreferences.get().enabled && mc.player != null && !mc.player.isSpectator()
            && mc.gameMode != null && !mc.gameMode.hasInfiniteItems();
    }

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, ID, CivitatumHud::render);
        event.replaceLayer(VanillaGuiLayers.EFFECTS, new StatusEffectsLayer());
    }

    public static void hideReplaced(RenderGuiLayerEvent.Pre event) {
        if (!active()) return;
        var id = event.getName();
        if (id.equals(VanillaGuiLayers.HOTBAR) || id.equals(VanillaGuiLayers.PLAYER_HEALTH)
                || id.equals(VanillaGuiLayers.FOOD_LEVEL)
                // Armour and experience are shown in the inventory's equipment window instead.
                || id.equals(VanillaGuiLayers.ARMOR_LEVEL) || id.equals(VanillaGuiLayers.EXPERIENCE_BAR)
                || id.equals(VanillaGuiLayers.EXPERIENCE_LEVEL)
                // AppleSkin's own layers sit on the vanilla hearts and food bar and are hidden with them; only its
                // estimate of the held food is shown, on this HUD's food bar.
                || id.getNamespace().equals(APPLESKIN)) {
            event.setCanceled(true);
        }
        if (id.equals(ARS_MANA) && ArsBridge.mana(Minecraft.getInstance().player) != null) event.setCanceled(true);
    }

    public static void render(GuiGraphics g, DeltaTracker tick) {
        var mc = Minecraft.getInstance();
        if (!active() || mc.options.hideGui || mc.screen instanceof AbstractContainerScreen<?>) return;
        var player = mc.player;
        int x = 8, y = g.guiHeight() - 34, w = Math.clamp(g.guiWidth() / 4, 76, 116);
        int healthColor = player.hasEffect(MobEffects.POISON) ? 0xFF80A33F
            : player.hasEffect(MobEffects.WITHER) ? 0xFF766B60 : 0xFFAE4236;
        bar(g, x, y, w, player.getHealth(), player.getMaxHealth(), healthColor);
        // Absorption remains visible, proportional to the health scale, with its numeric value.
        if (player.getAbsorptionAmount() > 0) {
            int aw = Math.min(w - 4, Math.round((w - 4) * player.getAbsorptionAmount() / player.getMaxHealth()));
            g.fill(x + w - 2 - aw, y + 2, x + w - 2, y + 4, 0xFFE3C066);
        }
        var mana = ArsBridge.mana(player);
        bar(g, x, y + 10, w, mana == null ? 0 : mana.current(), mana == null ? 0 : mana.max(), 0xFF496FAD);
        if (mana != null && mana.max() > 0 && mana.reserved() > 0) {
            int rw = (int) Math.min(w - 4, (w - 4) * mana.reserved());
            g.fill(x + w - 2 - rw, y + 12, x + w - 2, y + 17, 0xBB161C2D);
        }
        int level = player.getFoodData().getFoodLevel();
        bar(g, x, y + 20, w, level, MAX_FOOD, FOOD_COLOR);
        // The food the held item would restore, flashing past the current level.
        int restored = AppleSkinBridge.heldHunger(player);
        if (restored > 0) {
            int from = fill(w, level, MAX_FOOD), to = fill(w, level + restored, MAX_FOOD);
            g.fill(x + 2 + from, y + 22, x + 2 + to, y + 27, alpha(FOOD_PREVIEW, flash(mc)));
        }
        slot(g, player.getMainHandItem(), x + w + 8, y + 2);
        var selected = AbilitySelection.selected(player);
        slot(g, selected == null ? ItemStack.EMPTY : selected.icon(), x + w + 36, y + 2);
        if (selected != null && !selected.ready().getAsBoolean()) {
            g.fill(x + w + 39, y + 5, x + w + 55, y + 21, 0x8818130E);
        }
        // Shield / off-hand use must still be visible after hiding the vanilla hotbar.
        if (!player.getOffhandItem().isEmpty()) slot(g, player.getOffhandItem(), x + w + 64, y + 2);
    }

    /** Pulses between nothing and AppleSkin's flash alpha. */
    private static float flash(Minecraft mc) {
        float ticks = mc.gui.getGuiTicks() + mc.getTimer().getGameTimeDeltaPartialTick(false);
        double wave = (Math.sin(ticks / 4.0) + 1) / 2;
        return (float) (wave * AppleSkinBridge.flashAlpha());
    }

    private static int alpha(int color, float alpha) {
        return (Math.round(Math.clamp(alpha, 0, 1) * 255) << 24) | (color & 0xFFFFFF);
    }

    private static int fill(int w, double current, double max) {
        return (int) Math.round((w - 4) * Math.clamp(current / max, 0, 1));
    }

    private static void bar(GuiGraphics g, int x, int y, int w, double current, double max, int color) {
        g.fill(x, y, x + w, y + BAR_HEIGHT, 0xCC17130E);
        MorrowindSkin.frame(g, x, y, w, BAR_HEIGHT, false);
        if (max > 0) g.fill(x + 2, y + 2, x + 2 + fill(w, current, max), y + 7, color);
    }

    private static void slot(GuiGraphics g, ItemStack item, int x, int y) {
        g.fill(x, y, x + SLOT, y + SLOT, 0x9917130E);
        MorrowindSkin.frame(g, x, y, SLOT, SLOT, true);
        g.renderItem(item, x + 4, y + 4);
        g.renderItemDecorations(Minecraft.getInstance().font, item, x + 4, y + 4);
    }
}
