package net.minecraft.client.resources.model;

import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ResolvedModel extends ModelDebugName, net.neoforged.neoforge.client.extensions.ResolvedModelExtension {
    boolean DEFAULT_AMBIENT_OCCLUSION = true;
    UnbakedModel.GuiLight DEFAULT_GUI_LIGHT = UnbakedModel.GuiLight.SIDE;

    UnbakedModel wrapped();

    @Nullable
    ResolvedModel parent();

    static TextureSlots findTopTextureSlots(ResolvedModel model) {
        ResolvedModel resolvedmodel = model;

        TextureSlots.Resolver textureslots$resolver;
        for (textureslots$resolver = new TextureSlots.Resolver(); resolvedmodel != null; resolvedmodel = resolvedmodel.parent()) {
            textureslots$resolver.addLast(resolvedmodel.wrapped().textureSlots());
        }

        return textureslots$resolver.resolve(model);
    }

    default TextureSlots getTopTextureSlots() {
        return findTopTextureSlots(this);
    }

    static boolean findTopAmbientOcclusion(ResolvedModel model) {
        while (model != null) {
            Boolean obool = model.wrapped().ambientOcclusion();
            if (obool != null) {
                return obool;
            }

            model = model.parent();
        }

        return true;
    }

    default boolean getTopAmbientOcclusion() {
        return findTopAmbientOcclusion(this);
    }

    static UnbakedModel.GuiLight findTopGuiLight(ResolvedModel model) {
        while (model != null) {
            UnbakedModel.GuiLight unbakedmodel$guilight = model.wrapped().guiLight();
            if (unbakedmodel$guilight != null) {
                return unbakedmodel$guilight;
            }

            model = model.parent();
        }

        return DEFAULT_GUI_LIGHT;
    }

    default UnbakedModel.GuiLight getTopGuiLight() {
        return findTopGuiLight(this);
    }

    static UnbakedGeometry findTopGeometry(ResolvedModel model) {
        while (model != null) {
            UnbakedGeometry unbakedgeometry = model.wrapped().geometry();
            if (unbakedgeometry != null) {
                return unbakedgeometry;
            }

            model = model.parent();
        }

        return UnbakedGeometry.EMPTY;
    }

    default UnbakedGeometry getTopGeometry() {
        return findTopGeometry(this);
    }

    default QuadCollection bakeTopGeometry(TextureSlots textureSlots, ModelBaker modelBaker, ModelState modelState) {
        return this.getTopGeometry().bake(textureSlots, modelBaker, modelState, this, this.getTopAdditionalProperties());
    }

    static TextureAtlasSprite resolveParticleSprite(TextureSlots textureSlots, ModelBaker modelBaker, ModelDebugName debugName) {
        return modelBaker.sprites().resolveSlot(textureSlots, "particle", debugName);
    }

    default TextureAtlasSprite resolveParticleSprite(TextureSlots textureSlots, ModelBaker modelBaker) {
        return resolveParticleSprite(textureSlots, modelBaker, this);
    }

    static ItemTransform findTopTransform(ResolvedModel model, ItemDisplayContext displayContext) {
        while (model != null) {
            ItemTransforms itemtransforms = model.wrapped().transforms();
            if (itemtransforms != null) {
                ItemTransform itemtransform = itemtransforms.getTransform(displayContext);
                if (itemtransform != ItemTransform.NO_TRANSFORM) {
                    return itemtransform;
                }
            }

            model = model.parent();
        }

        return ItemTransform.NO_TRANSFORM;
    }

    static ItemTransforms findTopTransforms(ResolvedModel model) {
        ItemTransform itemtransform = findTopTransform(model, ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
        ItemTransform itemtransform1 = findTopTransform(model, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
        ItemTransform itemtransform2 = findTopTransform(model, ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
        ItemTransform itemtransform3 = findTopTransform(model, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND);
        ItemTransform itemtransform4 = findTopTransform(model, ItemDisplayContext.HEAD);
        ItemTransform itemtransform5 = findTopTransform(model, ItemDisplayContext.GUI);
        ItemTransform itemtransform6 = findTopTransform(model, ItemDisplayContext.GROUND);
        ItemTransform itemtransform7 = findTopTransform(model, ItemDisplayContext.FIXED);
        ItemTransform itemtransform8 = findTopTransform(model, ItemDisplayContext.ON_SHELF);
        com.google.common.collect.ImmutableMap.Builder<ItemDisplayContext, ItemTransform> moddedTransforms = com.google.common.collect.ImmutableMap.builder();
        for (ItemDisplayContext context : ItemDisplayContext.values()) {
            if (context.isModded()) {
                ItemTransform transform = findTopTransform(model, context);
                if (transform != ItemTransform.NO_TRANSFORM) {
                    moddedTransforms.put(context, transform);
                }
            }
        }
        return new ItemTransforms(
            itemtransform, itemtransform1, itemtransform2, itemtransform3, itemtransform4, itemtransform5, itemtransform6, itemtransform7, itemtransform8, moddedTransforms.build()
        );
    }

    default ItemTransforms getTopTransforms() {
        return findTopTransforms(this);
    }
}
