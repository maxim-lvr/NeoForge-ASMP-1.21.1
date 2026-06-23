package net.maximlvr.asmpthings.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.client.model.CrawlerHandModel;
import net.maximlvr.asmpthings.entity.custom.CrawlerHandEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public class CrawlerHandRenderer extends EntityRenderer<CrawlerHandEntity> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AsmpThingsMod.MOD_ID,
            "textures/entity/crawler_hand.png"
    );

    private final CrawlerHandModel<CrawlerHandEntity> model;

    public CrawlerHandRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new CrawlerHandModel<>(context.bakeLayer(CrawlerHandModel.LAYER_LOCATION));
        this.shadowRadius = 0.25F;
    }

    @Override
    public void render(CrawlerHandEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        Direction normal = entity.getAttachedNormal();

        // Petit décalage vers l'extérieur de la surface pour éviter que le modèle rentre dans le mur.
        poseStack.translate(
                normal.getStepX() * 0.035D,
                normal.getStepY() * 0.035D,
                normal.getStepZ() * 0.035D
        );

        applySurfaceRotation(poseStack, normal);

        this.model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTicks, 0.0F, 0.0F);

        this.model.renderToBuffer(
                poseStack,
                buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),
                packedLight,
                OverlayTexture.NO_OVERLAY,
                0xFFFFFFFF
        );

        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private void applySurfaceRotation(PoseStack poseStack, Direction normal) {
        switch (normal) {
            case UP -> {
                // Sol : modèle à plat normal.
            }

            case DOWN -> {
                // Plafond : retourné.
                poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            }

            case NORTH -> {
                // Mur nord.
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            }

            case SOUTH -> {
                // Mur sud.
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            }

            case EAST -> {
                // Mur est.
                poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            }

            case WEST -> {
                // Mur ouest.
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(CrawlerHandEntity entity) {
        return TEXTURE;
    }
}