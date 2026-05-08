package net.minecraft.client.resources.model;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ResolvableModel {
    void resolveDependencies(ResolvableModel.Resolver resolver);

    @OnlyIn(Dist.CLIENT)
    public interface Resolver {
        void markDependency(ResourceLocation dependency);
    }
}
