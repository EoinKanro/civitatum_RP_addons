package team.civitatum.inventory.client;

import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.SmithingMenu;

/**
 * Workstations beside the player's items: the menu's own slots keep their places relative to each other, and the
 * block's work and supplies are drawn as bars under them.
 *
 * <p>Other mods' menus are listed by class name, and only those whose screens hold nothing but slots, a recipe book
 * and gauges: buttons, text boxes and the screen's own pictures are lost here, so a menu that needs them keeps its
 * own screen. The scales are read from each mod's screen: the method returns pixels of that screen's bar.
 */
final class BlockMenus {
    /** A bar from 0 to 1 and its colour. The first bar of a menu is its work and is drawn thicker. */
    record Gauge(ToDoubleFunction<AbstractContainerMenu> level, int colour) {}

    static final int WORK = 0xFFBDA06B, FUEL = 0xFFBB7744, WATER = 0xFF6F98B8;
    private static final String AETHER = "com.aetherteam.aether.inventory.menu.";

    /** Menus of other mods; an empty list is a block with slots only. */
    private static final Map<String, List<Gauge>> MENUS = Map.ofEntries(
        Map.entry(AETHER + "AltarMenu", furnace("getBurnProgress", 24, "getLitProgress", 13)),
        Map.entry(AETHER + "FreezerMenu", furnace("getBurnProgress", 24, "getLitProgress", 13)),
        Map.entry(AETHER + "IncubatorMenu", work(m -> CompatBridge.progress(m, "getIncubationProgressScaled", 54))),
        Map.entry("vectorwing.farmersdelight.common.block.entity.container.CookingPotMenu", List.of(
            new Gauge(m -> CompatBridge.progress(m, "getCookProgressionScaled", 24), WORK),
            new Gauge(m -> CompatBridge.progress(m, "isHeated", 1), FUEL))),
        // mashing, then fermenting, each with its own bar height
        Map.entry("net.satisfy.vinery.core.menu.ApplePressMenu", List.of(
            new Gauge(m -> CompatBridge.progress(m, "getScaledProgress", 0, 41), WORK),
            new Gauge(m -> CompatBridge.progress(m, "getScaledProgress", 1, 30), WORK))),
        Map.entry("net.satisfy.herbalbrews.client.gui.handler.CauldronGuiHandler",
            work(m -> CompatBridge.ratio(m, "getCookingTime", "getRequiredDuration"))),
        Map.entry("net.satisfy.herbalbrews.client.gui.handler.TeaKettleGuiHandler", List.of(
            new Gauge(m -> CompatBridge.ratio(m, "getCookingTime", "getRequiredDuration"), WORK),
            new Gauge(m -> CompatBridge.progress(m, "getHeatLevel", 100), FUEL),
            new Gauge(m -> CompatBridge.progress(m, "getWaterLevel", 100), WATER))),
        Map.entry("net.satisfy.beachparty.client.gui.handler.MiniFridgeGuiHandler",
            work(m -> CompatBridge.progress(m, "getShakeXProgress", 22))),
        Map.entry("net.satisfy.beachparty.client.gui.handler.PalmBarGuiHandler",
            work(m -> CompatBridge.progress(m, "getShakeXProgress", 22))),
        Map.entry("io.github.razordevs.deep_aether.screen.CombinerMenu",
            work(m -> CompatBridge.progress(m, "getScaledProgress", 14))),
        Map.entry("quek.undergarden.inventory.InfuserMenu",
            work(m -> CompatBridge.progress(m, "getInfusingProgress", 1))),
        Map.entry("com.iafenvoy.iceandfire.screen.menu.DragonForgeMenu",
            work(m -> CompatBridge.ratio(m, "getCookTime", "getMaxCookTime"))),
        Map.entry("doggytalents.common.inventory.container.RiceMillMenu",
            work(m -> CompatBridge.progress(m, "getGrindProgress", 1))),
        Map.entry("doggytalents.common.inventory.container.FoodBowlContainer", List.of()),
        Map.entry("com.iafenvoy.iceandfire.screen.menu.PodiumMenu", List.of()),
        Map.entry("net.luckystudio.cozyhome.block.custom.drawers.DrawerScreenHandler", List.of()),
        Map.entry("net.luckystudio.cozyhome.block.custom.counters.StorageCounterScreenHandler", List.of()),
        Map.entry("net.mcreator.undergardendelight.world.inventory.UndergardenCabinetMenu", List.of()),
        Map.entry("net.astralya.hexalia.menu.NestingBlockMenu", List.of()),
        Map.entry("com.alaharranhonor.swem.menu.LockerContainer", List.of()),
        Map.entry("net.mehvahdjukaar.supplementaries.common.inventories.PulleyContainerMenu", List.of()));

    private static final List<Gauge> FURNACE = furnace("getBurnProgress", 1, "getLitProgress", 1);
    // brewing ticks count down from 400
    private static final List<Gauge> BREWING = List.of(new Gauge(m -> {
        double brewing = CompatBridge.progress(m, "getBrewingTicks", 400);
        return brewing > 0 ? 1 - brewing : 0;
    }, WORK), new Gauge(m -> CompatBridge.progress(m, "getFuel", 20), FUEL));

    /** Whether the menu is a workstation from the list: vanilla's slot-only ones or another mod's. */
    static boolean listed(Class<?> type) {
        return GrindstoneMenu.class.isAssignableFrom(type) || SmithingMenu.class.isAssignableFrom(type)
            || MENUS.containsKey(type.getName());
    }

    /** The bars of a workstation, the work first; none for a crafting table, a grindstone or a cupboard. */
    static List<Gauge> gauges(AbstractContainerMenu menu) {
        if (menu instanceof AbstractFurnaceMenu) return FURNACE;
        if (menu instanceof BrewingStandMenu) return BREWING;
        return MENUS.getOrDefault(menu.getClass().getName(), List.of());
    }

    private static List<Gauge> work(ToDoubleFunction<AbstractContainerMenu> level) {
        return List.of(new Gauge(level, WORK));
    }

    private static List<Gauge> furnace(String burn, double burnScale, String lit, double litScale) {
        return List.of(new Gauge(m -> CompatBridge.progress(m, burn, burnScale), WORK),
            new Gauge(m -> CompatBridge.progress(m, lit, litScale), FUEL));
    }

    private BlockMenus() {}
}
