package team.civitatum.menu;

import java.util.List;
import net.mehvahdjukaar.supplementaries.client.screens.ConfigButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Removes the buttons other mods put on every {@code TitleScreen}, our subclass included:
 * Better ModList's mods button, Supplementaries' gear (by its config it opens a mod list) and Cumulus' "Menu List".
 * Must run after their own {@code ScreenEvent.Init.Post} listeners.
 */
public final class ForeignButtons {
    private static final String SUPPLEMENTARIES = "supplementaries";
    /** Cumulus comes inside The Aether. */
    private static final String CUMULUS = "cumulus_menus";
    /** Cumulus builds "Menu List" as a plain vanilla button, so only its label tells it apart. */
    private static final String CUMULUS_MENU_LIST = "gui.cumulus_menus.button.menu_list";

    private ForeignButtons() {
    }

    public static void remove(ScreenEvent.Init.Post event) {
        boolean supplementaries = ModList.get().isLoaded(SUPPLEMENTARIES);
        boolean cumulus = ModList.get().isLoaded(CUMULUS);
        for (GuiEventListener widget : List.copyOf(event.getListenersList())) {
            if (ModsMenu.isBetterModListButton(widget)
                || supplementaries && Supplementaries.isConfigButton(widget)
                || cumulus && isCumulusMenuList(widget)) {
                event.removeListener(widget);
            }
        }
    }

    private static boolean isCumulusMenuList(GuiEventListener widget) {
        return widget instanceof AbstractWidget button
            && button.getMessage().getContents() instanceof TranslatableContents label
            && label.getKey().equals(CUMULUS_MENU_LIST);
    }

    /** The only place that touches Supplementaries classes, so it is loaded only when that mod is present. */
    private static final class Supplementaries {
        static boolean isConfigButton(GuiEventListener widget) {
            return widget instanceof ConfigButton;
        }
    }
}
