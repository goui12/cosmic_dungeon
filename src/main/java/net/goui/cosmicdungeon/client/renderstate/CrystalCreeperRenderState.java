package net.goui.cosmicdungeon.client.renderstate;

import net.goui.cosmicdungeon.entity.CrystalCreeperEntity;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

public class CrystalCreeperRenderState extends LivingEntityRenderState {

    /** The eater animation the entity syncs to the client. */
    public AnimationState eatAnimation;

    /** Used by renderer to scale the whole model by growth stage. */
    public float visualScale = 1.0F;

    /** Required by animation system. Your renderer fills this automatically. */
    public float ageInTicks;

    // inside CrystalCreeperRenderState
    public CrystalCreeperEntity.Variant variant = CrystalCreeperEntity.Variant.TEAL;

}
