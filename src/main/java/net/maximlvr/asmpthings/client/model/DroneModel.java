package net.maximlvr.asmpthings.client.model;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.entity.custom.DroneEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

public class DroneModel extends HierarchicalModel<DroneEntity> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "drone"), "main");
	private final ModelPart root;
	private final ModelPart drone;
	private final ModelPart body;
	private final ModelPart gun;
	private final ModelPart Aile2;
	private final ModelPart Aile1;
	private final ModelPart torso;

	public DroneModel(ModelPart root) {
		this.root = root;
		this.drone = root.getChild("drone");
		this.body = this.drone.getChild("body");
		this.gun = this.body.getChild("gun");
		this.Aile2 = this.body.getChild("Aile2");
		this.Aile1 = this.body.getChild("Aile1");
		this.torso = this.body.getChild("torso");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition drone = partdefinition.addOrReplaceChild("drone", CubeListBuilder.create(), PartPose.offsetAndRotation(-3.0F, 19.0F, -3.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition body = drone.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition gun = body.addOrReplaceChild("gun", CubeListBuilder.create(), PartPose.offset(-3.0F, 2.0F, 3.0F));

		PartDefinition cube_r1 = gun.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(22, 22).addBox(-1.0329F, 0.3461F, -1.0F, 4.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.2618F));

		PartDefinition cube_r2 = gun.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(34, 2).addBox(-2.0F, -2.0F, 0.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, 4.0F, -1.0F, 0.0F, 0.0F, 0.2618F));

		PartDefinition Aile2 = body.addOrReplaceChild("Aile2", CubeListBuilder.create().texOffs(14, 28).addBox(-5.5F, -1.0F, -0.5F, 11.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.0F, -9.0F, 9.0F));

		PartDefinition Aile1 = body.addOrReplaceChild("Aile1", CubeListBuilder.create().texOffs(14, 28).addBox(-5.5F, -1.0F, -0.5F, 11.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.0F, -9.0F, -3.0F));

		PartDefinition torso = body.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(32, 12).addBox(-3.5F, -11.0F, -3.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(32, 12).addBox(-3.5F, -11.0F, 8.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-7.0F, -8.0F, -1.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(0, 20).addBox(-3.0F, -6.0F, 1.0F, 6.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(0, 28).addBox(1.0F, -7.0F, 0.0F, 1.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition cube_r3 = torso.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(0, 16).addBox(-13.0F, -2.0F, -1.0F, 14.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -5.0F, 9.0F, 0.0F, -1.5708F, 0.0F));

		PartDefinition cube_r4 = torso.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(34, 7).addBox(0.5F, -4.0F, -0.7F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0F, 3.0F, 3.0F, -0.0223F, -0.0922F, 0.2451F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}


    @Override
    public ModelPart root() { return root; }

    @Override
    public void setupAnim(DroneEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        root.yRot = netHeadYaw * ((float) Math.PI / 180);
        animate(entity.idleAnimation, DroneAnimations.IDLE, ageInTicks);
        animate(entity.attackAnimation, DroneAnimations.ATTACK, ageInTicks);
    }
}
