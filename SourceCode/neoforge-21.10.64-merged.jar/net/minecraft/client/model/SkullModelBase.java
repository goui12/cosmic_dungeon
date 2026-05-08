package net.minecraft.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class SkullModelBase extends Model<SkullModelBase.State> {
    public SkullModelBase(ModelPart root) {
        super(root, RenderType::entityTranslucent);
    }

    @OnlyIn(Dist.CLIENT)
    public static class State {
        public float animationPos;
        public float yRot;
        public float xRot;
    }
}
