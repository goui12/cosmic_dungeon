package net.goui.cosmicdungeon.client.anim;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

/** Y-flip fixed export, with blowdart “glued” to the hand (no offsets). */
public final class GoblinAmbusherAnimation {
    private GoblinAmbusherAnimation() {}

    public static final AnimationDefinition WalkAnimation = AnimationDefinition.Builder.withLength(0.5F)
            .looping()
            .addAnimation("r_foot", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,   KeyframeAnimations.degreeVec(52.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.125F, KeyframeAnimations.degreeVec(52.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.2083F,KeyframeAnimations.degreeVec(69.7546F,-3.6561F,-5.2163F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F,  KeyframeAnimations.degreeVec(52.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.375F, KeyframeAnimations.degreeVec(32.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F,   KeyframeAnimations.degreeVec(52.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("l_foot", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,   KeyframeAnimations.degreeVec(60.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.125F, KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F,  KeyframeAnimations.degreeVec(60.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.375F, KeyframeAnimations.degreeVec(50.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F,   KeyframeAnimations.degreeVec(60.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("l_toe", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,   KeyframeAnimations.degreeVec(-27.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.125F, KeyframeAnimations.degreeVec(45.0F,  0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F,  KeyframeAnimations.degreeVec(-27.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.375F, KeyframeAnimations.degreeVec(-62.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F,   KeyframeAnimations.degreeVec(-27.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("r_toe", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,   KeyframeAnimations.degreeVec(-25.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.125F, KeyframeAnimations.degreeVec(-25.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.2083F,KeyframeAnimations.degreeVec(-45.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F,  KeyframeAnimations.degreeVec(-25.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.375F, KeyframeAnimations.degreeVec(25.0F,  0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F,   KeyframeAnimations.degreeVec(-25.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("l_leg", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,   KeyframeAnimations.degreeVec(-60.0F,-12.5F,0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.125F, KeyframeAnimations.degreeVec(-80.0F,-12.5F,0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F,  KeyframeAnimations.degreeVec(-60.0F,-12.5F,0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.375F, KeyframeAnimations.degreeVec(2.5F,  -12.5F,0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F,   KeyframeAnimations.degreeVec(-60.0F,-12.5F,0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("r_leg", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,   KeyframeAnimations.degreeVec(-50.0F,15.0F,0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.125F, KeyframeAnimations.degreeVec(12.5F, 15.0F,0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F,  KeyframeAnimations.degreeVec(-50.0F,15.0F,0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.375F, KeyframeAnimations.degreeVec(-97.5F,15.0F,0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F,   KeyframeAnimations.degreeVec(-50.0F,15.0F,0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("clothes", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,   KeyframeAnimations.degreeVec(-15.0F,0.0F,0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.125F, KeyframeAnimations.degreeVec(-31.1894F,15.0949F,-8.9589F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F,  KeyframeAnimations.degreeVec(-15.0F,0.0F,0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.375F, KeyframeAnimations.degreeVec(-63.0591F,-5.7358F,11.125F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F,   KeyframeAnimations.degreeVec(-15.0F,0.0F,0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("clothes2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,   KeyframeAnimations.degreeVec(-15.0F,0.0F,0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.125F, KeyframeAnimations.degreeVec(14.5874F,0.4522F,0.0597F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F,  KeyframeAnimations.degreeVec(-27.5F,0.0F,0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.375F, KeyframeAnimations.degreeVec(7.5F,   0.0F,0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F,   KeyframeAnimations.degreeVec(-15.0F,0.0F,0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("l_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,   KeyframeAnimations.degreeVec(-14.8774F,-1.936F,-7.2472F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.125F, KeyframeAnimations.degreeVec(-2.3774F, -1.936F,-7.2472F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F,  KeyframeAnimations.degreeVec(-14.8774F,-1.936F,-7.2472F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.375F, KeyframeAnimations.degreeVec(-47.3882F, 2.8965F,-8.5311F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F,   KeyframeAnimations.degreeVec(-14.8774F,-1.936F,-7.2472F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("r_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,   KeyframeAnimations.degreeVec(12.3964F, 1.6189F, 5.1758F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.125F, KeyframeAnimations.degreeVec(-87.6036F,1.6189F, 5.1758F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F,  KeyframeAnimations.degreeVec(12.3964F, 1.6189F, 5.1758F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.375F, KeyframeAnimations.degreeVec(32.3964F, 1.6189F, 5.1758F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F,   KeyframeAnimations.degreeVec(12.3964F, 1.6189F, 5.1758F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("r_hand", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,   KeyframeAnimations.degreeVec(-82.5F,0.0F,0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.125F, KeyframeAnimations.degreeVec(-70.0F,0.0F,0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F,  KeyframeAnimations.degreeVec(-80.0F,0.0F,0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.375F, KeyframeAnimations.degreeVec(-90.0F,0.0F,0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F,   KeyframeAnimations.degreeVec(-80.0F,0.0F,0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("l_hand", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,   KeyframeAnimations.degreeVec(-37.5F,0.0F,0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.125F, KeyframeAnimations.degreeVec(-27.5F,0.0F,0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F,  KeyframeAnimations.degreeVec(-37.5F,0.0F,0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.375F, KeyframeAnimations.degreeVec(-42.5F,0.0F,0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F,   KeyframeAnimations.degreeVec(-37.5F,0.0F,0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,   KeyframeAnimations.degreeVec(-12.5F,0.0F,0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.125F, KeyframeAnimations.degreeVec(-12.5115F,-2.4407F,0.5414F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F,  KeyframeAnimations.degreeVec(-12.5F, 0.0F,0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.375F, KeyframeAnimations.degreeVec(-12.5462F,4.8812F,-1.0848F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F,   KeyframeAnimations.degreeVec(-12.5F, 0.0F,0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("r_earring1", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,   KeyframeAnimations.degreeVec(0.0F,0.0F,0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.125F, KeyframeAnimations.degreeVec(5.0F,0.0F,0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.25F,  KeyframeAnimations.degreeVec(-2.5F,0.0F,0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.375F, KeyframeAnimations.degreeVec(-5.0F,0.0F,0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.5F,   KeyframeAnimations.degreeVec(0.0F,0.0F,0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("l_earring2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,   KeyframeAnimations.degreeVec(0.0F,0.0F,0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.125F, KeyframeAnimations.degreeVec(5.0F,0.0F,0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.25F,  KeyframeAnimations.degreeVec(-2.5F,0.0F,0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.375F, KeyframeAnimations.degreeVec(-5.0F,0.0F,0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.5F,   KeyframeAnimations.degreeVec(0.0F,0.0F,0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("main", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,   KeyframeAnimations.degreeVec(20.0F,0.0F,0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.125F, KeyframeAnimations.degreeVec(19.9825F,-0.8548F, 2.3494F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F,  KeyframeAnimations.degreeVec(20.0F,0.0F,0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.375F, KeyframeAnimations.degreeVec(19.9825F, 0.8548F,-2.3494F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F,   KeyframeAnimations.degreeVec(20.0F,0.0F,0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("main", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new Keyframe(0.0F,   KeyframeAnimations.posVec(0.0F, -4.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.125F, KeyframeAnimations.posVec(0.0F, -4.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F,  KeyframeAnimations.posVec(0.0F, -4.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.375F, KeyframeAnimations.posVec(0.0F, -4.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F,   KeyframeAnimations.posVec(0.0F, -4.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .build();

    public static final AnimationDefinition ShootAnimation = AnimationDefinition.Builder.withLength(1.0F)
            .addAnimation("r_foot", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,  KeyframeAnimations.degreeVec(52.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F, KeyframeAnimations.degreeVec(52.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F,  KeyframeAnimations.degreeVec(52.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(52.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.0F,  KeyframeAnimations.degreeVec(52.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("l_foot", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,  KeyframeAnimations.degreeVec(60.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F, KeyframeAnimations.degreeVec(60.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F,  KeyframeAnimations.degreeVec(60.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(60.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.0F,  KeyframeAnimations.degreeVec(60.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("l_toe", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,  KeyframeAnimations.degreeVec(-27.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F, KeyframeAnimations.degreeVec(-27.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F,  KeyframeAnimations.degreeVec(-27.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(-27.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.0F,  KeyframeAnimations.degreeVec(-27.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("r_toe", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,  KeyframeAnimations.degreeVec(-25.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F, KeyframeAnimations.degreeVec(-25.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F,  KeyframeAnimations.degreeVec(-25.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(-25.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.0F,  KeyframeAnimations.degreeVec(-25.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("l_leg", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,  KeyframeAnimations.degreeVec(-60.0F, -12.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F, KeyframeAnimations.degreeVec(-60.0F, -12.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F,  KeyframeAnimations.degreeVec(-60.0F, -12.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(-60.0F, -12.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.0F,  KeyframeAnimations.degreeVec(-60.0F, -12.5F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("r_leg", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,  KeyframeAnimations.degreeVec(-50.0F, 15.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F, KeyframeAnimations.degreeVec(-50.0F, 15.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F,  KeyframeAnimations.degreeVec(-50.0F, 15.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(-50.0F, 15.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.0F,  KeyframeAnimations.degreeVec(-50.0F, 15.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("clothes", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,  KeyframeAnimations.degreeVec(-15.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F, KeyframeAnimations.degreeVec(-15.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F,  KeyframeAnimations.degreeVec(-15.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(-15.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.0F,  KeyframeAnimations.degreeVec(-15.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("clothes2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,  KeyframeAnimations.degreeVec(-15.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F, KeyframeAnimations.degreeVec(-15.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F,  KeyframeAnimations.degreeVec(-15.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(-15.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.0F,  KeyframeAnimations.degreeVec(-15.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("l_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,  KeyframeAnimations.degreeVec(-14.8774F, -1.936F, -7.2472F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F, KeyframeAnimations.degreeVec(-106.5527F, 12.4419F, 12.4414F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F,  KeyframeAnimations.degreeVec(-106.5527F, 12.4419F, 12.4414F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(-106.5527F, 12.4419F, 12.4414F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.0F,  KeyframeAnimations.degreeVec(-14.8774F, -1.936F, -7.2472F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("r_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,  KeyframeAnimations.degreeVec(12.3964F, 1.6189F, 5.1758F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F, KeyframeAnimations.degreeVec(-140.1036F, -19.1426F, 23.6343F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F,  KeyframeAnimations.degreeVec(-140.1036F, -19.1426F, 23.6343F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(-140.1036F, -19.1426F, 23.6343F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.0F,  KeyframeAnimations.degreeVec(12.3964F, 1.6189F, 5.1758F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("r_hand", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,  KeyframeAnimations.degreeVec(-82.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F, KeyframeAnimations.degreeVec(-2.5F,  0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F,  KeyframeAnimations.degreeVec(-2.5F,  0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(-2.5F,  0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.0F,  KeyframeAnimations.degreeVec(-82.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("l_hand", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,  KeyframeAnimations.degreeVec(-37.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F, KeyframeAnimations.degreeVec(-79.426F, 66.5428F, -22.1496F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F,  KeyframeAnimations.degreeVec(-79.426F, 66.5428F, -22.1496F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(-79.426F, 66.5428F, -22.1496F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.0F,  KeyframeAnimations.degreeVec(-37.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,  KeyframeAnimations.degreeVec(-12.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F, KeyframeAnimations.degreeVec(-20.0F,  0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F,  KeyframeAnimations.degreeVec(-20.0F,  0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(-20.0F,  0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.0F,  KeyframeAnimations.degreeVec(-12.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("main", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,  KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F, KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F,  KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.0F,  KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("main", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new Keyframe(0.0F,  KeyframeAnimations.posVec(0.0F, -4.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F, KeyframeAnimations.posVec(0.0F, -4.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F,  KeyframeAnimations.posVec(0.0F, -4.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.posVec(0.0F, -4.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.0F,  KeyframeAnimations.posVec(0.0F, -4.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("blowdart", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,  KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F, KeyframeAnimations.degreeVec(-27.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F,  KeyframeAnimations.degreeVec(-27.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(-27.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("blowdart", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new Keyframe(0.0F,  KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F, KeyframeAnimations.posVec(1.0F, -6.0F, 10.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F,  KeyframeAnimations.posVec(1.0F, -6.0F, 10.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.posVec(1.0F, -6.0F, 10.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.0F,  KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .build();
}
