package team.civitatum.inventory.client;

import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CrafterScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.client.gui.screens.inventory.SmokerScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;

/**
 * Vanilla block screens whose backgrounds this mod paints dark ({@code assets/minecraft/textures/gui/container}).
 * Their titles are drawn in vanilla's dark grey, unreadable on stone, so that one colour is swapped for ours there.
 *
 * <p>Exact classes only: a mod's subclass may draw its own light background under the same labels.
 */
public final class RestyledScreens {
    /** The grey of {@code AbstractContainerScreen.renderLabels} and of the labels most screens copy from it. */
    private static final int VANILLA_LABEL = 0x404040;
    /** The window titles of the inventory screen, drawn with a shadow as there. */
    public static final int LABEL = 0xFFE9E3D4;
    private static final Set<Class<? extends Screen>> SCREENS = Set.of(ContainerScreen.class,
        ShulkerBoxScreen.class, DispenserScreen.class, HopperScreen.class, FurnaceScreen.class,
        BlastFurnaceScreen.class, SmokerScreen.class, CraftingScreen.class, BrewingStandScreen.class,
        AnvilScreen.class, EnchantmentScreen.class, GrindstoneScreen.class, SmithingScreen.class,
        StonecutterScreen.class, LoomScreen.class, CartographyTableScreen.class, BeaconScreen.class,
        CrafterScreen.class);

    /** Whether text of this colour is a label on a dark background now. Vanilla passes it without alpha. */
    public static boolean darkLabel(int color) {
        if ((color & 0xFFFFFF) != VANILLA_LABEL) return false;
        return restyled(Minecraft.getInstance().screen);
    }

    public static boolean restyled(Screen screen) {
        return screen != null && SCREENS.contains(screen.getClass());
    }

    private RestyledScreens() {}
}
