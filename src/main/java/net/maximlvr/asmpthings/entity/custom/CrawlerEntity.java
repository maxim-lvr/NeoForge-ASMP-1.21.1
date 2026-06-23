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
import net.minecraft.core.Direction;

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

    private boolean climbingWall = false;
    private int climbTicks = 0;

    private int spiderPathMode = 0;
    // 0 = direct
    // 1 = cherche un bord de plateforme
    // 2 = grimpe

    private int spiderPathCooldown = 0;
    private Vec3 cachedSpiderDirection = new Vec3(0.0D, 0.0D, 0.0D);

    private double climbDirX = 0.0D;
    private double climbDirZ = 0.0D;
    private Direction climbSurfaceNormal = Direction.UP;

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
        Vec3 moveDirection = chooseSpiderMoveDirection(player);

        double dirX = moveDirection.x;
        double dirZ = moveDirection.z;

        double horizontalDistance = horizontalDistanceTo(player.getX(), player.getZ());

        if (horizontalDistance < 0.001D) {
            return;
        }

        float targetYaw = (float) (Mth.atan2(dirZ, dirX) * (180.0F / Math.PI)) - 90.0F;

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

        boolean wallInMoveDirection = hasWallInDirection(dirX, dirZ);
        boolean shouldClimb = this.spiderPathMode == 2 || wallInMoveDirection;

        if (shouldClimb) {
            setClimbDirection(dirX, dirZ);

            this.climbingWall = true;
            this.climbTicks = 24;
        }

        if (this.climbTicks > 0) {
            this.climbTicks--;
        } else {
            this.climbingWall = false;
        }

        double yMotion = motion.y;

        if (this.climbingWall) {
            yMotion = 0.20D;
            speed *= 0.55D;
        }

        this.setDeltaMovement(
                dirX * speed,
                yMotion,
                dirZ * speed
        );

        this.hasImpulse = true;

        if (horizontalDistance <= 2.4D && Math.abs(player.getY() - this.getY()) < 2.2D) {
            tryAttack(player);
        }
    }

    private Vec3 chooseSpiderMoveDirection(Player player) {
        if (this.spiderPathCooldown > 0) {
            this.spiderPathCooldown--;

            if (this.cachedSpiderDirection.lengthSqr() > 0.001D) {
                return this.cachedSpiderDirection;
            }
        }

        double dx = player.getX() - this.getX();
        double dz = player.getZ() - this.getZ();

        double length = Math.sqrt(dx * dx + dz * dz);

        if (length < 0.001D) {
            return new Vec3(0.0D, 0.0D, 0.0D);
        }

        Vec3 direct = new Vec3(dx / length, 0.0D, dz / length);

        double yDifference = player.getY() - this.getY();

        // Joueur plus haut : comportement araignée.
        if (yDifference > 2.0D) {
            // Si un mur est proche dans la direction du joueur, on va directement grimper.
            if (hasWallInDirection(direct.x, direct.z) || hasClimbableWallNearDirection(direct)) {
                this.spiderPathMode = 2;
                this.cachedSpiderDirection = direct;
                this.spiderPathCooldown = 8;
                return direct;
            }

            // Si le mob est sous une plateforme, il cherche un bord.
            if (isUnderPlatform()) {
                Vec3 edgeDirection = findPlatformEdgeDirection(direct);

                this.spiderPathMode = 1;
                this.cachedSpiderDirection = edgeDirection;
                this.spiderPathCooldown = 12;
                return edgeDirection;
            }
        }

        this.spiderPathMode = 0;
        this.cachedSpiderDirection = direct;
        this.spiderPathCooldown = 4;

        return direct;
    }

    private double horizontalDistanceTo(double x, double z) {
        double dx = x - this.getX();
        double dz = z - this.getZ();

        return Math.sqrt(dx * dx + dz * dz);
    }

    private boolean isUnderPlatform() {
        BlockPos center = BlockPos.containing(this.getX(), this.getY(), this.getZ());

        for (int y = 1; y <= 5; y++) {
            BlockPos check = center.above(y);

            if (isSolidBlock(check)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasWallInDirection(double dirX, double dirZ) {
        double length = Math.sqrt(dirX * dirX + dirZ * dirZ);

        if (length < 0.001D) {
            return false;
        }

        dirX /= length;
        dirZ /= length;

        double[] distances = {
                0.75D,
                1.1D,
                1.45D
        };

        for (double distance : distances) {
            double checkX = this.getX() + dirX * distance;
            double checkZ = this.getZ() + dirZ * distance;

            BlockPos low = BlockPos.containing(checkX, this.getY() + 0.15D, checkZ);
            BlockPos mid = BlockPos.containing(checkX, this.getY() + 0.9D, checkZ);
            BlockPos high = BlockPos.containing(checkX, this.getY() + 1.7D, checkZ);

            if (isSolidBlock(low) || isSolidBlock(mid) || isSolidBlock(high)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasClimbableWallNearDirection(Vec3 direction) {
        double dirX = direction.x;
        double dirZ = direction.z;

        double length = Math.sqrt(dirX * dirX + dirZ * dirZ);

        if (length < 0.001D) {
            return false;
        }

        dirX /= length;
        dirZ /= length;

        double[] forwardDistances = {
                1.5D,
                2.5D,
                3.5D,
                4.5D,
                5.5D
        };

        double[] sideOffsets = {
                0.0D,
                0.8D,
                -0.8D,
                1.5D,
                -1.5D
        };

        double sideX = -dirZ;
        double sideZ = dirX;

        for (double forward : forwardDistances) {
            for (double side : sideOffsets) {
                double checkX = this.getX() + dirX * forward + sideX * side;
                double checkZ = this.getZ() + dirZ * forward + sideZ * side;

                if (isClimbableWallColumn(checkX, checkZ)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isClimbableWallColumn(double x, double z) {
        int solidCount = 0;

        for (int y = 0; y <= 4; y++) {
            BlockPos check = BlockPos.containing(x, this.getY() + y * 0.75D, z);

            if (isSolidBlock(check)) {
                solidCount++;
            }
        }

        return solidCount >= 2;
    }

    private Vec3 findPlatformEdgeDirection(Vec3 direct) {
        Vec3 left = new Vec3(-direct.z, 0.0D, direct.x);
        Vec3 right = new Vec3(direct.z, 0.0D, -direct.x);

        double leftScore = scoreEdgeDirection(left);
        double rightScore = scoreEdgeDirection(right);

        if (leftScore < rightScore) {
            return left;
        }

        return right;
    }

    private double scoreEdgeDirection(Vec3 direction) {
        double score = 0.0D;

        double[] distances = {
                1.0D,
                2.0D,
                3.0D,
                4.0D,
                5.0D,
                6.0D
        };

        for (double distance : distances) {
            double checkX = this.getX() + direction.x * distance;
            double checkZ = this.getZ() + direction.z * distance;

            BlockPos ground = findGroundBelowAt(checkX, checkZ, this.getY());

            if (ground == null) {
                score += 10.0D;
                continue;
            }

            int freeHeight = countFreeBlocksAbove(ground, 6);

            // Plus il y a de hauteur libre, plus c'est probablement un bord de plateforme.
            if (freeHeight >= 5) {
                score -= 12.0D;
            } else if (freeHeight >= 4) {
                score -= 6.0D;
            } else if (freeHeight >= 3) {
                score -= 2.0D;
            } else {
                score += 2.5D;
            }

            // Si une colonne grimpable est dans cette direction, c'est aussi intéressant.
            if (isClimbableWallColumn(checkX, checkZ)) {
                score -= 5.0D;
            }
        }

        return score;
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
        if (this.climbingWall) {
            return;
        }

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

        if (this.climbingWall) {
            updateClimbingHandComfort(serverLevel, hand, handIndex);
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
        if (!hand.isAttachedToSurface()) {
            if (hand.canStartStep() && canHandStartAlternatedStep(serverLevel, handIndex, true)) {
                hand.moveToSurfaceNearBody(
                        comfortX,
                        this.getY(),
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
                hand.moveToSurfaceNearBody(
                        comfortX,
                        this.getY(),
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
                hand.moveToSurfaceNearBody(
                        comfortX,
                        this.getY(),
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
                hand.moveToSurfaceNearBody(
                        comfortX,
                        this.getY(),
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

    private void updateClimbingHandComfort(ServerLevel serverLevel, CrawlerHandEntity hand, int handIndex) {
        double dirX = this.climbDirX;
        double dirZ = this.climbDirZ;

        double length = Math.sqrt(dirX * dirX + dirZ * dirZ);

        if (length < 0.001D) {
            Vec3 fallback = getHorizontalMoveDirection();
            dirX = fallback.x;
            dirZ = fallback.z;
            length = Math.sqrt(dirX * dirX + dirZ * dirZ);
        }

        if (length < 0.001D) {
            return;
        }

        dirX /= length;
        dirZ /= length;

        double sideX = -dirZ;
        double sideZ = dirX;

        double sideSign = switch (handIndex) {
            case 0, 2 -> -1.0D;
            case 1, 3 -> 1.0D;
            default -> 0.0D;
        };

        double verticalOffset = switch (handIndex) {
            case 0, 1 -> 1.15D;   // mains avant : plus hautes sur le mur
            case 2, 3 -> -0.35D;  // mains arrière : plus basses
            default -> 0.0D;
        };

        double wallDistance = 1.05D;
        double sideSpread = lerpDouble(0.95D, 0.45D, this.compactAmount);

        double comfortX = this.getX() + dirX * wallDistance + sideX * sideSign * sideSpread;
        double comfortY = this.getY() + verticalOffset;
        double comfortZ = this.getZ() + dirZ * wallDistance + sideZ * sideSign * sideSpread;

        double dx = comfortX - hand.getX();
        double dy = comfortY - hand.getY();
        double dz = comfortZ - hand.getZ();

        double distanceToComfort = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (hand.isMoving()) {
            hand.resetDiscomfortTicks();
            return;
        }

        boolean wrongSurface = hand.getAttachedNormal() != this.climbSurfaceNormal;

        if (!hand.isAttachedToSurface() || wrongSurface) {
            if (hand.canStartStep() && canHandStartAlternatedStep(serverLevel, handIndex, true)) {
                hand.moveToSurfaceNearBody(
                        comfortX,
                        comfortY,
                        comfortZ,
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        true,
                        this.climbSurfaceNormal
                );
            }
            return;
        }

        if (distanceToComfort > 0.85D) {
            int ticks = hand.addDiscomfortTick();

            int requiredTicks = 3 + getHandStepDelay(handIndex);

            if (ticks >= requiredTicks && hand.canStartStep() && canHandStartAlternatedStep(serverLevel, handIndex, false)) {
                hand.moveToSurfaceNearBody(
                        comfortX,
                        comfortY,
                        comfortZ,
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        false,
                        this.climbSurfaceNormal
                );
            }

            return;
        }

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

    private boolean hasWallInFront() {
        Vec3 direction = getHorizontalMoveDirection();
        return hasWallInDirection(direction.x, direction.z);
    }

    private boolean isSolidBlock(BlockPos pos) {
        return !this.level()
                .getBlockState(pos)
                .getCollisionShape(this.level(), pos)
                .isEmpty();
    }

    private void setClimbDirection(double dirX, double dirZ) {
        double length = Math.sqrt(dirX * dirX + dirZ * dirZ);

        if (length < 0.001D) {
            Vec3 fallback = getHorizontalMoveDirection();
            dirX = fallback.x;
            dirZ = fallback.z;
            length = Math.sqrt(dirX * dirX + dirZ * dirZ);
        }

        if (length < 0.001D) {
            return;
        }

        this.climbDirX = dirX / length;
        this.climbDirZ = dirZ / length;

        this.climbSurfaceNormal = getWallSurfaceNormalFromMoveDirection(
                this.climbDirX,
                this.climbDirZ
        );
    }

    private Direction getWallSurfaceNormalFromMoveDirection(double dirX, double dirZ) {
        // Si le crawler va vers +X, le mur lui présente sa face WEST.
        if (Math.abs(dirX) > Math.abs(dirZ)) {
            return dirX > 0.0D ? Direction.WEST : Direction.EAST;
        }

        // Si le crawler va vers +Z, le mur lui présente sa face NORTH.
        return dirZ > 0.0D ? Direction.NORTH : Direction.SOUTH;
    }

}