package net.maximlvr.asmpthings.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.client.model.CrawlerArmModel;
import net.maximlvr.asmpthings.client.model.CrawlerModel;
import net.maximlvr.asmpthings.entity.custom.CrawlerEntity;
import net.maximlvr.asmpthings.entity.custom.CrawlerHandEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.mojang.math.Axis;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class CrawlerRenderer extends MobRenderer<CrawlerEntity, CrawlerModel<CrawlerEntity>> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AsmpThingsMod.MOD_ID,
            "textures/entity/crawler.png"
    );

    private static final ResourceLocation ARM_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AsmpThingsMod.MOD_ID,
            "textures/entity/crawler_arm.png"
    );

    private final CrawlerArmModel armModel;

    public CrawlerRenderer(EntityRendererProvider.Context context) {
        super(context, new CrawlerModel<>(context.bakeLayer(CrawlerModel.LAYER_LOCATION)), 0.7F);
        this.armModel = new CrawlerArmModel(context.bakeLayer(CrawlerArmModel.LAYER_LOCATION));
    }

    @Override
    public void render(CrawlerEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        renderCrawlerArms(entity, partialTicks, poseStack, buffer, packedLight);

        renderDebugPath(entity, partialTicks, poseStack, buffer);
    }

    @Override
    protected void setupRotations(CrawlerEntity entity, PoseStack poseStack, float bob, float yBodyRot, float partialTick, float scale) {
        super.setupRotations(entity, poseStack, bob, yBodyRot, partialTick, scale);
        applySurfaceRotation(entity.getSurfaceNormal(), poseStack);
    }

    private void applySurfaceRotation(Direction normal, PoseStack poseStack) {
        switch (normal) {
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            case NORTH -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            case WEST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            default -> {
            }
        }
    }

    private void renderCrawlerArms(CrawlerEntity crawler, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (!crawler.shouldRenderProceduralHands()) {
            return;
        }

        List<CrawlerHandEntity> hands = crawler.level().getEntitiesOfClass(
                CrawlerHandEntity.class,
                crawler.getBoundingBox().inflate(6.0D),
                hand -> crawler.getUUID().equals(hand.getOwnerId())
        );

        Vec3 bodyPos = crawler.getPosition(partialTicks);

        for (CrawlerHandEntity hand : hands) {
            Vec3 handPos = hand.getPosition(partialTicks);
            Vec3 localHandPos = handPos.subtract(bodyPos);

            Vec3 shoulderPos = getShoulderPosition(crawler, hand.getHandIndex());

            renderThreePartArm(
                    crawler,
                    hand.getHandIndex(),
                    shoulderPos,
                    localHandPos.add(0.0D, 0.18D, 0.0D),
                    poseStack,
                    buffer,
                    packedLight
            );
        }
    }

    private void renderThreePartArm(
            CrawlerEntity crawler,
            int handIndex,
            Vec3 start,
            Vec3 end,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        Vec3 fullDirection = end.subtract(start);
        double fullLength = fullDirection.length();

        if (fullLength < 0.15D) {
            return;
        }

        Vec3 bend = getArmBendVector(crawler, handIndex, fullLength);

        Vec3 joint1 = lerpVec(start, end, 0.33D).add(bend);
        Vec3 joint2 = lerpVec(start, end, 0.66D).add(bend.scale(0.55D)).add(0.0D, -0.15D, 0.0D);

        renderArmBetween(start, joint1, poseStack, buffer, packedLight);
        renderArmBetween(joint1, joint2, poseStack, buffer, packedLight);
        renderArmBetween(joint2, end, poseStack, buffer, packedLight);
    }

    private Vec3 getArmBendVector(CrawlerEntity crawler, int handIndex, double armLength) {
        double yawRad = Math.toRadians(crawler.getYRot());

        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);

        Vec3 right = new Vec3(cos, 0.0D, sin);
        Vec3 forward = new Vec3(-sin, 0.0D, cos);

        double sideSign = switch (handIndex) {
            case 0, 2 -> -1.0D;
            case 1, 3 -> 1.0D;
            default -> 0.0D;
        };

        double frontBackSign = switch (handIndex) {
            case 0, 1 -> -1.0D;
            case 2, 3 -> 1.0D;
            default -> 0.0D;
        };

        double bendStrength = Math.min(0.85D, armLength * 0.25D);

        return right.scale(sideSign * bendStrength)
                .add(forward.scale(frontBackSign * 0.25D))
                .add(0.0D, 0.35D, 0.0D);
    }

    private Vec3 lerpVec(Vec3 a, Vec3 b, double progress) {
        return new Vec3(
                a.x + (b.x - a.x) * progress,
                a.y + (b.y - a.y) * progress,
                a.z + (b.z - a.z) * progress
        );
    }

    private Vec3 getShoulderPosition(CrawlerEntity crawler, int handIndex) {
        double localX;
        double localY = 0.65D;
        double localZ;

        switch (handIndex) {
            case 0 -> {
                localX = -0.55D;
                localZ = -0.55D;
            }
            case 1 -> {
                localX = 0.55D;
                localZ = -0.55D;
            }
            case 2 -> {
                localX = -0.55D;
                localZ = 0.55D;
            }
            case 3 -> {
                localX = 0.55D;
                localZ = 0.55D;
            }
            default -> {
                localX = 0.0D;
                localZ = 0.0D;
            }
        }

        double yawRad = Math.toRadians(crawler.getYRot());

        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);

        double rotatedX = localX * cos - localZ * sin;
        double rotatedZ = localX * sin + localZ * cos;

        return new Vec3(rotatedX, localY, rotatedZ);
    }

    private void renderArmBetween(Vec3 start, Vec3 end, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Vec3 direction = end.subtract(start);
        double length = direction.length();

        if (length < 0.05D) {
            return;
        }

        Vector3f from = new Vector3f(0.0F, -1.0F, 0.0F);
        Vector3f to = new Vector3f(
                (float) (direction.x / length),
                (float) (direction.y / length),
                (float) (direction.z / length)
        );

        Quaternionf rotation = new Quaternionf();
        from.rotationTo(to, rotation);

        poseStack.pushPose();

        poseStack.translate(start.x, start.y, start.z);
        poseStack.mulPose(rotation);

        // Le modèle fait 1 bloc de long vers -Y.
        // On l'étire pour atteindre la main.
        poseStack.scale(1.0F, (float) length, 1.0F);

        this.armModel.renderToBuffer(
                poseStack,
                buffer.getBuffer(RenderType.entityCutoutNoCull(ARM_TEXTURE)),
                packedLight,
                OverlayTexture.NO_OVERLAY,
                0xFFFFFFFF
        );

        poseStack.popPose();
    }

    private void renderDebugPath(CrawlerEntity crawler, float partialTicks, PoseStack poseStack, MultiBufferSource buffer) {
        if (!crawler.shouldRenderDebugPath()) {
            return;
        }

        List<DebugPoint> points = parseDebugPoints(crawler.getDebugPathPoints());

        if (points.isEmpty()) {
            return;
        }

        Vec3 bodyPos = crawler.getPosition(partialTicks);
        VertexConsumer lineBuffer = buffer.getBuffer(RenderType.lines());

        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 start = points.get(i).center().subtract(bodyPos);
            Vec3 end = points.get(i + 1).center().subtract(bodyPos);

            addLineVertex(lineBuffer, poseStack, start, 40, 230, 255, 255);
            addLineVertex(lineBuffer, poseStack, end, 40, 230, 255, 255);
        }

        for (DebugPoint point : points) {
            Vec3 local = point.center().subtract(bodyPos);
            float red = point.normal() == Direction.UP ? 0.15F : 1.0F;
            float green = point.normal() == Direction.DOWN ? 0.35F : 0.9F;
            float blue = point.normal().getAxis().isHorizontal() ? 0.15F : 1.0F;

            LevelRenderer.renderLineBox(
                    poseStack,
                    buffer.getBuffer(RenderType.lines()),
                    new AABB(
                            local.x - 0.12D,
                            local.y - 0.12D,
                            local.z - 0.12D,
                            local.x + 0.12D,
                            local.y + 0.12D,
                            local.z + 0.12D
                    ),
                    red,
                    green,
                    blue,
                    1.0F
            );
        }
    }

    private void addLineVertex(VertexConsumer consumer, PoseStack poseStack, Vec3 point, int red, int green, int blue, int alpha) {
        PoseStack.Pose pose = poseStack.last();

        consumer.addVertex(pose, (float) point.x, (float) point.y, (float) point.z)
                .setColor(red, green, blue, alpha)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private List<DebugPoint> parseDebugPoints(String encoded) {
        List<DebugPoint> points = new ArrayList<>();

        if (encoded == null || encoded.isBlank()) {
            return points;
        }

        String[] entries = encoded.split(";");

        for (String entry : entries) {
            String[] values = entry.split(",");

            if (values.length != 4) {
                continue;
            }

            try {
                int x = Integer.parseInt(values[0]);
                int y = Integer.parseInt(values[1]);
                int z = Integer.parseInt(values[2]);
                Direction normal = Direction.from3DDataValue(Integer.parseInt(values[3]));

                points.add(new DebugPoint(new Vec3(x + 0.5D, y + 0.5D, z + 0.5D), normal));
            } catch (NumberFormatException ignored) {
            }
        }

        return points;
    }

    @Override
    public ResourceLocation getTextureLocation(CrawlerEntity entity) {
        return TEXTURE;
    }

    private record DebugPoint(Vec3 center, Direction normal) {
    }
}
