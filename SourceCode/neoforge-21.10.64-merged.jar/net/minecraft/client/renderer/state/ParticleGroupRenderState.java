package net.minecraft.client.renderer.state;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ParticleGroupRenderState {
    void submit(SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState);

    default void clear() {
    }
}
