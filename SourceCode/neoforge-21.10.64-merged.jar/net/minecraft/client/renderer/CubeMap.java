package net.minecraft.client.renderer;

import com.mojang.blaze3d.ProjectionType;
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
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.CubeMapTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
public class CubeMap implements AutoCloseable {
    private static final int SIDES = 6;
    private final GpuBuffer vertexBuffer;
    private final CachedPerspectiveProjectionMatrixBuffer projectionMatrixUbo;
    private final ResourceLocation location;

    public CubeMap(ResourceLocation baseImageLocation) {
        this.location = baseImageLocation;
        this.projectionMatrixUbo = new CachedPerspectiveProjectionMatrixBuffer("cubemap", 0.05F, 10.0F);
        this.vertexBuffer = initializeVertices();
    }

    public void render(Minecraft minecraft, float xRot, float yRot) {
        RenderSystem.setProjectionMatrix(
            this.projectionMatrixUbo.getBuffer(minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight(), 85.0F), ProjectionType.PERSPECTIVE
        );
        RenderPipeline renderpipeline = RenderPipelines.PANORAMA;
        RenderTarget rendertarget = Minecraft.getInstance().getMainRenderTarget();
        GpuTextureView gputextureview = rendertarget.getColorTextureView();
        GpuTextureView gputextureview1 = rendertarget.getDepthTextureView();
        RenderSystem.AutoStorageIndexBuffer rendersystem$autostorageindexbuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        GpuBuffer gpubuffer = rendersystem$autostorageindexbuffer.getBuffer(36);
        Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
        matrix4fstack.pushMatrix();
        matrix4fstack.rotationX((float) Math.PI);
        matrix4fstack.rotateX(xRot * (float) (Math.PI / 180.0));
        matrix4fstack.rotateY(yRot * (float) (Math.PI / 180.0));
        GpuBufferSlice gpubufferslice = RenderSystem.getDynamicUniforms()
            .writeTransform(new Matrix4f(matrix4fstack), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f(), 0.0F);
        matrix4fstack.popMatrix();

        try (RenderPass renderpass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> "Cubemap", gputextureview, OptionalInt.empty(), gputextureview1, OptionalDouble.empty())) {
            renderpass.setPipeline(renderpipeline);
            RenderSystem.bindDefaultUniforms(renderpass);
            renderpass.setVertexBuffer(0, this.vertexBuffer);
            renderpass.setIndexBuffer(gpubuffer, rendersystem$autostorageindexbuffer.type());
            renderpass.setUniform("DynamicTransforms", gpubufferslice);
            renderpass.bindSampler("Sampler0", minecraft.getTextureManager().getTexture(this.location).getTextureView());
            renderpass.drawIndexed(0, 0, 36, 1);
        }
    }

    private static GpuBuffer initializeVertices() {
        GpuBuffer gpubuffer;
        try (ByteBufferBuilder bytebufferbuilder = ByteBufferBuilder.exactlySized(DefaultVertexFormat.POSITION.getVertexSize() * 4 * 6)) {
            BufferBuilder bufferbuilder = new BufferBuilder(bytebufferbuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            bufferbuilder.addVertex(-1.0F, -1.0F, 1.0F);
            bufferbuilder.addVertex(-1.0F, 1.0F, 1.0F);
            bufferbuilder.addVertex(1.0F, 1.0F, 1.0F);
            bufferbuilder.addVertex(1.0F, -1.0F, 1.0F);
            bufferbuilder.addVertex(1.0F, -1.0F, 1.0F);
            bufferbuilder.addVertex(1.0F, 1.0F, 1.0F);
            bufferbuilder.addVertex(1.0F, 1.0F, -1.0F);
            bufferbuilder.addVertex(1.0F, -1.0F, -1.0F);
            bufferbuilder.addVertex(1.0F, -1.0F, -1.0F);
            bufferbuilder.addVertex(1.0F, 1.0F, -1.0F);
            bufferbuilder.addVertex(-1.0F, 1.0F, -1.0F);
            bufferbuilder.addVertex(-1.0F, -1.0F, -1.0F);
            bufferbuilder.addVertex(-1.0F, -1.0F, -1.0F);
            bufferbuilder.addVertex(-1.0F, 1.0F, -1.0F);
            bufferbuilder.addVertex(-1.0F, 1.0F, 1.0F);
            bufferbuilder.addVertex(-1.0F, -1.0F, 1.0F);
            bufferbuilder.addVertex(-1.0F, -1.0F, -1.0F);
            bufferbuilder.addVertex(-1.0F, -1.0F, 1.0F);
            bufferbuilder.addVertex(1.0F, -1.0F, 1.0F);
            bufferbuilder.addVertex(1.0F, -1.0F, -1.0F);
            bufferbuilder.addVertex(-1.0F, 1.0F, 1.0F);
            bufferbuilder.addVertex(-1.0F, 1.0F, -1.0F);
            bufferbuilder.addVertex(1.0F, 1.0F, -1.0F);
            bufferbuilder.addVertex(1.0F, 1.0F, 1.0F);

            try (MeshData meshdata = bufferbuilder.buildOrThrow()) {
                gpubuffer = RenderSystem.getDevice().createBuffer(() -> "Cube map vertex buffer", 32, meshdata.vertexBuffer());
            }
        }

        return gpubuffer;
    }

    public void registerTextures(TextureManager textureManager) {
        textureManager.register(this.location, new CubeMapTexture(this.location));
    }

    @Override
    public void close() {
        this.vertexBuffer.close();
        this.projectionMatrixUbo.close();
    }
}
