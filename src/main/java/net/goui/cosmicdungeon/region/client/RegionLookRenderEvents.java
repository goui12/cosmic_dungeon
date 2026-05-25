// file: src/main/java/net/goui/cosmicdungeon/region/client/RegionLookRenderEvents.java
package net.goui.cosmicdungeon.region.client;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.OptionalDouble;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID, value = Dist.CLIENT)
public final class RegionLookRenderEvents {
    private RegionLookRenderEvents() {}

    /**
     * X-ray line pass for region outlines.
     *
     * NeoForge/Minecraft 1.21.10 moved shader/depth/blend/cull/write settings
     * out of RenderStateShard builder calls and into RenderPipeline.
     *
     * This clones the vanilla line pipeline's shader/uniform/sampler setup,
     * then changes only the behavior we need for the x-ray overlay:
     * - translucent blending
     * - no culling
     * - color write enabled
     * - depth write disabled
     * - depth test disabled
     */
    private static final RenderPipeline REGION_LOOK_LINES_XRAY_PIPELINE =
            makeRegionLookLinesXrayPipeline();

    private static final RenderType REGION_LOOK_LINES_XRAY = RenderType.create(
            "cosmicdungeon_region_look_lines_xray",
            1536,
            false,
            false,
            REGION_LOOK_LINES_XRAY_PIPELINE,
            RenderType.CompositeState.builder()
                    .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.empty()))
                    .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                    .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                    .createCompositeState(false)
    );

    private static RenderPipeline makeRegionLookLinesXrayPipeline() {
        RenderPipeline base = RenderPipelines.LINES;

        RenderPipeline.Builder builder = RenderPipeline.builder()
                .withLocation(ResourceLocation.fromNamespaceAndPath(
                        CosmicDungeonMod.MOD_ID,
                        "pipeline/region_look_lines_xray"
                ))
                .withVertexShader(base.getVertexShader())
                .withFragmentShader(base.getFragmentShader())
                .withVertexFormat(base.getVertexFormat(), base.getVertexFormatMode())
                .withPolygonMode(base.getPolygonMode())
                .withCull(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withColorWrite(true, true)
                .withDepthWrite(false)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withColorLogic(base.getColorLogic())
                .withDepthBias(base.getDepthBiasScaleFactor(), base.getDepthBiasConstant());

        for (String sampler : base.getSamplers()) {
            builder.withSampler(sampler);
        }

        for (RenderPipeline.UniformDescription uniform : base.getUniforms()) {
            if (uniform.textureFormat() == null) {
                builder.withUniform(uniform.name(), uniform.type());
            } else {
                builder.withUniform(uniform.name(), uniform.type(), uniform.textureFormat());
            }
        }

        return builder.build();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        if (!RegionLookClient.isEnabled() || !RegionLookClient.isInSameDimensionAsClient()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        var liveRegions = RegionLookClient.getRegionsToRender();
        if (liveRegions.isEmpty()) return;

        /*
         * Snapshot the list before rendering.
         *
         * All-mode can refresh periodically from the server while the client is running.
         * Copying avoids rendering directly from the mutable allRegions list.
         */
        var regions = new ArrayList<>(liveRegions);
        if (regions.isEmpty()) return;

        boolean singleMode = RegionLookClient.isSingleEnabled();

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        var camPos = mc.gameRenderer.getMainCamera().getPosition();

        poseStack.pushPose();
        try {
            poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

            /*
             * IMPORTANT:
             *
             * Do NOT grab both VertexConsumers before drawing.
             *
             * In 1.21.x, BufferSource uses shared buffers for many RenderTypes.
             * Requesting another shared RenderType can end/switch the previous one.
             * If we keep the old VertexConsumer and write to it after the switch,
             * BufferBuilder throws:
             *
             *   java.lang.IllegalStateException: Not building!
             *
             * So we render in two clean batches:
             * 1. all normal depth-tested lines
             * 2. all x-ray lines
             */

            RenderType depthRenderType = RenderType.lines();
            VertexConsumer depthConsumer = bufferSource.getBuffer(depthRenderType);
            for (var r : regions) {
                renderRegionBox(poseStack, depthConsumer, r, singleMode);
            }
            bufferSource.endBatch(depthRenderType);

            VertexConsumer xrayConsumer = bufferSource.getBuffer(REGION_LOOK_LINES_XRAY);
            for (var r : regions) {
                renderRegionBox(poseStack, xrayConsumer, r, singleMode);
            }
            bufferSource.endBatch(REGION_LOOK_LINES_XRAY);
        } finally {
            poseStack.popPose();
        }
    }

    private static void renderRegionBox(
            PoseStack poseStack,
            VertexConsumer consumer,
            RegionLookClient.RenderRegion region,
            boolean singleMode
    ) {
        AABB box = makeBox(region.min(), region.max());

        float rf;
        float gf;
        float bf;
        float af;

        if (singleMode) {
            // Keep classic single-region look (black)
            rf = 0.0F;
            gf = 0.0F;
            bf = 0.0F;
            af = 0.85F;

            // If you want single mode to ALSO be colored by name, replace the 4 lines above with:
            // int rgb = stableColorFromName(region.name());
            // rf = ((rgb >> 16) & 0xFF) / 255.0F;
            // gf = ((rgb >> 8) & 0xFF) / 255.0F;
            // bf = (rgb & 0xFF) / 255.0F;
            // af = 0.90F;
        } else {
            // All-mode: stable per-region color
            int rgb = stableColorFromName(region.name());
            rf = ((rgb >> 16) & 0xFF) / 255.0F;
            gf = ((rgb >> 8) & 0xFF) / 255.0F;
            bf = (rgb & 0xFF) / 255.0F;
            af = 0.85F;
        }

        ShapeRenderer.renderLineBox(poseStack.last(), consumer, box, rf, gf, bf, af);
    }

    private static AABB makeBox(net.minecraft.core.BlockPos min, net.minecraft.core.BlockPos max) {
        int minX = Math.min(min.getX(), max.getX());
        int minY = Math.min(min.getY(), max.getY());
        int minZ = Math.min(min.getZ(), max.getZ());
        int maxX = Math.max(min.getX(), max.getX());
        int maxY = Math.max(min.getY(), max.getY());
        int maxZ = Math.max(min.getZ(), max.getZ());
        return new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }

    /**
     * Cheap stable color: hash -> HSV-ish spread -> RGB
     * (No allocations, deterministic across sessions)
     */
    private static int stableColorFromName(String name) {
        int h = (name == null ? 0 : name.hashCode());
        float hue = ((h & 0x7FFFFFFF) % 360) / 360.0F;
        float sat = 0.75F;
        float val = 0.95F;
        return hsvToRgb(hue, sat, val);
    }

    // Minimal HSV->RGB (0..1 floats), returns 0xRRGGBB
    private static int hsvToRgb(float h, float s, float v) {
        float hh = (h - Mth.floor(h)) * 6.0F;
        int i = (int) hh;
        float f = hh - i;
        float p = v * (1.0F - s);
        float q = v * (1.0F - f * s);
        float t = v * (1.0F - (1.0F - f) * s);

        float r;
        float g;
        float b;

        switch (i) {
            case 0 -> {
                r = v;
                g = t;
                b = p;
            }
            case 1 -> {
                r = q;
                g = v;
                b = p;
            }
            case 2 -> {
                r = p;
                g = v;
                b = t;
            }
            case 3 -> {
                r = p;
                g = q;
                b = v;
            }
            case 4 -> {
                r = t;
                g = p;
                b = v;
            }
            default -> {
                r = v;
                g = p;
                b = q;
            }
        }

        int ri = Mth.clamp((int) (r * 255.0F), 0, 255);
        int gi = Mth.clamp((int) (g * 255.0F), 0, 255);
        int bi = Mth.clamp((int) (b * 255.0F), 0, 255);

        return (ri << 16) | (gi << 8) | bi;
    }
}