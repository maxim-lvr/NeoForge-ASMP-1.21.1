package net.maximlvr.asmpthings.block.custom;

import net.maximlvr.asmpthings.block.entity.ArmiPlatformBlockEntity;
import net.maximlvr.asmpthings.entity.custom.ArmiBossPlatforms;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ArmiPlatformBlock extends Block implements EntityBlock {
    private final int color;

    public ArmiPlatformBlock(Properties properties, int color) {
        super(properties);
        this.color = color;
    }

    public int color() { return color; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArmiPlatformBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext
                && entityContext.getEntity() instanceof Player player
                && ArmiBossPlatforms.hidden(state, pos, player)) {
            return Shapes.empty();
        }
        return Shapes.block();
    }
}
