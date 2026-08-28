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

public class CubeBossBeamEntity extends Entity {
    public static final int WARNING_TICKS = 20;
    public static final int ACTIVE_TICKS = 16;
    public static final int TOTAL_TICKS = WARNING_TICKS + ACTIVE_TICKS;
    public static final float DAMAGE = 8.0F;

    private static final double HITBOX_HALF_WIDTH = 0.28D;

    private static final EntityDataAccessor<Integer> DATA_AGE =
            SynchedEntityData.defineId(CubeBossBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DIRECTION =
            SynchedEntityData.defineId(CubeBossBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LENGTH =
            SynchedEntityData.defineId(CubeBossBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_WARNING_TICKS =
            SynchedEntityData.defineId(CubeBossBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ACTIVE_TICKS =
            SynchedEntityData.defineId(CubeBossBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_MOTION_X =
            SynchedEntityData.defineId(CubeBossBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_MOTION_Y =
            SynchedEntityData.defineId(CubeBossBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_MOTION_Z =
            SynchedEntityData.defineId(CubeBossBeamEntity.class, EntityDataSerializers.FLOAT);

    private final Set<UUID> damagedPlayers = new HashSet<>();

    public CubeBossBeamEntity(EntityType<? extends CubeBossBeamEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public void setup(Direction direction, int length) {
        this.entityData.set(DATA_DIRECTION, direction.ordinal());
        this.entityData.set(DATA_LENGTH, Math.max(1, length));
        this.entityData.set(DATA_WARNING_TICKS, WARNING_TICKS);
        this.entityData.set(DATA_ACTIVE_TICKS, ACTIVE_TICKS);
        this.entityData.set(DATA_MOTION_X, 0.0F);
        this.entityData.set(DATA_MOTION_Y, 0.0F);
        this.entityData.set(DATA_MOTION_Z, 0.0F);
    }

    public void setupMoving(Direction direction, int length, int warningTicks, int activeTicks, double motionX, double motionY, double motionZ) {
        setup(direction, length);
        this.entityData.set(DATA_WARNING_TICKS, Math.max(0, warningTicks));
        this.entityData.set(DATA_ACTIVE_TICKS, Math.max(1, activeTicks));
        this.entityData.set(DATA_MOTION_X, (float) motionX);
        this.entityData.set(DATA_MOTION_Y, (float) motionY);
        this.entityData.set(DATA_MOTION_Z, (float) motionZ);
    }

    public boolean isWarning() {
        return getBeamAge() < getWarningTicks();
    }

    public boolean isActive() {
        int age = getBeamAge();
        return age >= getWarningTicks() && age < getTotalTicks();
    }

    public int getBeamAge() {
        return this.entityData.get(DATA_AGE);
    }

    public Direction getBeamDirection() {
        Direction[] directions = Direction.values();
        int index = this.entityData.get(DATA_DIRECTION);
        return directions[Math.floorMod(index, directions.length)];
    }

    public int getBeamLength() {
        return this.entityData.get(DATA_LENGTH);
    }

    public int getWarningTicks() {
        return this.entityData.get(DATA_WARNING_TICKS);
    }

    public int getActiveTicks() {
        return this.entityData.get(DATA_ACTIVE_TICKS);
    }

    public int getTotalTicks() {
        return getWarningTicks() + getActiveTicks();
    }

    public float getWarningProgress(float partialTicks) {
        int warningTicks = Math.max(1, getWarningTicks());
        return Math.min(1.0F, (getBeamAge() + partialTicks) / warningTicks);
    }

    public float getActiveProgress(float partialTicks) {
        int activeTicks = Math.max(1, getActiveTicks());
        return Math.min(1.0F, Math.max(0.0F, (getBeamAge() - getWarningTicks() + partialTicks) / activeTicks));
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            if (isActive()) {
                moveActiveBeam();
            }

            return;
        }

        int nextAge = getBeamAge() + 1;
        this.entityData.set(DATA_AGE, nextAge);

        if (nextAge >= getWarningTicks() && nextAge < getTotalTicks()) {
            moveActiveBeam();
            hurtPlayersInBeam();
        }

        if (nextAge >= getTotalTicks()) {
            this.discard();
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_AGE, 0);
        builder.define(DATA_DIRECTION, Direction.UP.ordinal());
        builder.define(DATA_LENGTH, 1);
        builder.define(DATA_WARNING_TICKS, WARNING_TICKS);
        builder.define(DATA_ACTIVE_TICKS, ACTIVE_TICKS);
        builder.define(DATA_MOTION_X, 0.0F);
        builder.define(DATA_MOTION_Y, 0.0F);
        builder.define(DATA_MOTION_Z, 0.0F);
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

    private void hurtPlayersInBeam() {
        AABB hitBox = getBeamHitBox();

        for (Player player : this.level().getEntitiesOfClass(Player.class, hitBox)) {
            if (this.damagedPlayers.contains(player.getUUID())) {
                continue;
            }

            if (player.hurt(this.damageSources().source(DamageTypes.MAGIC, this), DAMAGE)) {
                this.damagedPlayers.add(player.getUUID());
            }
        }
    }

    private void moveActiveBeam() {
        double motionX = this.entityData.get(DATA_MOTION_X);
        double motionY = this.entityData.get(DATA_MOTION_Y);
        double motionZ = this.entityData.get(DATA_MOTION_Z);

        if (motionX == 0.0D && motionY == 0.0D && motionZ == 0.0D) {
            return;
        }

        this.setPos(this.getX() + motionX, this.getY() + motionY, this.getZ() + motionZ);
    }

    private AABB getBeamHitBox() {
        Direction direction = getBeamDirection();
        double endX = this.getX() + direction.getStepX() * getBeamLength();
        double endY = this.getY() + direction.getStepY() * getBeamLength();
        double endZ = this.getZ() + direction.getStepZ() * getBeamLength();

        return new AABB(
                Math.min(this.getX(), endX) - HITBOX_HALF_WIDTH,
                Math.min(this.getY(), endY) - HITBOX_HALF_WIDTH,
                Math.min(this.getZ(), endZ) - HITBOX_HALF_WIDTH,
                Math.max(this.getX(), endX) + HITBOX_HALF_WIDTH,
                Math.max(this.getY(), endY) + HITBOX_HALF_WIDTH,
                Math.max(this.getZ(), endZ) + HITBOX_HALF_WIDTH
        );
    }
}
