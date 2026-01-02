package net.goui.cosmicdungeon.client.renderstate;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

/**
 * Render state for Goblin Ambusher.
 *
 * Entity owns the AnimationState instances; this class just holds references.
 */
public class GoblinAmbusherRenderState extends LivingEntityRenderState {
    /** Driven by GoblinAmbusherEntity.walkLoop. */
    public AnimationState walkAnimation;

    /** Driven by GoblinAmbusherEntity.attackAnimation. */
    public AnimationState attackAnimation;

    public boolean isAttacking() {
        return attackAnimation != null && attackAnimation.isStarted();
    }
}
