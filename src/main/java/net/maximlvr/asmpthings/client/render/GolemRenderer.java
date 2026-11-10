package net.maximlvr.asmpthings.client.render;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.client.model.GolemModel;
import net.maximlvr.asmpthings.entity.custom.GolemEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class GolemRenderer extends MobRenderer<GolemEntity, GolemModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AsmpThingsMod.MOD_ID, "textures/entity/golem.png");

    public GolemRenderer(EntityRendererProvider.Context context) {
        super(context, new GolemModel(context.bakeLayer(GolemModel.LAYER_LOCATION)), 0.9F);
    }

    @Override
    public ResourceLocation getTextureLocation(GolemEntity entity) { return TEXTURE; }
}
