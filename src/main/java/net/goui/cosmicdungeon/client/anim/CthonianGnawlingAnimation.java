package net.goui.cosmicdungeon.client.anim;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

public final class CthonianGnawlingAnimation {
    private CthonianGnawlingAnimation() {}

    public static final AnimationDefinition WALKING;

    static {
        AnimationDefinition.Builder builder = AnimationDefinition.Builder.withLength(1.0F).looping();

        addToothChomp(builder, "Tooth_1", -25.0F);
        addToothChomp(builder, "Tooth_2", -17.5F);
        addToothChomp(builder, "Tooth_3", -47.5F);
        addToothChomp(builder, "Tooth_4", -10.0F);
        addToothChomp(builder, "Tooth_5", -12.5F);
        addToothChomp(builder, "Tooth_6", -12.5F);
        addToothChomp(builder, "Tooth_7", -20.0F);
        addToothChomp(builder, "Tooth_8", -25.0F);

        addWave(builder, "Head_Seg", 2.5F);
        addZeroPosition(builder, "Head_Seg");

        addWave(builder, "Body_1", 2.5F);
        addZeroPosition(builder, "Body_1");
        addWave(builder, "Body_1_Seg", 2.5F);
        addWave(builder, "Body_2", 2.5F);
        addWave(builder, "Body_2_Seg", 2.5F);
        addWave(builder, "Body_3", 2.5F);
        addWave(builder, "Body_3_Seg", 2.5F);
        addWave(builder, "Body_4", 2.5F);
        addWave(builder, "Body_4_Seg", 2.5F);

        addWave(builder, "Body_5", -2.5F);
        addWave(builder, "Body_5_Seg", -2.5F);
        addWave(builder, "Body_6", -2.5F);
        addWave(builder, "Body_6_Seg", -2.5F);
        addWave(builder, "Body_7", -2.5F);
        addWave(builder, "Body_7_Seg", -2.5F);
        addWave(builder, "Body_8", -2.5F);
        addWave(builder, "Body_8_Seg", -2.5F);
        addWave(builder, "Body_9", -2.5F);
        addWave(builder, "Body_9_Seg", -2.5F);
        addWave(builder, "Body_10", -2.5F);

        WALKING = builder.build();
    }

    private static void addToothChomp(AnimationDefinition.Builder builder, String bone, float yawDegrees) {
        builder.addAnimation(bone, new AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, yawDegrees, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.75F, KeyframeAnimations.degreeVec(0.0F, yawDegrees, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ));
    }

    private static void addWave(AnimationDefinition.Builder builder, String bone, float yawDegrees) {
        builder.addAnimation(bone, new AnimationChannel(
                AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, yawDegrees, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, -yawDegrees, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, yawDegrees, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ));
    }

    private static void addZeroPosition(AnimationDefinition.Builder builder, String bone) {
        builder.addAnimation(bone, new AnimationChannel(
                AnimationChannel.Targets.POSITION,
                new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ));
    }
}
