package net.minecraft.client.renderer.state;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ParticlesRenderState {
    public final List<ParticleGroupRenderState> particles = new ArrayList<>();

    public void reset() {
        this.particles.forEach(ParticleGroupRenderState::clear);
        this.particles.clear();
    }

    public void add(ParticleGroupRenderState renderState) {
        this.particles.add(renderState);
    }

    public void submit(SubmitNodeStorage nodeStorage, CameraRenderState cameraRenderState) {
        for (ParticleGroupRenderState particlegrouprenderstate : this.particles) {
            particlegrouprenderstate.submit(nodeStorage, cameraRenderState);
        }
    }
}
