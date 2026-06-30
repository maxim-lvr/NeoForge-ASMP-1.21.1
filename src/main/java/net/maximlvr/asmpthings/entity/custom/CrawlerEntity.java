package net.maximlvr.asmpthings.entity.custom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.maximlvr.asmpthings.entity.ModEntities;
import net.minecraft.world.entity.Entity;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.minecraft.core.Direction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class CrawlerEntity extends Monster {
    private static final EntityDataAccessor<Boolean> DATA_DEBUG_PATH =
            SynchedEntityData.defineId(CrawlerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_DEBUG_PATH_POINTS =
            SynchedEntityData.defineId(CrawlerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_SURFACE_NORMAL =
            SynchedEntityData.defineId(CrawlerEntity.class, EntityDataSerializers.INT);

    private static final int SURFACE_PATH_RECALCULATE_TICKS = 34;
    private static final int SURFACE_PATH_MAX_NODES = 1200;
    private static final int SURFACE_PATH_MAX_DISTANCE = 34;
    private static final int SURFACE_PATH_DEBUG_POINTS = 16;
    private static final double SURFACE_WAYPOINT_REACHED_DISTANCE = 0.85D;
    private static final int SURFACE_TARGET_REPATH_DISTANCE = 4;
    private static final int SURFACE_NORMAL_CONFIRM_TICKS = 6;
    private static final double STANDING_BODY_HEIGHT = 2.85D;
    private static final double THREE_BLOCK_BODY_HEIGHT = 1.35D;
    private static final double TWO_BLOCK_BODY_HEIGHT = 0.18D;
    private static final double COMPACT_BODY_HEIGHT = 0.95D;
    private static final boolean USE_PROCEDURAL_HANDS = false;
    private static final double SURFACE_PROJECTION_DISTANCE = 1.35D;
    private static final int SURFACE_SWITCH_CONFIRM_TICKS = 8;
    private static final int SURFACE_SWITCH_LOCK_TICKS = 14;

    private static boolean debugCrawlerPaths = false;

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
    private int surfacePathCooldown = 0;
    private List<SurfaceNode> currentSurfacePath = List.of();
    private int surfacePathIndex = 1;
    private BlockPos lastSurfacePathTargetPos = null;
    private double surfacePathYMotion = 0.0D;
    private Direction currentSurfaceNormal = Direction.UP;
    private Direction pendingSurfaceNormal = Direction.UP;
    private int pendingSurfaceNormalTicks = 0;
    private Direction pendingMovementSurface = Direction.UP;
    private int pendingMovementSurfaceTicks = 0;
    private int surfaceSwitchLockTicks = 0;

    public CrawlerEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    public static boolean isDebugCrawlerPaths() {
        return debugCrawlerPaths;
    }

    public static void setDebugCrawlerPaths(boolean enabled) {
        debugCrawlerPaths = enabled;
    }

    public boolean shouldRenderDebugPath() {
        return this.entityData.get(DATA_DEBUG_PATH);
    }

    public String getDebugPathPoints() {
        return this.entityData.get(DATA_DEBUG_PATH_POINTS);
    }

    public Direction getSurfaceNormal() {
        return Direction.from3DDataValue(this.entityData.get(DATA_SURFACE_NORMAL));
    }

    public boolean shouldRenderProceduralHands() {
        return USE_PROCEDURAL_HANDS;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_DEBUG_PATH, false);
        builder.define(DATA_DEBUG_PATH_POINTS, "");
        builder.define(DATA_SURFACE_NORMAL, Direction.UP.get3DDataValue());
    }

    private void ensureHandsSpawned() {
        if (!USE_PROCEDURAL_HANDS) {
            removeDisabledHands();
            return;
        }

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

        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this,
                Villager.class,
                true
        ));
    }

    private void chasePlayer(LivingEntity target) {
        this.surfacePathYMotion = 0.0D;
        Vec3 moveDirection = chooseSurfaceProjectionDirection(target);

        double dirX = moveDirection.x;
        double dirZ = moveDirection.z;
        double directionLengthSqr = dirX * dirX + dirZ * dirZ;

        double horizontalDistance = horizontalDistanceTo(target.getX(), target.getZ());

        if (horizontalDistance < 0.001D && Math.abs(this.surfacePathYMotion) < 0.001D) {
            return;
        }

        if (directionLengthSqr > 0.001D) {
            float targetYaw = (float) (Mth.atan2(dirZ, dirX) * (180.0F / Math.PI)) - 90.0F;

            float smoothedYaw = Mth.approachDegrees(this.getYRot(), targetYaw, 10.0F);

            this.setYRot(smoothedYaw);
            this.yBodyRot = smoothedYaw;
            this.yHeadRot = smoothedYaw;
        }

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

        boolean wallInMoveDirection = hasLowBlockingWallInDirection(dirX, dirZ);
        boolean shouldClimb = this.spiderPathMode == 2 || wallInMoveDirection || Math.abs(this.surfacePathYMotion) > 0.001D;

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

        if (Math.abs(this.surfacePathYMotion) > 0.001D) {
            yMotion = this.surfacePathYMotion;
            speed *= 0.62D;
        } else if (this.climbingWall) {
            yMotion = 0.20D;
            speed *= 0.55D;
        }

        this.setDeltaMovement(
                dirX * speed,
                yMotion,
                dirZ * speed
        );

        this.hasImpulse = true;

        if (horizontalDistance <= 2.4D && Math.abs(target.getY() - this.getY()) < 2.2D) {
            tryAttack(target);
        }
    }

    private Vec3 chooseSurfaceProjectionDirection(LivingEntity target) {
        Direction surface = this.currentSurfaceNormal;

        if (this.surfaceSwitchLockTicks > 0) {
            this.surfaceSwitchLockTicks--;
        }

        if (!isSurfaceStillSupported(surface)) {
            surface = Direction.UP;
            this.currentSurfaceNormal = Direction.UP;
            this.pendingMovementSurface = Direction.UP;
            this.pendingMovementSurfaceTicks = 0;
        }

        Vec3 projectedDirection = projectTargetDirectionOnSurface(target, surface);

        if (projectedDirection.lengthSqr() < 0.001D) {
            if (surface.getAxis().isHorizontal() && Math.abs(target.getY() - this.getY()) > 0.35D) {
                this.surfacePathYMotion = Mth.clamp((target.getY() - this.getY()) * 0.12D, -0.18D, 0.22D);
                updateSurfaceNormal(surface);
                updateProjectionDebugPath(target, surface, Vec3.ZERO);
                return Vec3.ZERO;
            }

            updateSurfaceNormal(surface);
            return Vec3.ZERO;
        }

        Direction forcedSurface = stabilizeSurfaceChoice(surface, findForcedSurfaceChange(surface, projectedDirection));

        if (forcedSurface != surface) {
            surface = forcedSurface;
            projectedDirection = projectTargetDirectionOnSurface(target, surface);
        }

        updateSurfaceNormal(surface);
        this.spiderPathMode = surface == Direction.UP ? 0 : 2;

        if (surface == Direction.UP) {
            this.surfacePathYMotion = 0.0D;
        } else if (surface == Direction.DOWN) {
            this.surfacePathYMotion = 0.0D;
        } else {
            double dy = target.getY() - this.getY();
            this.surfacePathYMotion = Mth.clamp(dy * 0.12D, -0.18D, 0.22D);
        }

        this.cachedSpiderDirection = projectedDirection;
        updateProjectionDebugPath(target, surface, projectedDirection);
        return projectedDirection;
    }

    private Direction stabilizeSurfaceChoice(Direction currentSurface, Direction wantedSurface) {
        if (wantedSurface == currentSurface) {
            this.pendingMovementSurface = wantedSurface;
            this.pendingMovementSurfaceTicks = 0;
            return currentSurface;
        }

        if (this.surfaceSwitchLockTicks > 0 && isSurfaceStillSupported(currentSurface)) {
            return currentSurface;
        }

        if (wantedSurface != this.pendingMovementSurface) {
            this.pendingMovementSurface = wantedSurface;
            this.pendingMovementSurfaceTicks = 1;
            return currentSurface;
        }

        this.pendingMovementSurfaceTicks++;

        if (this.pendingMovementSurfaceTicks < SURFACE_SWITCH_CONFIRM_TICKS) {
            return currentSurface;
        }

        this.surfaceSwitchLockTicks = SURFACE_SWITCH_LOCK_TICKS;
        this.pendingMovementSurfaceTicks = 0;
        return wantedSurface;
    }

    private Vec3 projectTargetDirectionOnSurface(LivingEntity target, Direction surface) {
        double dx = target.getX() - this.getX();
        double dy = target.getY() - this.getY();
        double dz = target.getZ() - this.getZ();

        if (surface == Direction.UP || surface == Direction.DOWN) {
            return normalizeHorizontal(dx, dz);
        }

        if (surface.getAxis() == Direction.Axis.X) {
            return normalizeHorizontal(0.0D, dz);
        }

        return normalizeHorizontal(dx, 0.0D);
    }

    private Vec3 normalizeHorizontal(double dx, double dz) {
        double length = Math.sqrt(dx * dx + dz * dz);

        if (length < 0.001D) {
            return Vec3.ZERO;
        }

        return new Vec3(dx / length, 0.0D, dz / length);
    }

    private Direction findForcedSurfaceChange(Direction surface, Vec3 projectedDirection) {
        if (surface == Direction.UP) {
            if (canStayOnFloor(projectedDirection)) {
                return Direction.UP;
            }

            if (hasWallInDirection(projectedDirection.x, projectedDirection.z)) {
                return getWallSurfaceNormalFromMoveDirection(projectedDirection.x, projectedDirection.z);
            }

            return Direction.UP;
        }

        if (surface == Direction.DOWN) {
            return isCeilingSupportedAhead(projectedDirection) ? Direction.DOWN : Direction.UP;
        }

        if (isWallSupportedAhead(surface, projectedDirection)) {
            return surface;
        }

        BlockPos ground = findGroundBelowBody();

        if (ground != null && countFreeBlocksAbove(ground, 3) >= 2) {
            return Direction.UP;
        }

        return surface;
    }

    private boolean canStayOnFloor(Vec3 direction) {
        double checkX = this.getX() + direction.x * SURFACE_PROJECTION_DISTANCE;
        double checkZ = this.getZ() + direction.z * SURFACE_PROJECTION_DISTANCE;
        BlockPos groundFeetPos = findGroundBelowAt(checkX, checkZ, this.getY());

        if (groundFeetPos == null) {
            return false;
        }

        return countFreeBlocksAbove(groundFeetPos, 3) >= 2
                && !hasLowBlockingWallInDirection(direction.x, direction.z);
    }

    private boolean hasLowBlockingWallInDirection(double dirX, double dirZ) {
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
            BlockPos mid = BlockPos.containing(checkX, this.getY() + 0.85D, checkZ);

            if (isSolidBlock(low) || isSolidBlock(mid)) {
                return true;
            }
        }

        return false;
    }

    private boolean isWallSupportedAhead(Direction surface, Vec3 direction) {
        BlockPos current = BlockPos.containing(this.position());
        BlockPos ahead = BlockPos.containing(
                this.getX() + direction.x * SURFACE_PROJECTION_DISTANCE,
                this.getY() + this.surfacePathYMotion * 3.0D,
                this.getZ() + direction.z * SURFACE_PROJECTION_DISTANCE
        );

        return hasSurfaceSupport(current, surface) || hasSurfaceSupport(ahead, surface);
    }

    private boolean isCeilingSupportedAhead(Vec3 direction) {
        BlockPos ahead = BlockPos.containing(
                this.getX() + direction.x * SURFACE_PROJECTION_DISTANCE,
                this.getY(),
                this.getZ() + direction.z * SURFACE_PROJECTION_DISTANCE
        );

        return hasSurfaceSupport(ahead, Direction.DOWN);
    }

    private boolean isSurfaceStillSupported(Direction surface) {
        if (surface == Direction.UP) {
            return findGroundBelowBody() != null;
        }

        return hasSurfaceSupport(BlockPos.containing(this.position()), surface);
    }

    private boolean hasSurfaceSupport(BlockPos airPos, Direction surface) {
        Direction supportDirection = surface.getOpposite();

        return isSolidBlock(airPos.relative(supportDirection))
                || isSolidBlock(airPos.above().relative(supportDirection))
                || isSolidBlock(airPos.below().relative(supportDirection));
    }

    private void updateProjectionDebugPath(LivingEntity target, Direction surface, Vec3 direction) {
        this.entityData.set(DATA_DEBUG_PATH, debugCrawlerPaths);

        if (!debugCrawlerPaths) {
            this.entityData.set(DATA_DEBUG_PATH_POINTS, "");
            return;
        }

        Vec3 start = this.position();
        Vec3 projection = start.add(
                direction.x * SURFACE_PROJECTION_DISTANCE * 4.0D,
                this.surfacePathYMotion * 8.0D,
                direction.z * SURFACE_PROJECTION_DISTANCE * 4.0D
        );

        this.entityData.set(
                DATA_DEBUG_PATH_POINTS,
                encodeDebugPoint(start, surface)
                        + ";"
                        + encodeDebugPoint(projection, surface)
                        + ";"
                        + encodeDebugPoint(target.position(), Direction.UP)
        );
    }

    private String encodeDebugPoint(Vec3 point, Direction surface) {
        return (int) Math.floor(point.x)
                + ","
                + (int) Math.floor(point.y)
                + ","
                + (int) Math.floor(point.z)
                + ","
                + surface.get3DDataValue();
    }

    private Vec3 chooseSpiderMoveDirection(LivingEntity target) {
        Vec3 pathDirection = chooseSurfacePathDirection(target);

        if (pathDirection.lengthSqr() > 0.001D) {
            return pathDirection;
        }

        if (this.spiderPathCooldown > 0) {
            this.spiderPathCooldown--;

            if (this.cachedSpiderDirection.lengthSqr() > 0.001D) {
                return this.cachedSpiderDirection;
            }
        }

        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();

        double length = Math.sqrt(dx * dx + dz * dz);

        if (length < 0.001D) {
            return new Vec3(0.0D, 0.0D, 0.0D);
        }

        Vec3 direct = new Vec3(dx / length, 0.0D, dz / length);

        double yDifference = target.getY() - this.getY();

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

    private Vec3 chooseSurfacePathDirection(LivingEntity target) {
        if (shouldRecalculateSurfacePath(target)) {
            this.currentSurfacePath = findSurfacePath(target);
            this.surfacePathIndex = this.currentSurfacePath.size() > 1 ? 1 : 0;
            this.lastSurfacePathTargetPos = BlockPos.containing(target.position());
            this.surfacePathCooldown = SURFACE_PATH_RECALCULATE_TICKS + this.random.nextInt(8);
            updateDebugPathData(target);
        } else if (this.surfacePathCooldown > 0) {
            this.surfacePathCooldown--;
            syncDebugToggleWithoutRepath(target);
        }

        if (this.currentSurfacePath.size() < 2) {
            updateSurfaceNormal(Direction.UP);
            return Vec3.ZERO;
        }

        SurfaceNode next = selectNextSurfaceWaypoint();

        if (next == null) {
            updateSurfaceNormal(Direction.UP);
            return Vec3.ZERO;
        }

        updateSurfaceNormal(next.normal());

        Vec3 targetPoint = nodeCenter(next);
        double dx = targetPoint.x - this.getX();
        double dy = targetPoint.y - this.getY();
        double dz = targetPoint.z - this.getZ();
        double horizontalLength = Math.sqrt(dx * dx + dz * dz);
        this.surfacePathYMotion = calculateSurfacePathYMotion(dy);

        if (dy > 0.35D || next.normal() != Direction.UP) {
            this.spiderPathMode = 2;
        }

        if (horizontalLength < 0.18D) {
            if (Math.abs(dy) > 0.35D) {
                Vec3 fallback = getHorizontalMoveDirection();
                return fallback.lengthSqr() > 0.001D ? fallback : Vec3.ZERO;
            }

            return Vec3.ZERO;
        }

        this.cachedSpiderDirection = new Vec3(dx / horizontalLength, 0.0D, dz / horizontalLength);
        return this.cachedSpiderDirection;
    }

    private double calculateSurfacePathYMotion(double dy) {
        if (Math.abs(dy) < 0.18D) {
            return 0.0D;
        }

        double wanted = dy * 0.18D;

        return Mth.clamp(wanted, -0.22D, 0.24D);
    }

    private boolean shouldRecalculateSurfacePath(LivingEntity target) {
        if (this.currentSurfacePath.size() < 2) {
            return this.surfacePathCooldown <= 0;
        }

        if (this.surfacePathIndex >= this.currentSurfacePath.size()) {
            return true;
        }

        SurfaceNode currentWaypoint = this.currentSurfacePath.get(this.surfacePathIndex);

        if (createSurfaceNode(currentWaypoint.airPos()) == null) {
            return true;
        }

        BlockPos targetPos = BlockPos.containing(target.position());

        if (this.lastSurfacePathTargetPos == null) {
            return true;
        }

        boolean targetMovedFar = this.lastSurfacePathTargetPos.distManhattan(targetPos) >= SURFACE_TARGET_REPATH_DISTANCE;

        return targetMovedFar && this.surfacePathCooldown <= 0;
    }

    private SurfaceNode selectNextSurfaceWaypoint() {
        if (this.surfacePathIndex <= 0) {
            this.surfacePathIndex = 1;
        }

        while (this.surfacePathIndex < this.currentSurfacePath.size() - 1) {
            SurfaceNode node = this.currentSurfacePath.get(this.surfacePathIndex);
            Vec3 center = nodeCenter(node);
            double dx = center.x - this.getX();
            double dy = center.y - this.getY();
            double dz = center.z - this.getZ();
            double distanceSqr = dx * dx + dy * dy + dz * dz;

            if (distanceSqr > SURFACE_WAYPOINT_REACHED_DISTANCE * SURFACE_WAYPOINT_REACHED_DISTANCE) {
                break;
            }

            this.surfacePathIndex++;
        }

        return this.currentSurfacePath.get(Math.min(this.surfacePathIndex, this.currentSurfacePath.size() - 1));
    }

    private void updateSurfaceNormal(Direction wantedNormal) {
        if (wantedNormal == this.currentSurfaceNormal) {
            this.pendingSurfaceNormal = wantedNormal;
            this.pendingSurfaceNormalTicks = 0;
            this.entityData.set(DATA_SURFACE_NORMAL, this.currentSurfaceNormal.get3DDataValue());
            return;
        }

        if (wantedNormal != this.pendingSurfaceNormal) {
            this.pendingSurfaceNormal = wantedNormal;
            this.pendingSurfaceNormalTicks = 1;
            return;
        }

        this.pendingSurfaceNormalTicks++;

        if (this.pendingSurfaceNormalTicks >= SURFACE_NORMAL_CONFIRM_TICKS || wantedNormal == Direction.UP) {
            this.currentSurfaceNormal = wantedNormal;
            this.pendingSurfaceNormalTicks = 0;
            this.entityData.set(DATA_SURFACE_NORMAL, this.currentSurfaceNormal.get3DDataValue());
        }
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

    private void tryAttack(LivingEntity target) {
        if (this.attackCooldown > 0) {
            return;
        }

        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return;
        }

        this.doHurtTarget(target);
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

        if (USE_PROCEDURAL_HANDS) {
            updateHandsComfort(serverLevel);
        }
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

        int currentFreeHeight = countFreeBlocksAbove(groundFeetPos, 6);
        int aheadFreeHeight = getLowestFreeHeightAhead(groundFeetPos);
        int effectiveFreeHeight = Math.min(currentFreeHeight, aheadFreeHeight);

        double targetHeight = getBodyHeightForFreeHeight(effectiveFreeHeight);

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

    private void removeDisabledHands() {
        if (this.level().isClientSide()) {
            return;
        }

        removeHand(this.frontLeftHandId);
        removeHand(this.frontRightHandId);
        removeHand(this.backLeftHandId);
        removeHand(this.backRightHandId);

        this.frontLeftHandId = null;
        this.frontRightHandId = null;
        this.backLeftHandId = null;
        this.backRightHandId = null;
        this.handsSpawned = false;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ensureHandsSpawned();

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        updateBodyHeightAndHands(serverLevel);

        LivingEntity nearestTarget = findNearestCrawlerTarget(serverLevel);

        if (nearestTarget == null) {
            return;
        }

        this.setTarget(nearestTarget);
        chasePlayer(nearestTarget);
    }

    private LivingEntity findNearestCrawlerTarget(ServerLevel serverLevel) {
        LivingEntity nearest = null;
        double nearestDistance = 32.0D * 32.0D;

        Player player = serverLevel.getNearestPlayer(this, 32.0D);

        if (player != null && !player.isCreative() && !player.isSpectator()) {
            nearest = player;
            nearestDistance = this.distanceToSqr(player);
        }

        List<Villager> villagers = serverLevel.getEntitiesOfClass(
                Villager.class,
                this.getBoundingBox().inflate(32.0D),
                villager -> villager.isAlive() && !villager.isBaby()
        );

        for (Villager villager : villagers) {
            double distance = this.distanceToSqr(villager);

            if (distance < nearestDistance) {
                nearest = villager;
                nearestDistance = distance;
            }
        }

        return nearest;
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

        int lowestFreeHeight = countFreeBlocksAbove(currentGroundFeetPos, 6);

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

            int freeHeight = countFreeBlocksAbove(aheadGroundFeetPos, 6);

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

    private List<SurfaceNode> findSurfacePath(LivingEntity target) {
        SurfaceNode start = findNearestSurfaceNode(BlockPos.containing(this.position()), 3);
        SurfaceNode goal = findNearestSurfaceNode(BlockPos.containing(target.position()), 5);

        if (start == null || goal == null) {
            return List.of();
        }

        PriorityQueue<PathRecord> open = new PriorityQueue<>(Comparator.comparingDouble(PathRecord::estimatedTotalCost));
        Map<SurfaceNode, Double> costByNode = new HashMap<>();
        Map<SurfaceNode, SurfaceNode> cameFrom = new HashMap<>();
        Set<SurfaceNode> closed = new HashSet<>();

        costByNode.put(start, 0.0D);
        open.add(new PathRecord(start, 0.0D, surfaceHeuristic(start, goal)));

        int visited = 0;

        while (!open.isEmpty() && visited < SURFACE_PATH_MAX_NODES) {
            PathRecord current = open.poll();

            if (!closed.add(current.node())) {
                continue;
            }

            visited++;

            if (current.node().airPos().distManhattan(goal.airPos()) <= 1) {
                return reconstructPath(cameFrom, current.node());
            }

            for (SurfaceNode neighbor : getSurfaceNeighbors(current.node(), start.airPos())) {
                if (closed.contains(neighbor)) {
                    continue;
                }

                double stepCost = current.node().normal() == neighbor.normal() ? 1.0D : 1.7D;
                double newCost = costByNode.get(current.node()) + stepCost;

                if (newCost >= costByNode.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    continue;
                }

                costByNode.put(neighbor, newCost);
                cameFrom.put(neighbor, current.node());
                open.add(new PathRecord(neighbor, newCost, newCost + surfaceHeuristic(neighbor, goal)));
            }
        }

        return List.of();
    }

    private SurfaceNode findNearestSurfaceNode(BlockPos center, int radius) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(center);
        visited.add(center);

        while (!queue.isEmpty()) {
            BlockPos pos = queue.removeFirst();
            SurfaceNode node = createSurfaceNode(pos);

            if (node != null) {
                return node;
            }

            if (center.distManhattan(pos) >= radius) {
                continue;
            }

            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);

                if (visited.add(next)) {
                    queue.add(next);
                }
            }
        }

        return null;
    }

    private List<SurfaceNode> getSurfaceNeighbors(SurfaceNode node, BlockPos startPos) {
        List<SurfaceNode> neighbors = new ArrayList<>();

        for (Direction direction : Direction.values()) {
            BlockPos nextPos = node.airPos().relative(direction);

            if (nextPos.distManhattan(startPos) > SURFACE_PATH_MAX_DISTANCE) {
                continue;
            }

            SurfaceNode next = createSurfaceNode(nextPos);

            if (next != null) {
                neighbors.add(next);
            }
        }

        return neighbors;
    }

    private SurfaceNode createSurfaceNode(BlockPos airPos) {
        if (isSolidBlock(airPos)) {
            return null;
        }

        Direction bestNormal = null;

        for (Direction normal : Direction.values()) {
            if (isSolidBlock(airPos.relative(normal.getOpposite()))) {
                if (normal == Direction.UP && countFreeBlocksAbove(airPos, 3) < 2) {
                    continue;
                }

                bestNormal = normal;

                if (normal == Direction.UP) {
                    break;
                }
            }
        }

        if (bestNormal == null) {
            return null;
        }

        return new SurfaceNode(airPos.immutable(), bestNormal);
    }

    private double surfaceHeuristic(SurfaceNode from, SurfaceNode to) {
        return Math.abs(from.airPos().getX() - to.airPos().getX())
                + Math.abs(from.airPos().getY() - to.airPos().getY()) * 1.25D
                + Math.abs(from.airPos().getZ() - to.airPos().getZ());
    }

    private List<SurfaceNode> reconstructPath(Map<SurfaceNode, SurfaceNode> cameFrom, SurfaceNode end) {
        List<SurfaceNode> path = new ArrayList<>();
        SurfaceNode current = end;

        path.add(current);

        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            path.add(0, current);
        }

        return path;
    }

    private Vec3 nodeCenter(SurfaceNode node) {
        if (node.normal() == Direction.UP) {
            int freeHeight = countFreeBlocksAbove(node.airPos(), 6);
            return new Vec3(
                    node.airPos().getX() + 0.5D,
                    node.airPos().getY() + getBodyHeightForFreeHeight(freeHeight),
                    node.airPos().getZ() + 0.5D
            );
        }

        return Vec3.atCenterOf(node.airPos());
    }

    private double getBodyHeightForFreeHeight(int freeHeight) {
        if (freeHeight <= 2) {
            return TWO_BLOCK_BODY_HEIGHT;
        }

        if (freeHeight == 3) {
            return THREE_BLOCK_BODY_HEIGHT;
        }

        return lerpDouble(STANDING_BODY_HEIGHT, COMPACT_BODY_HEIGHT, this.compactAmount);
    }

    private void updateDebugPathData(LivingEntity target) {
        this.entityData.set(DATA_DEBUG_PATH, debugCrawlerPaths);

        if (!debugCrawlerPaths || this.currentSurfacePath.isEmpty()) {
            if (debugCrawlerPaths && target != null) {
                this.entityData.set(DATA_DEBUG_PATH_POINTS, encodeFallbackDebugPath(target));
            } else {
                this.entityData.set(DATA_DEBUG_PATH_POINTS, "");
            }
            return;
        }

        StringBuilder builder = new StringBuilder();
        int count = Math.min(SURFACE_PATH_DEBUG_POINTS, this.currentSurfacePath.size());

        for (int i = 0; i < count; i++) {
            SurfaceNode node = this.currentSurfacePath.get(i);

            if (i > 0) {
                builder.append(';');
            }

            Vec3 debugPoint = nodeCenter(node);

            builder.append((int) Math.floor(debugPoint.x))
                    .append(',')
                    .append((int) Math.floor(debugPoint.y))
                    .append(',')
                    .append((int) Math.floor(debugPoint.z))
                    .append(',')
                    .append(node.normal().get3DDataValue());
        }

        this.entityData.set(DATA_DEBUG_PATH_POINTS, builder.toString());
    }

    private void syncDebugToggleWithoutRepath(LivingEntity target) {
        if (this.entityData.get(DATA_DEBUG_PATH) == debugCrawlerPaths) {
            return;
        }

        updateDebugPathData(target);
    }

    private String encodeFallbackDebugPath(LivingEntity target) {
        BlockPos start = BlockPos.containing(this.position());
        BlockPos end = BlockPos.containing(target.position());

        return start.getX() + "," + start.getY() + "," + start.getZ() + "," + Direction.UP.get3DDataValue()
                + ";"
                + end.getX() + "," + end.getY() + "," + end.getZ() + "," + Direction.UP.get3DDataValue();
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

    private record SurfaceNode(BlockPos airPos, Direction normal) {
    }

    private record PathRecord(SurfaceNode node, double cost, double estimatedTotalCost) {
    }

}
