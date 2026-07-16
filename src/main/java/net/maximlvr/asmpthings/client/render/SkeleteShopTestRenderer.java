package net.maximlvr.asmpthings.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.block.custom.HorizontalFacingBlock;
import net.maximlvr.asmpthings.block.entity.SkeleteShopTestBlockEntity;
import net.maximlvr.asmpthings.client.model.SkeleteShopWipTestModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class SkeleteShopTestRenderer implements BlockEntityRenderer<SkeleteShopTestBlockEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "textures/entity/keleteshoptest.png");

    private final SkeleteShopWipTestModel model;

    public SkeleteShopTestRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new SkeleteShopWipTestModel(context.bakeLayer(SkeleteShopWipTestModel.LAYER_LOCATION));
    }

    @Override
    public void render(SkeleteShopTestBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = blockEntity.getLevel();
        float ageInTicks = level == null ? 0.0F : level.getGameTime() + partialTick;
        Direction facing = blockEntity.getBlockState().getValue(HorizontalFacingBlock.FACING);

        poseStack.pushPose();
        poseStack.translate(0.5D, 1.5D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.scale(1.0F, -1.0F, -1.0F);

        model.setupAnim(ageInTicks);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutout(TEXTURE));
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, 0xFFFFFFFF);
        poseStack.popPose();
    }
}
