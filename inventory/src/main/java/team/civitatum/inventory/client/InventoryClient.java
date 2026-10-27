package team.civitatum.inventory.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

public final class InventoryClient {
    private static final String CATEGORY = "key.categories.civitatum_inventory";
    /** Slots of the vanilla player menu: 5 crafting, 4 armour, 27 storage, 9 hotbar and the off hand. */
    private static final int VANILLA_INVENTORY_SLOTS = 46;
    public static final KeyMapping TOGGLE = mapping("toggle", GLFW.GLFW_KEY_F8);
    public static final KeyMapping ACTIVATE = mapping("activate_key", GLFW.GLFW_KEY_BACKSLASH);
    public static final KeyMapping NEXT = mapping("next_ability", GLFW.GLFW_KEY_PAGE_DOWN);
    public static final KeyMapping PREVIOUS = mapping("previous_ability", GLFW.GLFW_KEY_PAGE_UP);
    private static boolean bypass;
    private static boolean skipBackpack;

    private static KeyMapping mapping(String name, int key) {
        return new KeyMapping("key.civitatum_inventory." + name, InputConstants.Type.KEYSYM, key, CATEGORY);
    }

    public static void register(IEventBus mod, IEventBus game) {
        mod.addListener((RegisterKeyMappingsEvent event) -> {
            event.register(TOGGLE);
            event.register(ACTIVATE);
            event.register(NEXT);
            event.register(PREVIOUS);
        });
        mod.addListener(CivitatumHud::register);
        game.addListener(CivitatumHud::hideReplaced);
        game.addListener(InventoryClient::opening);
        game.addListener(InventoryClient::tick);
        game.addListener(InventoryClient::key);
    }

    private static void opening(ScreenEvent.Opening event) {
        if (!InventoryPreferences.get().enabled || bypass) return;
        var mc = Minecraft.getInstance();
        var opened = event.getNewScreen();
        if (mc.player == null || mc.gameMode == null || mc.gameMode.hasInfiniteItems()
                || opened == null || opened instanceof CivitatumInventoryScreen) return;
        boolean vanillaInventory = mc.player.inventoryMenu.slots.size() == VANILLA_INVENTORY_SLOTS;
        if (opened.getClass() == InventoryScreen.class && vanillaInventory) {
            var screen = new CivitatumInventoryScreen(mc.player);
            screen.allowAutoBackpack(!skipBackpack);
            event.setNewScreen(screen);
        } else if (CompatBridge.supported(opened)) {
            var nativeScreen = (AbstractContainerScreen<?>) opened;
            event.setNewScreen(new CivitatumInventoryScreen(mc.player, nativeScreen.getMenu(), nativeScreen));
        }
    }

    private static void tick(ClientTickEvent.Post event) {
        while (TOGGLE.consumeClick()) toggle();
        var mc = Minecraft.getInstance();
        boolean inWorld = mc.screen == null && mc.player != null;
        while (NEXT.consumeClick()) if (inWorld) AbilitySelection.cycle(mc.player, 1);
        while (PREVIOUS.consumeClick()) if (inWorld) AbilitySelection.cycle(mc.player, -1);
        while (ACTIVATE.consumeClick()) if (inWorld && !mc.player.isSpectator()) AbilitySelection.activate(mc.player);
    }

    private static void key(ScreenEvent.KeyPressed.Pre event) {
        var screen = event.getScreen();
        boolean inventory = screen instanceof CivitatumInventoryScreen || screen instanceof InventoryScreen
            || CompatBridge.supported(screen);
        if (inventory && TOGGLE.matches(event.getKeyCode(), event.getScanCode())) {
            toggle();
            event.setCanceled(true);
        }
    }

    private static void toggle() {
        var mc = Minecraft.getInstance();
        var preferences = InventoryPreferences.get();
        preferences.enabled = !preferences.enabled;
        preferences.save();
        if (mc.gameMode != null && mc.gameMode.hasInfiniteItems()) return;
        if (mc.screen instanceof CivitatumInventoryScreen screen) {
            screen.openNative();
        } else if (mc.player != null && mc.screen instanceof InventoryScreen) {
            mc.setScreen(new InventoryScreen(mc.player));
        } else if (mc.player != null && CompatBridge.supported(mc.screen)) {
            var screen = (AbstractContainerScreen<?>) mc.screen;
            if (CompatBridge.kind(screen.getMenu()) == CompatBridge.Kind.ACCESSORIES) CompatBridge.openAccessories();
            else mc.setScreen(new CivitatumInventoryScreen(mc.player, screen.getMenu(), screen));
        }
    }

    public static void personalInventory(boolean equipment) {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.player.closeContainer();
        skipBackpack = equipment;
        try {
            mc.setScreen(new InventoryScreen(mc.player));
        } finally {
            skipBackpack = false;
        }
    }

    /** Opens a screen without the replacement in {@link #opening}. */
    public static void nativeScreen(Screen screen) {
        bypass = true;
        try {
            Minecraft.getInstance().setScreen(screen);
        } finally {
            bypass = false;
        }
    }
}
