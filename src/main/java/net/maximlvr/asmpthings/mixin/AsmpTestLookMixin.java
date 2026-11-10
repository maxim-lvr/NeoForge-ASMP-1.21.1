package net.maximlvr.asmpthings.mixin;

import net.maximlvr.asmpthings.client.AsmpTestClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class AsmpTestLookMixin {
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void asmpthings$testLook(double yaw, double pitch, CallbackInfo ci) {
        if ((Object) this == Minecraft.getInstance().player && AsmpTestClient.controlling()) {
            AsmpTestClient.turn(yaw, pitch); ci.cancel();
        }
    }
}
