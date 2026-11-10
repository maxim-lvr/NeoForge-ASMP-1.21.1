package net.maximlvr.asmpthings.entity.custom;

import net.maximlvr.asmpthings.effect.ModEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KraberSphereEntity extends Entity {
    public static final int DELAY = 20;
    public static final int GROWTH = 120;
    public static final float MAX_RADIUS = 12;
    private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(KraberSphereEntity.class, EntityDataSerializers.INT);
    private UUID owner;
    private final Map<UUID, Integer> nextDamage = new HashMap<>();

    public KraberSphereEntity(EntityType<? extends KraberSphereEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    public void setOwner(Player player) { owner = player.getUUID(); }

    public static float radiusAt(float age) {
        return Math.clamp((age - DELAY) * 0.1F, 0, MAX_RADIUS);
    }

    public float radius(float partial) { return radiusAt(entityData.get(AGE) + partial); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(AGE, 0); }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;
        int age = entityData.get(AGE) + 1;
        entityData.set(AGE, age);
        if (age > DELAY + GROWTH) {
            discard();
            return;
        }
        float radius = radiusAt(age);
        if (radius <= 0) return;
        Player shooter = owner == null ? null : level.getPlayerByUUID(owner);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(radius))) {
            if (!target.isAlive() || target.isSpectator()) continue;
            if (target instanceof Player player && (player.isCreative()
                    || (shooter != null && player != shooter && !shooter.canHarmPlayer(player)))) continue;
            var box = target.getBoundingBox();
            double x = Math.clamp(getX(), box.minX, box.maxX);
            double y = Math.clamp(getY(), box.minY, box.maxY);
            double z = Math.clamp(getZ(), box.minZ, box.maxZ);
            if (position().distanceToSqr(x, y, z) > radius * radius) continue;
            target.addEffect(new MobEffectInstance(ModEffects.STASIS, 3, 0, false, false, false));
            target.stopUsingItem();
            if (target.isPassenger()) target.stopRiding();
            if (age >= nextDamage.getOrDefault(target.getUUID(), 0)) {
                target.hurt(shooter == null ? damageSources().magic() : damageSources().indirectMagic(this, shooter), 2F);
                nextDamage.put(target.getUUID(), age + 20);
            }
            target.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            target.fallDistance = 0;
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() { return new AABB(position(), position()).inflate(MAX_RADIUS); }
    @Override
    public boolean shouldBeSaved() { return false; }
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}
