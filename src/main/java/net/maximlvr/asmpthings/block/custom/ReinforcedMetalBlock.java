package net.maximlvr.asmpthings.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class ReinforcedMetalBlock extends Block {
    private static final int BREAK_TICKS = 60 * 20;

    public ReinforcedMetalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        // Fixed survival mining time, independent of tools, enchantments and effects.
        return 1.0F / BREAK_TICKS;
    }
}
