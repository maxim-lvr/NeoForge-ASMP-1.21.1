package net.maximlvr.asmpthings.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.client.animation.SkeleteShopWipTestAnimations;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class SkeleteShopWipTestModel extends Model {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AsmpThingsMod.MOD_ID, "skeleteshopwiptest"),
            "main"
    );

    private final ModelPart hips;
    private final ModelPart armll;

    public SkeleteShopWipTestModel(ModelPart root) {
        super(RenderType::entityCutout);
        this.hips = root.getChild("hips");
        ModelPart bust = this.hips.getChild("bust");
        ModelPart arml = bust.getChild("arml");
        this.armll = arml.getChild("armll");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition hips = partdefinition.addOrReplaceChild("hips", CubeListBuilder.create(), PartPose.offset(-3.0F, 23.0F, -4.0F));
        hips.addOrReplaceChild("back_r1", CubeListBuilder.create().texOffs(9, 11).addBox(-0.6967F, -5.506F, -0.9042F, 2.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.2618F, 0.0F, 0.2182F));

        PartDefinition bust = hips.addOrReplaceChild("bust", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        bust.addOrReplaceChild("bust_r1", CubeListBuilder.create().texOffs(4, 8).addBox(-3.8895F, -5.9812F, -2.0667F, 8.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, -4.0F, 2.0F, -0.2618F, 0.0F, 0.2182F));

        PartDefinition head = bust.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(0, 2).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, -10.0F, 3.0F, -0.1304F, -0.0114F, 0.2625F));

        PartDefinition arml = bust.addOrReplaceChild("arml", CubeListBuilder.create(), PartPose.offset(0.0F, -9.0F, 3.0F));
        arml.addOrReplaceChild("arml_r1", CubeListBuilder.create().texOffs(2, 2).addBox(-8.5F, -1.0F, -1.0F, 9.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.2006F, 0.2662F, 0.0F, 0.0F, 0.0F, 0.2182F));

        PartDefinition armll = arml.addOrReplaceChild("armll", CubeListBuilder.create(), PartPose.offset(-8.2006F, -0.7338F, 0.0F));
        armll.addOrReplaceChild("armll_r1", CubeListBuilder.create().texOffs(0, 2).addBox(-11.122F, -1.1391F, -1.0F, 11.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.2006F, -0.2662F, 0.0F, 0.0F, 0.0F, -1.7017F));

        PartDefinition armr = bust.addOrReplaceChild("armr", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        armr.addOrReplaceChild("armr_r1", CubeListBuilder.create().texOffs(0, 2).addBox(-1.1783F, -0.1528F, -1.0F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.0F, -8.0F, 4.0F, 0.0F, 0.0F, -0.4363F));

        PartDefinition armrl = armr.addOrReplaceChild("armrl", CubeListBuilder.create(), PartPose.offset(3.0F, 1.0F, 4.0F));
        armrl.addOrReplaceChild("armrl_r1", CubeListBuilder.create().texOffs(0, 2).addBox(-1.0F, -8.0F, -1.0F, 2.0F, 8.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.0F, -1.0F, 0.0F, -0.5672F, 0.0F, -1.5708F));

        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    public void setupAnim(float ageInTicks) {
        this.hips.getAllParts().forEach(ModelPart::resetPose);
        this.armll.zRot += armllRotation(ageInTicks);
    }

    private float armllRotation(float ageInTicks) {
        float cycleTime = (ageInTicks / 20.0F) % SkeleteShopWipTestAnimations.LOOP_DELAY_SECONDS;

        if (cycleTime > SkeleteShopWipTestAnimations.LENGTH_SECONDS) {
            return 0.0F;
        }

        return (float) Math.toRadians(interpolateDegrees(cycleTime));
    }

    private float interpolateDegrees(float time) {
        float[] times = SkeleteShopWipTestAnimations.ARMLL_TIMES;
        float[] values = SkeleteShopWipTestAnimations.ARMLL_Z_ROTATION_DEGREES;

        for (int i = 0; i < times.length - 1; i++) {
            if (time >= times[i] && time <= times[i + 1]) {
                float progress = (time - times[i]) / (times[i + 1] - times[i]);
                return values[i] + (values[i + 1] - values[i]) * progress;
            }
        }

        return 0.0F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        hips.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
