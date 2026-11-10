package net.maximlvr.asmpthings.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.maximlvr.asmpthings.entity.custom.UndertakerEntity;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class UndertakerModel extends HierarchicalModel<UndertakerEntity> {
    private static final AnimationDefinition AIM = stationaryAim(UndertakerAnimations.WALK_LOOP_AIMING);
    private static final AnimationDefinition SHOOT = stationaryAim(UndertakerAnimations.WALK_LOOP_AIMING_SHOOT);
    private final ModelPart root = UndertakerGeometry.createRoot();

    @Override public ModelPart root() { return root; }

    @Override
    public void setupAnim(UndertakerEntity entity, float swing, float amount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        animate(entity.idleState, UndertakerAnimations.IDLE, age);
        animate(entity.walkState, UndertakerAnimations.WALK_LOOP, age, 0.7F);
        animate(entity.aimState, AIM, age);
        animate(entity.shootState, SHOOT, age);
    }

    // Use the forward-pointing gun animations without their walking leg tracks.
    private static AnimationDefinition stationaryAim(AnimationDefinition source) {
        var builder = AnimationDefinition.Builder.withLength(source.lengthInSeconds()).looping();
        UndertakerAnimations.IDLE.boneAnimations().forEach((bone, channels) -> {
            if (!bone.equals("leftArm")) channels.forEach(channel -> builder.addAnimation(bone, channel));
        });
        source.boneAnimations().get("leftArm").forEach(channel -> builder.addAnimation("leftArm", channel));
        return builder.build();
    }

    public Vec3 muzzleOffset(float bodyYaw) {
        PoseStack pose = new PoseStack();
        root.translateAndRotate(pose);
        ModelPart part = root;
        for (String bone : new String[]{"body", "leftArm", "leftItemBone", "leftItemBone2"}) {
            part = part.getChild(bone);
            part.translateAndRotate(pose);
        }
        Vector3f tip = pose.last().pose().transformPosition(1.5F/16, 15.8F/16, -5.6F/16, new Vector3f());
        return new Vec3(tip.x, 1.501 - tip.y, -tip.z).yRot((float) Math.toRadians(-bodyYaw));
    }
}
