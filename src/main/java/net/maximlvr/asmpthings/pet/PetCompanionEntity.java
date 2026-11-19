package net.maximlvr.asmpthings.pet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.Optional;
import java.util.UUID;

public final class PetCompanionEntity extends Entity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(PetCompanionEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> KIND = SynchedEntityData.defineId(PetCompanionEntity.class, EntityDataSerializers.STRING);
    private Vec3 lerpTarget = Vec3.ZERO;
    private float lerpYaw;
    private int lerpSteps;
    public PetCompanionEntity(EntityType<? extends PetCompanionEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        setInvulnerable(true);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER, Optional.empty());
        builder.define(KIND, PetKind.PET_1.id);
    }
    public void configure(ServerPlayer owner, PetKind kind) {
        entityData.set(OWNER, Optional.of(owner.getUUID()));
        entityData.set(KIND, kind.id);
        setPos(target(owner));
    }
    public PetKind kind() {
        PetKind kind = PetKind.fromId(entityData.get(KIND));
        return kind == null ? PetKind.PET_1 : kind;
    }
    private Vec3 target(ServerPlayer owner) {
        Vec3 forward = Vec3.directionFromRotation(0, owner.yBodyRot);
        Vec3 side = new Vec3(forward.z, 0, -forward.x);
        return owner.position().subtract(forward.scale(0.85)).add(side.scale(0.85))
                .add(0, owner.getEyeHeight() + 0.15 + Math.sin((tickCount + owner.getId()) * 0.12) * 0.06, 0);
    }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (lerpSteps > 0) {
                setPos(position().lerp(lerpTarget, 1.0 / lerpSteps));
                setYRot(getYRot() + Mth.wrapDegrees(lerpYaw - getYRot()) / lerpSteps);
                lerpSteps--;
            }
            return;
        }
        UUID ownerId = entityData.get(OWNER).orElse(null);
        ServerPlayer owner = ownerId == null ? null : ((ServerLevel) level()).getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null || !owner.isAlive() || owner.isSpectator() || owner.level() != level()
                || PetKind.fromStack(owner.getData(ModPets.EQUIPMENT).getStackInSlot(0)) != kind()) {
            discard();
            return;
        }
        Vec3 target = target(owner);
        if (position().distanceToSqr(target) > 64) setPos(target);
        else setPos(position().lerp(target, 0.28));
        setYRot(Mth.rotLerp(0.25F, getYRot(), owner.yBodyRot));
        setDeltaMovement(Vec3.ZERO);
    }
    @Override public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
        lerpTarget = new Vec3(x, y, z);
        lerpYaw = yaw;
        lerpSteps = Math.max(1, steps);
    }
    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) entityData.set(OWNER, Optional.of(tag.getUUID("Owner")));
        entityData.set(KIND, tag.getString("PetKind"));
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        entityData.get(OWNER).ifPresent(uuid -> tag.putUUID("Owner", uuid));
        tag.putString("PetKind", entityData.get(KIND));
    }
}
