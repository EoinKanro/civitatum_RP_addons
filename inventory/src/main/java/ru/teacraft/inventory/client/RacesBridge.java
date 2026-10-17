package ru.teacraft.inventory.client;

import com.mojang.logging.LogUtils;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;

/** Optional adapter. Civitatum Races is not linked or bundled into this mod. */
public final class RacesBridge {
    public record Details(String name, String race, String sign, String positive, String negative, String ability,
            int food, int cooldown, long remainingTicks, int curseDeaths) {}
    private static boolean failed;
    public static Details read(Player player) {
        if (failed || !ModList.get().isLoaded("civitatum")) return null;
        try {
            var client = Class.forName("ru.teacraft.civitatum.client.ClientCharacters");
            var state = client.getMethod("state", Player.class).invoke(null,player);
            if(state==null)return null;
            var character = state.getClass().getMethod("character").invoke(state);
            if(character==null)return null;
            if (!(boolean) character.getClass().getField("selected").get(character)) return null;
            String id = field(character,"race");
            var definition = Class.forName("ru.teacraft.civitatum.RaceCatalog").getMethod("get",String.class).invoke(null,id);
            long until = ((Number)state.getClass().getMethod("cooldownUntil").invoke(state)).longValue();
            String sign = optionalField(character,"constellation");
            if (sign.isBlank()) sign = optionalField(character,"birthsign");
            return new Details(field(character,"name"),field(definition,"name"),sign,field(definition,"positive"),
                field(definition,"negative"),field(definition,"active"),integer(definition,"food"),integer(definition,"cooldown"),
                Math.max(0,until-player.level().getGameTime()),((Number)state.getClass().getMethod("curseDeaths").invoke(state)).intValue());
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException | IllegalAccessException ex) {
            failed=true; LogUtils.getLogger().warn("Civitatum Inventory: optional race adapter unavailable",ex); return null;
        } catch (java.lang.reflect.InvocationTargetException | RuntimeException ex) {
            // Character data may not yet have arrived when opening the first client screen.
            return null;
        }
    }
    public static void activate() {
        if(failed || !ModList.get().isLoaded("civitatum")) return;
        try {
            var payload=Class.forName("ru.teacraft.civitatum.RaceNetwork$Serverbound")
                .getConstructor(int.class,long.class,String.class).newInstance(1,0L,"");
            PacketDistributor.sendToServer((CustomPacketPayload)payload);
        } catch (ReflectiveOperationException | RuntimeException ex) { LogUtils.getLogger().warn("Civitatum Inventory: could not request ability",ex); }
    }
    private static String field(Object object,String key) throws NoSuchFieldException,IllegalAccessException {
        var value=object.getClass().getField(key).get(object); return value==null?"":value.toString();
    }
    private static String optionalField(Object object,String key) {
        try { return field(object,key); } catch(ReflectiveOperationException ex) { return ""; }
    }
    private static int integer(Object object,String key) throws NoSuchFieldException,IllegalAccessException {
        return ((Number)object.getClass().getField(key).get(object)).intValue();
    }
}
