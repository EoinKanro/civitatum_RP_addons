package team.civitatum.couple;

import java.util.UUID;
import net.minecraft.world.entity.player.Player;
import team.civitatum.couple.network.ClientBridge;
import team.civitatum.couple.server.CoupleServer;

public final class CarryHooks {
    public static UUID carrierId(Player player) {
        return player.level().isClientSide ? ClientBridge.carrierOf.apply(player.getUUID()) : CoupleServer.carrierOf(player.getUUID());
    }
    public static UUID passengerId(Player player) {
        return player.level().isClientSide ? ClientBridge.passengerOf.apply(player.getUUID()) : CoupleServer.passengerOf(player.getUUID());
    }
    public static boolean carried(Player player) { return carrierId(player) != null; }
    public static boolean carrying(Player player) { return passengerId(player) != null; }
    public static boolean occupied(Player player) { return carried(player) || carrying(player); }
    public static boolean pair(Player carrier, Player passenger) { return carrier.getUUID().equals(carrierId(passenger)); }
    public static Player carrier(Player player) {
        UUID id = carrierId(player);
        return id == null ? null : player.level().getPlayerByUUID(id);
    }
    private CarryHooks() {}
}
