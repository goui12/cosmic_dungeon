package net.minecraft.client.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.state.ParticleGroupRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NoRenderParticleGroup extends ParticleGroup<NoRenderParticle> {
    private static final ParticleGroupRenderState EMPTY_RENDER_STATE = (p_446463_, p_451402_) -> {};

    public NoRenderParticleGroup(ParticleEngine p_446228_) {
        super(p_446228_);
    }

    @Override
    public ParticleGroupRenderState extractRenderState(Frustum p_446598_, Camera p_446980_, float p_445524_) {
        return EMPTY_RENDER_STATE;
    }
}
