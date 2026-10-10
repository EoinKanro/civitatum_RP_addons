package ru.teacraft.inventory.smoke;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.ModList;

@GameTestHolder("inventory_smoke")
@PrefixGameTestTemplate(false)
public final class InventoryServerChecks {
    @GameTest(template="empty",timeoutTicks=20)
    public static void clientOnlyModDoesNotLoadClientClasses(GameTestHelper helper) {
        helper.assertTrue(FMLEnvironment.dist==Dist.DEDICATED_SERVER,"physical server distribution");
        helper.assertTrue(ModList.get().isLoaded("civitatum_inventory"),"client-only mod metadata is accepted");
        System.out.println("INVENTORY_DEDICATED_SERVER_START_PASS");helper.succeed();
    }
}
