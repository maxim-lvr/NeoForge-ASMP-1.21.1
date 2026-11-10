package net.maximlvr.asmpthings.entity.custom;

import net.maximlvr.asmpthings.network.payload.ArmiBossPathPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ArmiBossPathfinding {
    private ArmiBossPathfinding() {
    }

    public static List<BlockPos> find(ServerPlayer player, ArmiBossEntity boss, BlockPos floor) {
        var level = player.serverLevel();
        BlockPos target = floor.above();
        BlockPos center = boss.blockPosition();
        int half = ArmiBossEntity.ARENA_HALF_SIZE;
        if (Math.abs((long) target.getX() - center.getX()) > half
                || Math.abs((long) target.getZ() - center.getZ()) > half
                || Math.abs((long) target.getY() - player.getBlockY()) > 16
                || !level.hasChunkAt(target) || level.isOutsideBuildHeight(target)
                || !level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                || ArmiBossPlatforms.hidden(level.getBlockState(floor), floor, player)
                || !level.getFluidState(target).isEmpty()) {
            return List.of();
        }
        AABB targetBox = player.getDimensions(Pose.STANDING).makeBoundingBox(Vec3.atBottomCenterOf(target));
        if (!level.getWorldBorder().isWithinBounds(targetBox) || !level.noCollision(player, targetBox)) {
            return List.of();
        }

        // Vanilla walking pathfinder, with an unspawned zombie as a conservative player-sized probe.
        Zombie probe = new Zombie(EntityType.ZOMBIE, level);
        probe.setPos(player.position());
        probe.setOnGround(true);
        AABB bossBox = boss.getBoundingBox().inflate(0.1);
        WalkNodeEvaluator evaluator = new WalkNodeEvaluator() {
            @Override
            public PathType getPathTypeOfMob(PathfindingContext context, int x, int y, int z, Mob mob) {
                AABB bounds = mob.getDimensions(Pose.STANDING).makeBoundingBox(new Vec3(x + 0.5, y, z + 0.5));
                BlockPos support = new BlockPos(x, y - 1, z);
                if (ArmiBossPlatforms.hidden(level.getBlockState(support), support, player)) {
                    return PathType.BLOCKED;
                }
                if (Math.abs((long) x - center.getX()) > half || Math.abs((long) z - center.getZ()) > half
                        || !level.getWorldBorder().isWithinBounds(bounds) || bossBox.intersects(bounds)) {
                    return PathType.BLOCKED;
                }
                return super.getPathTypeOfMob(context, x, y, z, mob);
            }
        };
        evaluator.setCanPassDoors(true);
        PathNavigationRegion region = new PathNavigationRegion(level,
                center.offset(-half, -16, -half), center.offset(half, 16, half));
        Path path = new PathFinder(evaluator, 8192).findPath(region, probe, Set.of(target), 160, 0, 1);
        if (path == null || !path.canReach() || path.getNodeCount() > ArmiBossPathPayload.MAX_NODES) {
            return List.of();
        }
        List<BlockPos> nodes = new ArrayList<>(path.getNodeCount());
        for (int i = 0; i < path.getNodeCount(); i++) {
            nodes.add(path.getNode(i).asBlockPos());
        }
        return nodes;
    }
}
