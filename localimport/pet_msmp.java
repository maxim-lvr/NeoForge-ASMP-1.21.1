// Made with Blockbench 5.2.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class pet_msmp<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "pet_msmp"), "main");
	private final ModelPart pet;
	private final ModelPart body;
	private final ModelPart legs;
	private final ModelPart Turbine;

	public pet_msmp(ModelPart root) {
		this.pet = root.getChild("pet");
		this.body = this.pet.getChild("body");
		this.legs = this.body.getChild("legs");
		this.Turbine = this.body.getChild("Turbine");
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

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		pet.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}