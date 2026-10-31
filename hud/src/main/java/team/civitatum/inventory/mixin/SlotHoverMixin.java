package team.civitatum.inventory.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.civitatum.inventory.client.MorrowindSkin;
import team.civitatum.inventory.client.RestyledScreens;

/**
 * The slot under the cursor in the restyled vanilla screens: the see-through gold of the inventory instead of
 * vanilla's white. NeoForge asks {@code getSlotColor} for every highlight, so other screens keep their own colour.
 * Screens that draw slots of their own (the grid of Tom's Storage terminals) highlight them with the default colour,
 * so that one is gold too, while a restyled screen is open.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class SlotHoverMixin {
    /** Vanilla's highlight, see-through white. */
    private static final int WHITE_HOVER = 0x80FFFFFF;

    @Inject(method = "getSlotColor", at = @At("HEAD"), cancellable = true)
    private void civitatumHud$goldHover(int index, CallbackInfoReturnable<Integer> result) {
        var screen = (AbstractContainerScreen<?>) (Object) this;
        if (RestyledScreens.restyled(screen)) result.setReturnValue(MorrowindSkin.SLOT_HOVER);
    }

    @ModifyConstant(method = "renderSlotHighlight(Lnet/minecraft/client/gui/GuiGraphics;III)V",
        constant = @Constant(intValue = WHITE_HOVER))
    private static int civitatumHud$goldDefaultHover(int colour) {
        return RestyledScreens.restyled(Minecraft.getInstance().screen) ? MorrowindSkin.SLOT_HOVER : colour;
    }
}
