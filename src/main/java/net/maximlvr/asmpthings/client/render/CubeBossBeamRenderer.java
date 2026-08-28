package net.maximlvr.asmpthings.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.entity.custom.CubeBossBeamEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public class CubeBossBeamRenderer extends EntityRenderer<CubeBossBeamEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AsmpThingsMod.MOD_ID,
            "textures/entity/beam_cube_boss.png"
    );

    public CubeBossBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(
            CubeBossBeamEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        Direction direction = entity.getBeamDirection();
        int length = entity.getBeamLength();
        float warningProgress = entity.getWarningProgress(partialTicks);
        float activeProgress = entity.getActiveProgress(partialTicks);
        float halfWidth = entity.isWarning() ? 0.08F + 0.08F * warningProgress : 0.18F + 0.04F * activeProgress;
        int alpha = entity.isWarning() ? 70 + (int) (90.0F * warningProgress) : 230;

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        PoseStack.Pose pose = poseStack.last();

        for (int segment = 0; segment < length; segment++) {
            renderBeamSegment(pose, consumer, direction, segment, segment + 1, halfWidth, alpha);
        }

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(CubeBossBeamEntity entity) {
        return TEXTURE;
    }

    private void renderBeamSegment(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            Direction direction,
            int start,
            int end,
            float halfWidth,
            int alpha
    ) {
        float signedStart = start * direction.getAxisDirection().getStep();
        float signedEnd = end * direction.getAxisDirection().getStep();

        switch (direction.getAxis()) {
            case X -> renderXSegment(pose, consumer, signedStart, signedEnd, halfWidth, alpha);
            case Y -> renderYSegment(pose, consumer, signedStart, signedEnd, halfWidth, alpha);
            case Z -> renderZSegment(pose, consumer, signedStart, signedEnd, halfWidth, alpha);
        }
    }

    private void renderXSegment(PoseStack.Pose pose, VertexConsumer consumer, float x0, float x1, float halfWidth, int alpha) {
        addQuad(pose, consumer, x0, -halfWidth, -halfWidth, x1, -halfWidth, -halfWidth, x1, halfWidth, -halfWidth, x0, halfWidth, -halfWidth, 0, 0, -1, alpha);
        addQuad(pose, consumer, x0, halfWidth, halfWidth, x1, halfWidth, halfWidth, x1, -halfWidth, halfWidth, x0, -halfWidth, halfWidth, 0, 0, 1, alpha);
        addQuad(pose, consumer, x0, halfWidth, -halfWidth, x1, halfWidth, -halfWidth, x1, halfWidth, halfWidth, x0, halfWidth, halfWidth, 0, 1, 0, alpha);
        addQuad(pose, consumer, x0, -halfWidth, halfWidth, x1, -halfWidth, halfWidth, x1, -halfWidth, -halfWidth, x0, -halfWidth, -halfWidth, 0, -1, 0, alpha);
    }

    private void renderYSegment(PoseStack.Pose pose, VertexConsumer consumer, float y0, float y1, float halfWidth, int alpha) {
        addQuad(pose, consumer, -halfWidth, y0, -halfWidth, -halfWidth, y1, -halfWidth, halfWidth, y1, -halfWidth, halfWidth, y0, -halfWidth, 0, 0, -1, alpha);
        addQuad(pose, consumer, halfWidth, y0, halfWidth, halfWidth, y1, halfWidth, -halfWidth, y1, halfWidth, -halfWidth, y0, halfWidth, 0, 0, 1, alpha);
        addQuad(pose, consumer, halfWidth, y0, -halfWidth, halfWidth, y1, -halfWidth, halfWidth, y1, halfWidth, halfWidth, y0, halfWidth, 1, 0, 0, alpha);
        addQuad(pose, consumer, -halfWidth, y0, halfWidth, -halfWidth, y1, halfWidth, -halfWidth, y1, -halfWidth, -halfWidth, y0, -halfWidth, -1, 0, 0, alpha);
    }

    private void renderZSegment(PoseStack.Pose pose, VertexConsumer consumer, float z0, float z1, float halfWidth, int alpha) {
        addQuad(pose, consumer, -halfWidth, -halfWidth, z0, -halfWidth, -halfWidth, z1, -halfWidth, halfWidth, z1, -halfWidth, halfWidth, z0, -1, 0, 0, alpha);
        addQuad(pose, consumer, halfWidth, halfWidth, z0, halfWidth, halfWidth, z1, halfWidth, -halfWidth, z1, halfWidth, -halfWidth, z0, 1, 0, 0, alpha);
        addQuad(pose, consumer, -halfWidth, halfWidth, z0, -halfWidth, halfWidth, z1, halfWidth, halfWidth, z1, halfWidth, halfWidth, z0, 0, 1, 0, alpha);
        addQuad(pose, consumer, halfWidth, -halfWidth, z0, halfWidth, -halfWidth, z1, -halfWidth, -halfWidth, z1, -halfWidth, -halfWidth, z0, 0, -1, 0, alpha);
    }

    private void addQuad(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float normalX, float normalY, float normalZ,
            int alpha
    ) {
        addVertex(pose, consumer, x0, y0, z0, 0.0F, 0.0F, normalX, normalY, normalZ, alpha);
        addVertex(pose, consumer, x1, y1, z1, 0.0F, 1.0F, normalX, normalY, normalZ, alpha);
        addVertex(pose, consumer, x2, y2, z2, 1.0F, 1.0F, normalX, normalY, normalZ, alpha);
        addVertex(pose, consumer, x3, y3, z3, 1.0F, 0.0F, normalX, normalY, normalZ, alpha);
    }

    private void addVertex(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float x, float y, float z,
            float u, float v,
            float normalX, float normalY, float normalZ,
            int alpha
    ) {
        consumer.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, normalX, normalY, normalZ);
    }
}
