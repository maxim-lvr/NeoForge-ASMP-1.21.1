package net.maximlvr.asmpthings.client.pet;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.pet.PetCompanionEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

/** Geometry imported unchanged from localimport/pet_msmp.java. */
public final class PetCompanionModel extends HierarchicalModel<PetCompanionEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "pet_companion"), "main");
    private final ModelPart root;
    public PetCompanionModel(ModelPart root) {
        this.root = root;
    }
    @Override public ModelPart root() { return root; }
    @Override public void setupAnim(PetCompanionEntity pet, float limbSwing, float limbAmount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
    }
	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition pet = partdefinition.addOrReplaceChild("pet", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition body = pet.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -9.0F, -4.0F, 6.0F, 5.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition legs = body.addOrReplaceChild("legs", CubeListBuilder.create().texOffs(16, 18).addBox(-2.0F, -4.0F, 2.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(8, 20).addBox(1.0F, -4.0F, -2.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(4, 20).addBox(-2.0F, -4.0F, -2.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 20).addBox(1.0F, -4.0F, 2.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition Turbine = body.addOrReplaceChild("Turbine", CubeListBuilder.create().texOffs(0, 13).addBox(-8.65F, -10.8F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(16, 13).addBox(4.35F, -10.8F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition cube_r1 = Turbine.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(8, 18).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 0.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.8F, -9.4F, 0.0F, 0.0F, 0.0F, -0.5236F));

		PartDefinition cube_r2 = Turbine.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(0, 18).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 0.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.8F, -9.4F, 0.0F, 0.0F, 0.0F, 0.5236F));

		return LayerDefinition.create(meshdefinition, 32, 32);
	}

}
