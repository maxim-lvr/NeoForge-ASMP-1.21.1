package net.maximlvr.asmpthings.block.custom;

import net.maximlvr.asmpthings.world.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class DeepslatePortalBlock extends Block {

    public DeepslatePortalBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide()) {
            return;
        }

        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        ServerLevel currentLevel = player.serverLevel();

        if (currentLevel.dimension().equals(ModDimensions.ASMP_DIMENSION_KEY)) {
            teleportBackToSpawn(player);
        } else {
            teleportToAsmpDimension(player);
        }
    }

    private void teleportToAsmpDimension(ServerPlayer player) {
        ServerLevel targetLevel = player.server.getLevel(ModDimensions.ASMP_DIMENSION_KEY);

        if (targetLevel == null) {
            return;
        }

        BlockPos targetPos = new BlockPos(0, 100, 0);

        prepareSafePlatform(targetLevel, targetPos);

        player.teleportTo(
                targetLevel,
                targetPos.getX() + 0.5D,
                targetPos.getY(),
                targetPos.getZ() + 0.5D,
                player.getYRot(),
                player.getXRot()
        );
    }

    private void teleportBackToSpawn(ServerPlayer player) {
        ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);

        if (overworld == null) {
            return;
        }

        BlockPos spawnPos = player.getRespawnPosition();

        if (spawnPos == null) {
            spawnPos = overworld.getSharedSpawnPos();
        }

        prepareSafePlatform(overworld, spawnPos);

        player.teleportTo(
                overworld,
                spawnPos.getX() + 0.5D,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5D,
                player.getYRot(),
                player.getXRot()
        );
    }

    private void prepareSafePlatform(ServerLevel level, BlockPos pos) {
        BlockPos groundPos = pos.below();

        level.setBlock(groundPos, Blocks.DEEPSLATE.defaultBlockState(), 3);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
    }
}