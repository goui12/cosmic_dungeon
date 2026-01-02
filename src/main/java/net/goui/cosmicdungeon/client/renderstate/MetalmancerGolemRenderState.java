package net.goui.cosmicdungeon.client.renderstate;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

/**
 * Render state for the Metalmancer Golem.
 * Currently just uses the base LivingEntityRenderState fields.
 */
public class MetalmancerGolemRenderState extends LivingEntityRenderState {
    /** EDIT THESE MANUALLY.  */
    public AnimationState walkAnimation;
    public AnimationState attackAnimation;
    public AnimationState summonAnimation;
    public AnimationState idle1Animation;
    public AnimationState idle2Animation;
    public AnimationState idle3Animation;
    public AnimationState walkingAnimation;
    public AnimationState deathAnimation;
}

