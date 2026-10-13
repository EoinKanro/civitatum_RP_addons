package team.civitatum.couple.network;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/** No client classes in common handlers: dedicated servers can load the entire protocol. */
public final class ClientBridge {
    public static Consumer<CoupleNetwork.Invitation> invitation = p -> {};
    public static Consumer<CoupleNetwork.ClearInvitation> clearInvitation = p -> {};
    public static Consumer<CoupleNetwork.CarryState> carryState = p -> {};
    public static Consumer<CoupleNetwork.KissEffect> kissEffect = p -> {};
    public static Function<UUID, UUID> carrierOf = id -> null;
    public static Function<UUID, UUID> passengerOf = id -> null;
    private ClientBridge() {}
}
