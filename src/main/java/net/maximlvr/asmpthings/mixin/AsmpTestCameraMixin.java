package net.maximlvr.asmpthings.mixin;

import net.maximlvr.asmpthings.client.AsmpTestClient;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class AsmpTestCameraMixin {
    @Shadow private boolean detached;
    @Shadow protected abstract void setPosition(Vec3 position);
    @Shadow protected abstract void setRotation(float yaw, float pitch, float roll);
    @Inject(method = "setup", at = @At("TAIL"))
    private void asmpthings$testView(BlockGetter level, Entity entity, boolean detached, boolean reversed,
                                    float partialTick, CallbackInfo ci) {
        var target = AsmpTestClient.target();
        if (target != null) {
            setPosition(target.getPosition(partialTick).add(0, target.getEyeHeight(), 0));
            setRotation(AsmpTestClient.viewYaw(), AsmpTestClient.viewPitch(), 0);
            this.detached = true;
        }
    }
}
