package ru.teacraft.civitatum.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.teacraft.civitatum.client.ClientCharacters;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @Inject(method="scale(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;F)V",at=@At("TAIL"))
    private void civitatum$pose(AbstractClientPlayer p,PoseStack pose,float partial,CallbackInfo ci) {
        var c=ClientCharacters.get(p);
        if(!c.selected) return;
        if(ClientCharacters.preview!=null && p.getUUID().equals(ClientCharacters.previewPlayer)) {
            var saved=ClientCharacters.state(p).character();
            float previous=saved.selected ? saved.height : 1.8f;
            float ratio=c.height/previous;
            pose.scale(ratio,ratio,ratio);
        }
        if(p.isCrouching() && c.height>=2.2) {
            float vanillaCrouch=1.5f*c.height/1.8f;
            if(vanillaCrouch>1.8f) pose.scale(1,1.8f/vanillaCrouch,1);
        }
    }
}
