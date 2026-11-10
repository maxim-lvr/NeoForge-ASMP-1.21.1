package net.maximlvr.asmpthings.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.client.model.ArmiBossModel;
import net.maximlvr.asmpthings.client.ArmiBossCinematicClient;
import net.maximlvr.asmpthings.entity.custom.ArmiBossEntity;
import net.maximlvr.asmpthings.entity.custom.ArmiBossGeometry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class ArmiBossRenderer extends EntityRenderer<ArmiBossEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AsmpThingsMod.MOD_ID, "textures/entity/armi_boss.png");
    private final ArmiBossModel model;

    public ArmiBossRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new ArmiBossModel(context.bakeLayer(ArmiBossModel.LAYER_LOCATION));
    }

    @Override
    public void render(ArmiBossEntity entity, float yaw, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int packedLight) {
        poses.pushPose();
        poses.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        model.setupAnim(entity, 0, 0, entity.tickCount + partialTick, 0, 0);
        // The imported model already contains the three different hands.
        model.setEyeArmVisible(!ArmiBossCinematicClient.isViewingFrom(entity));
        renderBoss(poses, buffers, packedLight);
        model.setEyeArmVisible(true);
        poses.popPose();
        var rescued = entity.level().getEntity(entity.rescuedPlayerId());
        if (rescued != null) {
            var offset = rescued.getPosition(partialTick).subtract(entity.getPosition(partialTick));
            poses.pushPose();
            poses.translate(offset.x, offset.y - 0.35, offset.z);
            poses.mulPose(Axis.YP.rotationDegrees(180 - yaw));
            poses.mulPose(Axis.XP.rotationDegrees(90));
            float scale = ArmiBossGeometry.SCALE * 0.25F;
            poses.scale(scale, scale, scale);
            // Render only the fourth palm and fingers, face up beneath the carried player.
            poses.translate(-1.0 / 16, 3.5 / 16, 0);
            var hand = model.eyeHand();
            poses.translate(-hand.x / 16.0, -hand.y / 16.0, -hand.z / 16.0);
            hand.render(poses, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),
                    packedLight, OverlayTexture.NO_OVERLAY, -1);
            poses.popPose();
        }
        super.render(entity, yaw, partialTick, poses, buffers, packedLight);
    }

    private void renderBoss(PoseStack poses, MultiBufferSource buffers, int packedLight) {
        poses.pushPose();
        double palmY = (ArmiBossGeometry.OFFSET_Y - 11.5 / 16.0) * ArmiBossGeometry.SCALE;
        poses.translate(0, palmY, 0);
        // Roll around the palm like a clock: reverse up/down without reversing its face.
        poses.mulPose(Axis.ZP.rotationDegrees(180));
        poses.scale(ArmiBossGeometry.SCALE, ArmiBossGeometry.SCALE, ArmiBossGeometry.SCALE);
        poses.translate(0, 11.5 / 16.0, 0);
        model.renderToBuffer(poses, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),
                packedLight, OverlayTexture.NO_OVERLAY, -1);
        poses.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(ArmiBossEntity entity) { return TEXTURE; }
}
