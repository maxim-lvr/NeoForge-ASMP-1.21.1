package net.maximlvr.asmpthings.effect;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, AsmpThingsMod.MOD_ID);
    public static final DeferredHolder<MobEffect, MobEffect> STASIS = EFFECTS.register("stasis", StasisEffect::new);

    private ModEffects() {}

    public static boolean isFrozen(Entity entity) {
        return entity instanceof LivingEntity living && living.isAlive() && living.hasEffect(STASIS);
    }

    private static class StasisEffect extends MobEffect {
        StasisEffect() { super(MobEffectCategory.HARMFUL, 0x36CFF0); }
        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return true; }
        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            entity.setDeltaMovement(Vec3.ZERO);
            entity.fallDistance = 0;
            entity.stopUsingItem();
            return true;
        }
    }
}
