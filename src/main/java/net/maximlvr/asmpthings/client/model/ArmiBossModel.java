package net.maximlvr.asmpthings.client.model;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.client.animation.ArmiBossAnimations;
import net.maximlvr.asmpthings.entity.custom.ArmiBossEntity;
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

public final class ArmiBossModel extends HierarchicalModel<ArmiBossEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "armi_boss"), "main");
    private final ModelPart root;

    public ArmiBossModel(ModelPart root) {
        this.root = root.getChild("armi");
    }

    public static LayerDefinition createBodyLayer() {
        // Imported from localimport/armi_boss.bbmodel, including its three distinct hands.
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition part0 = root.addOrReplaceChild("armi", CubeListBuilder.create(), PartPose.offsetAndRotation(3.0000000F, -18.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        PartDefinition part1 = part0.addOrReplaceChild("arm1", CubeListBuilder.create(), PartPose.offsetAndRotation(-3.0000000F, 42.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        part1.addOrReplaceChild("cube2", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0000000F, -16.0000000F, -1.0000000F, 4.0000000F, 16.0000000F, 3.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        PartDefinition part3 = part1.addOrReplaceChild("arm1_1", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0000000F, -16.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        part3.addOrReplaceChild("cube4", CubeListBuilder.create().texOffs(14, 0).addBox(-2.0000000F, -16.0000000F, -1.0000000F, 4.0000000F, 16.0000000F, 3.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        PartDefinition part5 = part3.addOrReplaceChild("hand_eye", CubeListBuilder.create(), PartPose.offsetAndRotation(-1.0000000F, -16.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        part5.addOrReplaceChild("cube6", CubeListBuilder.create().texOffs(0, 19).addBox(-3.0000000F, -7.0000000F, -1.0000000F, 8.0000000F, 7.0000000F, 2.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        PartDefinition part7 = part5.addOrReplaceChild("finger1", CubeListBuilder.create(), PartPose.offsetAndRotation(-3.0000000F, -2.0000000F, 0.0000000F, 0.0000000F, -0.5672320F, 0.0000000F));
        part7.addOrReplaceChild("cube8", CubeListBuilder.create().texOffs(20, 19).addBox(-4.0000000F, -1.0000000F, -1.0000000F, 4.0000000F, 2.0000000F, 1.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        PartDefinition part9 = part7.addOrReplaceChild("finger1_2", CubeListBuilder.create(), PartPose.offsetAndRotation(-4.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        part9.addOrReplaceChild("cube10", CubeListBuilder.create().texOffs(28, 9).addBox(-2.0000000F, -1.0000000F, -1.0000000F, 2.0000000F, 2.0000000F, 1.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        PartDefinition part11 = part5.addOrReplaceChild("finger2", CubeListBuilder.create(), PartPose.offsetAndRotation(-2.0000000F, -7.0000000F, 0.0000000F, 0.4363323F, 0.0000000F, 0.0000000F));
        part11.addOrReplaceChild("cube12", CubeListBuilder.create().texOffs(20, 22).addBox(-4.0000000F, -1.0000000F, -1.0000000F, 4.0000000F, 2.0000000F, 1.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 1.5707963F));
        PartDefinition part13 = part11.addOrReplaceChild("finger2_1", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0000000F, -4.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        part13.addOrReplaceChild("cube14", CubeListBuilder.create().texOffs(10, 28).addBox(-2.0000000F, -1.0000000F, -1.0000000F, 2.0000000F, 2.0000000F, 1.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, -0.7417649F, 1.5707963F));
        PartDefinition part15 = part5.addOrReplaceChild("finger3", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0000000F, -7.0000000F, 0.0000000F, 0.6108652F, 0.0000000F, 0.0000000F));
        part15.addOrReplaceChild("cube16", CubeListBuilder.create().texOffs(20, 25).addBox(-4.0000000F, -1.0000000F, -1.0000000F, 4.0000000F, 2.0000000F, 1.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 1.5707963F));
        PartDefinition part17 = part15.addOrReplaceChild("finger3_1", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0000000F, -4.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        part17.addOrReplaceChild("cube18", CubeListBuilder.create().texOffs(28, 0).addBox(-4.0000000F, -1.0000000F, -1.0000000F, 4.0000000F, 2.0000000F, 1.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, -0.7417649F, 1.5707963F));
        PartDefinition part19 = part5.addOrReplaceChild("finger4", CubeListBuilder.create(), PartPose.offsetAndRotation(2.0000000F, -7.0000000F, 0.0000000F, 0.9162979F, 0.0000000F, 0.0000000F));
        part19.addOrReplaceChild("cube20", CubeListBuilder.create().texOffs(0, 28).addBox(-4.0000000F, -1.0000000F, -1.0000000F, 4.0000000F, 2.0000000F, 1.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 1.5707963F));
        PartDefinition part21 = part19.addOrReplaceChild("finger4_1", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0000000F, -4.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        part21.addOrReplaceChild("cube22", CubeListBuilder.create().texOffs(28, 6).addBox(-3.0000000F, -1.0000000F, -1.0000000F, 3.0000000F, 2.0000000F, 1.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, -0.6108652F, 1.5707963F));
        PartDefinition part23 = part5.addOrReplaceChild("finger5", CubeListBuilder.create(), PartPose.offsetAndRotation(4.0000000F, -7.0000000F, 0.0000000F, 0.8726646F, 0.0000000F, 0.0000000F));
        part23.addOrReplaceChild("cube24", CubeListBuilder.create().texOffs(28, 3).addBox(-3.0000000F, -1.0000000F, -1.0000000F, 3.0000000F, 2.0000000F, 1.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 1.5707963F));
        PartDefinition part25 = part23.addOrReplaceChild("finger5_1", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0000000F, -3.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        part25.addOrReplaceChild("cube26", CubeListBuilder.create().texOffs(28, 12).addBox(-2.0000000F, -1.0000000F, -1.0000000F, 2.0000000F, 2.0000000F, 1.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, -0.6544985F, 1.5707963F));
        PartDefinition part27 = part0.addOrReplaceChild("arm2", CubeListBuilder.create(), PartPose.offsetAndRotation(4.0000000F, 42.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.1745329F));
        part27.addOrReplaceChild("cube28", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0000000F, -16.0000000F, -1.0000000F, 4.0000000F, 16.0000000F, 3.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        PartDefinition part29 = part27.addOrReplaceChild("arm2_2", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0000000F, -16.0000000F, 0.0000000F, 0.1230874F, -0.0446574F, -0.7445173F));
        part29.addOrReplaceChild("cube30", CubeListBuilder.create().texOffs(14, 0).addBox(-2.0000000F, -16.0000000F, -1.0000000F, 4.0000000F, 16.0000000F, 3.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        PartDefinition part31 = part29.addOrReplaceChild("hand2", CubeListBuilder.create(), PartPose.offsetAndRotation(-1.0000000F, -16.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, -1.0035643F));
        part31.addOrReplaceChild("cube32", CubeListBuilder.create().texOffs(3, 33).addBox(-3.0000000F, -7.0000000F, -1.0000000F, 8.0000000F, 7.0000000F, 2.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        PartDefinition part33 = part31.addOrReplaceChild("finger6", CubeListBuilder.create(), PartPose.offsetAndRotation(-3.0000000F, -2.0000000F, 0.0000000F, 0.0000000F, -0.5672320F, 0.0000000F));
        part33.addOrReplaceChild("cube34", CubeListBuilder.create().texOffs(20, 19).addBox(-4.0000000F, -1.0000000F, -1.0000000F, 4.0000000F, 2.0000000F, 1.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        PartDefinition part35 = part33.addOrReplaceChild("finger1_3", CubeListBuilder.create(), PartPose.offsetAndRotation(-4.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        part35.addOrReplaceChild("cube36", CubeListBuilder.create().texOffs(28, 9).addBox(-2.0000000F, -1.0000000F, -1.0000000F, 2.0000000F, 2.0000000F, 1.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        PartDefinition part37 = part31.addOrReplaceChild("finger7", CubeListBuilder.create(), PartPose.offsetAndRotation(-2.0000000F, -7.0000000F, 0.0000000F, 0.4363323F, 0.0000000F, 0.0000000F));
        part37.addOrReplaceChild("cube38", CubeListBuilder.create().texOffs(20, 22).addBox(-4.0000000F, -1.0000000F, -1.0000000F, 4.0000000F, 2.0000000F, 1.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 1.5707963F));
        PartDefinition part39 = part37.addOrReplaceChild("finger2_2", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0000000F, -4.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        part39.addOrReplaceChild("cube40", CubeListBuilder.create().texOffs(10, 28).addBox(-2.0000000F, -1.0000000F, -1.0000000F, 2.0000000F, 2.0000000F, 1.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, -0.7417649F, 1.5707963F));
        PartDefinition part41 = part31.addOrReplaceChild("finger8", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0000000F, -7.0000000F, 0.0000000F, 0.6108652F, 0.0000000F, 0.0000000F));
        part41.addOrReplaceChild("cube42", CubeListBuilder.create().texOffs(20, 25).addBox(-4.0000000F, -1.0000000F, -1.0000000F, 4.0000000F, 2.0000000F, 1.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 1.5707963F));
        PartDefinition part43 = part41.addOrReplaceChild("finger3_2", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0000000F, -4.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        part43.addOrReplaceChild("cube44", CubeListBuilder.create().texOffs(28, 0).addBox(-4.0000000F, -1.0000000F, -1.0000000F, 4.0000000F, 2.0000000F, 1.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, -0.7417649F, 1.5707963F));
        PartDefinition part45 = part31.addOrReplaceChild("finger9", CubeListBuilder.create(), PartPose.offsetAndRotation(2.0000000F, -7.0000000F, 0.0000000F, 0.9162979F, 0.0000000F, 0.0000000F));
        part45.addOrReplaceChild("cube46", CubeListBuilder.create().texOffs(0, 28).addBox(-4.0000000F, -1.0000000F, -1.0000000F, 4.0000000F, 2.0000000F, 1.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 1.5707963F));
        PartDefinition part47 = part45.addOrReplaceChild("finger4_2", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0000000F, -4.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        part47.addOrReplaceChild("cube48", CubeListBuilder.create().texOffs(28, 6).addBox(-3.0000000F, -1.0000000F, -1.0000000F, 3.0000000F, 2.0000000F, 1.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, -0.6108652F, 1.5707963F));
        PartDefinition part49 = part31.addOrReplaceChild("finger10", CubeListBuilder.create(), PartPose.offsetAndRotation(4.0000000F, -7.0000000F, 0.0000000F, 0.8726646F, 0.0000000F, 0.0000000F));
        part49.addOrReplaceChild("cube50", CubeListBuilder.create().texOffs(28, 3).addBox(-3.0000000F, -1.0000000F, -1.0000000F, 3.0000000F, 2.0000000F, 1.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 1.5707963F));
        PartDefinition part51 = part49.addOrReplaceChild("finger5_2", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0000000F, -3.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        part51.addOrReplaceChild("cube52", CubeListBuilder.create().texOffs(28, 12).addBox(-2.0000000F, -1.0000000F, -1.0000000F, 2.0000000F, 2.0000000F, 1.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, -0.6544985F, 1.5707963F));
        PartDefinition part53 = part0.addOrReplaceChild("arm3", CubeListBuilder.create(), PartPose.offsetAndRotation(-10.0000000F, 42.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, -0.3490659F));
        part53.addOrReplaceChild("cube54", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0000000F, -16.0000000F, -1.0000000F, 4.0000000F, 16.0000000F, 3.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        PartDefinition part55 = part53.addOrReplaceChild("arm3_1", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0000000F, -16.0000000F, 0.0000000F, -0.1044489F, -0.1297734F, 0.6119817F));
        part55.addOrReplaceChild("cube56", CubeListBuilder.create().texOffs(14, 0).addBox(-2.0000000F, -16.0000000F, -1.0000000F, 4.0000000F, 16.0000000F, 3.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        PartDefinition part57 = part55.addOrReplaceChild("hand_ear", CubeListBuilder.create(), PartPose.offsetAndRotation(-1.0000000F, -16.0000000F, 0.0000000F, 2.0352061F, 1.4732542F, 1.7715013F));
        part57.addOrReplaceChild("cube58", CubeListBuilder.create().texOffs(23, 33).addBox(-3.0000000F, -7.0000000F, -1.0000000F, 8.0000000F, 7.0000000F, 2.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        part57.addOrReplaceChild("cube59", CubeListBuilder.create().texOffs(19, 47).addBox(0.0000000F, -6.0000000F, -2.0000000F, 2.0000000F, 5.0000000F, 1.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        part57.addOrReplaceChild("cube60", CubeListBuilder.create().texOffs(26, 46).addBox(-1.0000000F, -6.0000000F, -3.0000000F, 1.0000000F, 4.0000000F, 2.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        part57.addOrReplaceChild("cube61", CubeListBuilder.create().texOffs(10, 48).addBox(0.0000000F, -7.0000000F, -3.0000000F, 2.0000000F, 1.0000000F, 2.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        PartDefinition part62 = part57.addOrReplaceChild("finger11", CubeListBuilder.create(), PartPose.offsetAndRotation(-3.0000000F, -2.0000000F, 0.0000000F, 0.0000000F, -0.5672320F, 0.0000000F));
        part62.addOrReplaceChild("cube63", CubeListBuilder.create().texOffs(20, 19).addBox(-4.0000000F, -1.0000000F, -1.0000000F, 4.0000000F, 2.0000000F, 1.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        PartDefinition part64 = part62.addOrReplaceChild("finger1_4", CubeListBuilder.create(), PartPose.offsetAndRotation(-4.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        part64.addOrReplaceChild("cube65", CubeListBuilder.create().texOffs(28, 9).addBox(-2.0000000F, -1.0000000F, -1.0000000F, 2.0000000F, 2.0000000F, 1.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        PartDefinition part66 = part57.addOrReplaceChild("finger12", CubeListBuilder.create(), PartPose.offsetAndRotation(-2.0000000F, -7.0000000F, 0.0000000F, 0.4363323F, 0.0000000F, 0.0000000F));
        part66.addOrReplaceChild("cube67", CubeListBuilder.create().texOffs(20, 22).addBox(-4.0000000F, -1.0000000F, -1.0000000F, 4.0000000F, 2.0000000F, 1.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 1.5707963F));
        PartDefinition part68 = part66.addOrReplaceChild("finger2_3", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0000000F, -4.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        part68.addOrReplaceChild("cube69", CubeListBuilder.create().texOffs(10, 28).addBox(-2.0000000F, -1.0000000F, -1.0000000F, 2.0000000F, 2.0000000F, 1.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, -0.7417649F, 1.5707963F));
        PartDefinition part70 = part57.addOrReplaceChild("finger13", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0000000F, -7.0000000F, 0.0000000F, 0.6108652F, 0.0000000F, 0.0000000F));
        part70.addOrReplaceChild("cube71", CubeListBuilder.create().texOffs(20, 25).addBox(-4.0000000F, -1.0000000F, -1.0000000F, 4.0000000F, 2.0000000F, 1.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 1.5707963F));
        PartDefinition part72 = part70.addOrReplaceChild("finger3_3", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0000000F, -4.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        part72.addOrReplaceChild("cube73", CubeListBuilder.create().texOffs(28, 0).addBox(-4.0000000F, -1.0000000F, -1.0000000F, 4.0000000F, 2.0000000F, 1.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, -0.7417649F, 1.5707963F));
        PartDefinition part74 = part57.addOrReplaceChild("finger14", CubeListBuilder.create(), PartPose.offsetAndRotation(2.0000000F, -7.0000000F, 0.0000000F, 0.9162979F, 0.0000000F, 0.0000000F));
        part74.addOrReplaceChild("cube75", CubeListBuilder.create().texOffs(0, 28).addBox(-4.0000000F, -1.0000000F, -1.0000000F, 4.0000000F, 2.0000000F, 1.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 1.5707963F));
        PartDefinition part76 = part74.addOrReplaceChild("finger4_3", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0000000F, -4.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        part76.addOrReplaceChild("cube77", CubeListBuilder.create().texOffs(28, 6).addBox(-3.0000000F, -1.0000000F, -1.0000000F, 3.0000000F, 2.0000000F, 1.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, -0.6108652F, 1.5707963F));
        PartDefinition part78 = part57.addOrReplaceChild("finger15", CubeListBuilder.create(), PartPose.offsetAndRotation(4.0000000F, -7.0000000F, 0.0000000F, 0.8726646F, 0.0000000F, 0.0000000F));
        part78.addOrReplaceChild("cube79", CubeListBuilder.create().texOffs(28, 3).addBox(-3.0000000F, -1.0000000F, -1.0000000F, 3.0000000F, 2.0000000F, 1.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 1.5707963F));
        PartDefinition part80 = part78.addOrReplaceChild("finger5_3", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0000000F, -3.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F));
        part80.addOrReplaceChild("cube81", CubeListBuilder.create().texOffs(28, 12).addBox(-2.0000000F, -1.0000000F, -1.0000000F, 2.0000000F, 2.0000000F, 1.0000000F, new CubeDeformation(0.0000000F)), PartPose.offsetAndRotation(0.0000000F, 0.0000000F, 0.0000000F, 0.0000000F, -0.6544985F, 1.5707963F));
        return LayerDefinition.create(mesh, 64, 64);
    }


    @Override
    public ModelPart root() { return root; }

    public ModelPart eyeHand() { return root.getChild("arm1").getChild("arm1_1").getChild("hand_eye"); }

    public void setEyeArmVisible(boolean visible) { root.getChild("arm1").visible = visible; }

    @Override
    public void setupAnim(ArmiBossEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float headYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        animate(entity.idleAnimationState, ArmiBossAnimations.IDLE, ageInTicks);
    }
}
