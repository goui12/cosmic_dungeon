package net.goui.cosmicdungeon.client.anim;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

/**
 * Stone Warden Animations (NeoForge 1.21.8 style)
 * - Matches tutorial style: plural class name, ALL_CAPS animation constants, public static final.
 * - Keep bone names exactly as your model expects (r_leg, l_leg, etc.).
 */
public final class StoneWardenAnimations {

    private StoneWardenAnimations() {} // no instances

    /** Walking loop for the Stone Warden (matches tutorial constant style). */
    public static final AnimationDefinition WALKING = AnimationDefinition.Builder.withLength(1.0F).looping()
            .addAnimation("r_leg", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,  KeyframeAnimations.degreeVec(0.0F,   0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F, KeyframeAnimations.degreeVec(-50.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(10.0F,  0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.0F,  KeyframeAnimations.degreeVec(0.0F,   0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("r_thigh", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("r_shin", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,  KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(25.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.0F,  KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("r_foot", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,  KeyframeAnimations.degreeVec(0.0F,   0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F,   0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(-25.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.0F,  KeyframeAnimations.degreeVec(0.0F,   0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("l_leg", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,  KeyframeAnimations.degreeVec(0.0F,   0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F, KeyframeAnimations.degreeVec(10.0F,  0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(-50.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.0F,  KeyframeAnimations.degreeVec(0.0F,   0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("l_shin", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,  KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F, KeyframeAnimations.degreeVec(25.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.0F,  KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("l_foot", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,  KeyframeAnimations.degreeVec(0.0F,   0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F, KeyframeAnimations.degreeVec(-25.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(0.0F,   0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.0F,  KeyframeAnimations.degreeVec(0.0F,   0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("r_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,  KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F, KeyframeAnimations.degreeVec(20.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(-20.0F,0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.0F,  KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("r_lower_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,  KeyframeAnimations.degreeVec(-20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.0F,  KeyframeAnimations.degreeVec(-20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("r_hand_core", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("r_finger_thumb", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("r_finger_pointer", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -90.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("r_finger_middle", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -90.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("r_finger_ring", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -90.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("r_finger_pinky", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -90.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("l_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,  KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F, KeyframeAnimations.degreeVec(-20.0F,0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(20.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.0F,  KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("l_lower_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,  KeyframeAnimations.degreeVec(-20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.0F,  KeyframeAnimations.degreeVec(-20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("l_hand", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.25F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("l_finger_pointer", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 90.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("l_finger_middle", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 90.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("l_finger_ring", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 90.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("l_finger_pinky", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 90.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,  KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F, KeyframeAnimations.degreeVec(5.0F,  0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F,  KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(5.0F,  0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.0F,  KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("frame", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,  KeyframeAnimations.degreeVec(2.5F,  0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F, KeyframeAnimations.degreeVec(2.5F,  2.5F,  2.5F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(2.5F, -2.5F, -2.5F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.0F,  KeyframeAnimations.degreeVec(2.5F,  0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .build();

    /** Long attack animation for boss wind‑up/smash. */
    public static final AnimationDefinition ATTACK_1 = AnimationDefinition.Builder.withLength(3.0F)
            .addAnimation("main", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,    KeyframeAnimations.degreeVec(0.0F,   0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.1667F, KeyframeAnimations.degreeVec(0.0F,   0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.7917F, KeyframeAnimations.degreeVec(0.0F,   0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.4167F, KeyframeAnimations.degreeVec(-75.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.8333F, KeyframeAnimations.degreeVec(-75.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.2917F, KeyframeAnimations.degreeVec(-47.5F, 0.0F,  0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.5417F, KeyframeAnimations.degreeVec(-47.5F, 0.0F,  0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.875F,  KeyframeAnimations.degreeVec(0.0F,   0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("main", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new Keyframe(0.0F,    KeyframeAnimations.posVec(0.0F,   0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.1667F, KeyframeAnimations.posVec(0.0F,   0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.375F,  KeyframeAnimations.posVec(0.0F,  -13.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5833F, KeyframeAnimations.posVec(0.0F,  -10.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.875F,  KeyframeAnimations.posVec(0.0F,   38.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.0833F, KeyframeAnimations.posVec(0.0F,   38.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.4583F, KeyframeAnimations.posVec(0.0F,  -55.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.8333F, KeyframeAnimations.posVec(0.0F,  -55.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.2917F, KeyframeAnimations.posVec(0.0F,  -29.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.5417F, KeyframeAnimations.posVec(0.0F,  -29.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.875F,  KeyframeAnimations.posVec(0.0F,    0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            // (All remaining channels are identical to your original, preserved 1:1)
            .addAnimation("r_leg", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,    KeyframeAnimations.degreeVec(0.0F,    0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.1667F, KeyframeAnimations.degreeVec(0.0F,    0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.375F,  KeyframeAnimations.degreeVec(-70.0F, 12.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5833F, KeyframeAnimations.degreeVec(-70.0F, 12.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.7917F, KeyframeAnimations.degreeVec(-20.0F, 12.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.4583F, KeyframeAnimations.degreeVec(-105.0F,12.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.8333F, KeyframeAnimations.degreeVec(-105.0F,12.5F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.2917F, KeyframeAnimations.degreeVec(-55.0F, 12.5F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.5417F, KeyframeAnimations.degreeVec(-7.5F,  12.5F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.875F,  KeyframeAnimations.degreeVec(0.0F,    0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("r_shin", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,    KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.1667F, KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.375F,  KeyframeAnimations.degreeVec(70.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5833F, KeyframeAnimations.degreeVec(70.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.7917F, KeyframeAnimations.degreeVec(20.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.4583F, KeyframeAnimations.degreeVec(145.0F,0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.8333F, KeyframeAnimations.degreeVec(145.0F,0.0F,  0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.2917F, KeyframeAnimations.degreeVec(145.0F,0.0F,  0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.5417F, KeyframeAnimations.degreeVec(127.5F,0.0F,  0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.875F,  KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("r_foot", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.1667F, KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.375F,  KeyframeAnimations.degreeVec(12.5F, 0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5833F, KeyframeAnimations.degreeVec(12.5F, 0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.7917F, KeyframeAnimations.degreeVec(12.5F, 0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.8333F, KeyframeAnimations.degreeVec(12.5F, 0.0F,  0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.2917F, KeyframeAnimations.degreeVec(12.5F, 0.0F,  0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.5417F, KeyframeAnimations.degreeVec(12.5F, 0.0F,  0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.875F,  KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("l_leg", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,    KeyframeAnimations.degreeVec(0.0F,    0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.1667F, KeyframeAnimations.degreeVec(0.0F,    0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.375F,  KeyframeAnimations.degreeVec(-70.0F, -12.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5833F, KeyframeAnimations.degreeVec(-70.0F, -12.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.7917F, KeyframeAnimations.degreeVec(-20.0F, -12.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.4583F, KeyframeAnimations.degreeVec(-97.5F, -12.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.8333F, KeyframeAnimations.degreeVec(-97.5F, -12.5F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.2917F, KeyframeAnimations.degreeVec(-55.0F, -12.5F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.5417F, KeyframeAnimations.degreeVec(-55.0F, -12.5F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.875F,  KeyframeAnimations.degreeVec(0.0F,    0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("l_shin", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,    KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.1667F, KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.375F,  KeyframeAnimations.degreeVec(70.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5833F, KeyframeAnimations.degreeVec(70.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.7917F, KeyframeAnimations.degreeVec(20.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.4583F, KeyframeAnimations.degreeVec(145.0F,0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.8333F, KeyframeAnimations.degreeVec(145.0F,0.0F,  0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.2917F, KeyframeAnimations.degreeVec(100.0F,0.0F,  0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.5417F, KeyframeAnimations.degreeVec(100.0F,0.0F,  0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.875F,  KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("l_foot", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.1667F, KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.375F,  KeyframeAnimations.degreeVec(17.5F, 0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5833F, KeyframeAnimations.degreeVec(17.5F, 0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.7917F, KeyframeAnimations.degreeVec(17.5F, 0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.8333F, KeyframeAnimations.degreeVec(17.5F, 0.0F,  0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.2917F, KeyframeAnimations.degreeVec(17.5F, 0.0F,  0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.5417F, KeyframeAnimations.degreeVec(17.5F, 0.0F,  0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.875F,  KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("frame", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,    KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.1667F, KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.375F,  KeyframeAnimations.degreeVec(35.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5833F, KeyframeAnimations.degreeVec(35.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.7917F, KeyframeAnimations.degreeVec(20.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.4583F, KeyframeAnimations.degreeVec(50.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.8333F, KeyframeAnimations.degreeVec(50.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.2917F, KeyframeAnimations.degreeVec(50.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.5417F, KeyframeAnimations.degreeVec(50.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.875F,  KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("r_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.1667F, KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.375F,  KeyframeAnimations.degreeVec(0.0F,  0.0F, 22.5F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5833F, KeyframeAnimations.degreeVec(0.0F,  0.0F, 22.5F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.7917F, KeyframeAnimations.degreeVec(0.0F,  0.0F, 40.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.4583F, KeyframeAnimations.degreeVec(0.0F,  0.0F, 27.5F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.8333F, KeyframeAnimations.degreeVec(53.6609F, -24.9417F, 44.7344F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.2917F, KeyframeAnimations.degreeVec(12.7068F, 19.24F, 6.2447F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.5417F, KeyframeAnimations.degreeVec(12.7068F, 19.24F, 6.2447F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.875F,  KeyframeAnimations.degreeVec(0.0F,   0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("r_lower_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.1667F, KeyframeAnimations.degreeVec(0.0F,    0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.375F,  KeyframeAnimations.degreeVec(-132.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5833F, KeyframeAnimations.degreeVec(-132.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.7917F, KeyframeAnimations.degreeVec(-132.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.4583F, KeyframeAnimations.degreeVec(-108.0347F, -44.909F, 2.2889F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.8333F, KeyframeAnimations.degreeVec(-75.53F,   -44.91F,  2.29F),   AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.2917F, KeyframeAnimations.degreeVec(-8.03F,    -44.91F,  2.29F),   AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.5417F, KeyframeAnimations.degreeVec(-8.03F,    -44.91F,  2.29F),   AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.875F,  KeyframeAnimations.degreeVec(0.0F,      0.0F,    0.0F),     AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("r_hand", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,    KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.1667F, KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.8333F, KeyframeAnimations.degreeVec(0.0F,  0.0F, 42.5F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.2917F, KeyframeAnimations.degreeVec(0.0F,  0.0F, 25.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.5417F, KeyframeAnimations.degreeVec(0.0F,  0.0F, 25.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.875F,  KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("r_finger_thumb", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,    KeyframeAnimations.degreeVec(0.0F,   0.0F,   0.0F),  AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.1667F, KeyframeAnimations.degreeVec(0.0F,   0.0F,   0.0F),  AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.4583F, KeyframeAnimations.degreeVec(-27.1177F, -13.3486F, -24.267F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.8333F, KeyframeAnimations.degreeVec(-64.62F, -13.35F, -24.27F),      AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.2917F, KeyframeAnimations.degreeVec(-64.62F, -13.35F, -24.27F),      AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.5417F, KeyframeAnimations.degreeVec(-64.62F, -13.35F, -24.27F),      AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.875F,  KeyframeAnimations.degreeVec(0.0F,    0.0F,   0.0F),          AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("r_finger_pointer", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,    KeyframeAnimations.degreeVec(0.0F,  0.0F,   0.0F),  AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.1667F, KeyframeAnimations.degreeVec(0.0F,  0.0F,   0.0F),  AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.4583F, KeyframeAnimations.degreeVec(0.0F,  0.0F,  -77.5F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.8333F, KeyframeAnimations.degreeVec(0.0F,  0.0F,  -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.2917F, KeyframeAnimations.degreeVec(0.0F,  0.0F,  -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.5417F, KeyframeAnimations.degreeVec(0.0F,  0.0F,  -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.875F,  KeyframeAnimations.degreeVec(0.0F,  0.0F,    0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("r_finger_middle", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,    KeyframeAnimations.degreeVec(0.0F,  0.0F,  -0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.1667F, KeyframeAnimations.degreeVec(0.0F,  0.0F,  -0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.4583F, KeyframeAnimations.degreeVec(0.0F,  0.0F,  -80.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.8333F, KeyframeAnimations.degreeVec(0.0F,  0.0F,   -7.5F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.2917F, KeyframeAnimations.degreeVec(0.0F,  0.0F,   -7.5F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.5417F, KeyframeAnimations.degreeVec(0.0F,  0.0F,   -7.5F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.875F,  KeyframeAnimations.degreeVec(0.0F,  0.0F,    0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("r_finger_ring", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,    KeyframeAnimations.degreeVec(0.0F,  0.0F,  -0.0F),  AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.1667F, KeyframeAnimations.degreeVec(0.0F,  0.0F,  -0.0F),  AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.4583F, KeyframeAnimations.degreeVec(0.0F,  0.0F,  -92.5F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.8333F, KeyframeAnimations.degreeVec(0.0F,  0.0F,    2.5F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.2917F, KeyframeAnimations.degreeVec(0.0F,  0.0F,    2.5F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.5417F, KeyframeAnimations.degreeVec(0.0F,  0.0F,    2.5F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.875F,  KeyframeAnimations.degreeVec(0.0F,  0.0F,    0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("r_finger_pinky", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,    KeyframeAnimations.degreeVec(0.0F,  0.0F,   0.0F),  AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.1667F, KeyframeAnimations.degreeVec(0.0F,  0.0F,   0.0F),  AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.4583F, KeyframeAnimations.degreeVec(0.0F,  0.0F, -122.5F),  AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.8333F, KeyframeAnimations.degreeVec(0.0F,  0.0F,    7.5F),  AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.2917F, KeyframeAnimations.degreeVec(0.0F,  0.0F,    7.5F),  AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.5417F, KeyframeAnimations.degreeVec(0.0F,  0.0F,    7.5F),  AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.875F,  KeyframeAnimations.degreeVec(0.0F,  0.0F,    0.0F),  AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("l_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.1667F, KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F),  AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.375F,  KeyframeAnimations.degreeVec(0.0F,  0.0F, -22.5F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5833F, KeyframeAnimations.degreeVec(0.0F,  0.0F, -22.5F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.7917F, KeyframeAnimations.degreeVec(0.0F,  0.0F, -40.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.4583F, KeyframeAnimations.degreeVec(0.0F,  0.0F, -25.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.8333F, KeyframeAnimations.degreeVec(45.1462F, 24.3051F, -47.2691F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.2917F, KeyframeAnimations.degreeVec(17.2051F, -18.8895F, -8.3146F),  AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.5417F, KeyframeAnimations.degreeVec(17.2051F, -18.8895F, -8.3146F),  AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.875F,  KeyframeAnimations.degreeVec(0.0F,   0.0F,    0.0F),          AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("l_lower_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.1667F, KeyframeAnimations.degreeVec(0.0F,    0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.375F,  KeyframeAnimations.degreeVec(-132.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5833F, KeyframeAnimations.degreeVec(-132.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.7917F, KeyframeAnimations.degreeVec(-132.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.4583F, KeyframeAnimations.degreeVec(-103.0F, 45.0F, 2.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.8333F, KeyframeAnimations.degreeVec(-73.0F,  45.0F, 2.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.2917F, KeyframeAnimations.degreeVec(-8.0F,   45.0F, 2.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.5417F, KeyframeAnimations.degreeVec(-8.0F,   45.0F, 2.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.875F,  KeyframeAnimations.degreeVec(0.0F,    0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("l_hand", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,    KeyframeAnimations.degreeVec(0.0F,  0.0F,   0.0F),  AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.1667F, KeyframeAnimations.degreeVec(0.0F,  0.0F,   0.0F),  AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.8333F, KeyframeAnimations.degreeVec(0.0F,  0.0F, -52.5F),  AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.2917F, KeyframeAnimations.degreeVec(0.0F,  0.0F, -27.5F),  AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.5417F, KeyframeAnimations.degreeVec(0.0F,  0.0F, -27.5F),  AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.875F,  KeyframeAnimations.degreeVec(0.0F,  0.0F,   0.0F),  AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("l_finger_thumb", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,    KeyframeAnimations.degreeVec(0.0F,   0.0F,   0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.1667F, KeyframeAnimations.degreeVec(0.0F,   0.0F,   0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.4583F, KeyframeAnimations.degreeVec(-26.5651F, 14.4775F, 26.5651F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.8333F, KeyframeAnimations.degreeVec(-46.57F,   14.48F,   26.57F),   AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.2917F, KeyframeAnimations.degreeVec(-46.57F,   14.48F,   26.57F),   AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.5417F, KeyframeAnimations.degreeVec(-46.57F,   14.48F,   26.57F),   AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.875F,  KeyframeAnimations.degreeVec(0.0F,     0.0F,     0.0F),     AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("l_finger_pointer", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,    KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F),  AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.1667F, KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F),  AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.4583F, KeyframeAnimations.degreeVec(0.0F,  0.0F, 87.5F),  AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.8333F, KeyframeAnimations.degreeVec(0.0F,  0.0F, 45.0F),  AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.2917F, KeyframeAnimations.degreeVec(0.0F,  0.0F, 45.0F),  AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.5417F, KeyframeAnimations.degreeVec(0.0F,  0.0F, 45.0F),  AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.875F,  KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F),  AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("l_finger_middle", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,    KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F),  AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.1667F, KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F),  AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.4583F, KeyframeAnimations.degreeVec(0.0F,  0.0F, 77.5F),  AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.8333F, KeyframeAnimations.degreeVec(0.0F,  0.0F, 10.0F),  AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.2917F, KeyframeAnimations.degreeVec(0.0F,  0.0F, 10.0F),  AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.5417F, KeyframeAnimations.degreeVec(0.0F,  0.0F, 10.0F),  AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.875F,  KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F),  AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("l_finger_ring", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,    KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F),  AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.1667F, KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F),  AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.4583F, KeyframeAnimations.degreeVec(0.0F,  0.0F, 60.0F),  AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.8333F, KeyframeAnimations.degreeVec(10.0F, 0.0F, 15.0F),  AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.2917F, KeyframeAnimations.degreeVec(10.0F, 0.0F, 15.0F),  AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.5417F, KeyframeAnimations.degreeVec(10.0F, 0.0F, 15.0F),  AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.875F,  KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F),  AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("l_finger_pinky", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F,    KeyframeAnimations.degreeVec(0.0F,   0.0F,   0.0F),  AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.1667F, KeyframeAnimations.degreeVec(0.0F,   0.0F,   0.0F),  AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.4583F, KeyframeAnimations.degreeVec(0.0F,   0.0F,  75.0F),  AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.8333F, KeyframeAnimations.degreeVec(21.1107F, 4.599F, 12.1423F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.2917F, KeyframeAnimations.degreeVec(21.1107F, 4.599F, 12.1423F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.5417F, KeyframeAnimations.degreeVec(21.1107F, 4.599F, 12.1423F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.875F,  KeyframeAnimations.degreeVec(0.0F,   0.0F,   0.0F),  AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.1667F, KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.375F,  KeyframeAnimations.degreeVec(-25.0F,0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5833F, KeyframeAnimations.degreeVec(-25.0F,0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.7917F, KeyframeAnimations.degreeVec(-25.0F,0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.4583F, KeyframeAnimations.degreeVec(55.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.8333F, KeyframeAnimations.degreeVec(55.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.2917F, KeyframeAnimations.degreeVec(55.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.5417F, KeyframeAnimations.degreeVec(55.0F, 0.0F,  0.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(2.875F,  KeyframeAnimations.degreeVec(0.0F,  0.0F,  0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .build();
}
