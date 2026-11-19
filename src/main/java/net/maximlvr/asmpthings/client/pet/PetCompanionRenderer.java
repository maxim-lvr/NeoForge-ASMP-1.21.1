package net.maximlvr.asmpthings.client.pet;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.pet.PetCompanionEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class PetCompanionRenderer extends EntityRenderer<PetCompanionEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "textures/entity/pet_1.png");
    private final PetCompanionModel model;
    public PetCompanionRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new PetCompanionModel(context.bakeLayer(PetCompanionModel.LAYER_LOCATION));
        shadowRadius = 0;
    }
    @Override public ResourceLocation getTextureLocation(PetCompanionEntity pet) { return TEXTURE; }
    @Override public void render(PetCompanionEntity pet, float yaw, float partialTick, PoseStack poses, MultiBufferSource buffers, int light) {
        poses.pushPose();
        poses.mulPose(Axis.YP.rotationDegrees(180 - yaw));
        poses.scale(-0.6F, -0.6F, 0.6F);
        poses.translate(0, -1.5, 0);
        model.setupAnim(pet, 0, 0, pet.tickCount + partialTick, 0, 0);
        model.renderToBuffer(poses, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light,
                OverlayTexture.NO_OVERLAY, pet.kind().tint);
        poses.popPose();
        super.render(pet, yaw, partialTick, poses, buffers, light);
    }
}
