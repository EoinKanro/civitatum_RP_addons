package ru.teacraft.civitatum.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Holder;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.teacraft.civitatum.*;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @ModifyVariable(method="giveExperiencePoints",at=@At("HEAD"),argsOnly=true)
    private int civitatum$xp(int value) { return RaceAbilities.experience((Player)(Object)this,value); }
    @ModifyVariable(method="causeFoodExhaustion",at=@At("HEAD"),argsOnly=true)
    private float civitatum$exhaustion(float value) {
        var r=RaceService.race((Player)(Object)this);
        return r!=null && r.dragon() ? value*1.1f : value;
    }
}
