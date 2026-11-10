package net.maximlvr.asmpthings.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.client.model.UndertakerModel;
import net.maximlvr.asmpthings.entity.custom.UndertakerEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class UndertakerRenderer extends MobRenderer<UndertakerEntity, UndertakerModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AsmpThingsMod.MOD_ID, "textures/entity/undertaker.png");

    public UndertakerRenderer(EntityRendererProvider.Context context) {
        super(context, new UndertakerModel(), 0.5F);
    }

    @Override public ResourceLocation getTextureLocation(UndertakerEntity entity) { return TEXTURE; }

    @Override
    public void render(UndertakerEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        super.render(entity, yaw, partialTick, pose, buffers, light);
        if (!entity.hasLaser()) return;
        Vec3 start = model.muzzleOffset(net.minecraft.util.Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot));
        Vec3 end = entity.laserEndOffset();
        Vec3 axis = end.subtract(start).normalize();
        Vec3 side = axis.cross(Math.abs(axis.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize().scale(0.035);
        Vec3 up = axis.cross(side);
        var vertices = buffers.getBuffer(RenderType.lightning());
        for (Vec3 offset : new Vec3[]{side, up}) {
            Vec3[] corners = {start.add(offset), end.add(offset), end.subtract(offset), start.subtract(offset)};
            for (int face = 0; face < 2; face++) {
                for (int i = 0; i < 4; i++) {
                    Vec3 point = corners[face == 0 ? i : 3 - i];
                    vertices.addVertex(pose.last(), (float) point.x, (float) point.y, (float) point.z).setColor(255, 25, 15, 255);
                }
            }
        }
    }
}
