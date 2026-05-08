package net.minecraft.client.renderer.block.model;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record SimpleModelWrapper(QuadCollection quads, boolean useAmbientOcclusion, TextureAtlasSprite particleIcon, @Nullable net.minecraft.client.renderer.chunk.ChunkSectionLayer renderType) implements BlockModelPart {
    @Deprecated // Neo: Use render type aware version
    public SimpleModelWrapper(QuadCollection quads, boolean useAmbientOcclusion, TextureAtlasSprite particleIcon) {
        this(quads, useAmbientOcclusion, particleIcon, null);
    }

    public static SimpleModelWrapper bake(ModelBaker baker, ResourceLocation modelLocation, ModelState modelState) {
        ResolvedModel resolvedmodel = baker.getModel(modelLocation);
        return bake(baker, resolvedmodel, modelState);
    }

    // Neo: split off to allow baking an existing ResolvedModel into a BlockModelPart
    public static SimpleModelWrapper bake(ModelBaker p_405335_, ResolvedModel resolvedmodel, ModelState p_405869_) {
        TextureSlots textureslots = resolvedmodel.getTopTextureSlots();
        boolean flag = resolvedmodel.getTopAmbientOcclusion();
        TextureAtlasSprite textureatlassprite = resolvedmodel.resolveParticleSprite(textureslots, p_405335_);
        QuadCollection quadcollection = resolvedmodel.bakeTopGeometry(textureslots, p_405335_, p_405869_);
        var renderTypeGroup = resolvedmodel.getTopAdditionalProperties().getOptional(net.neoforged.neoforge.client.model.NeoForgeModelProperties.RENDER_TYPE);
        var renderTypes = renderTypeGroup == null || renderTypeGroup.isEmpty() ? null : renderTypeGroup.block();
        return new SimpleModelWrapper(quadcollection, flag, textureatlassprite, renderTypes);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable Direction direction) {
        return this.quads.getQuads(direction);
    }

    @Override
    public net.minecraft.client.renderer.chunk.ChunkSectionLayer getRenderType(net.minecraft.world.level.block.state.BlockState state) {
        return this.renderType != null ? this.renderType : BlockModelPart.super.getRenderType(state);
    }
}
