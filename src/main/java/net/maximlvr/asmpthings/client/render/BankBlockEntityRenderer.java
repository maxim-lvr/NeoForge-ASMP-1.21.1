package net.maximlvr.asmpthings.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.maximlvr.asmpthings.block.entity.BankBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;

public class BankBlockEntityRenderer implements BlockEntityRenderer<BankBlockEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public BankBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(BankBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        BakedModel model = blockRenderer.getBlockModel(state);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.solid());
        int softenedLight = softenLight(packedLight);

        blockRenderer.getModelRenderer().renderModel(
                poseStack.last(),
                vertexConsumer,
                state,
                model,
                1.0F,
                1.0F,
                1.0F,
                softenedLight,
                packedOverlay
        );
    }

    private int softenLight(int packedLight) {
        int blockLight = Math.max(LightTexture.block(packedLight), 10);
        int skyLight = Math.max(LightTexture.sky(packedLight), 10);
        return LightTexture.pack(blockLight, skyLight);
    }
}
