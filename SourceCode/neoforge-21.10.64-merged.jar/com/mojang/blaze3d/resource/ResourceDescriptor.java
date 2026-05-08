package com.mojang.blaze3d.resource;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ResourceDescriptor<T> {
    T allocate();

    default void prepare(T target) {
    }

    void free(T target);

    default boolean canUsePhysicalResource(ResourceDescriptor<?> descriptor) {
        return this.equals(descriptor);
    }
}
