package net.minecraft.client.resources.model;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ModelBaker extends net.neoforged.neoforge.client.extensions.ModelBakerExtension {
    ResolvedModel getModel(ResourceLocation modelLocation);

    SpriteGetter sprites();

    <T> T compute(ModelBaker.SharedOperationKey<T> key);

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface SharedOperationKey<T> {
        T compute(ModelBaker baker);
    }
}
