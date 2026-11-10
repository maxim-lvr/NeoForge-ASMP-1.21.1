package net.maximlvr.asmpthings.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.maximlvr.asmpthings.block.custom.ArmiPlatformBlock;
import net.maximlvr.asmpthings.block.entity.ArmiPlatformBlockEntity;
import net.maximlvr.asmpthings.entity.custom.ArmiBossPlatforms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.Blocks;

public final class ArmiPlatformRenderer implements BlockEntityRenderer<ArmiPlatformBlockEntity> {
    public ArmiPlatformRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(ArmiPlatformBlockEntity entity, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int light, int overlay) {
        var state = entity.getBlockState();
        var minecraft = Minecraft.getInstance();
        if (ArmiBossPlatforms.hidden(state, entity.getBlockPos(), minecraft.player)) return;
        if (!(state.getBlock() instanceof ArmiPlatformBlock platform)) return;
        var wool = platform.color() == 1 ? Blocks.RED_WOOL : Blocks.BLUE_WOOL;
        minecraft.getBlockRenderer().renderSingleBlock(wool.defaultBlockState(), poses, buffers, light, overlay);
    }

    @Override
    public int getViewDistance() { return 192; }
}
