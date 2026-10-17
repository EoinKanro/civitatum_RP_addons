package ru.teacraft.inventory.smoke;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

/** Verify physical dedicated-server startup also tolerates the client-only artifact. */
@Mod(value="inventory_smoke",dist=Dist.DEDICATED_SERVER)
public final class InventoryServerSmoke {
    public InventoryServerSmoke() {}
}
