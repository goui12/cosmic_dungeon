package net.goui.cosmicdungeon.client.renderstate;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

/**
 * Render state for Goblin Ambusher (NeoForge 1.21.8).
 *
 * - Inherits standard LivingEntity fields (ageInTicks, walkAnimationPos, walkAnimationSpeed, etc.).
 * - Exposes an AnimationState "attackAnimation" which we use to drive the ShootAnimation.
 */
public class GoblinAmbusherRenderState extends LivingEntityRenderState {
    /** Optional channel; walk uses walkAnimationPos/Speed. */
    public final AnimationState walkAnimation = new AnimationState();

    /** Used by GoblinAmbusherModel to play the ShootAnimation (copied from entity.attackAnimation). */
    public final AnimationState attackAnimation = new AnimationState();

    public boolean isAttacking() { return attackAnimation.isStarted(); }
}
