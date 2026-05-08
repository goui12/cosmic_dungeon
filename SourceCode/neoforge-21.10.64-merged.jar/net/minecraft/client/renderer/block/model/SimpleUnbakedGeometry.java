package net.minecraft.client.renderer.block.model;

import java.util.List;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.client.resources.model.SpriteGetter;
import net.minecraft.client.resources.model.UnbakedGeometry;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record SimpleUnbakedGeometry(List<BlockElement> elements) implements UnbakedGeometry, net.neoforged.neoforge.client.model.ExtendedUnbakedGeometry {
    @Override
    public QuadCollection bake(TextureSlots textureSlots, ModelBaker baker, ModelState modelState, ModelDebugName debugName, net.minecraft.util.context.ContextMap additionalProperties) {
        var transform = additionalProperties.getOptional(net.neoforged.neoforge.client.model.NeoForgeModelProperties.TRANSFORM);
        if (transform != null) {
            modelState = net.neoforged.neoforge.client.model.UnbakedElementsHelper.composeRootTransformIntoModelState(modelState, transform);
        }
        return bake(this.elements, textureSlots, baker.sprites(), modelState, debugName);
    }

    public static QuadCollection bake(
        List<BlockElement> elements, TextureSlots textureSlots, SpriteGetter sprites, ModelState modelState, ModelDebugName debugName
    ) {
        QuadCollection.Builder quadcollection$builder = new QuadCollection.Builder();

        for (BlockElement blockelement : elements) {
            blockelement.faces()
                .forEach(
                    (p_405876_, p_404900_) -> {
                        TextureAtlasSprite textureatlassprite = sprites.resolveSlot(textureSlots, p_404900_.texture(), debugName);
                        if (p_404900_.cullForDirection() == null) {
                            quadcollection$builder.addUnculledFace(bakeFace(blockelement, p_404900_, textureatlassprite, p_405876_, modelState));
                        } else {
                            quadcollection$builder.addCulledFace(
                                Direction.rotate(modelState.transformation().getMatrix(), p_404900_.cullForDirection()),
                                bakeFace(blockelement, p_404900_, textureatlassprite, p_405876_, modelState)
                            );
                        }
                    }
                );
        }

        return quadcollection$builder.build();
    }

    public static BakedQuad bakeFace(
        BlockElement element, BlockElementFace face, TextureAtlasSprite sprite, Direction direction, ModelState modelState
    ) {
        return FaceBakery.bakeQuad(
            element.from(), element.to(), face, sprite, direction, modelState, element.rotation(), element.shade(), element.lightEmission()
        );
    }
}
