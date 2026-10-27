package team.civitatum.inventory.xaero;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;
import xaero.common.minimap.render.MinimapRenderer;
import xaero.hud.minimap.common.config.MinimapConfigConstants;

/**
 * Switches Xaero's own minimap frame off whatever its "Frame Style" setting is: the frame of the inventory windows is
 * drawn instead ({@code MinimapRendererMixin}). Xaero draws its frame only for a style below the last one, "Off"; the
 * shader colour a coloured style sets is reset right after the frame code. The FBO and safe-mode renderers both
 * inherit this method.
 */
@Mixin(MinimapRenderer.class)
public abstract class MinimapFrameMixin {
    @ModifyExpressionValue(
        method = "renderMinimap",
        at = @At(value = "INVOKE", target = "Lxaero/lib/client/config/ClientConfigManager;getEffective("
            + "Lxaero/lib/common/config/option/ConfigOption;)Ljava/lang/Object;"),
        slice = @Slice(
            from = @At(value = "FIELD", target = "Lxaero/hud/minimap/common/config/option/MinimapProfiledConfigOptions;"
                + "FRAME:Lxaero/lib/common/config/option/RangeConfigOption;"),
            to = @At(value = "FIELD", target = "Lxaero/hud/minimap/common/config/MinimapConfigConstants;"
                + "FRAME_NAMES:[Lnet/minecraft/network/chat/Component;"))
    )
    private Object civitatumHud$frameOff(Object frameStyle) {
        return MinimapConfigConstants.FRAME_NAMES.length - 1;
    }
}
