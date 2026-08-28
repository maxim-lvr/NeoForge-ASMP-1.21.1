package net.maximlvr.asmpthings.client.model;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.entity.custom.CubeBossArrowEntity;
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

public class CubeBossArrowModel extends HierarchicalModel<CubeBossArrowEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "cube_boss_arrow"),
            "main"
    );

    private final ModelPart root;

    public CubeBossArrowModel(ModelPart root) {
        this.root = root.getChild("Bernard_boss");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();

        PartDefinition bernardBoss = partDefinition.addOrReplaceChild(
                "Bernard_boss",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(5.0F, 15.0F, 0.0F, 0.0F, 0.0F, -0.7854F)
        );

        PartDefinition arrowMain = bernardBoss.addOrReplaceChild(
                "arrow_main",
                CubeListBuilder.create()
                        .texOffs(0, 7)
                        .addBox(-11.0F, -6.0F, -1.0F, 12.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(5.0F, 5.0F, 0.0F)
        );

        arrowMain.addOrReplaceChild(
                "arrow_cube_04_r1",
                CubeListBuilder.create()
                        .texOffs(16, 21).addBox(-20.0F, -6.0F, -0.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(16, 18).addBox(-28.0F, -6.0F, -0.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(16, 18).addBox(-24.0F, -4.0F, -0.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(16, 15).addBox(-22.0F, -2.0F, -0.5F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(-18.0F, -6.0F, -0.5F, 18.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.7854F)
        );

        arrowMain.addOrReplaceChild(
                "arrow_01_r1",
                CubeListBuilder.create()
                        .texOffs(0, 15)
                        .addBox(-6.0F, 0.0F, -1.0F, 6.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(1.0F, -6.0F, 0.0F, 0.0F, 0.0F, 1.5708F)
        );

        return LayerDefinition.create(meshDefinition, 64, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(CubeBossArrowEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
    }
}
