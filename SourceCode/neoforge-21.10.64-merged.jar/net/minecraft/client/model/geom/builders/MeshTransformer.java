package net.minecraft.client.model.geom.builders;

import net.minecraft.client.model.geom.PartPose;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@FunctionalInterface
@OnlyIn(Dist.CLIENT)
public interface MeshTransformer {
    MeshTransformer IDENTITY = p_396294_ -> p_396294_;

    static MeshTransformer scaling(float scale) {
        float f = 24.016F * (1.0F - scale);
        return p_362687_ -> p_362687_.transformed(p_362796_ -> p_362796_.scaled(scale).translated(0.0F, f, 0.0F));
    }

    MeshDefinition apply(MeshDefinition mesh);
}
