package net.maximlvr.asmpthings.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;

public class DroneEntity extends FlyingMob {
    public static final double RANGE = 24;
    public static final int SHOT_INTERVAL = 60;
    private static final EntityDataAccessor<Vector3f> LASER_END = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> LASER_TICKS = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.INT);
    public final AnimationState idleAnimation = new AnimationState();
    public final AnimationState attackAnimation = new AnimationState();
    private int attackAnimationTicks;

    public DroneEntity(EntityType<? extends DroneEntity> type, Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 16).add(Attributes.FOLLOW_RANGE, RANGE)
                .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FLYING_SPEED, 0.35)
                .add(Attributes.ATTACK_DAMAGE, 4);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanPassDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new PatrolGoal());
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LASER_END, new Vector3f());
        builder.define(LASER_TICKS, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (attackAnimationTicks > 0) {
                idleAnimation.stop();
                if (--attackAnimationTicks == 0) attackAnimation.stop();
            } else {
                idleAnimation.startIfStopped(tickCount);
            }
        } else if (entityData.get(LASER_TICKS) > 0) {
            entityData.set(LASER_TICKS, entityData.get(LASER_TICKS) - 1);
        }
    }

    @Override
    public void handleEntityEvent(byte event) {
        if (event == 4) {
            attackAnimation.start(tickCount);
            attackAnimationTicks = SHOT_INTERVAL;
        } else {
            super.handleEntityEvent(event);
        }
    }

    public boolean hasLaser() {
        return entityData.get(LASER_TICKS) > 0;
    }

    public Vec3 laserEndOffset() {
        return new Vec3(entityData.get(LASER_END));
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return hasLaser() ? getBoundingBox().expandTowards(laserEndOffset()).inflate(0.2) : super.getBoundingBoxForCulling();
    }

    private void shoot(Player target) {
        if (!(level() instanceof ServerLevel server)) return;
        Vec3 start = position().add(0, 0.3, 0);
        Vec3 direction = target.getBoundingBox().getCenter().subtract(start).normalize();
        Vec3 end = server.clip(new ClipContext(start, start.add(direction.scale(RANGE)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getLocation();
        var hit = ProjectileUtil.getEntityHitResult(this, start, end,
                getBoundingBox().expandTowards(end.subtract(start)).inflate(1),
                entity -> entity.isAlive() && entity.isPickable() && !entity.isSpectator(), start.distanceToSqr(end));
        if (hit != null) {
            end = hit.getLocation();
            hit.getEntity().hurt(damageSources().mobProjectile(this, this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
        }
        entityData.set(LASER_END, end.subtract(position()).toVector3f());
        entityData.set(LASER_TICKS, 6);
        server.broadcastEntityEvent(this, (byte) 4);
        server.playSound(null, blockPosition(), SoundEvents.GUARDIAN_ATTACK, SoundSource.HOSTILE, 0.7F, 1.8F);
    }

    private Vec3 flightDestination(double x, double z) {
        // A local downward ray also works indoors; a surface heightmap would select the roof.
        int top = Math.min(level().getMaxBuildHeight() - 2, getBlockY() + 3);
        int bottom = Math.max(level().getMinBuildHeight(), getBlockY() - 64);
        if (!level().hasChunksAt(BlockPos.containing(x, bottom, z), BlockPos.containing(x, top, z))) return null;
        var ground = level().clip(new ClipContext(new Vec3(x, top, z), new Vec3(x, bottom, z),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
        if (ground.getType() == HitResult.Type.MISS) return null;
        double preferred = ground.getLocation().y + 5 + random.nextDouble();
        // Allow a little extra altitude, or fly lower under a ceiling if necessary.
        for (double offset : new double[]{0, 1, 2, -1, -2, -3, -4}) {
            Vec3 candidate = new Vec3(x, preferred + offset, z);
            if (candidate.y + getBbHeight() >= level().getMaxBuildHeight()) continue;
            if (level().noCollision(this, getBoundingBox().move(candidate.subtract(position())))) return candidate;
        }
        return null;
    }

    private final class PatrolGoal extends Goal {
        private int nextPath;
        private int shotCooldown = SHOT_INTERVAL;

        private PatrolGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() { return true; }

        @Override
        public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            Player target = getTarget() instanceof Player player && player.isAlive()
                    && !player.isCreative() && !player.isSpectator() && distanceToSqr(player) <= RANGE * RANGE ? player : null;
            if (target != null) {
                getLookControl().setLookAt(target, 30, 30);
                if (--shotCooldown <= 0 && getSensing().hasLineOfSight(target)) {
                    shoot(target);
                    shotCooldown = SHOT_INTERVAL;
                }
            } else {
                shotCooldown = SHOT_INTERVAL;
            }
            if (--nextPath > 0) return;
            nextPath = 20;
            if (target == null && !getNavigation().isDone() && !horizontalCollision) return;
            for (int attempt = 0; attempt < 5; attempt++) {
                double x;
                double z;
                if (target != null) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    x = target.getX() + Math.cos(angle) * 7;
                    z = target.getZ() + Math.sin(angle) * 7;
                } else {
                    x = getX() + random.nextInt(17) - 8;
                    z = getZ() + random.nextInt(17) - 8;
                }
                Vec3 destination = flightDestination(x, z);
                if (destination != null && getNavigation().moveTo(destination.x, destination.y, destination.z, 1)) break;
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
            shotCooldown = SHOT_INTERVAL;
        }
    }
}
