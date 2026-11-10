package net.maximlvr.asmpthings.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.maximlvr.asmpthings.client.ArmiBossCinematicClient;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class ArmiBossViewMixin {
    @Inject(method = {"bobView", "bobHurt"}, at = @At("HEAD"), cancellable = true)
    private void asmpthings$keepArmiCameraStill(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        if (ArmiBossCinematicClient.isActive()) {
            ci.cancel();
        }
    }
}
