package net.maximlvr.asmpthings.entity.custom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.maximlvr.asmpthings.entity.ModEntities;
import net.minecraft.world.entity.Entity;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

public class CrawlerEntity extends Monster {

    private UUID frontLeftHandId;
    private UUID frontRightHandId;
    private UUID backLeftHandId;
    private UUID backRightHandId;

    private boolean bodyMotionInitialized = false;
    private double lastBodyX;
    private double lastBodyZ;
    private double bodyMoveX;
    private double bodyMoveZ;

    private boolean handsSpawned = false;
    private int attackCooldown = 0;
    private double compactAmount = 0.0D;

    public CrawlerEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    private void ensureHandsSpawned() {
        if (this.level().isClientSide()) {
            return;
        }

        if (this.handsSpawned) {
            return;
        }

        spawnHand(0, -1.2D, 0.0D, -2.5D);
        spawnHand(1, 1.2D, 0.0D, -2.5D);
        spawnHand(2, -1.2D, 0.0D, 2.5D);
        spawnHand(3, 1.2D, 0.0D, 2.5D);

        this.handsSpawned = true;
    }

    private void updateBodyMoveMemory() {
        if (!this.bodyMotionInitialized) {
            this.lastBodyX = this.getX();
            this.lastBodyZ = this.getZ();
            this.bodyMoveX = 0.0D;
            this.bodyMoveZ = 0.0D;
            this.bodyMotionInitialized = true;
            return;
        }

        this.bodyMoveX = this.getX() - this.lastBodyX;
        this.bodyMoveZ = this.getZ() - this.lastBodyZ;

        this.lastBodyX = this.getX();
        this.lastBodyZ = this.getZ();
    }

    private void spawnHand(int index, double offsetX, double offsetY, double offsetZ) {
        CrawlerHandEntity hand = ModEntities.CRAWLER_HAND.get().create(this.level());

        if (hand == null) {
            return;
        }

        hand.setOwner(this);
        hand.setHandIndex(index);
        hand.setPos(
                this.getX() + offsetX,
                this.getY() + offsetY,
                this.getZ() + offsetZ
        );

        this.level().addFreshEntity(hand);
        hand.forceAnchorHere();

        switch (index) {
            case 0 -> this.frontLeftHandId = hand.getUUID();
            case 1 -> this.frontRightHandId = hand.getUUID();
            case 2 -> this.backLeftHandId = hand.getUUID();
            case 3 -> this.backRightHandId = hand.getUUID();
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));

        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 32.0F));

        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(
                this,
                Player.class,
                true
        ));
    }

    private void chasePlayer(Player player) {
        double dx = player.getX() - this.getX();
        double dz = player.getZ() - this.getZ();

        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        if (horizontalDistance < 0.001D) {
            return;
        }

        double dirX = dx / horizontalDistance;
        double dirZ = dz / horizontalDistance;

        // Rotation du corps vers le joueur.
        float targetYaw = (float) (Mth.atan2(dz, dx) * (180.0F / Math.PI)) - 90.0F;

        this.setYRot(targetYaw);
        this.yBodyRot = targetYaw;
        this.yHeadRot = targetYaw;

        double speed;

        if (horizontalDistance > 12.0D) {
            speed = 0.38D;
        } else if (horizontalDistance > 4.0D) {
            speed = 0.32D;
        } else if (horizontalDistance > 2.2D) {
            speed = 0.22D;
        } else {
            speed = 0.0D;
        }

        Vec3 motion = this.getDeltaMovement();

        BlockPos groundFeetPos = findGroundBelowBody();

        if (groundFeetPos != null) {
            int aheadFreeHeight = getLowestFreeHeightAhead(groundFeetPos);

            if (aheadFreeHeight <= 2 && this.getY() > groundFeetPos.getY() + 0.25D) {
                speed *= 0.35D;
            }
        }

        this.setDeltaMovement(
                dirX * speed,
                motion.y,
                dirZ * speed
        );

        this.hasImpulse = true;

        if (horizontalDistance <= 2.4D) {
            tryAttack(player);
        }
    }

    private void tryAttack(Player player) {
        if (this.attackCooldown > 0) {
            return;
        }

        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        this.doHurtTarget(player);
        this.attackCooldown = 20;
    }

    @Override
    public void remove(RemovalReason reason) {
        removeHand(this.frontLeftHandId);
        removeHand(this.frontRightHandId);
        removeHand(this.backLeftHandId);
        removeHand(this.backRightHandId);

        super.remove(reason);
    }

    private void updateBodyHeightAndHands(ServerLevel serverLevel) {
        applyBodyHeightControl();
        updateBodyMoveMemory();
        updateHandsComfort(serverLevel);
    }

    private void applyBodyHeightControl() {
        BlockPos groundFeetPos = findGroundBelowBody();

        if (groundFeetPos == null) {
            return;
        }

        updateCompactAmount(groundFeetPos);

        int currentFreeHeight = countFreeBlocksAbove(groundFeetPos, 5);
        int aheadFreeHeight = getLowestFreeHeightAhead(groundFeetPos);
        int effectiveFreeHeight = Math.min(currentFreeHeight, aheadFreeHeight);

        double targetHeight;

        if (effectiveFreeHeight <= 2) {
            // Tunnel 2 blocs détecté devant ou autour : on se baisse avant d'entrer.
            targetHeight = 0.05D;
        } else if (effectiveFreeHeight == 3) {
            targetHeight = 0.65D;
        } else {
            targetHeight = lerpDouble(2.0D, 0.75D, this.compactAmount);
        }

        double targetY = groundFeetPos.getY() + targetHeight;
        double currentY = this.getY();
        double dy = targetY - currentY;

        Vec3 motion = this.getDeltaMovement();

        double wantedYSpeed = dy * 0.55D;

        wantedYSpeed = Math.max(-0.55D, Math.min(0.45D, wantedYSpeed));

        if (Math.abs(dy) < 0.035D) {
            wantedYSpeed = 0.0D;
        }

        this.setDeltaMovement(
                motion.x,
                wantedYSpeed,
                motion.z
        );
    }

    private BlockPos findGroundBelowBody() {
        return findGroundBelowAt(this.getX(), this.getZ(), this.getY());
    }

    private void updateHandsComfort(ServerLevel serverLevel) {
        double normalSpread = 1.0D;
        double compactSpread = 0.55D;

        double spread = lerpDouble(normalSpread, compactSpread, this.compactAmount);

        updateHandComfort(serverLevel, this.frontLeftHandId, 0, -1.2D * spread, -2.8D * spread);
        updateHandComfort(serverLevel, this.frontRightHandId, 1, 1.2D * spread, -2.8D * spread);
        updateHandComfort(serverLevel, this.backLeftHandId, 2, -1.2D * spread, 2.8D * spread);
        updateHandComfort(serverLevel, this.backRightHandId, 3, 1.2D * spread, 2.8D * spread);
    }

    private void updateHandComfort(ServerLevel serverLevel, UUID handId, int handIndex, double offsetX, double offsetZ) {
        if (handId == null) {
            return;
        }

        Entity entity = serverLevel.getEntity(handId);

        if (!(entity instanceof CrawlerHandEntity hand)) {
            return;
        }

        double yawRad = Math.toRadians(this.getYRot());

        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);

        double rotatedX = offsetX * cos - offsetZ * sin;
        double rotatedZ = offsetX * sin + offsetZ * cos;

        double comfortX = this.getX() + rotatedX;
        double comfortZ = this.getZ() + rotatedZ;

        // Anticipation : la patte vise un peu devant, dans la direction du corps.
        double moveLength = Math.sqrt(this.bodyMoveX * this.bodyMoveX + this.bodyMoveZ * this.bodyMoveZ);

        if (moveLength > 0.015D) {
            double dirX = this.bodyMoveX / moveLength;
            double dirZ = this.bodyMoveZ / moveLength;

            double predictionDistance = 0.85D;

            comfortX += dirX * predictionDistance;
            comfortZ += dirZ * predictionDistance;
        }

        double distanceToBody = hand.horizontalDistanceTo(this.getX(), this.getZ());
        double distanceToComfort = hand.horizontalDistanceTo(comfortX, comfortZ);
        double minComfortDistance = lerpDouble(2.75D, 1.35D, this.compactAmount);
        double maxComfortDistance = lerpDouble(3.45D, 2.15D, this.compactAmount);
        double maxArmDistance = lerpDouble(4.0D, 2.65D, this.compactAmount);

        if (hand.isMoving()) {
            hand.resetDiscomfortTicks();
            return;
        }

        // Pas au sol : replacement rapide, sans délai.
        if (!hand.hasGroundBelow() || !hand.isAnchored()) {
            if (hand.canStartStep() && canHandStartAlternatedStep(serverLevel, handIndex, true)) {
                hand.moveToGroundNearBody(
                        comfortX,
                        comfortZ,
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        true
                );
            }
            return;
        }

        // Bras beaucoup trop long : replacement rapide, avec très peu de délai.
        if (distanceToBody > maxArmDistance) {
            int ticks = hand.addDiscomfortTick();

            if (ticks >= 2 && hand.canStartStep() && canHandStartAlternatedStep(serverLevel, handIndex, true)) {
                hand.moveToGroundNearBody(
                        comfortX,
                        comfortZ,
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        true
                );
            }

            return;
        }

        // Trop proche / trop loin de la zone idéale : petit délai avant de bouger.
        if (distanceToBody < minComfortDistance || distanceToBody > maxComfortDistance) {
            int ticks = hand.addDiscomfortTick();

            int requiredTicks = 4 + getHandStepDelay(handIndex);

            if (ticks >= requiredTicks && hand.canStartStep() && canHandStartAlternatedStep(serverLevel, handIndex, false)) {
                hand.moveToGroundNearBody(
                        comfortX,
                        comfortZ,
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        false
                );
            }

            return;
        }

        // Hors confort latéral : délai plus long, déplacement lent.
        if (distanceToComfort > 1.05D) {
            int ticks = hand.addDiscomfortTick();

            int requiredTicks = 6 + getHandStepDelay(handIndex);

            if (ticks >= requiredTicks && hand.canStartStep() && canHandStartAlternatedStep(serverLevel, handIndex, false)) {
                hand.moveToGroundNearBody(
                        comfortX,
                        comfortZ,
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        false
                );
            }

            return;
        }

        // Patte confortable : reset.
        hand.resetDiscomfortTicks();
    }

    private int getHandStepDelay(int handIndex) {
        return switch (handIndex) {
            case 0 -> 0;
            case 1 -> 3;
            case 2 -> 5;
            case 3 -> 2;
            default -> 0;
        };
    }

    private void removeHand(UUID handId) {
        if (handId == null) {
            return;
        }

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Entity entity = serverLevel.getEntity(handId);

        if (entity instanceof CrawlerHandEntity hand) {
            hand.discard();
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ensureHandsSpawned();

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        updateBodyHeightAndHands(serverLevel);

        Player nearestPlayer = serverLevel.getNearestPlayer(
                this,
                32.0D
        );

        if (nearestPlayer == null) {
            return;
        }

        if (nearestPlayer.isCreative() || nearestPlayer.isSpectator()) {
            return;
        }

        this.setTarget(nearestPlayer);
        chasePlayer(nearestPlayer);
    }

    private void slowDownHorizontalMovement() {
        Vec3 motion = this.getDeltaMovement();

        this.setDeltaMovement(
                motion.x * 0.75D,
                motion.y,
                motion.z * 0.75D
        );
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D);
    }

    private boolean canHandStartAlternatedStep(ServerLevel serverLevel, int handIndex, boolean urgent) {
        int movingHands = countMovingHands(serverLevel);

        // Même en urgence, on évite que les 4 pattes sautent ensemble.
        if (urgent) {
            return movingHands < 2;
        }

        // Pas non urgent : une seule patte à la fois donne un rendu plus propre.
        if (movingHands > 0) {
            return false;
        }

        int activeGroup = (this.tickCount / 8) % 2;
        int handGroup = getHandGaitGroup(handIndex);

        return activeGroup == handGroup;
    }

    private int getHandGaitGroup(int handIndex) {
        // Diagonales :
        // 0 = avant gauche
        // 1 = avant droite
        // 2 = arrière gauche
        // 3 = arrière droite
        return switch (handIndex) {
            case 0, 3 -> 0;
            case 1, 2 -> 1;
            default -> 0;
        };
    }

    private int countMovingHands(ServerLevel serverLevel) {
        int count = 0;

        if (isHandMoving(serverLevel, this.frontLeftHandId)) {
            count++;
        }

        if (isHandMoving(serverLevel, this.frontRightHandId)) {
            count++;
        }

        if (isHandMoving(serverLevel, this.backLeftHandId)) {
            count++;
        }

        if (isHandMoving(serverLevel, this.backRightHandId)) {
            count++;
        }

        return count;
    }

    private boolean isHandMoving(ServerLevel serverLevel, UUID handId) {
        if (handId == null) {
            return false;
        }

        Entity entity = serverLevel.getEntity(handId);

        return entity instanceof CrawlerHandEntity hand && hand.isMoving();
    }

    private void updateCompactAmount(BlockPos groundFeetPos) {
        int currentFreeHeight = countFreeBlocksAbove(groundFeetPos, 5);
        int aheadFreeHeight = getLowestFreeHeightAhead(groundFeetPos);

        int effectiveFreeHeight = Math.min(currentFreeHeight, aheadFreeHeight);

        double targetCompact = calculateEnvironmentCompactness(groundFeetPos);

        // Anticipation tunnel bas.
        if (effectiveFreeHeight <= 2) {
            targetCompact = 1.0D;
        } else if (effectiveFreeHeight == 3) {
            targetCompact = Math.max(targetCompact, 0.75D);
        } else if (effectiveFreeHeight == 4) {
            targetCompact = Math.max(targetCompact, 0.35D);
        }

        // Si on doit passer direct sous 2 blocs, il faut se baisser vite.
        double smoothFactor = effectiveFreeHeight <= 2 ? 0.45D : 0.18D;

        this.compactAmount += (targetCompact - this.compactAmount) * smoothFactor;

        if (this.compactAmount < 0.01D) {
            this.compactAmount = 0.0D;
        }

        if (this.compactAmount > 0.99D) {
            this.compactAmount = 1.0D;
        }
    }

    private double calculateEnvironmentCompactness(BlockPos groundFeetPos) {
        double compact = 0.0D;

        // 1) Plafond bas au-dessus du corps.
        int freeHeight = countFreeBlocksAbove(groundFeetPos, 5);

        if (freeHeight <= 2) {
            compact = Math.max(compact, 1.0D);
        } else if (freeHeight == 3) {
            compact = Math.max(compact, 0.65D);
        } else if (freeHeight == 4) {
            compact = Math.max(compact, 0.25D);
        }

        // 2) Couloir étroit sur les côtés.
        int blockedSides = countBlockedHorizontalSides();

        if (blockedSides >= 3) {
            compact = Math.max(compact, 1.0D);
        } else if (blockedSides == 2) {
            compact = Math.max(compact, 0.65D);
        } else if (blockedSides == 1) {
            compact = Math.max(compact, 0.25D);
        }

        return compact;
    }

    private int countFreeBlocksAbove(BlockPos feetPos, int maxCheck) {
        int free = 0;

        for (int i = 0; i < maxCheck; i++) {
            BlockPos checkPos = feetPos.above(i);

            boolean blocked = !this.level()
                    .getBlockState(checkPos)
                    .getCollisionShape(this.level(), checkPos)
                    .isEmpty();

            if (blocked) {
                break;
            }

            free++;
        }

        return free;
    }

    private int countBlockedHorizontalSides() {
        int blocked = 0;

        BlockPos center = BlockPos.containing(this.getX(), this.getY(), this.getZ());

        if (isBlockedAround(center.offset(1, 0, 0))) {
            blocked++;
        }

        if (isBlockedAround(center.offset(-1, 0, 0))) {
            blocked++;
        }

        if (isBlockedAround(center.offset(0, 0, 1))) {
            blocked++;
        }

        if (isBlockedAround(center.offset(0, 0, -1))) {
            blocked++;
        }

        return blocked;
    }

    private boolean isBlockedAround(BlockPos pos) {
        // On teste au niveau du corps et un peu au-dessus.
        boolean blockedMiddle = !this.level()
                .getBlockState(pos)
                .getCollisionShape(this.level(), pos)
                .isEmpty();

        boolean blockedUpper = !this.level()
                .getBlockState(pos.above())
                .getCollisionShape(this.level(), pos.above())
                .isEmpty();

        return blockedMiddle || blockedUpper;
    }

    private double lerpDouble(double start, double end, double progress) {
        return start + (end - start) * progress;
    }

    private Vec3 getHorizontalMoveDirection() {
        Vec3 motion = this.getDeltaMovement();

        double dx = motion.x;
        double dz = motion.z;

        double length = Math.sqrt(dx * dx + dz * dz);

        if (length > 0.01D) {
            return new Vec3(dx / length, 0.0D, dz / length);
        }

        // Fallback : direction du regard / corps.
        double yawRad = Math.toRadians(this.getYRot());

        return new Vec3(
                -Math.sin(yawRad),
                0.0D,
                Math.cos(yawRad)
        );
    }

    private int getLowestFreeHeightAhead(BlockPos currentGroundFeetPos) {
        Vec3 direction = getHorizontalMoveDirection();

        int lowestFreeHeight = countFreeBlocksAbove(currentGroundFeetPos, 5);

        double[] distances = {
                0.75D,
                1.25D,
                1.75D,
                2.25D,
                2.75D,
                3.25D
        };

        for (double distance : distances) {
            double checkX = this.getX() + direction.x * distance;
            double checkZ = this.getZ() + direction.z * distance;

            BlockPos aheadGroundFeetPos = findGroundBelowAt(checkX, checkZ, this.getY());

            if (aheadGroundFeetPos == null) {
                continue;
            }

            int freeHeight = countFreeBlocksAbove(aheadGroundFeetPos, 5);

            lowestFreeHeight = Math.min(lowestFreeHeight, freeHeight);
        }

        return lowestFreeHeight;
    }

    private BlockPos findGroundBelowAt(double xPos, double zPos, double referenceY) {
        int x = (int) Math.floor(xPos);
        int z = (int) Math.floor(zPos);

        int startY = (int) Math.floor(referenceY + 3.0D);
        int minY = Math.max(this.level().getMinBuildHeight() + 1, startY - 16);

        for (int y = startY; y >= minY; y--) {
            BlockPos feetPos = new BlockPos(x, y, z);
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
}