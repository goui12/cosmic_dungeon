package net.goui.cosmicdungeon.client.renderstate;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

public class StoneWardenRenderState extends LivingEntityRenderState {
    public final AnimationState walkAnimation = new AnimationState();
    public final AnimationState attackAnimation = new AnimationState();
}
