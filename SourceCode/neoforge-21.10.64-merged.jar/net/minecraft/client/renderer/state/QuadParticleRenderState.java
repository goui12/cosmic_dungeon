package net.minecraft.client.renderer.state;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ParticleFeatureRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
public class QuadParticleRenderState implements SubmitNodeCollector.ParticleGroupRenderer, ParticleGroupRenderState {
    private static final int INITIAL_PARTICLE_CAPACITY = 1024;
    private static final int FLOATS_PER_PARTICLE = 12;
    private static final int INTS_PER_PARTICLE = 2;
    private final Map<SingleQuadParticle.Layer, QuadParticleRenderState.Storage> particles = new HashMap<>();
    private int particleCount;

    public void add(
        SingleQuadParticle.Layer layer,
        float x,
        float y,
        float z,
        float xRot,
        float yRot,
        float zRot,
        float wRot,
        float quadSize,
        float u0,
        float u1,
        float v0,
        float v1,
        int color,
        int packedLight
    ) {
        this.particles
            .computeIfAbsent(layer, p_451410_ -> new QuadParticleRenderState.Storage())
            .add(
                x,
                y,
                z,
                xRot,
                yRot,
                zRot,
                wRot,
                quadSize,
                u0,
                u1,
                v0,
                v1,
                color,
                packedLight
            );
        this.particleCount++;
    }

    @Override
    public void clear() {
        this.particles.values().forEach(QuadParticleRenderState.Storage::clear);
        this.particleCount = 0;
    }

    @Nullable
    @Override
    public QuadParticleRenderState.PreparedBuffers prepare(ParticleFeatureRenderer.ParticleBufferCache cache) {
        int i = this.particleCount * 4;

        Object object;
        try (ByteBufferBuilder bytebufferbuilder = ByteBufferBuilder.exactlySized(i * DefaultVertexFormat.PARTICLE.getVertexSize())) {
            BufferBuilder bufferbuilder = new BufferBuilder(bytebufferbuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
            Map<SingleQuadParticle.Layer, QuadParticleRenderState.PreparedLayer> map = new HashMap<>();
            int j = 0;

            for (Entry<SingleQuadParticle.Layer, QuadParticleRenderState.Storage> entry : this.particles.entrySet()) {
                entry.getValue()
                    .forEachParticle(
                        (p_450929_, p_451248_, p_451307_, p_451411_, p_451517_, p_451115_, p_451484_, p_451330_, p_451081_, p_451392_, p_451178_, p_451023_, p_451176_, p_451252_) -> this.renderRotatedQuad(
                            bufferbuilder,
                            p_450929_,
                            p_451248_,
                            p_451307_,
                            p_451411_,
                            p_451517_,
                            p_451115_,
                            p_451484_,
                            p_451330_,
                            p_451081_,
                            p_451392_,
                            p_451178_,
                            p_451023_,
                            p_451176_,
                            p_451252_
                        )
                    );
                if (entry.getValue().count() > 0) {
                    map.put(entry.getKey(), new QuadParticleRenderState.PreparedLayer(j, entry.getValue().count() * 6));
                }

                j += entry.getValue().count() * 4;
            }

            MeshData meshdata = bufferbuilder.build();
            if (meshdata != null) {
                cache.write(meshdata.vertexBuffer());
                RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS).getBuffer(meshdata.drawState().indexCount());
                GpuBufferSlice gpubufferslice = RenderSystem.getDynamicUniforms()
                    .writeTransform(
                        RenderSystem.getModelViewMatrix(),
                        new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
                        new Vector3f(),
                        RenderSystem.getTextureMatrix(),
                        RenderSystem.getShaderLineWidth()
                    );
                return new QuadParticleRenderState.PreparedBuffers(meshdata.drawState().indexCount(), gpubufferslice, map);
            }

            object = null;
        }

        return (QuadParticleRenderState.PreparedBuffers)object;
    }

    @Override
    public void render(
        QuadParticleRenderState.PreparedBuffers preparedBuffers,
        ParticleFeatureRenderer.ParticleBufferCache cache,
        RenderPass renderPass,
        TextureManager textureManager,
        boolean translucent
    ) {
        RenderSystem.AutoStorageIndexBuffer rendersystem$autostorageindexbuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        renderPass.setVertexBuffer(0, cache.get());
        renderPass.setIndexBuffer(rendersystem$autostorageindexbuffer.getBuffer(preparedBuffers.indexCount), rendersystem$autostorageindexbuffer.type());
        renderPass.setUniform("DynamicTransforms", preparedBuffers.dynamicTransforms);

        for (Entry<SingleQuadParticle.Layer, QuadParticleRenderState.PreparedLayer> entry : preparedBuffers.layers.entrySet()) {
            if (translucent == entry.getKey().translucent()) {
                renderPass.setPipeline(entry.getKey().pipeline());
                renderPass.bindSampler("Sampler0", textureManager.getTexture(entry.getKey().textureAtlasLocation()).getTextureView());
                renderPass.drawIndexed(entry.getValue().vertexOffset, 0, entry.getValue().indexCount, 1);
            }
        }
    }

    protected void renderRotatedQuad(
        VertexConsumer conumer,
        float x,
        float y,
        float z,
        float xRot,
        float yRot,
        float zRot,
        float wRot,
        float quadSize,
        float u0,
        float u1,
        float v0,
        float v1,
        int color,
        int lightColor
    ) {
        Quaternionf quaternionf = new Quaternionf(xRot, yRot, zRot, wRot);
        this.renderVertex(conumer, quaternionf, x, y, z, 1.0F, -1.0F, quadSize, u1, v1, color, lightColor);
        this.renderVertex(conumer, quaternionf, x, y, z, 1.0F, 1.0F, quadSize, u1, v0, color, lightColor);
        this.renderVertex(conumer, quaternionf, x, y, z, -1.0F, 1.0F, quadSize, u0, v0, color, lightColor);
        this.renderVertex(conumer, quaternionf, x, y, z, -1.0F, -1.0F, quadSize, u0, v1, color, lightColor);
    }

    private void renderVertex(
        VertexConsumer consumer,
        Quaternionf rotation,
        float x,
        float y,
        float z,
        float cornerX,
        float cornerY,
        float quadSize,
        float u,
        float v,
        int color,
        int packedLight
    ) {
        Vector3f vector3f = new Vector3f(cornerX, cornerY, 0.0F).rotate(rotation).mul(quadSize).add(x, y, z);
        consumer.addVertex(vector3f.x(), vector3f.y(), vector3f.z()).setUv(u, v).setColor(color).setLight(packedLight);
    }

    @Override
    public void submit(SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        if (this.particleCount > 0) {
            nodeCollector.submitParticleGroup(this);
        }
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface ParticleConsumer {
        void consume(
            float x,
            float y,
            float z,
            float xRot,
            float yRot,
            float zRot,
            float wRot,
            float quadSize,
            float u0,
            float u1,
            float v0,
            float v1,
            int color,
            int packedLight
        );
    }

    @OnlyIn(Dist.CLIENT)
    public record PreparedBuffers(int indexCount, GpuBufferSlice dynamicTransforms, Map<SingleQuadParticle.Layer, QuadParticleRenderState.PreparedLayer> layers) {
    }

    @OnlyIn(Dist.CLIENT)
    public record PreparedLayer(int vertexOffset, int indexCount) {
    }

    @OnlyIn(Dist.CLIENT)
    static class Storage {
        private int capacity = 1024;
        private float[] floatValues = new float[12288];
        private int[] intValues = new int[2048];
        private int currentParticleIndex;

        public void add(
            float x,
            float y,
            float z,
            float xRot,
            float yRot,
            float zRot,
            float wRot,
            float quadSize,
            float u0,
            float u1,
            float v0,
            float v1,
            int color,
            int packedLight
        ) {
            if (this.currentParticleIndex >= this.capacity) {
                this.grow();
            }

            int i = this.currentParticleIndex * 12;
            this.floatValues[i++] = x;
            this.floatValues[i++] = y;
            this.floatValues[i++] = z;
            this.floatValues[i++] = xRot;
            this.floatValues[i++] = yRot;
            this.floatValues[i++] = zRot;
            this.floatValues[i++] = wRot;
            this.floatValues[i++] = quadSize;
            this.floatValues[i++] = u0;
            this.floatValues[i++] = u1;
            this.floatValues[i++] = v0;
            this.floatValues[i] = v1;
            i = this.currentParticleIndex * 2;
            this.intValues[i++] = color;
            this.intValues[i] = packedLight;
            this.currentParticleIndex++;
        }

        public void forEachParticle(QuadParticleRenderState.ParticleConsumer consumer) {
            for (int i = 0; i < this.currentParticleIndex; i++) {
                int j = i * 12;
                int k = i * 2;
                consumer.consume(
                    this.floatValues[j++],
                    this.floatValues[j++],
                    this.floatValues[j++],
                    this.floatValues[j++],
                    this.floatValues[j++],
                    this.floatValues[j++],
                    this.floatValues[j++],
                    this.floatValues[j++],
                    this.floatValues[j++],
                    this.floatValues[j++],
                    this.floatValues[j++],
                    this.floatValues[j],
                    this.intValues[k++],
                    this.intValues[k]
                );
            }
        }

        public void clear() {
            this.currentParticleIndex = 0;
        }

        private void grow() {
            this.capacity *= 2;
            this.floatValues = Arrays.copyOf(this.floatValues, this.capacity * 12);
            this.intValues = Arrays.copyOf(this.intValues, this.capacity * 2);
        }

        public int count() {
            return this.currentParticleIndex;
        }
    }
}
