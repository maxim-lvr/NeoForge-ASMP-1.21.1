package net.maximlvr.asmpthings.entity.custom;

import net.minecraft.core.BlockPos;
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

public class CubeBossSwordEntity extends Entity {
    public static final int WARNING_TICKS = 20;
    public static final int ACTIVE_TICKS = 4;
    public static final int TOTAL_TICKS = WARNING_TICKS + ACTIVE_TICKS;
    public static final float DAMAGE = 6.0F;
    private static final double HITBOX_HEIGHT = 0.6D;

    private static final EntityDataAccessor<Integer> DATA_AGE =
            SynchedEntityData.defineId(CubeBossSwordEntity.class, EntityDataSerializers.INT);

    private final Set<UUID> damagedPlayers = new HashSet<>();

    public CubeBossSwordEntity(EntityType<? extends CubeBossSwordEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public boolean isWarning() {
        return getSwordAge() < WARNING_TICKS;
    }

    public boolean isActive() {
        int age = getSwordAge();
        return age >= WARNING_TICKS && age < TOTAL_TICKS;
    }

    public int getSwordAge() {
        return this.entityData.get(DATA_AGE);
    }

    public float getWarningProgress(float partialTicks) {
        return Math.min(1.0F, (getSwordAge() + partialTicks) / WARNING_TICKS);
    }

    public float getActiveProgress(float partialTicks) {
        return Math.min(1.0F, Math.max(0.0F, (getSwordAge() - WARNING_TICKS + partialTicks) / ACTIVE_TICKS));
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            return;
        }

        int nextAge = getSwordAge() + 1;
        this.entityData.set(DATA_AGE, nextAge);

        if (nextAge >= WARNING_TICKS && nextAge < TOTAL_TICKS) {
            hurtPlayersOnThisBlock();
        }

        if (nextAge >= TOTAL_TICKS) {
            this.discard();
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_AGE, 0);
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

    private void hurtPlayersOnThisBlock() {
        BlockPos blockPos = this.blockPosition();
        AABB hitBox = new AABB(
                blockPos.getX(), this.getY() - 0.05D, blockPos.getZ(),
                blockPos.getX() + 1.0D, this.getY() + HITBOX_HEIGHT, blockPos.getZ() + 1.0D
        );

        for (Player player : this.level().getEntitiesOfClass(Player.class, hitBox)) {
            if (this.damagedPlayers.contains(player.getUUID())) {
                continue;
            }

            if (player.blockPosition().getX() != blockPos.getX() || player.blockPosition().getZ() != blockPos.getZ()) {
                continue;
            }

            if (player.getY() > this.getY() + HITBOX_HEIGHT) {
                continue;
            }

            if (player.hurt(this.damageSources().source(DamageTypes.MAGIC, this), DAMAGE)) {
                this.damagedPlayers.add(player.getUUID());
            }
        }
    }
}
