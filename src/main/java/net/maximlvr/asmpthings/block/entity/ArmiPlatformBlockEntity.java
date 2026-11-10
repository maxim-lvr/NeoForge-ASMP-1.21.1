package net.maximlvr.asmpthings.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class ArmiPlatformBlockEntity extends BlockEntity {
    public ArmiPlatformBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARMI_PLATFORM.get(), pos, state);
    }
}
