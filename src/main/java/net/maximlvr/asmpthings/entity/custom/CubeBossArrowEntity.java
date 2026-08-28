package net.maximlvr.asmpthings.entity.custom;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CubeBossArrowEntity extends Entity {
    private static final int MODE_DIRECTION_PREVIEW = 0;
    private static final int MODE_FALLING_RAIN = 1;
    private static final int PREVIEW_TRAVEL_TICKS = 24;
    private static final int PREVIEW_RANDOM_TICKS = 44;
    private static final int PREVIEW_TOTAL_TICKS = 92;
    private static final int DIRECTION_SHUFFLE_INTERVAL_TICKS = 3;
    private static final int FALLING_MAX_LIFETIME_TICKS = 100;
    private static final double FALLING_INITIAL_SPEED = -0.025D;
    private static final double FALLING_ACCELERATION = -0.045D;
    private static final double FALLING_MAX_SPEED = -1.25D;
    private static final float FALLING_DAMAGE = 6.0F;
    private static final double FALLING_HITBOX_HALF_WIDTH = 0.22D;
    private static final double FALLING_HITBOX_HEIGHT = 0.55D;
    private static final double FALLING_HOMING_STEP = 0.045D;

    private static final EntityDataAccessor<Integer> DATA_MODE =
            SynchedEntityData.defineId(CubeBossArrowEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DIRECTION =
            SynchedEntityData.defineId(CubeBossArrowEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FINAL_DIRECTION =
            SynchedEntityData.defineId(CubeBossArrowEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_HOMING =
            SynchedEntityData.defineId(CubeBossArrowEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_TARGET_X =
            SynchedEntityData.defineId(CubeBossArrowEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TARGET_Y =
            SynchedEntityData.defineId(CubeBossArrowEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_TARGET_Z =
            SynchedEntityData.defineId(CubeBossArrowEntity.class, EntityDataSerializers.FLOAT);

    private boolean originInitialized = false;
    private double originX;
    private double originY;
    private double originZ;
    private double fallingSpeed = FALLING_INITIAL_SPEED;
    private int floorY = Integer.MIN_VALUE;
    private boolean previewFrozen = false;
    private final Set<UUID> damagedPlayers = new HashSet<>();

    public CubeBossArrowEntity(EntityType<? extends CubeBossArrowEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public void setupDirectionPreview(Direction finalDirection, double targetX, double targetY, double targetZ) {
        this.entityData.set(DATA_MODE, MODE_DIRECTION_PREVIEW);
        this.entityData.set(DATA_DIRECTION, Direction.UP.ordinal());
        this.entityData.set(DATA_FINAL_DIRECTION, finalDirection.ordinal());
        setTarget(targetX, targetY, targetZ);
        this.entityData.set(DATA_HOMING, false);
        this.originInitialized = false;
        this.previewFrozen = false;
    }

    public void setupFallingRain(int floorY) {
        setupFallingRain(floorY, this.getX(), this.getZ(), false);
    }

    public void setupFallingRain(int floorY, double targetX, double targetZ, boolean homing) {
        this.entityData.set(DATA_MODE, MODE_FALLING_RAIN);
        this.entityData.set(DATA_DIRECTION, Direction.DOWN.ordinal());
        this.entityData.set(DATA_FINAL_DIRECTION, Direction.DOWN.ordinal());
        setTarget(targetX, this.getY(), targetZ);
        this.entityData.set(DATA_HOMING, homing);
        this.floorY = floorY;
        this.fallingSpeed = FALLING_INITIAL_SPEED;
        this.originInitialized = false;
    }

    public Direction getArrowDirection() {
        Direction[] directions = Direction.values();
        int index = this.entityData.get(DATA_DIRECTION);
        return directions[Math.floorMod(index, directions.length)];
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.originInitialized) {
            this.originInitialized = true;
            this.originX = this.getX();
            this.originY = this.getY();
            this.originZ = this.getZ();
        }

        if (this.entityData.get(DATA_MODE) == MODE_FALLING_RAIN) {
            tickFallingRain();
        } else {
            tickDirectionPreview();
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_MODE, MODE_DIRECTION_PREVIEW);
        builder.define(DATA_DIRECTION, Direction.UP.ordinal());
        builder.define(DATA_FINAL_DIRECTION, Direction.UP.ordinal());
        builder.define(DATA_HOMING, false);
        builder.define(DATA_TARGET_X, 0.0F);
        builder.define(DATA_TARGET_Y, 0.0F);
        builder.define(DATA_TARGET_Z, 0.0F);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    private void tickDirectionPreview() {
        if (this.tickCount <= PREVIEW_TRAVEL_TICKS) {
            double progress = Math.min(1.0D, this.tickCount / (double) PREVIEW_TRAVEL_TICKS);
            double easedProgress = 1.0D - Math.pow(1.0D - progress, 3.0D);
            this.setPos(
                    lerp(this.originX, getTargetX(), easedProgress),
                    lerp(this.originY, getTargetY(), easedProgress),
                    lerp(this.originZ, getTargetZ(), easedProgress)
            );
        } else {
            this.setPos(getTargetX(), getTargetY(), getTargetZ());
        }

        if (!this.level().isClientSide) {
            int randomEndTick = PREVIEW_TRAVEL_TICKS + PREVIEW_RANDOM_TICKS;

            if (this.tickCount < randomEndTick && this.tickCount % DIRECTION_SHUFFLE_INTERVAL_TICKS == 0) {
                this.entityData.set(DATA_DIRECTION, getRandomPreviewDirection().ordinal());
            } else if (!this.previewFrozen && this.tickCount >= randomEndTick) {
                // W.I.P: cette direction figee servira plus tard a declencher une vraie attaque directionnelle.
                this.entityData.set(DATA_DIRECTION, this.entityData.get(DATA_FINAL_DIRECTION));
                this.previewFrozen = true;
            }

            if (this.tickCount >= PREVIEW_TOTAL_TICKS) {
                this.discard();
            }
        }
    }

    private void tickFallingRain() {
        this.fallingSpeed = Math.max(FALLING_MAX_SPEED, this.fallingSpeed + FALLING_ACCELERATION);
        double nextX = this.getX();
        double nextZ = this.getZ();

        if (this.entityData.get(DATA_HOMING)) {
            nextX = approach(nextX, getTargetX(), FALLING_HOMING_STEP);
            nextZ = approach(nextZ, getTargetZ(), FALLING_HOMING_STEP);
        }

        this.setPos(
                nextX,
                this.getY() + this.fallingSpeed,
                nextZ
        );

        if (!this.level().isClientSide) {
            hurtPlayersInRainArrow();

            if (hasReachedFloor() || this.tickCount >= FALLING_MAX_LIFETIME_TICKS) {
                this.discard();
            }
        }
    }

    private boolean hasReachedFloor() {
        return this.floorY != Integer.MIN_VALUE && this.getY() <= this.floorY + 0.35D;
    }

    private void hurtPlayersInRainArrow() {
        AABB hitBox = new AABB(
                this.getX() - FALLING_HITBOX_HALF_WIDTH,
                this.getY() - FALLING_HITBOX_HEIGHT * 0.5D,
                this.getZ() - FALLING_HITBOX_HALF_WIDTH,
                this.getX() + FALLING_HITBOX_HALF_WIDTH,
                this.getY() + FALLING_HITBOX_HEIGHT * 0.5D,
                this.getZ() + FALLING_HITBOX_HALF_WIDTH
        );

        for (Player player : this.level().getEntitiesOfClass(Player.class, hitBox)) {
            if (this.damagedPlayers.contains(player.getUUID())) {
                continue;
            }

            if (player.hurt(this.damageSources().source(DamageTypes.MAGIC, this), FALLING_DAMAGE)) {
                this.damagedPlayers.add(player.getUUID());
            }
        }
    }

    private Direction getRandomPreviewDirection() {
        return switch (this.random.nextInt(4)) {
            case 0 -> Direction.UP;
            case 1 -> Direction.DOWN;
            case 2 -> Direction.WEST;
            default -> Direction.EAST;
        };
    }

    private double lerp(double start, double end, double progress) {
        return start + (end - start) * progress;
    }

    private double approach(double value, double target, double step) {
        double distance = target - value;

        if (Math.abs(distance) <= step) {
            return target;
        }

        return value + Math.signum(distance) * step;
    }

    private void setTarget(double x, double y, double z) {
        this.entityData.set(DATA_TARGET_X, (float) x);
        this.entityData.set(DATA_TARGET_Y, (float) y);
        this.entityData.set(DATA_TARGET_Z, (float) z);
    }

    private double getTargetX() {
        return this.entityData.get(DATA_TARGET_X);
    }

    private double getTargetY() {
        return this.entityData.get(DATA_TARGET_Y);
    }

    private double getTargetZ() {
        return this.entityData.get(DATA_TARGET_Z);
    }
}
