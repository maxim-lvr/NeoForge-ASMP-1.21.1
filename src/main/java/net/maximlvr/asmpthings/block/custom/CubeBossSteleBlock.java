package net.maximlvr.asmpthings.block.custom;

import net.maximlvr.asmpthings.entity.ModEntities;
import net.maximlvr.asmpthings.entity.custom.CubeBossEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

public class CubeBossSteleBlock extends HorizontalFacingBlock {
    private static final int BOSS_DISTANCE = 14;
    private static final int BOSS_Y_OFFSET = 3;

    public CubeBossSteleBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (level.isClientSide || state.is(oldState.getBlock()) || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        spawnBoss(serverLevel, pos, state.getValue(FACING));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            boolean spawned = spawnBoss(serverLevel, pos, state.getValue(FACING));
            player.displayClientMessage(Component.literal(spawned ? "Cube Boss invoque." : "Un Cube Boss est deja present."), true);
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private boolean spawnBoss(ServerLevel level, BlockPos stelePos, Direction direction) {
        AABB lookupBox = new AABB(stelePos).inflate(128.0D, 64.0D, 128.0D);
        boolean bossAlreadyPresent = !level.getEntitiesOfClass(CubeBossEntity.class, lookupBox, CubeBossEntity::isAlive).isEmpty();

        if (bossAlreadyPresent) {
            return false;
        }

        CubeBossEntity boss = ModEntities.CUBE_BOSS.get().create(level);

        if (boss == null) {
            return false;
        }

        BlockPos bossBlockPos = stelePos.relative(direction, BOSS_DISTANCE).above(BOSS_Y_OFFSET);
        double bossX = bossBlockPos.getX() + 0.5D;
        double bossY = bossBlockPos.getY();
        double bossZ = bossBlockPos.getZ() + 0.5D;
        float yaw = getYawTowardStele(bossX, bossZ, stelePos);

        boss.setStelePos(stelePos);
        boss.moveTo(bossX, bossY, bossZ, yaw, 0.0F);
        boss.setYHeadRot(yaw);
        boss.setYBodyRot(yaw);
        level.addFreshEntity(boss);
        return true;
    }

    private float getYawTowardStele(double bossX, double bossZ, BlockPos stelePos) {
        double dx = stelePos.getX() + 0.5D - bossX;
        double dz = stelePos.getZ() + 0.5D - bossZ;
        return (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG);
    }
}
