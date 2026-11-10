// Made with Blockbench 5.1.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class armi_boss<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "armi_boss"), "main");
	private final ModelPart armi;
	private final ModelPart arm1;
	private final ModelPart arm2;
	private final ModelPart hand;
	private final ModelPart finger1;
	private final ModelPart finger1_2;
	private final ModelPart finger2;
	private final ModelPart finger2_1;
	private final ModelPart finger3;
	private final ModelPart finger3_1;
	private final ModelPart finger4;
	private final ModelPart finger4_1;
	private final ModelPart finger5;
	private final ModelPart finger5_1;

	public armi_boss(ModelPart root) {
		this.armi = root.getChild("armi");
		this.arm1 = this.armi.getChild("arm1");
		this.arm2 = this.arm1.getChild("arm2");
		this.hand = this.arm2.getChild("hand");
		this.finger1 = this.hand.getChild("finger1");
		this.finger1_2 = this.finger1.getChild("finger1_2");
		this.finger2 = this.hand.getChild("finger2");
		this.finger2_1 = this.finger2.getChild("finger2_1");
		this.finger3 = this.hand.getChild("finger3");
		this.finger3_1 = this.finger3.getChild("finger3_1");
		this.finger4 = this.hand.getChild("finger4");
		this.finger4_1 = this.finger4.getChild("finger4_1");
		this.finger5 = this.hand.getChild("finger5");
		this.finger5_1 = this.finger5.getChild("finger5_1");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition armi = partdefinition.addOrReplaceChild("armi", CubeListBuilder.create(), PartPose.offset(3.0F, -18.0F, 0.0F));

		PartDefinition arm1 = armi.addOrReplaceChild("arm1", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -16.0F, -1.0F, 4.0F, 16.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.0F, 42.0F, 0.0F));

		PartDefinition arm2 = arm1.addOrReplaceChild("arm2", CubeListBuilder.create().texOffs(14, 0).addBox(-2.0F, -16.0F, -1.0F, 4.0F, 16.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -16.0F, 0.0F));

		PartDefinition hand = arm2.addOrReplaceChild("hand", CubeListBuilder.create().texOffs(0, 19).addBox(-3.0F, -7.0F, -1.0F, 8.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.0F, -16.0F, 0.0F));

		PartDefinition finger1 = hand.addOrReplaceChild("finger1", CubeListBuilder.create().texOffs(20, 19).addBox(-4.0F, -1.0F, -1.0F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -2.0F, 0.0F, 0.0F, -0.5672F, 0.0F));

		PartDefinition finger1_2 = finger1.addOrReplaceChild("finger1_2", CubeListBuilder.create().texOffs(28, 9).addBox(-2.0F, -1.0F, -1.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.0F, 0.0F, 0.0F));

		PartDefinition finger2 = hand.addOrReplaceChild("finger2", CubeListBuilder.create(), PartPose.offsetAndRotation(-2.0F, -7.0F, 0.0F, 0.4363F, 0.0F, 0.0F));

		PartDefinition finger2_r1 = finger2.addOrReplaceChild("finger2_r1", CubeListBuilder.create().texOffs(20, 22).addBox(-4.0F, -1.0F, -1.0F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.5708F));

		PartDefinition finger2_1 = finger2.addOrReplaceChild("finger2_1", CubeListBuilder.create(), PartPose.offset(0.0F, -4.0F, 0.0F));

		PartDefinition finger2_1_r1 = finger2_1.addOrReplaceChild("finger2_1_r1", CubeListBuilder.create().texOffs(10, 28).addBox(-2.0F, -1.0F, -1.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7418F, 1.5708F));

		PartDefinition finger3 = hand.addOrReplaceChild("finger3", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -7.0F, 0.0F, 0.6109F, 0.0F, 0.0F));

		PartDefinition finger3_r1 = finger3.addOrReplaceChild("finger3_r1", CubeListBuilder.create().texOffs(20, 25).addBox(-4.0F, -1.0F, -1.0F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.5708F));

		PartDefinition finger3_1 = finger3.addOrReplaceChild("finger3_1", CubeListBuilder.create(), PartPose.offset(0.0F, -4.0F, 0.0F));

		PartDefinition finger3_1_r1 = finger3_1.addOrReplaceChild("finger3_1_r1", CubeListBuilder.create().texOffs(28, 0).addBox(-4.0F, -1.0F, -1.0F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7418F, 1.5708F));

		PartDefinition finger4 = hand.addOrReplaceChild("finger4", CubeListBuilder.create(), PartPose.offsetAndRotation(2.0F, -7.0F, 0.0F, 0.9163F, 0.0F, 0.0F));

		PartDefinition finger4_r1 = finger4.addOrReplaceChild("finger4_r1", CubeListBuilder.create().texOffs(0, 28).addBox(-4.0F, -1.0F, -1.0F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.5708F));

		PartDefinition finger4_1 = finger4.addOrReplaceChild("finger4_1", CubeListBuilder.create(), PartPose.offset(0.0F, -4.0F, 0.0F));

		PartDefinition finger4_1_r1 = finger4_1.addOrReplaceChild("finger4_1_r1", CubeListBuilder.create().texOffs(28, 6).addBox(-3.0F, -1.0F, -1.0F, 3.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.6109F, 1.5708F));

		PartDefinition finger5 = hand.addOrReplaceChild("finger5", CubeListBuilder.create(), PartPose.offsetAndRotation(4.0F, -7.0F, 0.0F, 0.8727F, 0.0F, 0.0F));

		PartDefinition finger5_r1 = finger5.addOrReplaceChild("finger5_r1", CubeListBuilder.create().texOffs(28, 3).addBox(-3.0F, -1.0F, -1.0F, 3.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.5708F));

		PartDefinition finger5_1 = finger5.addOrReplaceChild("finger5_1", CubeListBuilder.create(), PartPose.offset(0.0F, -3.0F, 0.0F));

		PartDefinition finger5_1_r1 = finger5_1.addOrReplaceChild("finger5_1_r1", CubeListBuilder.create().texOffs(28, 12).addBox(-2.0F, -1.0F, -1.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.6545F, 1.5708F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		armi.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}