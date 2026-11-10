package net.maximlvr.asmpthings.mixin;

import net.maximlvr.asmpthings.client.ArmiBossCinematicClient;
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
public abstract class ArmiBossCameraMixin {
    @Shadow private boolean detached;
    @Shadow protected abstract void setPosition(Vec3 position);
    @Shadow protected abstract void setRotation(float yaw, float pitch, float roll);

    @Inject(method = "setup", at = @At("TAIL"))
    private void asmpthings$orbitArmiBoss(BlockGetter level, Entity entity, boolean detached,
                                        boolean reversed, float partialTick, CallbackInfo ci) {
        if (ArmiBossCinematicClient.isActive()) {
            var frame = ArmiBossCinematicClient.cameraFrame(partialTick);
            setPosition(frame.position());
            setRotation(frame.yaw(), frame.pitch(), 0);
            // Detached rendering keeps the local player's body visible in first person too.
            this.detached = true;
        }
    }
}
