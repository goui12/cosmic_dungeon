package net.goui.cosmicdungeon.client.renderstate;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

/**
 * Render state for Magma Glob.
 *
 * Entity owns the AnimationState instances; this class just holds references.
 */
public class MagmaGlobRenderState extends LivingEntityRenderState {
    /** Age used for simple bobbing/phase-based effects in the model. */
    public float ageInTicks;

    /** Driven by MagmaGlobEntity.walkAnimation. */
    public AnimationState walkAnimation;

    /** Driven by MagmaGlobEntity.attackAnimation. */
    public AnimationState attackAnimation;
}
