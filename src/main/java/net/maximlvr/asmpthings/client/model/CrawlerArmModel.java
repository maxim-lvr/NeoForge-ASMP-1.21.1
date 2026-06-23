package net.maximlvr.asmpthings.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.maximlvr.asmpthings.AsmpThingsMod;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

public class CrawlerArmModel {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "crawler_arm"),
            "main"
    );

    private final ModelPart arm;

    public CrawlerArmModel(ModelPart root) {
        this.arm = root.getChild("arm");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();

        // Segment de 1 bloc de long, orienté vers le bas depuis son origine.
        // Il sera ensuite tourné + étiré dans le renderer.
        root.addOrReplaceChild(
                "arm",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.5F, -16.0F, -1.5F, 3.0F, 16.0F, 3.0F),
                PartPose.ZERO
        );

        return LayerDefinition.create(meshDefinition, 16, 16);
    }

    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        this.arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}