package ru.teacraft.inventory.mixin;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(Minecraft.class)
public interface MinecraftTargetAccessor {
    @Mutable @Accessor("mainRenderTarget") void civitatumInventory$setTarget(RenderTarget target);
}
