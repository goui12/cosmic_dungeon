package net.minecraft.client.renderer.block.model;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * @param vertices Joined 4 vertex records, each stores packed data according to the VertexFormat of the quad. Vanilla minecraft uses DefaultVertexFormats.BLOCK, Forge uses (usually) ITEM, use BakedQuad.getFormat() to get the correct format.
 *
 * @param hasAmbientOcclusion {@code false} to force-disable AO for this quad or {
 *                            @code true} to use the behavior dictated by {@link
 *                            net.neoforged.neoforge.client.extensions.IBakedModelExtension
 *                            #useAmbientOcclusion(
 *                            net.minecraft.world.level.block.state.BlockState,
 *                            net.neoforged.neoforge.model.data.ModelData,
 *                            net.minecraft.client.renderer.RenderType)}
 */
@OnlyIn(Dist.CLIENT)
public record BakedQuad(int[] vertices, int tintIndex, Direction direction, TextureAtlasSprite sprite, boolean shade, int lightEmission, boolean hasAmbientOcclusion) {
    public BakedQuad(int[] vertices, int tintIndex, Direction direction, TextureAtlasSprite sprite, boolean shade, int lightEmission) {
        this(vertices, tintIndex, direction, sprite, shade, lightEmission, true);
    }

    public boolean isTinted() {
        return this.tintIndex != -1;
    }
}
