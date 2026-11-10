package net.maximlvr.asmpthings.client.animation;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

public final class ArmiBossAnimations {
    public static final AnimationDefinition IDLE = AnimationDefinition.Builder.withLength(3.0000000F).looping()
        .addAnimation("finger1", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0000000F, KeyframeAnimations.degreeVec(0.0000000F, -2.5000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.1666700F, KeyframeAnimations.degreeVec(0.0000000F, -30.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(3.0000000F, KeyframeAnimations.degreeVec(0.0000000F, -2.5000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("finger2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0000000F, KeyframeAnimations.degreeVec(2.5000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5416700F, KeyframeAnimations.degreeVec(25.8200000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.5833300F, KeyframeAnimations.degreeVec(40.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(3.0000000F, KeyframeAnimations.degreeVec(2.5000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("finger4", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0000000F, KeyframeAnimations.degreeVec(-2.5000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0833300F, KeyframeAnimations.degreeVec(5.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(3.0000000F, KeyframeAnimations.degreeVec(-2.5000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("finger3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0000000F, KeyframeAnimations.degreeVec(2.5000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.2500000F, KeyframeAnimations.degreeVec(10.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(3.0000000F, KeyframeAnimations.degreeVec(2.5000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("finger5", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0000000F, KeyframeAnimations.degreeVec(2.5000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0833300F, KeyframeAnimations.degreeVec(10.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.2500000F, KeyframeAnimations.degreeVec(-14.1600000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(3.0000000F, KeyframeAnimations.degreeVec(2.5000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("arm1_1", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0000000F, KeyframeAnimations.degreeVec(-2.5000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0000000F, KeyframeAnimations.degreeVec(2.5000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(3.0000000F, KeyframeAnimations.degreeVec(-2.5000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("finger1_2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0000000F, KeyframeAnimations.degreeVec(0.0000000F, -15.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.5000000F, KeyframeAnimations.degreeVec(0.0000000F, -50.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(3.0000000F, KeyframeAnimations.degreeVec(0.0000000F, -15.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("finger2_1", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0000000F, KeyframeAnimations.degreeVec(12.5000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5416700F, KeyframeAnimations.degreeVec(-8.7800000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.5000000F, KeyframeAnimations.degreeVec(30.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.1250000F, KeyframeAnimations.degreeVec(12.7100000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(3.0000000F, KeyframeAnimations.degreeVec(12.5000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("finger3_1", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0000000F, KeyframeAnimations.degreeVec(10.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.5000000F, KeyframeAnimations.degreeVec(35.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(3.0000000F, KeyframeAnimations.degreeVec(10.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("finger4_1", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0000000F, KeyframeAnimations.degreeVec(10.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.5000000F, KeyframeAnimations.degreeVec(-25.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(3.0000000F, KeyframeAnimations.degreeVec(10.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("finger5_1", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0000000F, KeyframeAnimations.degreeVec(12.5000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.5000000F, KeyframeAnimations.degreeVec(22.5000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(3.0000000F, KeyframeAnimations.degreeVec(12.5000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("arm2_2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0000000F, KeyframeAnimations.degreeVec(0.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.8750000F, KeyframeAnimations.degreeVec(0.0000000F, 0.0000000F, -2.5000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.6666700F, KeyframeAnimations.degreeVec(0.0000000F, 0.0000000F, -5.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(3.0000000F, KeyframeAnimations.degreeVec(0.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("hand2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0000000F, KeyframeAnimations.degreeVec(0.0000000F, 0.0000000F, 2.5000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.2500000F, KeyframeAnimations.degreeVec(0.0000000F, 0.0000000F, 7.5000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.5000000F, KeyframeAnimations.degreeVec(0.0000000F, 0.0000000F, 2.5000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(3.0000000F, KeyframeAnimations.degreeVec(0.0000000F, 0.0000000F, 2.5000000F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("finger6", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0000000F, KeyframeAnimations.degreeVec(0.0000000F, -17.5000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0000000F, KeyframeAnimations.degreeVec(0.0000000F, -30.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.7500000F, KeyframeAnimations.degreeVec(0.0000000F, -17.5000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.4166700F, KeyframeAnimations.degreeVec(0.0000000F, -47.5000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(3.0000000F, KeyframeAnimations.degreeVec(0.0000000F, -17.5000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("finger1_3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0000000F, KeyframeAnimations.degreeVec(0.0000000F, -40.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.0000000F, KeyframeAnimations.degreeVec(0.0000000F, -35.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.7500000F, KeyframeAnimations.degreeVec(0.0000000F, -17.5000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.4166700F, KeyframeAnimations.degreeVec(0.0000000F, -57.5000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(3.0000000F, KeyframeAnimations.degreeVec(0.0000000F, -40.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("finger7", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0000000F, KeyframeAnimations.degreeVec(-15.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.1250000F, KeyframeAnimations.degreeVec(-17.5000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.9166700F, KeyframeAnimations.degreeVec(-5.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(3.0000000F, KeyframeAnimations.degreeVec(-15.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("finger2_2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0000000F, KeyframeAnimations.degreeVec(0.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.1250000F, KeyframeAnimations.degreeVec(5.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("finger8", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0000000F, KeyframeAnimations.degreeVec(-35.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.1250000F, KeyframeAnimations.degreeVec(-25.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.2500000F, KeyframeAnimations.degreeVec(-12.5000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(3.0000000F, KeyframeAnimations.degreeVec(-35.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("finger9", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0000000F, KeyframeAnimations.degreeVec(-50.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.4166700F, KeyframeAnimations.degreeVec(-55.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.9166700F, KeyframeAnimations.degreeVec(-27.5000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(3.0000000F, KeyframeAnimations.degreeVec(-50.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("finger10", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0000000F, KeyframeAnimations.degreeVec(-47.5000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.1250000F, KeyframeAnimations.degreeVec(-40.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.5000000F, KeyframeAnimations.degreeVec(-32.5000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(3.0000000F, KeyframeAnimations.degreeVec(-47.5000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("arm3_1", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0000000F, KeyframeAnimations.degreeVec(0.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.7083300F, KeyframeAnimations.degreeVec(0.0000000F, 0.0000000F, -2.5000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(3.0000000F, KeyframeAnimations.degreeVec(0.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("hand_ear", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0000000F, KeyframeAnimations.degreeVec(0.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.5000000F, KeyframeAnimations.degreeVec(-5.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.6666700F, KeyframeAnimations.degreeVec(-2.5000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(3.0000000F, KeyframeAnimations.degreeVec(0.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("finger11", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0000000F, KeyframeAnimations.degreeVec(0.0000000F, -35.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.5000000F, KeyframeAnimations.degreeVec(0.0000000F, -25.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0833300F, KeyframeAnimations.degreeVec(0.0000000F, -30.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(3.0000000F, KeyframeAnimations.degreeVec(0.0000000F, -35.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("finger1_4", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0000000F, KeyframeAnimations.degreeVec(0.0000000F, -72.5000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.5000000F, KeyframeAnimations.degreeVec(0.0000000F, -82.5000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(3.0000000F, KeyframeAnimations.degreeVec(0.0000000F, -72.5000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("finger12", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0000000F, KeyframeAnimations.degreeVec(72.5000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.5000000F, KeyframeAnimations.degreeVec(65.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0833300F, KeyframeAnimations.degreeVec(72.5000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(3.0000000F, KeyframeAnimations.degreeVec(72.5000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("finger13", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0000000F, KeyframeAnimations.degreeVec(15.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(3.0000000F, KeyframeAnimations.degreeVec(15.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("finger3_3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0000000F, KeyframeAnimations.degreeVec(0.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.0833300F, KeyframeAnimations.degreeVec(12.5000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(3.0000000F, KeyframeAnimations.degreeVec(0.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("finger14", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0000000F, KeyframeAnimations.degreeVec(-57.5000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.5000000F, KeyframeAnimations.degreeVec(-52.5000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(3.0000000F, KeyframeAnimations.degreeVec(-57.5000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("finger4_3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0000000F, KeyframeAnimations.degreeVec(0.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.5000000F, KeyframeAnimations.degreeVec(-10.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(3.0000000F, KeyframeAnimations.degreeVec(0.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("finger15", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0000000F, KeyframeAnimations.degreeVec(-60.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(2.5000000F, KeyframeAnimations.degreeVec(-65.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(3.0000000F, KeyframeAnimations.degreeVec(-60.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("finger5_3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0000000F, KeyframeAnimations.degreeVec(0.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(1.5416700F, KeyframeAnimations.degreeVec(10.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(3.0000000F, KeyframeAnimations.degreeVec(0.0000000F, 0.0000000F, 0.0000000F), AnimationChannel.Interpolations.LINEAR)
        ))
        .build();
}
