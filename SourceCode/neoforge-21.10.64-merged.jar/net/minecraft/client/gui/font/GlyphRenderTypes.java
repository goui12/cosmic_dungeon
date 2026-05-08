package net.minecraft.client.gui.font;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record GlyphRenderTypes(RenderType normal, RenderType seeThrough, RenderType polygonOffset, RenderPipeline guiPipeline,
                               // Neo: Allow linear filtering for in-world text
                               RenderType normalBlur, RenderType seeThroughBlur, RenderType polygonOffsetBlur) {

    /** @deprecated Neo: Use {@link GlyphRenderTypes(RenderType,RenderType,RenderType,RenderPipeline,RenderType,RenderType,RenderType)} instead */
    @Deprecated
    public GlyphRenderTypes(RenderType normal, RenderType seeThrough, RenderType polygonOffset, RenderPipeline guiPipeline) {
        this(normal, seeThrough, polygonOffset, guiPipeline, normal, seeThrough, polygonOffset);
    }

    public static GlyphRenderTypes createForIntensityTexture(ResourceLocation id) {
        return new GlyphRenderTypes(
            RenderType.textIntensity(id),
            RenderType.textIntensitySeeThrough(id),
            RenderType.textIntensityPolygonOffset(id),
            RenderPipelines.GUI_TEXT_INTENSITY,
            net.neoforged.neoforge.client.NeoForgeRenderTypes.getTextIntensityFiltered(id),
            net.neoforged.neoforge.client.NeoForgeRenderTypes.getTextIntensitySeeThroughFiltered(id),
            net.neoforged.neoforge.client.NeoForgeRenderTypes.getTextIntensityPolygonOffsetFiltered(id)
        );
    }

    public static GlyphRenderTypes createForColorTexture(ResourceLocation id) {
        return new GlyphRenderTypes(
            RenderType.text(id), RenderType.textSeeThrough(id), RenderType.textPolygonOffset(id), RenderPipelines.GUI_TEXT,
            net.neoforged.neoforge.client.NeoForgeRenderTypes.getTextFiltered(id),
            net.neoforged.neoforge.client.NeoForgeRenderTypes.getTextSeeThroughFiltered(id),
            net.neoforged.neoforge.client.NeoForgeRenderTypes.getTextPolygonOffsetFiltered(id)
        );
    }

    public RenderType select(Font.DisplayMode displayMode) {
        return this.select(displayMode, false);
    }

    /**
     * Neo: returns the
     * {@link RenderType}
     * to use for the given
     * {@link Font.DisplayMode}
     * and blur setting
     */
    public RenderType select(Font.DisplayMode displayMode, boolean blur) {
        return switch (displayMode) {
            case NORMAL -> blur ? this.normalBlur : this.normal;
            case SEE_THROUGH -> blur ? this.seeThroughBlur : this.seeThrough;
            case POLYGON_OFFSET -> blur ? this.polygonOffsetBlur : this.polygonOffset;
        };
    }
}
