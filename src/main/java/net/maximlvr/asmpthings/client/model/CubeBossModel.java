package net.maximlvr.asmpthings.client.model;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.client.animation.CubeBossAnimations;
import net.maximlvr.asmpthings.entity.custom.CubeBossEntity;
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

public class CubeBossModel<T extends CubeBossEntity> extends HierarchicalModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "cube_boss"),
            "main"
    );

    private final ModelPart root;

    public CubeBossModel(ModelPart root) {
        this.root = root.getChild("Bernard_boss");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();

        PartDefinition bernardBoss = partDefinition.addOrReplaceChild("Bernard_boss", CubeListBuilder.create(), PartPose.offset(12.0F, 7.0F, -17.0F));

        PartDefinition arrowMain = bernardBoss.addOrReplaceChild("arrow_main", CubeListBuilder.create(), PartPose.offset(42.0F, 0.0F, 34.0F));

        arrowMain.addOrReplaceChild("arrow_cube_04_r1", CubeListBuilder.create()
                        .texOffs(90, 82).addBox(-20.0F, -6.0F, -0.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(90, 85).addBox(-28.0F, -6.0F, -0.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(90, 85).addBox(-24.0F, -4.0F, -0.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(88, 88).addBox(-22.0F, -2.0F, -0.5F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(41, 65).addBox(-18.0F, -6.0F, -0.5F, 18.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.7854F));

        arrowMain.addOrReplaceChild("arrow_03_r1", CubeListBuilder.create()
                        .texOffs(80, 72).addBox(-11.0F, -6.0F, -1.0F, 12.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0087F));

        arrowMain.addOrReplaceChild("arrow_01_r1", CubeListBuilder.create()
                        .texOffs(60, 80).addBox(-5.9476F, 0.0085F, -1.0F, 6.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(1.0F, -6.0F, 0.0F, 0.0F, 0.0F, 1.5795F));

        PartDefinition bar = bernardBoss.addOrReplaceChild("BAR", CubeListBuilder.create(), PartPose.offset(-12.0F, 17.0F, 17.0F));

        PartDefinition barLeftDownFront = bar.addOrReplaceChild("BAR_Left_down_front", CubeListBuilder.create(), PartPose.offset(16.0F, 1.0F, -17.0F));
        barLeftDownFront.addOrReplaceChild("BAR_Left_down_front_r1", CubeListBuilder.create()
                        .texOffs(12, 96).addBox(-1.2929F, -10.2929F, -1.5858F, 3.0F, 10.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(8.0F, 5.0F, -5.0F, -2.5261F, 0.5236F, 2.5261F));

        PartDefinition barRightUpBack = bar.addOrReplaceChild("BAR_Right_up_back", CubeListBuilder.create(), PartPose.offset(-17.0F, -33.0F, 17.0F));
        barRightUpBack.addOrReplaceChild("BAR_Right_up_back_r1", CubeListBuilder.create()
                        .texOffs(12, 96).addBox(-2.0F, -10.0F, -1.0F, 3.0F, 10.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.6155F, -0.5236F, -0.6155F));

        PartDefinition barRightDownFront = bar.addOrReplaceChild("BAR_Right_down_front", CubeListBuilder.create(), PartPose.offset(17.0F, 0.0F, 17.0F));
        barRightDownFront.addOrReplaceChild("BAR_Right_down_front_r1", CubeListBuilder.create()
                        .texOffs(12, 96).addBox(-1.2929F, 0.4497F, -1.5503F, 3.0F, 10.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.7854F, 0.0F, -0.7854F));

        PartDefinition barRightUpFront = bar.addOrReplaceChild("BAR_Right_up_front", CubeListBuilder.create(), PartPose.offset(18.0F, -32.0F, 17.0F));
        barRightUpFront.addOrReplaceChild("BAR_Right_up_front_r1", CubeListBuilder.create()
                        .texOffs(12, 96).addBox(-2.0F, -10.0F, -1.0F, 3.0F, 10.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.7854F, 0.0F, 0.7854F));

        PartDefinition barLeftUpBack2 = bar.addOrReplaceChild("BAR_Left_up_back2", CubeListBuilder.create(), PartPose.offset(-21.0F, 6.0F, -24.0F));
        barLeftUpBack2.addOrReplaceChild("BAR_Left_up_back_r1", CubeListBuilder.create()
                        .texOffs(12, 96).addBox(-2.0F, -10.0F, -1.0F, 3.0F, 10.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.7854F, 0.0F, 0.7854F));

        PartDefinition barRightDownBack = bar.addOrReplaceChild("BAR_Right_down_back", CubeListBuilder.create(), PartPose.offset(-23.0F, 6.0F, 22.0F));
        barRightDownBack.addOrReplaceChild("BAR_Right_down_back_r1", CubeListBuilder.create()
                        .texOffs(12, 96).addBox(-2.0F, -10.0F, -1.0F, 3.0F, 10.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.6155F, -0.5236F, 0.6155F));

        PartDefinition barLeftUpBack = bar.addOrReplaceChild("BAR_Left_up_back", CubeListBuilder.create(), PartPose.offset(-17.0F, -33.0F, -17.0F));
        barLeftUpBack.addOrReplaceChild("BAR_Left_up_back_r2", CubeListBuilder.create()
                        .texOffs(12, 96).addBox(-2.0F, -10.0F, -1.0F, 3.0F, 10.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.765F, 0.0554F, -0.7633F));

        PartDefinition barLeftUpFront = bar.addOrReplaceChild("BAR_Left_up_front", CubeListBuilder.create(), PartPose.offset(17.0F, -32.0F, -17.0F));
        barLeftUpFront.addOrReplaceChild("BAR_Left_up_front_r1", CubeListBuilder.create()
                        .texOffs(12, 96).addBox(-2.0F, -10.0F, -1.0F, 3.0F, 10.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.7854F, 0.0F, 0.7854F));

        PartDefinition mainBody = bernardBoss.addOrReplaceChild("main_body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-16.0F, -32.0F, -16.0F, 32.0F, 32.0F, 32.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-12.0F, 17.0F, 17.0F));

        PartDefinition eye = mainBody.addOrReplaceChild("EYE", CubeListBuilder.create(), PartPose.offset(19.0F, -16.5F, 0.0F));
        eye.addOrReplaceChild("eye_L", CubeListBuilder.create()
                        .texOffs(0, 64).addBox(-3.0F, -7.5F, -8.0F, 4.0F, 16.0F, 16.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-2.0F, 0.0F, 0.0F));

        PartDefinition eyeM = eye.addOrReplaceChild("eye_M", CubeListBuilder.create(), PartPose.offset(-1.0F, 0.0F, 0.0F));
        eyeM.addOrReplaceChild("eye_M_r1", CubeListBuilder.create()
                        .texOffs(40, 80).addBox(-1.0F, -3.7574F, -3.8284F, 2.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.7854F, 0.0F, 0.0F));

        PartDefinition eyeS = eye.addOrReplaceChild("eye_S", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        eyeS.addOrReplaceChild("eye_S_r1", CubeListBuilder.create()
                        .texOffs(72, 96).addBox(-1.0F, -2.0F, -2.0F, 2.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.25F, 0.0F, -1.5708F, 0.0F, 0.0F));

        PartDefinition arrowL = mainBody.addOrReplaceChild("arrow_L", CubeListBuilder.create(), PartPose.offsetAndRotation(1.0F, -16.0F, -17.0F, 0.0F, 0.0F, -0.7854F));
        arrowL.addOrReplaceChild("arrow_L_03_r1", CubeListBuilder.create()
                        .texOffs(80, 72).addBox(-11.0F, -6.0F, -1.0F, 12.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(6.4905F, 6.3731F, 0.0F, 0.0F, 0.0F, 0.0087F));
        arrowL.addOrReplaceChild("arrow_L_02_r1", CubeListBuilder.create()
                        .texOffs(41, 65).addBox(-17.0F, -6.0F, -0.5F, 18.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(2.4905F, 5.3731F, 0.0F, 0.0F, 0.0F, 0.7854F));
        arrowL.addOrReplaceChild("arrow_L_01_r1", CubeListBuilder.create()
                        .texOffs(60, 80).addBox(-5.9476F, 0.0085F, -1.0F, 6.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(7.4905F, 0.3731F, 0.0F, 0.0F, 0.0F, 1.5795F));

        PartDefinition arrowR = mainBody.addOrReplaceChild("arrow_R", CubeListBuilder.create(), PartPose.offsetAndRotation(1.0F, -16.0F, 17.0F, 0.0F, 0.0F, -0.7854F));
        arrowR.addOrReplaceChild("arrow_R_03_r1", CubeListBuilder.create()
                        .texOffs(80, 72).addBox(-11.0F, -6.0F, -1.0F, 12.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(6.364F, 7.7782F, 0.0F, 0.0F, 0.0F, 0.0087F));
        arrowR.addOrReplaceChild("arrow_R_02_r1", CubeListBuilder.create()
                        .texOffs(41, 65).addBox(-17.0F, -6.0F, -0.5F, 18.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(2.364F, 6.7782F, 0.0F, 0.0F, 0.0F, 0.7854F));
        arrowR.addOrReplaceChild("arrow_R_01_r1", CubeListBuilder.create()
                        .texOffs(60, 80).addBox(-5.9476F, 0.0085F, -1.0F, 6.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(7.364F, 1.7782F, 0.0F, 0.0F, 0.0F, 1.5795F));

        return LayerDefinition.create(meshDefinition, 128, 128);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose);

        if (entity.getAnimationMode() == CubeBossEntity.ANIMATION_IDLE) {
            this.animate(entity.idleAnimationState, CubeBossAnimations.IDLE, ageInTicks);
        } else if (entity.getAnimationMode() == CubeBossEntity.ANIMATION_RUN_CYCLE) {
            this.animate(entity.idleAnimationState, CubeBossAnimations.IDLE, ageInTicks);
            this.animate(entity.runCycleAnimationState, CubeBossAnimations.RUN_CYCLE, ageInTicks);
        } else if (entity.getAnimationMode() == CubeBossEntity.ANIMATION_ARROW_UP) {
            this.animate(entity.arrowUpAnimationState, CubeBossAnimations.ARROW_UP, ageInTicks);
        } else if (entity.getAnimationMode() == CubeBossEntity.ANIMATION_ARROW_DOWN) {
            this.animate(entity.arrowDownAnimationState, CubeBossAnimations.ARROW_DOWN, ageInTicks);
        } else if (entity.getAnimationMode() == CubeBossEntity.ANIMATION_ARROW_LEFT) {
            this.animate(entity.arrowLeftAnimationState, CubeBossAnimations.ARROW_LEFT, ageInTicks);
        } else if (entity.getAnimationMode() == CubeBossEntity.ANIMATION_ARROW_RIGHT) {
            this.animate(entity.arrowRightAnimationState, CubeBossAnimations.ARROW_RIGHT, ageInTicks);
        }
    }
}
