package net.maximlvr.asmpthings.mixin;

import net.maximlvr.asmpthings.effect.ModEffects;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class StasisEntityMixin {
    @Inject(method = {"move", "turn"}, at = @At("HEAD"), cancellable = true)
    private void asmpthings$preventStasisMovement(CallbackInfo ci) {
        if (ModEffects.isFrozen((Entity) (Object) this)) ci.cancel();
    }
}
