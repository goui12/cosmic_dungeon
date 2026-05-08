package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.renderer.blockentity.AbstractEndPortalRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
public abstract class RenderType extends RenderStateShard {
    public static final int MEGABYTE = 1048576;
    public static final int BIG_BUFFER_SIZE = 4194304;
    public static final int SMALL_BUFFER_SIZE = 786432;
    public static final int TRANSIENT_BUFFER_SIZE = 1536;
    public static final RenderType SOLID = create(
        "solid",
        1536,
        true,
        false,
        RenderPipelines.SOLID,
        RenderType.CompositeState.builder().setLightmapState(LIGHTMAP).setTextureState(BLOCK_SHEET_MIPPED).createCompositeState(true)
    );
    public static final RenderType CUTOUT_MIPPED = create(
        "cutout_mipped",
        1536,
        true,
        false,
        RenderPipelines.CUTOUT_MIPPED,
        RenderType.CompositeState.builder().setLightmapState(LIGHTMAP).setTextureState(BLOCK_SHEET_MIPPED).createCompositeState(true)
    );
    public static final RenderType CUTOUT = create(
        "cutout",
        1536,
        true,
        false,
        RenderPipelines.CUTOUT,
        RenderType.CompositeState.builder().setLightmapState(LIGHTMAP).setTextureState(BLOCK_SHEET).createCompositeState(true)
    );
    public static final RenderType TRANSLUCENT_MOVING_BLOCK = create(
        "translucent_moving_block",
        786432,
        false,
        true,
        RenderPipelines.TRANSLUCENT_MOVING_BLOCK,
        RenderType.CompositeState.builder()
            .setLightmapState(LIGHTMAP)
            .setTextureState(BLOCK_SHEET_MIPPED)
            .setOutputState(ITEM_ENTITY_TARGET)
            .createCompositeState(true)
    );
    public static final Function<ResourceLocation, RenderType> ARMOR_CUTOUT_NO_CULL = Util.memoize(
        p_414940_ -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(p_414940_, false))
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                .createCompositeState(true);
            return create("armor_cutout_no_cull", 1536, true, false, RenderPipelines.ARMOR_CUTOUT_NO_CULL, rendertype$compositestate);
        }
    );
    public static final Function<ResourceLocation, RenderType> ARMOR_TRANSLUCENT = Util.memoize(
        p_414951_ -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(p_414951_, false))
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                .createCompositeState(true);
            return create("armor_translucent", 1536, true, true, RenderPipelines.ARMOR_TRANSLUCENT, rendertype$compositestate);
        }
    );
    public static final Function<ResourceLocation, RenderType> ENTITY_SOLID = Util.memoize(
        p_414948_ -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(p_414948_, false))
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(true);
            return create("entity_solid", 1536, true, false, RenderPipelines.ENTITY_SOLID, rendertype$compositestate);
        }
    );
    public static final Function<ResourceLocation, RenderType> ENTITY_SOLID_Z_OFFSET_FORWARD = Util.memoize(
        p_414953_ -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(p_414953_, false))
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setLayeringState(VIEW_OFFSET_Z_LAYERING_FORWARD)
                .createCompositeState(true);
            return create("entity_solid_z_offset_forward", 1536, true, false, RenderPipelines.ENTITY_SOLID_Z_OFFSET_FORWARD, rendertype$compositestate);
        }
    );
    public static final Function<ResourceLocation, RenderType> ENTITY_CUTOUT = Util.memoize(
        p_414924_ -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(p_414924_, false))
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(true);
            return create("entity_cutout", 1536, true, false, RenderPipelines.ENTITY_CUTOUT, rendertype$compositestate);
        }
    );
    public static final BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_CUTOUT_NO_CULL = Util.memoize(
        (p_414949_, p_414950_) -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(p_414949_, false))
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(p_414950_);
            return create("entity_cutout_no_cull", 1536, true, false, RenderPipelines.ENTITY_CUTOUT_NO_CULL, rendertype$compositestate);
        }
    );
    public static final BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_CUTOUT_NO_CULL_Z_OFFSET = Util.memoize(
        (p_414941_, p_414942_) -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(p_414941_, false))
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                .createCompositeState(p_414942_);
            return create("entity_cutout_no_cull_z_offset", 1536, true, false, RenderPipelines.ENTITY_CUTOUT_NO_CULL_Z_OFFSET, rendertype$compositestate);
        }
    );
    public static final Function<ResourceLocation, RenderType> ITEM_ENTITY_TRANSLUCENT_CULL = Util.memoize(
        p_414938_ -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(p_414938_, false))
                .setOutputState(ITEM_ENTITY_TARGET)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(true);
            return create("item_entity_translucent_cull", 1536, true, true, RenderPipelines.ITEM_ENTITY_TRANSLUCENT_CULL, rendertype$compositestate);
        }
    );
    public static final BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_TRANSLUCENT = Util.memoize(
        (p_414943_, p_414944_) -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(p_414943_, false))
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(p_414944_);
            return create("entity_translucent", 1536, true, true, RenderPipelines.ENTITY_TRANSLUCENT, rendertype$compositestate);
        }
    );
    public static final BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_TRANSLUCENT_EMISSIVE = Util.memoize(
        (p_414957_, p_414958_) -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(p_414957_, false))
                .setOverlayState(OVERLAY)
                .createCompositeState(p_414958_);
            return create("entity_translucent_emissive", 1536, true, true, RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE, rendertype$compositestate);
        }
    );
    public static final Function<ResourceLocation, RenderType> ENTITY_SMOOTH_CUTOUT = Util.memoize(
        p_414928_ -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(p_414928_, false))
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(true);
            return create("entity_smooth_cutout", 1536, RenderPipelines.ENTITY_SMOOTH_CUTOUT, rendertype$compositestate);
        }
    );
    public static final BiFunction<ResourceLocation, Boolean, RenderType> BEACON_BEAM = Util.memoize(
        (p_414929_, p_414930_) -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(p_414929_, false))
                .createCompositeState(false);
            return create(
                "beacon_beam",
                1536,
                false,
                true,
                p_414930_ ? RenderPipelines.BEACON_BEAM_TRANSLUCENT : RenderPipelines.BEACON_BEAM_OPAQUE,
                rendertype$compositestate
            );
        }
    );
    public static final Function<ResourceLocation, RenderType> ENTITY_DECAL = Util.memoize(
        p_414925_ -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(p_414925_, false))
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(false);
            return create("entity_decal", 1536, RenderPipelines.ENTITY_DECAL, rendertype$compositestate);
        }
    );
    public static final Function<ResourceLocation, RenderType> ENTITY_NO_OUTLINE = Util.memoize(
        p_414954_ -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(p_414954_, false))
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(false);
            return create("entity_no_outline", 1536, false, true, RenderPipelines.ENTITY_NO_OUTLINE, rendertype$compositestate);
        }
    );
    public static final Function<ResourceLocation, RenderType> ENTITY_SHADOW = Util.memoize(
        p_414952_ -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(p_414952_, false))
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                .createCompositeState(false);
            return create("entity_shadow", 1536, false, false, RenderPipelines.ENTITY_SHADOW, rendertype$compositestate);
        }
    );
    public static final Function<ResourceLocation, RenderType> DRAGON_EXPLOSION_ALPHA = Util.memoize(
        p_414936_ -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(p_414936_, false))
                .createCompositeState(true);
            return create("entity_alpha", 1536, RenderPipelines.DRAGON_EXPLOSION_ALPHA, rendertype$compositestate);
        }
    );
    public static final Function<ResourceLocation, RenderType> EYES = Util.memoize(
        p_414934_ -> {
            RenderStateShard.TextureStateShard renderstateshard$texturestateshard = new RenderStateShard.TextureStateShard(p_414934_, false);
            return create(
                "eyes",
                1536,
                false,
                true,
                RenderPipelines.EYES,
                RenderType.CompositeState.builder().setTextureState(renderstateshard$texturestateshard).createCompositeState(false)
            );
        }
    );
    public static final RenderType LEASH = create(
        "leash",
        1536,
        RenderPipelines.LEASH,
        RenderType.CompositeState.builder().setTextureState(NO_TEXTURE).setLightmapState(LIGHTMAP).createCompositeState(false)
    );
    public static final RenderType WATER_MASK = create(
        "water_mask", 1536, RenderPipelines.WATER_MASK, RenderType.CompositeState.builder().setTextureState(NO_TEXTURE).createCompositeState(false)
    );
    public static final RenderType ARMOR_ENTITY_GLINT = create(
        "armor_entity_glint",
        1536,
        RenderPipelines.GLINT,
        RenderType.CompositeState.builder()
            .setTextureState(new RenderStateShard.TextureStateShard(ItemRenderer.ENCHANTED_GLINT_ARMOR, false))
            .setTexturingState(ARMOR_ENTITY_GLINT_TEXTURING)
            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
            .createCompositeState(false)
    );
    public static final RenderType GLINT_TRANSLUCENT = create(
        "glint_translucent",
        1536,
        RenderPipelines.GLINT,
        RenderType.CompositeState.builder()
            .setTextureState(new RenderStateShard.TextureStateShard(ItemRenderer.ENCHANTED_GLINT_ITEM, false))
            .setTexturingState(GLINT_TEXTURING)
            .setOutputState(ITEM_ENTITY_TARGET)
            .createCompositeState(false)
    );
    public static final RenderType GLINT = create(
        "glint",
        1536,
        RenderPipelines.GLINT,
        RenderType.CompositeState.builder()
            .setTextureState(new RenderStateShard.TextureStateShard(ItemRenderer.ENCHANTED_GLINT_ITEM, false))
            .setTexturingState(GLINT_TEXTURING)
            .createCompositeState(false)
    );
    public static final RenderType ENTITY_GLINT = create(
        "entity_glint",
        1536,
        RenderPipelines.GLINT,
        RenderType.CompositeState.builder()
            .setTextureState(new RenderStateShard.TextureStateShard(ItemRenderer.ENCHANTED_GLINT_ITEM, false))
            .setTexturingState(ENTITY_GLINT_TEXTURING)
            .createCompositeState(false)
    );
    public static final Function<ResourceLocation, RenderType> CRUMBLING = Util.memoize(
        p_414947_ -> {
            RenderStateShard.TextureStateShard renderstateshard$texturestateshard = new RenderStateShard.TextureStateShard(p_414947_, false);
            return create(
                "crumbling",
                1536,
                false,
                true,
                RenderPipelines.CRUMBLING,
                RenderType.CompositeState.builder().setTextureState(renderstateshard$texturestateshard).createCompositeState(false)
            );
        }
    );
    public static final Function<ResourceLocation, RenderType> TEXT = Util.memoize(
        p_414939_ -> create(
            "text",
            786432,
            false,
            false,
            RenderPipelines.TEXT,
            RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(p_414939_, false))
                .setLightmapState(LIGHTMAP)
                .createCompositeState(false)
        )
    );
    public static final RenderType TEXT_BACKGROUND = create(
        "text_background",
        1536,
        false,
        true,
        RenderPipelines.TEXT_BACKGROUND,
        RenderType.CompositeState.builder().setTextureState(NO_TEXTURE).setLightmapState(LIGHTMAP).createCompositeState(false)
    );
    public static final Function<ResourceLocation, RenderType> TEXT_INTENSITY = Util.memoize(
        p_414955_ -> create(
            "text_intensity",
            786432,
            false,
            false,
            RenderPipelines.TEXT_INTENSITY,
            RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(p_414955_, false))
                .setLightmapState(LIGHTMAP)
                .createCompositeState(false)
        )
    );
    public static final Function<ResourceLocation, RenderType> TEXT_POLYGON_OFFSET = Util.memoize(
        p_414956_ -> create(
            "text_polygon_offset",
            1536,
            false,
            true,
            RenderPipelines.TEXT_POLYGON_OFFSET,
            RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(p_414956_, false))
                .setLightmapState(LIGHTMAP)
                .createCompositeState(false)
        )
    );
    public static final Function<ResourceLocation, RenderType> TEXT_INTENSITY_POLYGON_OFFSET = Util.memoize(
        p_414945_ -> create(
            "text_intensity_polygon_offset",
            1536,
            false,
            true,
            RenderPipelines.TEXT_INTENSITY,
            RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(p_414945_, false))
                .setLightmapState(LIGHTMAP)
                .createCompositeState(false)
        )
    );
    public static final Function<ResourceLocation, RenderType> TEXT_SEE_THROUGH = Util.memoize(
        p_414935_ -> create(
            "text_see_through",
            1536,
            false,
            false,
            RenderPipelines.TEXT_SEE_THROUGH,
            RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(p_414935_, false))
                .setLightmapState(LIGHTMAP)
                .createCompositeState(false)
        )
    );
    public static final RenderType TEXT_BACKGROUND_SEE_THROUGH = create(
        "text_background_see_through",
        1536,
        false,
        true,
        RenderPipelines.TEXT_BACKGROUND_SEE_THROUGH,
        RenderType.CompositeState.builder().setTextureState(NO_TEXTURE).setLightmapState(LIGHTMAP).createCompositeState(false)
    );
    public static final Function<ResourceLocation, RenderType> TEXT_INTENSITY_SEE_THROUGH = Util.memoize(
        p_414946_ -> create(
            "text_intensity_see_through",
            1536,
            false,
            true,
            RenderPipelines.TEXT_INTENSITY_SEE_THROUGH,
            RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(p_414946_, false))
                .setLightmapState(LIGHTMAP)
                .createCompositeState(false)
        )
    );
    public static final RenderType LIGHTNING = create(
        "lightning",
        1536,
        false,
        true,
        RenderPipelines.LIGHTNING,
        RenderType.CompositeState.builder().setOutputState(WEATHER_TARGET).createCompositeState(false)
    );
    public static final RenderType DRAGON_RAYS = create(
        "dragon_rays", 1536, false, false, RenderPipelines.DRAGON_RAYS, RenderType.CompositeState.builder().createCompositeState(false)
    );
    public static final RenderType DRAGON_RAYS_DEPTH = create(
        "dragon_rays_depth", 1536, false, false, RenderPipelines.DRAGON_RAYS_DEPTH, RenderType.CompositeState.builder().createCompositeState(false)
    );
    public static final RenderType TRIPWIRE = create(
        "tripwire",
        1536,
        true,
        true,
        RenderPipelines.TRIPWIRE,
        RenderType.CompositeState.builder()
            .setLightmapState(LIGHTMAP)
            .setTextureState(BLOCK_SHEET_MIPPED)
            .setOutputState(WEATHER_TARGET)
            .createCompositeState(true)
    );
    public static final RenderType END_PORTAL = create(
        "end_portal",
        1536,
        false,
        false,
        RenderPipelines.END_PORTAL,
        RenderType.CompositeState.builder()
            .setTextureState(
                RenderStateShard.MultiTextureStateShard.builder()
                    .add(AbstractEndPortalRenderer.END_SKY_LOCATION, false)
                    .add(AbstractEndPortalRenderer.END_PORTAL_LOCATION, false)
                    .build()
            )
            .createCompositeState(false)
    );
    public static final RenderType END_GATEWAY = create(
        "end_gateway",
        1536,
        false,
        false,
        RenderPipelines.END_GATEWAY,
        RenderType.CompositeState.builder()
            .setTextureState(
                RenderStateShard.MultiTextureStateShard.builder()
                    .add(AbstractEndPortalRenderer.END_SKY_LOCATION, false)
                    .add(AbstractEndPortalRenderer.END_PORTAL_LOCATION, false)
                    .build()
            )
            .createCompositeState(false)
    );
    public static final RenderType.CompositeRenderType LINES = create(
        "lines",
        1536,
        RenderPipelines.LINES,
        RenderType.CompositeState.builder()
            .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.empty()))
            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
            .setOutputState(ITEM_ENTITY_TARGET)
            .createCompositeState(false)
    );
    public static final RenderType.CompositeRenderType SECONDARY_BLOCK_OUTLINE = create(
        "secondary_block_outline",
        1536,
        RenderPipelines.SECONDARY_BLOCK_OUTLINE,
        RenderType.CompositeState.builder()
            .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(7.0)))
            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
            .setOutputState(ITEM_ENTITY_TARGET)
            .createCompositeState(false)
    );
    public static final RenderType.CompositeRenderType LINE_STRIP = create(
        "line_strip",
        1536,
        RenderPipelines.LINE_STRIP,
        RenderType.CompositeState.builder()
            .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.empty()))
            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
            .setOutputState(ITEM_ENTITY_TARGET)
            .createCompositeState(false)
    );
    public static final Function<Double, RenderType.CompositeRenderType> DEBUG_LINE_STRIP = Util.memoize(
        p_404020_ -> create(
            "debug_line_strip",
            1536,
            RenderPipelines.DEBUG_LINE_STRIP,
            RenderType.CompositeState.builder().setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(p_404020_))).createCompositeState(false)
        )
    );
    public static final RenderType.CompositeRenderType DEBUG_FILLED_BOX = create(
        "debug_filled_box",
        1536,
        false,
        true,
        RenderPipelines.DEBUG_FILLED_BOX,
        RenderType.CompositeState.builder().setLayeringState(VIEW_OFFSET_Z_LAYERING).createCompositeState(false)
    );
    public static final RenderType.CompositeRenderType DEBUG_QUADS = create(
        "debug_quads", 1536, false, true, RenderPipelines.DEBUG_QUADS, RenderType.CompositeState.builder().createCompositeState(false)
    );
    public static final RenderType.CompositeRenderType DEBUG_TRIANGLE_FAN = create(
        "debug_triangle_fan", 1536, false, true, RenderPipelines.DEBUG_TRIANGLE_FAN, RenderType.CompositeState.builder().createCompositeState(false)
    );
    public static final RenderType.CompositeRenderType DEBUG_STRUCTURE_QUADS = create(
        "debug_structure_quads", 1536, false, true, RenderPipelines.DEBUG_STRUCTURE_QUADS, RenderType.CompositeState.builder().createCompositeState(false)
    );
    public static final RenderType.CompositeRenderType DEBUG_SECTION_QUADS = create(
        "debug_section_quads",
        1536,
        false,
        true,
        RenderPipelines.DEBUG_SECTION_QUADS,
        RenderType.CompositeState.builder().setLayeringState(VIEW_OFFSET_Z_LAYERING).createCompositeState(false)
    );
    public static final Function<ResourceLocation, RenderType> WEATHER_DEPTH_WRITE = createWeather(RenderPipelines.WEATHER_DEPTH_WRITE);
    public static final Function<ResourceLocation, RenderType> WEATHER_NO_DEPTH_WRITE = createWeather(RenderPipelines.WEATHER_NO_DEPTH_WRITE);
    public static final Function<ResourceLocation, RenderType> BLOCK_SCREEN_EFFECT = Util.memoize(
        p_414923_ -> create(
            "block_screen_effect",
            1536,
            false,
            false,
            RenderPipelines.BLOCK_SCREEN_EFFECT,
            RenderType.CompositeState.builder().setTextureState(new RenderStateShard.TextureStateShard(p_414923_, false)).createCompositeState(false)
        )
    );
    public static final Function<ResourceLocation, RenderType> FIRE_SCREEN_EFFECT = Util.memoize(
        p_414927_ -> create(
            "fire_screen_effect",
            1536,
            false,
            false,
            RenderPipelines.FIRE_SCREEN_EFFECT,
            RenderType.CompositeState.builder().setTextureState(new RenderStateShard.TextureStateShard(p_414927_, false)).createCompositeState(false)
        )
    );
    public final int bufferSize;
    public final boolean affectsCrumbling;
    public final boolean sortOnUpload;

    public static RenderType solid() {
        return SOLID;
    }

    public static RenderType cutoutMipped() {
        return CUTOUT_MIPPED;
    }

    public static RenderType cutout() {
        return CUTOUT;
    }

    public static RenderType translucentMovingBlock() {
        return TRANSLUCENT_MOVING_BLOCK;
    }

    public static RenderType armorCutoutNoCull(ResourceLocation location) {
        return ARMOR_CUTOUT_NO_CULL.apply(location);
    }

    public static RenderType createArmorDecalCutoutNoCull(ResourceLocation id) {
        RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
            .setTextureState(new RenderStateShard.TextureStateShard(id, false))
            .setLightmapState(LIGHTMAP)
            .setOverlayState(OVERLAY)
            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
            .createCompositeState(true);
        return create("armor_decal_cutout_no_cull", 1536, true, false, RenderPipelines.ARMOR_DECAL_CUTOUT_NO_CULL, rendertype$compositestate);
    }

    public static RenderType armorTranslucent(ResourceLocation id) {
        return ARMOR_TRANSLUCENT.apply(id);
    }

    public static RenderType entitySolid(ResourceLocation location) {
        return ENTITY_SOLID.apply(location);
    }

    public static RenderType entitySolidZOffsetForward(ResourceLocation location) {
        return ENTITY_SOLID_Z_OFFSET_FORWARD.apply(location);
    }

    public static RenderType entityCutout(ResourceLocation location) {
        return ENTITY_CUTOUT.apply(location);
    }

    public static RenderType entityCutoutNoCull(ResourceLocation location, boolean outline) {
        return ENTITY_CUTOUT_NO_CULL.apply(location, outline);
    }

    public static RenderType entityCutoutNoCull(ResourceLocation location) {
        return entityCutoutNoCull(location, true);
    }

    public static RenderType entityCutoutNoCullZOffset(ResourceLocation location, boolean outline) {
        return ENTITY_CUTOUT_NO_CULL_Z_OFFSET.apply(location, outline);
    }

    public static RenderType entityCutoutNoCullZOffset(ResourceLocation location) {
        return entityCutoutNoCullZOffset(location, true);
    }

    public static RenderType itemEntityTranslucentCull(ResourceLocation location) {
        return ITEM_ENTITY_TRANSLUCENT_CULL.apply(location);
    }

    public static RenderType entityTranslucent(ResourceLocation location, boolean outline) {
        return ENTITY_TRANSLUCENT.apply(location, outline);
    }

    public static RenderType entityTranslucent(ResourceLocation location) {
        return entityTranslucent(location, true);
    }

    public static RenderType entityTranslucentEmissive(ResourceLocation location, boolean outline) {
        return ENTITY_TRANSLUCENT_EMISSIVE.apply(location, outline);
    }

    public static RenderType entityTranslucentEmissive(ResourceLocation location) {
        return entityTranslucentEmissive(location, true);
    }

    public static RenderType entitySmoothCutout(ResourceLocation location) {
        return ENTITY_SMOOTH_CUTOUT.apply(location);
    }

    public static RenderType beaconBeam(ResourceLocation location, boolean colorFlag) {
        return BEACON_BEAM.apply(location, colorFlag);
    }

    public static RenderType entityDecal(ResourceLocation location) {
        return ENTITY_DECAL.apply(location);
    }

    public static RenderType entityNoOutline(ResourceLocation location) {
        return ENTITY_NO_OUTLINE.apply(location);
    }

    public static RenderType entityShadow(ResourceLocation location) {
        return ENTITY_SHADOW.apply(location);
    }

    public static RenderType dragonExplosionAlpha(ResourceLocation id) {
        return DRAGON_EXPLOSION_ALPHA.apply(id);
    }

    public static RenderType eyes(ResourceLocation location) {
        return EYES.apply(location);
    }

    public static RenderType breezeEyes(ResourceLocation location) {
        return ENTITY_TRANSLUCENT_EMISSIVE.apply(location, false);
    }

    public static RenderType breezeWind(ResourceLocation location, float u, float v) {
        return create(
            "breeze_wind",
            1536,
            false,
            true,
            RenderPipelines.BREEZE_WIND,
            RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(location, false))
                .setTexturingState(new RenderStateShard.OffsetTexturingStateShard(u, v))
                .setLightmapState(LIGHTMAP)
                .setOverlayState(NO_OVERLAY)
                .createCompositeState(false)
        );
    }

    public static RenderType energySwirl(ResourceLocation location, float u, float v) {
        return create(
            "energy_swirl",
            1536,
            false,
            true,
            RenderPipelines.ENERGY_SWIRL,
            RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(location, false))
                .setTexturingState(new RenderStateShard.OffsetTexturingStateShard(u, v))
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(false)
        );
    }

    public static RenderType leash() {
        return LEASH;
    }

    public static RenderType waterMask() {
        return WATER_MASK;
    }

    public static RenderType outline(ResourceLocation location) {
        return RenderType.CompositeRenderType.OUTLINE.apply(location, false);
    }

    public static RenderType armorEntityGlint() {
        return ARMOR_ENTITY_GLINT;
    }

    public static RenderType glintTranslucent() {
        return GLINT_TRANSLUCENT;
    }

    public static RenderType glint() {
        return GLINT;
    }

    public static RenderType entityGlint() {
        return ENTITY_GLINT;
    }

    public static RenderType crumbling(ResourceLocation location) {
        return CRUMBLING.apply(location);
    }

    public static RenderType text(ResourceLocation location) {
        return TEXT.apply(location);
    }

    public static RenderType textBackground() {
        return TEXT_BACKGROUND;
    }

    public static RenderType textIntensity(ResourceLocation id) {
        return TEXT_INTENSITY.apply(id);
    }

    public static RenderType textPolygonOffset(ResourceLocation id) {
        return TEXT_POLYGON_OFFSET.apply(id);
    }

    public static RenderType textIntensityPolygonOffset(ResourceLocation id) {
        return TEXT_INTENSITY_POLYGON_OFFSET.apply(id);
    }

    public static RenderType textSeeThrough(ResourceLocation location) {
        return TEXT_SEE_THROUGH.apply(location);
    }

    public static RenderType textBackgroundSeeThrough() {
        return TEXT_BACKGROUND_SEE_THROUGH;
    }

    public static RenderType textIntensitySeeThrough(ResourceLocation id) {
        return TEXT_INTENSITY_SEE_THROUGH.apply(id);
    }

    public static RenderType lightning() {
        return LIGHTNING;
    }

    public static RenderType dragonRays() {
        return DRAGON_RAYS;
    }

    public static RenderType dragonRaysDepth() {
        return DRAGON_RAYS_DEPTH;
    }

    public static RenderType tripwire() {
        return TRIPWIRE;
    }

    public static RenderType endPortal() {
        return END_PORTAL;
    }

    public static RenderType endGateway() {
        return END_GATEWAY;
    }

    public static RenderType lines() {
        return LINES;
    }

    public static RenderType secondaryBlockOutline() {
        return SECONDARY_BLOCK_OUTLINE;
    }

    public static RenderType lineStrip() {
        return LINE_STRIP;
    }

    public static RenderType debugLineStrip(double width) {
        return DEBUG_LINE_STRIP.apply(width);
    }

    public static RenderType debugFilledBox() {
        return DEBUG_FILLED_BOX;
    }

    public static RenderType debugQuads() {
        return DEBUG_QUADS;
    }

    public static RenderType debugTriangleFan() {
        return DEBUG_TRIANGLE_FAN;
    }

    public static RenderType debugStructureQuads() {
        return DEBUG_STRUCTURE_QUADS;
    }

    public static RenderType debugSectionQuads() {
        return DEBUG_SECTION_QUADS;
    }

    private static Function<ResourceLocation, RenderType> createWeather(RenderPipeline renderPipeline) {
        return Util.memoize(
            p_414933_ -> create(
                "weather",
                1536,
                false,
                false,
                renderPipeline,
                RenderType.CompositeState.builder()
                    .setTextureState(new RenderStateShard.TextureStateShard(p_414933_, false))
                    .setOutputState(WEATHER_TARGET)
                    .setLightmapState(LIGHTMAP)
                    .createCompositeState(false)
            )
        );
    }

    public static RenderType weather(ResourceLocation texture, boolean depthWrite) {
        return (depthWrite ? WEATHER_DEPTH_WRITE : WEATHER_NO_DEPTH_WRITE).apply(texture);
    }

    public static RenderType blockScreenEffect(ResourceLocation texture) {
        return BLOCK_SCREEN_EFFECT.apply(texture);
    }

    public static RenderType fireScreenEffect(ResourceLocation texture) {
        return FIRE_SCREEN_EFFECT.apply(texture);
    }

    public RenderType(String name, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, setupState, clearState);
        this.bufferSize = bufferSize;
        this.affectsCrumbling = affectsCrumbling;
        this.sortOnUpload = sortOnUpload;
    }

    public static RenderType.CompositeRenderType create(String name, int bufferSize, RenderPipeline renderPipeline, RenderType.CompositeState state) {
        return create(name, bufferSize, false, false, renderPipeline, state);
    }

    public static RenderType.CompositeRenderType create(
        String name, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, RenderPipeline renderPipeline, RenderType.CompositeState state
    ) {
        return new RenderType.CompositeRenderType(name, bufferSize, affectsCrumbling, sortOnUpload, renderPipeline, state);
    }

    public abstract void draw(MeshData meshData);

    public int bufferSize() {
        return this.bufferSize;
    }

    public abstract VertexFormat format();

    public abstract VertexFormat.Mode mode();

    public Optional<RenderType> outline() {
        return Optional.empty();
    }

    public boolean isOutline() {
        return false;
    }

    public abstract RenderPipeline pipeline();

    public boolean affectsCrumbling() {
        return this.affectsCrumbling;
    }

    public boolean canConsolidateConsecutiveGeometry() {
        return !this.mode().connectedPrimitives;
    }

    public boolean sortOnUpload() {
        return this.sortOnUpload;
    }

    @OnlyIn(Dist.CLIENT)
    static final class CompositeRenderType extends RenderType {
        static final BiFunction<ResourceLocation, Boolean, RenderType> OUTLINE = Util.memoize(
            (p_414959_, p_414960_) -> RenderType.create(
                "outline",
                1536,
                p_414960_ ? RenderPipelines.OUTLINE_CULL : RenderPipelines.OUTLINE_NO_CULL,
                RenderType.CompositeState.builder()
                    .setTextureState(new RenderStateShard.TextureStateShard(p_414959_, false))
                    .setOutputState(OUTLINE_TARGET)
                    .createCompositeState(RenderType.OutlineProperty.IS_OUTLINE)
            )
        );
        private final RenderType.CompositeState state;
        private final RenderPipeline renderPipeline;
        private final Optional<RenderType> outline;
        private final boolean isOutline;

        CompositeRenderType(
            String name, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, RenderPipeline renderPipeline, RenderType.CompositeState state
        ) {
            super(
                name,
                bufferSize,
                affectsCrumbling,
                sortOnUpload,
                () -> state.states.forEach(RenderStateShard::setupRenderState),
                () -> state.states.forEach(RenderStateShard::clearRenderState)
            );
            this.state = state;
            this.renderPipeline = renderPipeline;
            this.outline = state.outlineProperty == RenderType.OutlineProperty.AFFECTS_OUTLINE
                ? state.textureState.cutoutTexture().map(p_409065_ -> OUTLINE.apply(p_409065_, renderPipeline.isCull()))
                : Optional.empty();
            this.isOutline = state.outlineProperty == RenderType.OutlineProperty.IS_OUTLINE;
        }

        @Override
        public Optional<RenderType> outline() {
            return this.outline;
        }

        @Override
        public boolean isOutline() {
            return this.isOutline;
        }

        @Override
        public VertexFormat format() {
            return this.renderPipeline.getVertexFormat();
        }

        @Override
        public VertexFormat.Mode mode() {
            return this.renderPipeline.getVertexFormatMode();
        }

        @Override
        public RenderPipeline pipeline() {
            return this.renderPipeline;
        }

        @Override
        public void draw(MeshData meshData) {
            this.setupRenderState();
            GpuBufferSlice gpubufferslice = RenderSystem.getDynamicUniforms()
                .writeTransform(
                    RenderSystem.getModelViewMatrix(),
                    new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
                    new Vector3f(),
                    RenderSystem.getTextureMatrix(),
                    RenderSystem.getShaderLineWidth()
                );
            MeshData meshdata = meshData;

            try {
                GpuBuffer gpubuffer = this.renderPipeline.getVertexFormat().uploadImmediateVertexBuffer(meshData.vertexBuffer());
                GpuBuffer gpubuffer1;
                VertexFormat.IndexType vertexformat$indextype;
                if (meshData.indexBuffer() == null) {
                    RenderSystem.AutoStorageIndexBuffer rendersystem$autostorageindexbuffer = RenderSystem.getSequentialBuffer(meshData.drawState().mode());
                    gpubuffer1 = rendersystem$autostorageindexbuffer.getBuffer(meshData.drawState().indexCount());
                    vertexformat$indextype = rendersystem$autostorageindexbuffer.type();
                } else {
                    gpubuffer1 = this.renderPipeline.getVertexFormat().uploadImmediateIndexBuffer(meshData.indexBuffer());
                    vertexformat$indextype = meshData.drawState().indexType();
                }

                RenderTarget rendertarget = this.state.outputState.getRenderTarget();
                GpuTextureView gputextureview = RenderSystem.outputColorTextureOverride != null
                    ? RenderSystem.outputColorTextureOverride
                    : rendertarget.getColorTextureView();
                GpuTextureView gputextureview1 = rendertarget.useDepth
                    ? (RenderSystem.outputDepthTextureOverride != null ? RenderSystem.outputDepthTextureOverride : rendertarget.getDepthTextureView())
                    : null;

                try (RenderPass renderpass = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(
                            () -> "Immediate draw for " + this.getName(), gputextureview, OptionalInt.empty(), gputextureview1, OptionalDouble.empty()
                        )) {
                    renderpass.setPipeline(this.renderPipeline);
                    ScissorState scissorstate = RenderSystem.getScissorStateForRenderTypeDraws();
                    if (scissorstate.enabled()) {
                        renderpass.enableScissor(scissorstate.x(), scissorstate.y(), scissorstate.width(), scissorstate.height());
                    }

                    RenderSystem.bindDefaultUniforms(renderpass);
                    renderpass.setUniform("DynamicTransforms", gpubufferslice);
                    renderpass.setVertexBuffer(0, gpubuffer);

                    for (int i = 0; i < 12; i++) {
                        GpuTextureView gputextureview2 = RenderSystem.getShaderTexture(i);
                        if (gputextureview2 != null) {
                            renderpass.bindSampler("Sampler" + i, gputextureview2);
                        }
                    }

                    renderpass.setIndexBuffer(gpubuffer1, vertexformat$indextype);
                    renderpass.drawIndexed(0, 0, meshData.drawState().indexCount(), 1);
                }
            } catch (Throwable throwable2) {
                if (meshData != null) {
                    try {
                        meshdata.close();
                    } catch (Throwable throwable) {
                        throwable2.addSuppressed(throwable);
                    }
                }

                throw throwable2;
            }

            if (meshData != null) {
                meshData.close();
            }

            this.clearRenderState();
        }

        @Override
        public String toString() {
            return "RenderType[" + this.name + ":" + this.state + "]";
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static final class CompositeState {
        final RenderStateShard.EmptyTextureStateShard textureState;
        final RenderStateShard.OutputStateShard outputState;
        final RenderType.OutlineProperty outlineProperty;
        final ImmutableList<RenderStateShard> states;

        CompositeState(
            RenderStateShard.EmptyTextureStateShard textureState,
            RenderStateShard.LightmapStateShard lightmapState,
            RenderStateShard.OverlayStateShard overlayState,
            RenderStateShard.LayeringStateShard layeringState,
            RenderStateShard.OutputStateShard outputState,
            RenderStateShard.TexturingStateShard texturingState,
            RenderStateShard.LineStateShard lineState,
            RenderType.OutlineProperty outlineProperty
        ) {
            this.textureState = textureState;
            this.outputState = outputState;
            this.outlineProperty = outlineProperty;
            this.states = ImmutableList.of(textureState, lightmapState, overlayState, layeringState, outputState, texturingState, lineState);
        }

        @Override
        public String toString() {
            return "CompositeState[" + this.states + ", outlineProperty=" + this.outlineProperty + "]";
        }

        public static RenderType.CompositeState.CompositeStateBuilder builder() {
            return new RenderType.CompositeState.CompositeStateBuilder();
        }

        @OnlyIn(Dist.CLIENT)
        public static class CompositeStateBuilder {
            private RenderStateShard.EmptyTextureStateShard textureState = RenderStateShard.NO_TEXTURE;
            private RenderStateShard.LightmapStateShard lightmapState = RenderStateShard.NO_LIGHTMAP;
            private RenderStateShard.OverlayStateShard overlayState = RenderStateShard.NO_OVERLAY;
            private RenderStateShard.LayeringStateShard layeringState = RenderStateShard.NO_LAYERING;
            private RenderStateShard.OutputStateShard outputState = RenderStateShard.MAIN_TARGET;
            private RenderStateShard.TexturingStateShard texturingState = RenderStateShard.DEFAULT_TEXTURING;
            private RenderStateShard.LineStateShard lineState = RenderStateShard.DEFAULT_LINE;

            CompositeStateBuilder() {
            }

            public RenderType.CompositeState.CompositeStateBuilder setTextureState(RenderStateShard.EmptyTextureStateShard textureState) {
                this.textureState = textureState;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setLightmapState(RenderStateShard.LightmapStateShard lightmapState) {
                this.lightmapState = lightmapState;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setOverlayState(RenderStateShard.OverlayStateShard overlayState) {
                this.overlayState = overlayState;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setLayeringState(RenderStateShard.LayeringStateShard layerState) {
                this.layeringState = layerState;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setOutputState(RenderStateShard.OutputStateShard outputState) {
                this.outputState = outputState;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setTexturingState(RenderStateShard.TexturingStateShard texturingState) {
                this.texturingState = texturingState;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setLineState(RenderStateShard.LineStateShard lineState) {
                this.lineState = lineState;
                return this;
            }

            public RenderType.CompositeState createCompositeState(boolean outline) {
                return this.createCompositeState(outline ? RenderType.OutlineProperty.AFFECTS_OUTLINE : RenderType.OutlineProperty.NONE);
            }

            public RenderType.CompositeState createCompositeState(RenderType.OutlineProperty outlineState) {
                return new RenderType.CompositeState(
                    this.textureState,
                    this.lightmapState,
                    this.overlayState,
                    this.layeringState,
                    this.outputState,
                    this.texturingState,
                    this.lineState,
                    outlineState
                );
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static enum OutlineProperty {
        NONE("none"),
        IS_OUTLINE("is_outline"),
        AFFECTS_OUTLINE("affects_outline");

        private final String name;

        private OutlineProperty(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }
}
