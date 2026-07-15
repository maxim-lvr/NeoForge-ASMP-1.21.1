package net.maximlvr.asmpthings.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.maximlvr.asmpthings.entity.custom.CubeBossSwordEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class CubeBossSwordRenderer extends EntityRenderer<CubeBossSwordEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/item/iron_sword.png");
    private static final ItemStack SWORD_STACK = new ItemStack(Items.IRON_SWORD);

    private final ItemRenderer itemRenderer;

    public CubeBossSwordRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(
            CubeBossSwordEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        if (entity.isWarning()) {
            renderWarningTile(entity, partialTicks, poseStack, buffer);
        } else if (entity.isActive()) {
            renderSword(entity, partialTicks, poseStack, buffer, packedLight);
        }

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(CubeBossSwordEntity entity) {
        return TEXTURE;
    }

    private void renderWarningTile(CubeBossSwordEntity entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer) {
        float progress = entity.getWarningProgress(partialTicks);
        int alpha = 70 + (int) (120.0F * progress);
        VertexConsumer consumer = buffer.getBuffer(RenderType.debugQuads());
        PoseStack.Pose pose = poseStack.last();

        float min = -0.5F;
        float max = 0.5F;
        float y = 0.0125F;

        consumer.addVertex(pose, min, y, min).setColor(255, 20, 20, alpha);
        consumer.addVertex(pose, min, y, max).setColor(255, 20, 20, alpha);
        consumer.addVertex(pose, max, y, max).setColor(255, 20, 20, alpha);
        consumer.addVertex(pose, max, y, min).setColor(255, 20, 20, alpha);
    }

    private void renderSword(
            CubeBossSwordEntity entity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        float progress = entity.getActiveProgress(partialTicks);

        poseStack.pushPose();
        poseStack.translate(0.0D, -0.45D + progress * 0.95D, 0.0D);
        poseStack.mulPose(Axis.ZP.rotationDegrees(135.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees((entity.getId() * 37) % 360));
        poseStack.scale(1.35F, 1.35F, 1.35F);
        this.itemRenderer.renderStatic(
                SWORD_STACK,
                ItemDisplayContext.FIXED,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId()
        );
        poseStack.popPose();
    }
}
