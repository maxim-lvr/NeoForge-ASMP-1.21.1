package net.maximlvr.asmpthings.client.render;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.client.model.CubeBossModel;
import net.maximlvr.asmpthings.entity.custom.CubeBossEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class CubeBossRenderer extends MobRenderer<CubeBossEntity, CubeBossModel<CubeBossEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AsmpThingsMod.MOD_ID,
            "textures/entity/cube_boss.png"
    );

    public CubeBossRenderer(EntityRendererProvider.Context context) {
        super(context, new CubeBossModel<>(context.bakeLayer(CubeBossModel.LAYER_LOCATION)), 1.25F);
    }

    @Override
    public ResourceLocation getTextureLocation(CubeBossEntity entity) {
        return TEXTURE;
    }
}
