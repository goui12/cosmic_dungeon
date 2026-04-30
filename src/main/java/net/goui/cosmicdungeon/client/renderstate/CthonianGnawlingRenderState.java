package net.goui.cosmicdungeon.client.renderstate;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

public class CthonianGnawlingRenderState extends LivingEntityRenderState {
    public float ageInTicks;
    public float walkAmount;
    public boolean isLatched;
    public AnimationState chompAnimation;
}
