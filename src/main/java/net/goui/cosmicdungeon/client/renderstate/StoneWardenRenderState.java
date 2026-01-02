package net.goui.cosmicdungeon.client.renderstate;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

/**
 * Render state for Stone Warden.
 *
 * Entity owns the AnimationState instances; this class just holds references.
 */
public class StoneWardenRenderState extends LivingEntityRenderState {
    /** Driven by StoneWardenEntity.walkAnimation. */
    public AnimationState walkAnimation;

    /** Driven by StoneWardenEntity.attackAnimation. */
    public AnimationState attackAnimation;
}
