package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.WorldBorderRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
public class WorldBorderRenderer {
    public static final ResourceLocation FORCEFIELD_LOCATION = ResourceLocation.withDefaultNamespace("textures/misc/forcefield.png");
    private boolean needsRebuild = true;
    private double lastMinX;
    private double lastMinZ;
    private double lastBorderMinX;
    private double lastBorderMaxX;
    private double lastBorderMinZ;
    private double lastBorderMaxZ;
    private final GpuBuffer worldBorderBuffer = RenderSystem.getDevice()
        .createBuffer(() -> "World border vertex buffer", 40, 16 * DefaultVertexFormat.POSITION_TEX.getVertexSize());
    private final RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);

    private void rebuildWorldBorderBuffer(
        WorldBorderRenderState renderState, double renderDistance, double camZ, double camX, float depthFar, float vBottom, float vTop
    ) {
        try (ByteBufferBuilder bytebufferbuilder = ByteBufferBuilder.exactlySized(DefaultVertexFormat.POSITION_TEX.getVertexSize() * 4 * 4)) {
            double d0 = renderState.minX;
            double d1 = renderState.maxX;
            double d2 = renderState.minZ;
            double d3 = renderState.maxZ;
            double d4 = Math.max((double)Mth.floor(camZ - renderDistance), d2);
            double d5 = Math.min((double)Mth.ceil(camZ + renderDistance), d3);
            float f = (Mth.floor(d4) & 1) * 0.5F;
            float f1 = (float)(d5 - d4) / 2.0F;
            double d6 = Math.max((double)Mth.floor(camX - renderDistance), d0);
            double d7 = Math.min((double)Mth.ceil(camX + renderDistance), d1);
            float f2 = (Mth.floor(d6) & 1) * 0.5F;
            float f3 = (float)(d7 - d6) / 2.0F;
            BufferBuilder bufferbuilder = new BufferBuilder(bytebufferbuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            bufferbuilder.addVertex(0.0F, -depthFar, (float)(d3 - d4)).setUv(f2, vBottom);
            bufferbuilder.addVertex((float)(d7 - d6), -depthFar, (float)(d3 - d4)).setUv(f3 + f2, vBottom);
            bufferbuilder.addVertex((float)(d7 - d6), depthFar, (float)(d3 - d4)).setUv(f3 + f2, vTop);
            bufferbuilder.addVertex(0.0F, depthFar, (float)(d3 - d4)).setUv(f2, vTop);
            bufferbuilder.addVertex(0.0F, -depthFar, 0.0F).setUv(f, vBottom);
            bufferbuilder.addVertex(0.0F, -depthFar, (float)(d5 - d4)).setUv(f1 + f, vBottom);
            bufferbuilder.addVertex(0.0F, depthFar, (float)(d5 - d4)).setUv(f1 + f, vTop);
            bufferbuilder.addVertex(0.0F, depthFar, 0.0F).setUv(f, vTop);
            bufferbuilder.addVertex((float)(d7 - d6), -depthFar, 0.0F).setUv(f2, vBottom);
            bufferbuilder.addVertex(0.0F, -depthFar, 0.0F).setUv(f3 + f2, vBottom);
            bufferbuilder.addVertex(0.0F, depthFar, 0.0F).setUv(f3 + f2, vTop);
            bufferbuilder.addVertex((float)(d7 - d6), depthFar, 0.0F).setUv(f2, vTop);
            bufferbuilder.addVertex((float)(d1 - d6), -depthFar, (float)(d5 - d4)).setUv(f, vBottom);
            bufferbuilder.addVertex((float)(d1 - d6), -depthFar, 0.0F).setUv(f1 + f, vBottom);
            bufferbuilder.addVertex((float)(d1 - d6), depthFar, 0.0F).setUv(f1 + f, vTop);
            bufferbuilder.addVertex((float)(d1 - d6), depthFar, (float)(d5 - d4)).setUv(f, vTop);

            try (MeshData meshdata = bufferbuilder.buildOrThrow()) {
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.worldBorderBuffer.slice(), meshdata.vertexBuffer());
            }

            this.lastBorderMinX = d0;
            this.lastBorderMaxX = d1;
            this.lastBorderMinZ = d2;
            this.lastBorderMaxZ = d3;
            this.lastMinX = d6;
            this.lastMinZ = d4;
            this.needsRebuild = false;
        }
    }

    public void extract(WorldBorder worldBorder, Vec3 cameraPosition, double renderDistance, WorldBorderRenderState renderState) {
        renderState.minX = worldBorder.getMinX();
        renderState.maxX = worldBorder.getMaxX();
        renderState.minZ = worldBorder.getMinZ();
        renderState.maxZ = worldBorder.getMaxZ();
        if ((
                !(cameraPosition.x < renderState.maxX - renderDistance)
                    || !(cameraPosition.x > renderState.minX + renderDistance)
                    || !(cameraPosition.z < renderState.maxZ - renderDistance)
                    || !(cameraPosition.z > renderState.minZ + renderDistance)
            )
            && !(cameraPosition.x < renderState.minX - renderDistance)
            && !(cameraPosition.x > renderState.maxX + renderDistance)
            && !(cameraPosition.z < renderState.minZ - renderDistance)
            && !(cameraPosition.z > renderState.maxZ + renderDistance)) {
            renderState.alpha = 1.0 - worldBorder.getDistanceToBorder(cameraPosition.x, cameraPosition.z) / renderDistance;
            renderState.alpha = Math.pow(renderState.alpha, 4.0);
            renderState.alpha = Mth.clamp(renderState.alpha, 0.0, 1.0);
            renderState.tint = worldBorder.getStatus().getColor();
        } else {
            renderState.alpha = 0.0;
        }
    }

    public void render(WorldBorderRenderState renderState, Vec3 cameraPostion, double renderDistance, double depthFar) {
        if (!(renderState.alpha <= 0.0)) {
            double d0 = cameraPostion.x;
            double d1 = cameraPostion.z;
            float f = (float)depthFar;
            float f1 = ARGB.red(renderState.tint) / 255.0F;
            float f2 = ARGB.green(renderState.tint) / 255.0F;
            float f3 = ARGB.blue(renderState.tint) / 255.0F;
            float f4 = (float)(Util.getMillis() % 3000L) / 3000.0F;
            float f5 = (float)(-Mth.frac(cameraPostion.y * 0.5));
            float f6 = f5 + f;
            if (this.shouldRebuildWorldBorderBuffer(renderState)) {
                this.rebuildWorldBorderBuffer(renderState, renderDistance, d1, d0, f, f6, f5);
            }

            TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
            AbstractTexture abstracttexture = texturemanager.getTexture(FORCEFIELD_LOCATION);
            abstracttexture.setUseMipmaps(false);
            RenderPipeline renderpipeline = RenderPipelines.WORLD_BORDER;
            RenderTarget rendertarget = Minecraft.getInstance().getMainRenderTarget();
            RenderTarget rendertarget1 = Minecraft.getInstance().levelRenderer.getWeatherTarget();
            GpuTextureView gputextureview;
            GpuTextureView gputextureview1;
            if (rendertarget1 != null) {
                gputextureview = rendertarget1.getColorTextureView();
                gputextureview1 = rendertarget1.getDepthTextureView();
            } else {
                gputextureview = rendertarget.getColorTextureView();
                gputextureview1 = rendertarget.getDepthTextureView();
            }

            GpuBuffer gpubuffer = this.indices.getBuffer(6);
            GpuBufferSlice gpubufferslice = RenderSystem.getDynamicUniforms()
                .writeTransform(
                    RenderSystem.getModelViewMatrix(),
                    new Vector4f(f1, f2, f3, (float)renderState.alpha),
                    new Vector3f((float)(this.lastMinX - d0), (float)(-cameraPostion.y), (float)(this.lastMinZ - d1)),
                    new Matrix4f().translation(f4, f4, 0.0F),
                    0.0F
                );

            try (RenderPass renderpass = RenderSystem.getDevice()
                    .createCommandEncoder()
                    .createRenderPass(() -> "World border", gputextureview, OptionalInt.empty(), gputextureview1, OptionalDouble.empty())) {
                renderpass.setPipeline(renderpipeline);
                RenderSystem.bindDefaultUniforms(renderpass);
                renderpass.setUniform("DynamicTransforms", gpubufferslice);
                renderpass.setIndexBuffer(gpubuffer, this.indices.type());
                renderpass.bindSampler("Sampler0", abstracttexture.getTextureView());
                renderpass.setVertexBuffer(0, this.worldBorderBuffer);
                ArrayList<RenderPass.Draw<WorldBorderRenderer>> arraylist = new ArrayList<>();

                for (WorldBorderRenderState.DistancePerDirection worldborderrenderstate$distanceperdirection : renderState.closestBorder(d0, d1)) {
                    if (worldborderrenderstate$distanceperdirection.distance() < renderDistance) {
                        int i = worldborderrenderstate$distanceperdirection.direction().get2DDataValue();
                        arraylist.add(new RenderPass.Draw<>(0, this.worldBorderBuffer, gpubuffer, this.indices.type(), 6 * i, 6));
                    }
                }

                renderpass.drawMultipleIndexed(arraylist, null, null, Collections.emptyList(), this);
            }
        }
    }

    public void invalidate() {
        this.needsRebuild = true;
    }

    private boolean shouldRebuildWorldBorderBuffer(WorldBorderRenderState renderState) {
        return this.needsRebuild
            || renderState.minX != this.lastBorderMinX
            || renderState.minZ != this.lastBorderMinZ
            || renderState.maxX != this.lastBorderMaxX
            || renderState.maxZ != this.lastBorderMaxZ;
    }
}
