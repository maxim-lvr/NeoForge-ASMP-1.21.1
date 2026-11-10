package net.maximlvr.asmpthings.mixin;

import net.maximlvr.asmpthings.effect.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class StasisLivingMixin {
    @Inject(method = "aiStep", at = @At("HEAD"), cancellable = true)
    private void asmpthings$preventStasisAi(CallbackInfo ci) {
        if (ModEffects.isFrozen((LivingEntity) (Object) this)) ci.cancel();
    }
}
