package net.maximlvr.asmpthings.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.network.syncher.EntityDataAccessor;
import java.util.Optional;
import net.minecraft.core.Direction;

import java.util.UUID;

public class CrawlerHandEntity extends Entity {

    private record SurfaceTarget(BlockPos airPos, Direction normal) {
    }

    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_ID =
            SynchedEntityData.defineId(CrawlerHandEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private static final EntityDataAccessor<Integer> DATA_HAND_INDEX =
            SynchedEntityData.defineId(CrawlerHandEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> DATA_ATTACHED_NORMAL =
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

    private void setAttachedNormal(Direction normal) {
        this.entityData.set(DATA_ATTACHED_NORMAL, normal.ordinal());
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
            this.anchorX = this.targetX;
            this.anchorY = this.targetY;
            this.anchorZ = this.targetZ;

            this.anchored = true;
            this.moving = false;

            this.stepCooldownTicks = 8;
            this.discomfortTicks = 0;

            this.setPos(this.anchorX, this.anchorY, this.anchorZ);
            this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        }
    }

    public boolean isAttachedToSurface() {
        return this.anchored && !this.moving;
    }

    public Direction getAttachedNormal() {
        int id = this.entityData.get(DATA_ATTACHED_NORMAL);

        Direction[] values = Direction.values();

        if (id < 0 || id >= values.length) {
            return Direction.UP;
        }

        return values[id];
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
        builder.define(DATA_ATTACHED_NORMAL, Direction.UP.ordinal());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.entityData.set(DATA_OWNER_ID, Optional.of(tag.getUUID("Owner")));
        }

        if (tag.contains("HandIndex")) {
            this.entityData.set(DATA_HAND_INDEX, tag.getInt("HandIndex"));
        }

        if (tag.contains("AttachedNormal")) {
            this.entityData.set(DATA_ATTACHED_NORMAL, tag.getInt("AttachedNormal"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        UUID ownerId = getOwnerId();

        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }

        tag.putInt("HandIndex", getHandIndex());
        tag.putInt("AttachedNormal", this.entityData.get(DATA_ATTACHED_NORMAL));
    }

    public boolean canSupportBody() {
        return this.anchored && !this.moving && this.hasGroundBelow();
    }

    public double getSupportY() {
        return this.anchorY;
    }


    private boolean isBlockedAt(double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);

        return !this.level()
                .getBlockState(pos)
                .getCollisionShape(this.level(), pos)
                .isEmpty();
    }

    public void moveToSurfaceNearBody(
            double wantedX,
            double wantedY,
            double wantedZ,
            double bodyX,
            double bodyY,
            double bodyZ,
            boolean urgent
    ) {
        moveToSurfaceNearBody(
                wantedX,
                wantedY,
                wantedZ,
                bodyX,
                bodyY,
                bodyZ,
                urgent,
                null
        );
    }

    public void moveToSurfaceNearBody(
            double wantedX,
            double wantedY,
            double wantedZ,
            double bodyX,
            double bodyY,
            double bodyZ,
            boolean urgent,
            Direction preferredNormal
    ) {
        if (this.moving || this.stepCooldownTicks > 0) {
            return;
        }

        SurfaceTarget surface = findSurfaceNearBody(
                wantedX,
                wantedY,
                wantedZ,
                bodyX,
                bodyY,
                bodyZ,
                preferredNormal
        );

        double nextTargetX = surface.airPos().getX() + 0.5D;
        double nextTargetY = getEntityYForSurface(surface.airPos(), surface.normal());
        double nextTargetZ = surface.airPos().getZ() + 0.5D;

        double dx = nextTargetX - this.getX();
        double dy = nextTargetY - this.getY();
        double dz = nextTargetZ - this.getZ();

        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance < 0.35D) {
            this.anchorX = nextTargetX;
            this.anchorY = nextTargetY;
            this.anchorZ = nextTargetZ;
            setAttachedNormal(surface.normal());

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

        setAttachedNormal(surface.normal());
        this.anchored = false;
        this.moving = true;
    }

    private SurfaceTarget findSurfaceNearBody(
            double wantedX,
            double wantedY,
            double wantedZ,
            double bodyX,
            double bodyY,
            double bodyZ,
            Direction preferredNormal
    ) {
        SurfaceTarget bestTarget = null;
        double bestScore = Double.MAX_VALUE;

        int baseX = (int) Math.floor(wantedX);
        int baseY = (int) Math.floor(wantedY);
        int baseZ = (int) Math.floor(wantedZ);

        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos airPos = new BlockPos(baseX + dx, baseY + dy, baseZ + dz);

                    if (!isFreeForHand(airPos)) {
                        continue;
                    }

                    for (Direction normal : Direction.values()) {
                        BlockPos solidPos = airPos.relative(normal.getOpposite());

                        if (!isSolidSurface(solidPos)) {
                            continue;
                        }

                        if (!canReachFromBody(bodyX, bodyY, bodyZ, airPos)) {
                            continue;
                        }

                        double centerX = airPos.getX() + 0.5D;
                        double centerY = getEntityYForSurface(airPos, normal);
                        double centerZ = airPos.getZ() + 0.5D;

                        double distToWanted = distance(centerX, centerY, centerZ, wantedX, wantedY, wantedZ);
                        double distToBody = distance(centerX, centerY, centerZ, bodyX, bodyY, bodyZ);

                        if (distToBody > 4.4D) {
                            continue;
                        }

                        double normalPenalty = getNormalPenalty(normal);

                        if (preferredNormal != null && normal != preferredNormal) {
                            normalPenalty += 4.0D;
                        }

                        double score = distToWanted + normalPenalty;

                        if (score < bestScore) {
                            bestScore = score;
                            bestTarget = new SurfaceTarget(airPos, normal);
                        }
                    }
                }
            }
        }

        if (bestTarget != null) {
            return bestTarget;
        }

        BlockPos fallback = BlockPos.containing(this.getX(), this.getY(), this.getZ());
        return new SurfaceTarget(fallback, Direction.UP);
    }

    private boolean isFreeForHand(BlockPos pos) {
        return this.level()
                .getBlockState(pos)
                .getCollisionShape(this.level(), pos)
                .isEmpty();
    }

    private boolean isSolidSurface(BlockPos pos) {
        return !this.level()
                .getBlockState(pos)
                .getCollisionShape(this.level(), pos)
                .isEmpty();
    }

    private double getEntityYForSurface(BlockPos airPos, Direction normal) {
        if (normal == Direction.UP) {
            // Main posée sur le sol.
            return airPos.getY();
        }

        if (normal == Direction.DOWN) {
            // Main accrochée au plafond.
            return airPos.getY() + 0.5D;
        }

        // Main contre un mur.
        return airPos.getY() + 0.35D;
    }

    private double getNormalPenalty(Direction normal) {
        return switch (normal) {
            case UP -> 0.0D;       // sol prioritaire
            case NORTH, SOUTH, EAST, WEST -> 0.25D; // murs
            case DOWN -> 0.45D;    // plafond
        };
    }

    private double distance(double ax, double ay, double az, double bx, double by, double bz) {
        double dx = ax - bx;
        double dy = ay - by;
        double dz = az - bz;

        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private boolean canReachFromBody(double bodyX, double bodyY, double bodyZ, BlockPos targetAirPos) {
        double targetX = targetAirPos.getX() + 0.5D;
        double targetY = targetAirPos.getY() + 0.5D;
        double targetZ = targetAirPos.getZ() + 0.5D;

        double startX = bodyX;
        double startY = bodyY + 0.45D;
        double startZ = bodyZ;

        double dx = targetX - startX;
        double dy = targetY - startY;
        double dz = targetZ - startZ;

        int samples = 12;

        for (int i = 1; i < samples; i++) {
            double progress = i / (double) samples;

            double checkX = startX + dx * progress;
            double checkY = startY + dy * progress;
            double checkZ = startZ + dz * progress;

            if (isBlockedAt(checkX, checkY, checkZ)) {
                return false;
            }
        }

        return true;
    }

}