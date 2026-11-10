package net.maximlvr.asmpthings.mixin;

import net.maximlvr.asmpthings.client.ArmiBossCinematicClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class ArmiBossLookMixin {
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void asmpthings$lockArmiBossLook(double yaw, double pitch, CallbackInfo ci) {
        if ((Object) this == Minecraft.getInstance().player && ArmiBossCinematicClient.locksControls()) {
            ci.cancel();
        }
    }
}
