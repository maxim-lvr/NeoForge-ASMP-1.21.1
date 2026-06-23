package net.maximlvr.asmpthings.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import java.util.Optional;

import java.util.UUID;

public class CrawlerHandEntity extends Entity {

    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_ID =
            SynchedEntityData.defineId(CrawlerHandEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private static final EntityDataAccessor<Integer> DATA_HAND_INDEX =
            SynchedEntityData.defineId(CrawlerHandEntity.class, EntityDataSerializers.INT);

    private boolean anchored = false;
    private boolean moving = false;

    private double anchorX;
    private double anchorY;
    private double anchorZ;

    private double startX;
    private double startY;
    private double startZ;

    private double targetX;
    private double targetY;
    private double targetZ;

    private int stepTicks = 0;
    private int maxStepTicks = 8;

    private int discomfortTicks = 0;
    private int stepCooldownTicks = 0;

    public CrawlerHandEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.stepCooldownTicks > 0) {
            this.stepCooldownTicks--;
        }

        if (this.moving) {
            updateStepMovement();
            return;
        }

        if (this.anchored) {
            this.setPos(this.anchorX, this.anchorY, this.anchorZ);
            this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        }
    }

    public boolean canStartStep() {
        return !this.moving && this.stepCooldownTicks <= 0;
    }

    public int addDiscomfortTick() {
        this.discomfortTicks++;
        return this.discomfortTicks;
    }

    public void resetDiscomfortTicks() {
        this.discomfortTicks = 0;
    }

    private void updateStepMovement() {
        this.stepTicks++;

        double progress = Math.min(1.0D, this.stepTicks / (double) this.maxStepTicks);
        double eased = easeInOut(progress);

        double x = lerp(this.startX, this.targetX, eased);
        double z = lerp(this.startZ, this.targetZ, eased);

        double baseY = lerp(this.startY, this.targetY, eased);

        double arcHeight = this.maxStepTicks <= 8 ? 0.75D : 0.35D;
        double arc = Math.sin(progress * Math.PI) * arcHeight;

        double y = baseY + arc;

        this.setPos(x, y, z);

        if (progress >= 1.0D) {
            BlockPos finalGround = findGroundNear(this.targetX, this.targetY, this.targetZ);

            this.anchorX = finalGround.getX() + 0.5D;
            this.anchorY = finalGround.getY();
            this.anchorZ = finalGround.getZ() + 0.5D;

            this.anchored = true;
            this.moving = false;

            this.stepCooldownTicks = 8;
            this.discomfortTicks = 0;

            this.setPos(this.anchorX, this.anchorY, this.anchorZ);
            this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        }
    }

    public void moveToGroundNear(double wantedX, double wantedZ, boolean urgent) {
        if (this.moving || this.stepCooldownTicks > 0) {
            return;
        }

        BlockPos ground = findGroundNear(wantedX, this.getY(), wantedZ);

        double nextTargetX = ground.getX() + 0.5D;
        double nextTargetY = ground.getY();
        double nextTargetZ = ground.getZ() + 0.5D;

        double dx = nextTargetX - this.getX();
        double dy = nextTargetY - this.getY();
        double dz = nextTargetZ - this.getZ();

        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        // Anti-saut-sur-place : si la cible est quasiment la même, on s'ancre sans animation.
        if (horizontalDistance < 0.35D && Math.abs(dy) < 0.35D) {
            this.anchorX = nextTargetX;
            this.anchorY = nextTargetY;
            this.anchorZ = nextTargetZ;

            this.anchored = true;
            this.moving = false;

            this.discomfortTicks = 0;
            this.stepCooldownTicks = 6;

            this.setPos(this.anchorX, this.anchorY, this.anchorZ);
            this.setDeltaMovement(0.0D, 0.0D, 0.0D);
            return;
        }

        this.startX = this.getX();
        this.startY = this.getY();
        this.startZ = this.getZ();

        this.targetX = nextTargetX;
        this.targetY = nextTargetY;
        this.targetZ = nextTargetZ;

        this.stepTicks = 0;
        this.maxStepTicks = urgent ? 4 : 12;

        this.anchored = false;
        this.moving = true;
    }

    public boolean hasGroundBelow() {
        BlockPos feet = this.blockPosition();
        BlockPos below = feet.below();

        return !this.level()
                .getBlockState(below)
                .getCollisionShape(this.level(), below)
                .isEmpty();
    }

    public void forceAnchorHere() {
        BlockPos ground = findGroundNear(this.getX(), this.getY(), this.getZ());

        this.anchorX = ground.getX() + 0.5D;
        this.anchorY = ground.getY();
        this.anchorZ = ground.getZ() + 0.5D;

        this.anchored = true;
        this.moving = false;

        this.setPos(this.anchorX, this.anchorY, this.anchorZ);
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
    }

    public void clampAroundOwner(double ownerX, double ownerY, double ownerZ, double maxDistance) {
        double dx = this.getX() - ownerX;
        double dy = this.getY() - ownerY;
        double dz = this.getZ() - ownerZ;

        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance <= maxDistance || distance < 0.001D) {
            return;
        }

        double scale = maxDistance / distance;

        double clampedX = ownerX + dx * scale;
        double clampedY = ownerY + dy * scale;
        double clampedZ = ownerZ + dz * scale;

        this.setPos(clampedX, clampedY, clampedZ);

        this.anchored = false;
        this.moving = false;
    }

    public double horizontalDistanceTo(double x, double z) {
        double dx = x - this.getX();
        double dz = z - this.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    public boolean isAnchored() {
        return this.anchored;
    }

    public boolean isMoving() {
        return this.moving;
    }

    public boolean isTouchingGround() {
        return this.anchored && hasGroundBelow();
    }

    public void setOwner(CrawlerEntity owner) {
        this.entityData.set(DATA_OWNER_ID, Optional.of(owner.getUUID()));
    }

    public UUID getOwnerId() {
        return this.entityData.get(DATA_OWNER_ID).orElse(null);
    }

    public void setHandIndex(int handIndex) {
        this.entityData.set(DATA_HAND_INDEX, handIndex);
    }

    public int getHandIndex() {
        return this.entityData.get(DATA_HAND_INDEX);
    }

    private BlockPos findGroundNear(double x, double y, double z) {
        int baseX = (int) Math.floor(x);
        int baseZ = (int) Math.floor(z);

        int startY = (int) Math.floor(y + 1.0D);
        int minY = Math.max(this.level().getMinBuildHeight() + 1, startY - 16);

        for (int yy = startY; yy >= minY; yy--) {
            BlockPos feetPos = new BlockPos(baseX, yy, baseZ);
            BlockPos groundPos = feetPos.below();

            boolean groundSolid = this.level()
                    .getBlockState(groundPos)
                    .getCollisionShape(this.level(), groundPos)
                    .isEmpty() == false;

            boolean feetFree = this.level()
                    .getBlockState(feetPos)
                    .getCollisionShape(this.level(), feetPos)
                    .isEmpty();

            if (groundSolid && feetFree) {
                return feetPos;
            }
        }

        return BlockPos.containing(x, y, z);
    }

    private double lerp(double a, double b, double progress) {
        return a + (b - a) * progress;
    }

    private double easeInOut(double progress) {
        return progress * progress * (3.0D - 2.0D * progress);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER_ID, Optional.empty());
        builder.define(DATA_HAND_INDEX, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.entityData.set(DATA_OWNER_ID, Optional.of(tag.getUUID("Owner")));
        }

        if (tag.contains("HandIndex")) {
            this.entityData.set(DATA_HAND_INDEX, tag.getInt("HandIndex"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        UUID ownerId = getOwnerId();

        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }

        tag.putInt("HandIndex", getHandIndex());
    }

    public boolean canSupportBody() {
        return this.anchored && !this.moving && this.hasGroundBelow();
    }

    public double getSupportY() {
        return this.anchorY;
    }

    public void moveToGroundNearBody(
            double wantedX,
            double wantedZ,
            double bodyX,
            double bodyY,
            double bodyZ,
            boolean urgent
    ) {
        if (this.moving || this.stepCooldownTicks > 0) {
            return;
        }

        BlockPos ground = findGroundNearBody(wantedX, wantedZ, bodyX, bodyY, bodyZ);

        double nextTargetX = ground.getX() + 0.5D;
        double nextTargetY = ground.getY();
        double nextTargetZ = ground.getZ() + 0.5D;

        double dx = nextTargetX - this.getX();
        double dy = nextTargetY - this.getY();
        double dz = nextTargetZ - this.getZ();

        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        if (horizontalDistance < 0.35D && Math.abs(dy) < 0.35D) {
            this.anchorX = nextTargetX;
            this.anchorY = nextTargetY;
            this.anchorZ = nextTargetZ;

            this.anchored = true;
            this.moving = false;

            this.discomfortTicks = 0;
            this.stepCooldownTicks = 6;

            this.setPos(this.anchorX, this.anchorY, this.anchorZ);
            this.setDeltaMovement(0.0D, 0.0D, 0.0D);
            return;
        }

        this.startX = this.getX();
        this.startY = this.getY();
        this.startZ = this.getZ();

        this.targetX = nextTargetX;
        this.targetY = nextTargetY;
        this.targetZ = nextTargetZ;

        this.stepTicks = 0;
        this.maxStepTicks = urgent ? 4 : 12;

        this.anchored = false;
        this.moving = true;
    }

    private BlockPos findGroundNearBody(
            double wantedX,
            double wantedZ,
            double bodyX,
            double bodyY,
            double bodyZ
    ) {
        double dirX = wantedX - bodyX;
        double dirZ = wantedZ - bodyZ;

        // On teste d'abord la position idéale, puis des positions plus proches du corps.
        // Si le tunnel est étroit, les pattes se serrent naturellement.
        double[] scales = {
                1.0D,
                0.9D,
                0.8D,
                0.7D,
                0.6D,
                0.5D,
                0.4D
        };

        double[] sideOffsets = {
                0.0D,
                0.35D,
                -0.35D,
                0.7D,
                -0.7D
        };

        double length = Math.sqrt(dirX * dirX + dirZ * dirZ);

        double sideX = 0.0D;
        double sideZ = 0.0D;

        if (length > 0.001D) {
            sideX = -dirZ / length;
            sideZ = dirX / length;
        }

        for (double scale : scales) {
            double candidateCenterX = bodyX + dirX * scale;
            double candidateCenterZ = bodyZ + dirZ * scale;

            for (double sideOffset : sideOffsets) {
                double candidateX = candidateCenterX + sideX * sideOffset;
                double candidateZ = candidateCenterZ + sideZ * sideOffset;

                BlockPos ground = findGroundAtXZBelowBody(candidateX, candidateZ, bodyY);

                if (ground == null) {
                    continue;
                }

                if (!canReachFromBody(bodyX, bodyY, bodyZ, ground)) {
                    continue;
                }

                return ground;
            }
        }

        // Fallback : si rien n'est trouvé, on cherche près du corps.
        BlockPos fallback = findGroundAtXZBelowBody(bodyX, bodyZ, bodyY);

        if (fallback != null) {
            return fallback;
        }

        return BlockPos.containing(this.getX(), this.getY(), this.getZ());
    }

    private BlockPos findGroundAtXZBelowBody(double x, double z, double bodyY) {
        int baseX = (int) Math.floor(x);
        int baseZ = (int) Math.floor(z);

        // Très important : on ne cherche pas au-dessus du corps.
        int startY = (int) Math.floor(bodyY + 0.25D);

        // La main cherche sous le corps, pas à 30 blocs plus haut.
        int minY = Math.max(this.level().getMinBuildHeight() + 1, startY - 8);

        for (int y = startY; y >= minY; y--) {
            BlockPos feetPos = new BlockPos(baseX, y, baseZ);
            BlockPos groundPos = feetPos.below();

            boolean groundSolid = !this.level()
                    .getBlockState(groundPos)
                    .getCollisionShape(this.level(), groundPos)
                    .isEmpty();

            boolean feetFree = this.level()
                    .getBlockState(feetPos)
                    .getCollisionShape(this.level(), feetPos)
                    .isEmpty();

            if (groundSolid && feetFree) {
                return feetPos;
            }
        }

        return null;
    }

    private boolean canReachFromBody(double bodyX, double bodyY, double bodyZ, BlockPos targetFeetPos) {
        double targetX = targetFeetPos.getX() + 0.5D;
        double targetY = targetFeetPos.getY() + 0.4D;
        double targetZ = targetFeetPos.getZ() + 0.5D;

        double startX = bodyX;
        double startY = Math.min(bodyY - 0.4D, targetY + 1.0D);
        double startZ = bodyZ;

        double dx = targetX - startX;
        double dy = targetY - startY;
        double dz = targetZ - startZ;

        int samples = 10;

        for (int i = 1; i <= samples; i++) {
            double progress = i / (double) samples;

            double checkX = startX + dx * progress;
            double checkY = startY + dy * progress;
            double checkZ = startZ + dz * progress;

            if (isBlockedAt(checkX, checkY, checkZ)) {
                return false;
            }

            // On teste aussi un peu au-dessus pour éviter que la patte traverse un mur bas.
            if (isBlockedAt(checkX, checkY + 0.45D, checkZ)) {
                return false;
            }
        }

        return true;
    }

    private boolean isBlockedAt(double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);

        return !this.level()
                .getBlockState(pos)
                .getCollisionShape(this.level(), pos)
                .isEmpty();
    }
}