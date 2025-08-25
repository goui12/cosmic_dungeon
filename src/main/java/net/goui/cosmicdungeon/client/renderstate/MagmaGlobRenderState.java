package net.goui.cosmicdungeon.client.renderstate;

// MagmaGlobRenderState.java
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

public class MagmaGlobRenderState extends LivingEntityRenderState {
    public float ageInTicks;
    public AnimationState walkAnimation;
    public AnimationState attackAnimation;
}