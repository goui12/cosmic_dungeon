package net.minecraft.client.resources.model;

import net.minecraft.client.renderer.block.model.TextureSlots;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@FunctionalInterface
@OnlyIn(Dist.CLIENT)
public interface UnbakedGeometry extends net.neoforged.neoforge.client.extensions.UnbakedGeometryExtension {
    UnbakedGeometry EMPTY = (p_405112_, p_405354_, p_405672_, p_405151_) -> QuadCollection.EMPTY;

    /**
     * @deprecated Neo: Use {@link #bake(TextureSlots, ModelBaker, ModelState,
     *             ModelDebugName, net.minecraft.util.context.ContextMap)}.
     */
    @Deprecated
    QuadCollection bake(TextureSlots textureSlots, ModelBaker baker, ModelState modelState, ModelDebugName debugName);
}
