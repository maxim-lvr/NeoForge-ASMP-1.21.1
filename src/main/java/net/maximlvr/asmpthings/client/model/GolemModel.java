package net.maximlvr.asmpthings.client.model;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.entity.custom.GolemEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;

public class GolemModel extends HierarchicalModel<GolemEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "golem"), "main");
    private final ModelPart root;

    public GolemModel(ModelPart root) { this.root = root; }

    @Override
    public ModelPart root() { return root; }

    @Override
    public void setupAnim(GolemEntity entity, float swing, float amount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        animate(entity.idleAnimation, GolemAnimations.IDLE, age);
        animate(entity.walkState, GolemAnimations.WALK, age, 0.7F);
    }
}
