package team.civitatum.chacratercreation.mixin;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.teacraft.civitatum.client.*;
import team.civitatum.chacratercreation.client.ClientCharacters;
import team.civitatum.chacratercreation.client.SkinTemplates;

@Mixin(AbstractClientPlayer.class)
public abstract class ClientPlayerSkinMixin {
    @Inject(method="getSkin",at=@At("RETURN"),cancellable=true)
    private void civitatum$template(CallbackInfoReturnable<PlayerSkin> ci) {
        var p=(AbstractClientPlayer)(Object)this;
        ci.setReturnValue(SkinTemplates.skin(ClientCharacters.get(p),ci.getReturnValue()));
    }
}
