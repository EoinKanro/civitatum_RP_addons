package team.civitatum.couple.test;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod("couple_test")
public final class TestBootstrap {
    public TestBootstrap() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> {
            // Recorded peers have no live network listener to drive ServerPlayer.doTick.
            // Run the vanilla player update so equipment and movement reach real observers.
            for(var player:event.getServer().getPlayerList().getPlayers())
                if(player.connection instanceof ServerChecks.Recorder)player.doTick();
        });
    }
}
