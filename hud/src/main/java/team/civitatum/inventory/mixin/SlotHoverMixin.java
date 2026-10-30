package team.civitatum.inventory.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.civitatum.inventory.client.MorrowindSkin;
import team.civitatum.inventory.client.RestyledScreens;

/**
 * The slot under the cursor in the restyled vanilla screens: the see-through gold of the inventory instead of
 * vanilla's white. NeoForge asks {@code getSlotColor} for every highlight, so other screens keep their own colour.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class SlotHoverMixin {
    @Inject(method = "getSlotColor", at = @At("HEAD"), cancellable = true)
    private void civitatumHud$goldHover(int index, CallbackInfoReturnable<Integer> result) {
        var screen = (AbstractContainerScreen<?>) (Object) this;
        if (RestyledScreens.restyled(screen)) result.setReturnValue(MorrowindSkin.SLOT_HOVER);
    }
}
