package team.civitatum.inventory.client;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;

/**
 * TrashSlot's trash cell in our inventory. TrashSlot keeps doing all the work it does in any container screen: the
 * clicks and keys that trash and bring back items, the server's copy of the trashed stack, its hints. Only the cell is
 * ours: a slot of the items window in our style, drawn with the window's depth, where TrashSlot would draw its own
 * frame over the middle of our full-screen menu (see {@code TrashSlotMixin}). Reflection keeps TrashSlot optional.
 */
public final class TrashSlotBridge {
    private static final String MOD = "trashslot";
    private static final String API = "net.blay09.mods.trashslot.api.TrashSlotAPI";
    private static final String HANDLER = "net.blay09.mods.trashslot.client.TrashSlotGuiHandler";
    private static final String CONFIG = "net.blay09.mods.trashslot.config.TrashSlotConfig";
    private static boolean registered, broken;
    private static Method slot, component, visible, config;

    private TrashSlotBridge() {}

    /** Whether our cell stands in for TrashSlot's in our screen; if TrashSlot changed, its own cell is left be. */
    public static boolean active() {
        return registered && !broken;
    }

    public static boolean available() {
        return !broken && CompatBridge.loaded(MOD);
    }

    /**
     * Our screen as one TrashSlot shows its cell on from the start, as on vanilla's inventory. Done when the screen
     * first opens: TrashSlot sets its API up while the game loads, after our mod.
     */
    static void register() {
        if (registered || !available()) return;
        registered = true;
        try {
            Object layout = Class.forName(API).getMethod("registerSimpleLayout", Class.class)
                .invoke(null, CivitatumInventoryScreen.class);
            layout.getClass().getMethod("setEnabledByDefault").invoke(layout);
            var handler = Class.forName(HANDLER);
            slot = handler.getMethod("getTrashSlot");
            component = handler.getMethod("getTrashSlotComponent");
            visible = component.getReturnType().getMethod("isVisible");
            config = Class.forName(CONFIG).getMethod("getActive");
        } catch (ReflectiveOperationException | RuntimeException ex) {
            fail(ex);
        }
    }

    /** TrashSlot's slot in the screen open now, when TrashSlot is on the server too; null otherwise. */
    static Slot slot() {
        if (!registered || broken) return null;
        try {
            return component.invoke(null) == null ? null : (Slot) slot.invoke(null);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            fail(ex);
            return null;
        }
    }

    /** Whether the cell is shown: the player may hide it with TrashSlot's key. */
    static boolean shown() {
        if (!registered || broken) return false;
        try {
            Object cell = component.invoke(null);
            return cell != null && (boolean) visible.invoke(cell);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            fail(ex);
            return false;
        }
    }

    /** What the cell says when an item is carried over it, as TrashSlot's own cell does. */
    static Component carriedTooltip() {
        boolean instant = false;
        try {
            Object active = config.invoke(null);
            instant = active.getClass().getField("instantDeletion").getBoolean(active);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            // the wording only: an item trashed at once is the rarer setting
        }
        return Component.translatable(instant ? "tooltip.trashslot.destroy_item" : "tooltip.trashslot.trash_item");
    }

    private static void fail(Exception ex) {
        broken = true;
        TrashCell.hide();
        LogUtils.getLogger().warn("Civitatum HUD: TrashSlot is not supported in this version, its cell is off", ex);
    }
}
