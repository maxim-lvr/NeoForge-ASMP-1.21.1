package net.maximlvr.asmpthings.entity.custom;

import java.util.EnumSet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class UndertakerEntity extends PathfinderMob {
    public static final double DETECTION_RANGE = 30;
    public static final double FIRE_RANGE = 20;
    public static final int PREPARE_TICKS = 20;
    public static final int SHOT_INTERVAL = 60;
    private static final EntityDataAccessor<Boolean> AIMING = SynchedEntityData.defineId(UndertakerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SHOT_TICKS = SynchedEntityData.defineId(UndertakerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> LASER_START = SynchedEntityData.defineId(UndertakerEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3f> LASER_END = SynchedEntityData.defineId(UndertakerEntity.class, EntityDataSerializers.VECTOR3);
    public final AnimationState idleState = new AnimationState();
    public final AnimationState walkState = new AnimationState();
    public final AnimationState aimState = new AnimationState();
    public final AnimationState shootState = new AnimationState();

    public UndertakerEntity(EntityType<? extends UndertakerEntity> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.MOVEMENT_SPEED, 0.12)
                .add(Attributes.FOLLOW_RANGE, DETECTION_RANGE).add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.STEP_HEIGHT, 1);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PatrolAndAimGoal());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(AIMING, false);
        builder.define(SHOT_TICKS, 0);
        builder.define(LASER_START, new Vector3f());
        builder.define(LASER_END, new Vector3f());
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            boolean shooting = entityData.get(SHOT_TICKS) > 12;
            boolean walking = !isAiming() && walkAnimation.speed() > 0.01F;
            idleState.animateWhen(!isAiming() && !walking, tickCount);
            walkState.animateWhen(walking, tickCount);
            aimState.animateWhen(isAiming() && !shooting, tickCount);
            shootState.animateWhen(shooting, tickCount);
        } else if (entityData.get(SHOT_TICKS) > 0) {
            entityData.set(SHOT_TICKS, entityData.get(SHOT_TICKS) - 1);
        }
    }

    public boolean isAiming() { return entityData.get(AIMING); }
    public boolean hasLaser() { return entityData.get(SHOT_TICKS) > 16; }
    public Vec3 laserStartOffset() { return new Vec3(entityData.get(LASER_START)); }
    public Vec3 laserEndOffset() { return new Vec3(entityData.get(LASER_END)); }

    @Override
    public AABB getBoundingBoxForCulling() {
        return hasLaser() ? getBoundingBox().expandTowards(laserStartOffset()).expandTowards(laserEndOffset()).inflate(0.5)
                : getBoundingBox().inflate(0.6);
    }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }

    private boolean canDetect(Player player) {
        return player.isAlive() && !player.isCreative() && !player.isSpectator()
                && distanceToSqr(player) <= DETECTION_RANGE * DETECTION_RANGE
                && getSensing().hasLineOfSight(player);
    }

    private void shoot(Player target) {
        if (!(level() instanceof ServerLevel server)) return;
        Vec3 forward = Vec3.directionFromRotation(0, getYRot());
        Vec3 left = new Vec3(forward.z, 0, -forward.x);
        Vec3 muzzle = position().add(0, 1.65, 0).add(left.scale(0.4)).add(forward.scale(0.4));
        // The muzzle cannot start on the far side of a nearby wall.
        Vec3 start = server.clip(new ClipContext(getEyePosition(), muzzle,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getLocation();
        Vec3 direction = target.getBoundingBox().getCenter().subtract(start).normalize();
        Vec3 end = server.clip(new ClipContext(start, start.add(direction.scale(DETECTION_RANGE)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getLocation();
        var hit = ProjectileUtil.getEntityHitResult(this, start, end,
                new AABB(start, end).inflate(1), e -> e.isAlive() && e.isPickable() && !e.isSpectator()
                        && !(e instanceof Player p && p.isCreative()), start.distanceToSqr(end));
        if (hit != null) {
            end = hit.getLocation();
            hit.getEntity().hurt(damageSources().mobProjectile(this, this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
        }
        entityData.set(LASER_START, start.subtract(position()).toVector3f());
        entityData.set(LASER_END, end.subtract(position()).toVector3f());
        entityData.set(SHOT_TICKS, 20);
        server.playSound(null, blockPosition(), SoundEvents.GUARDIAN_ATTACK, SoundSource.HOSTILE, 0.7F, 1.5F);
    }

    private final class PatrolAndAimGoal extends Goal {
        private Player watched;
        private int nextScan;
        private int nextPath;
        private int prepare;
        private int cooldown;

        private PatrolAndAimGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean canUse() { return true; }
        @Override public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            if (cooldown > 0) cooldown--;
            if (watched != null && !canDetect(watched)) {
                watched = null;
                prepare = 0;
            }
            if (watched == null && --nextScan <= 0) {
                nextScan = 10;
                watched = level().players().stream().filter(UndertakerEntity.this::canDetect)
                        .min(java.util.Comparator.comparingDouble(UndertakerEntity.this::distanceToSqr)).orElse(null);
            }
            entityData.set(AIMING, watched != null);
            if (watched != null) {
                getNavigation().stop();
                getLookControl().setLookAt(watched, 15, 15);
                Vec3 delta = watched.position().subtract(position());
                float yaw = (float) (Math.atan2(-delta.x, delta.z) * 180 / Math.PI);
                setYRot(net.minecraft.util.Mth.rotLerp(0.2F, getYRot(), yaw));
                setYBodyRot(getYRot());
                if (++prepare >= PREPARE_TICKS && cooldown == 0 && distanceToSqr(watched) <= FIRE_RANGE * FIRE_RANGE) {
                    shoot(watched);
                    cooldown = SHOT_INTERVAL;
                }
                return;
            }
            prepare = 0;
            if (--nextPath > 0 || !getNavigation().isDone() && !horizontalCollision) return;
            nextPath = 20;
            Vec3 forward = Vec3.directionFromRotation(0, getYRot());
            // Prefer an actual straight path before sampling random destinations.
            for (int length : new int[]{10, 6, 3}) {
                var path = getNavigation().createPath(net.minecraft.core.BlockPos.containing(position().add(forward.scale(length))), 0);
                if (path != null && path.canReach() && getNavigation().moveTo(path, 1)) return;
            }
            Vec3 destination = DefaultRandomPos.getPosTowards(UndertakerEntity.this, 12, 2,
                    position().add(forward.scale(12)), Math.PI / 8);
            if (destination == null || !getNavigation().moveTo(destination.x, destination.y, destination.z, 1)) {
                destination = DefaultRandomPos.getPos(UndertakerEntity.this, 8, 2);
                if (destination != null) getNavigation().moveTo(destination.x, destination.y, destination.z, 1);
            }
        }

        @Override
        public void stop() {
            watched = null;
            prepare = 0;
            entityData.set(AIMING, false);
            getNavigation().stop();
        }
    }
}
