package team.civitatum.couple.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.civitatum.couple.CarryGeometry;
import team.civitatum.couple.CarryHooks;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setPosition(Vec3 position);

    @Inject(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V", shift = At.Shift.AFTER, ordinal = 0))
    private void couple$eye(BlockGetter level, Entity entity, boolean detached, boolean mirror, float partial, CallbackInfo ci) {
        if (!(entity instanceof Player player)) return;
        Player carrier = CarryHooks.carrier(player);
        if (carrier != null) setPosition(CarryGeometry.head(player, carrier, partial));
    }
}
