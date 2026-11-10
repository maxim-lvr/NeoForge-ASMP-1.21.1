package net.maximlvr.asmpthings.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.entity.custom.KraberSphereEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class KraberSphereRenderer extends EntityRenderer<KraberSphereEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AsmpThingsMod.MOD_ID, "textures/entity/sphere_spell.png");
    private static final int LATITUDES = 24;
    private static final int LONGITUDES = 48;

    public KraberSphereRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    @Override
    public ResourceLocation getTextureLocation(KraberSphereEntity entity) { return TEXTURE; }

    @Override
    public void render(KraberSphereEntity entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float radius = entity.radius(partial);
        if (radius <= 0) return;
        pose.pushPose();
        pose.scale(radius, radius, radius);
        var vertices = buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        for (int lat = 0; lat < LATITUDES; lat++) {
            for (int lon = 0; lon < LONGITUDES; lon++) {
                vertex(pose.last(), vertices, lon, lat);
                vertex(pose.last(), vertices, lon + 1, lat);
                vertex(pose.last(), vertices, lon + 1, lat + 1);
                vertex(pose.last(), vertices, lon, lat + 1);
            }
        }
        pose.popPose();
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer out, int lon, int lat) {
        float u = (float) lon / LONGITUDES, v = (float) lat / LATITUDES;
        double longitude = u * Math.PI * 2, latitude = v * Math.PI;
        float x = (float) (Math.sin(latitude) * Math.cos(longitude));
        float y = (float) Math.cos(latitude);
        float z = (float) (Math.sin(latitude) * Math.sin(longitude));
        out.addVertex(pose, x, y, z).setColor(255, 255, 255, 150).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, x, y, z);
    }
}
