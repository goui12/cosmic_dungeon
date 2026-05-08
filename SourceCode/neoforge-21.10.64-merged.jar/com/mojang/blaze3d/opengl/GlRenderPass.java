package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GlRenderPass implements RenderPass {
    protected static final int MAX_VERTEX_BUFFERS = 1;
    public static final boolean VALIDATION = SharedConstants.IS_RUNNING_IN_IDE && !Boolean.getBoolean("neoforge.disableGlValidation");
    private final GlCommandEncoder encoder;
    private final boolean hasDepthTexture;
    private boolean closed;
    @Nullable
    protected GlRenderPipeline pipeline;
    protected final GpuBuffer[] vertexBuffers = new GpuBuffer[1];
    @Nullable
    protected GpuBuffer indexBuffer;
    protected VertexFormat.IndexType indexType = VertexFormat.IndexType.INT;
    private final ScissorState scissorState = new ScissorState();
    protected final HashMap<String, GpuBufferSlice> uniforms = new HashMap<>();
    protected final HashMap<String, GpuTextureView> samplers = new HashMap<>();
    protected final Set<String> dirtyUniforms = new HashSet<>();
    protected int pushedDebugGroups;

    public GlRenderPass(GlCommandEncoder encoder, boolean hasDepthTexture) {
        this.encoder = encoder;
        this.hasDepthTexture = hasDepthTexture;
    }

    public boolean hasDepthTexture() {
        return this.hasDepthTexture;
    }

    @Override
    public void pushDebugGroup(Supplier<String> name) {
        if (this.closed) {
            throw new IllegalStateException("Can't use a closed render pass");
        } else {
            this.pushedDebugGroups++;
            this.encoder.getDevice().debugLabels().pushDebugGroup(name);
        }
    }

    @Override
    public void popDebugGroup() {
        if (this.closed) {
            throw new IllegalStateException("Can't use a closed render pass");
        } else if (this.pushedDebugGroups == 0) {
            throw new IllegalStateException("Can't pop more debug groups than was pushed!");
        } else {
            this.pushedDebugGroups--;
            this.encoder.getDevice().debugLabels().popDebugGroup();
        }
    }

    @Override
    public void setPipeline(RenderPipeline pipeline) {
        pipeline = com.mojang.blaze3d.systems.RenderSystem.applyPipelineModifiers(pipeline);
        if (this.pipeline == null || this.pipeline.info() != pipeline) {
            this.dirtyUniforms.addAll(this.uniforms.keySet());
            this.dirtyUniforms.addAll(this.samplers.keySet());
        }

        this.pipeline = this.encoder.getDevice().getOrCompilePipeline(pipeline);
    }

    @Override
    public void bindSampler(String name, @Nullable GpuTextureView texture) {
        if (texture == null) {
            this.samplers.remove(name);
        } else {
            this.samplers.put(name, texture);
        }

        this.dirtyUniforms.add(name);
    }

    @Override
    public void setUniform(String name, GpuBuffer buffer) {
        this.uniforms.put(name, buffer.slice());
        this.dirtyUniforms.add(name);
    }

    @Override
    public void setUniform(String name, GpuBufferSlice bufferSlice) {
        int i = this.encoder.getDevice().getUniformOffsetAlignment();
        if (bufferSlice.offset() % i > 0) {
            throw new IllegalArgumentException("Uniform buffer offset must be aligned to " + i);
        } else {
            this.uniforms.put(name, bufferSlice);
            this.dirtyUniforms.add(name);
        }
    }

    @Override
    public void setViewport(int x, int y, int width, int height){
        GlStateManager._viewport(x, y, width, height);
    }

    @Override
    public void enableScissor(int x, int y, int width, int height) {
        this.scissorState.enable(x, y, width, height);
    }

    @Override
    public void disableScissor() {
        this.scissorState.disable();
    }

    public boolean isScissorEnabled() {
        return this.scissorState.enabled();
    }

    public int getScissorX() {
        return this.scissorState.x();
    }

    public int getScissorY() {
        return this.scissorState.y();
    }

    public int getScissorWidth() {
        return this.scissorState.width();
    }

    public int getScissorHeight() {
        return this.scissorState.height();
    }

    @Override
    public void setVertexBuffer(int index, GpuBuffer buffer) {
        if (index >= 0 && index < 1) {
            this.vertexBuffers[index] = buffer;
        } else {
            throw new IllegalArgumentException("Vertex buffer slot is out of range: " + index);
        }
    }

    @Override
    public void setIndexBuffer(@Nullable GpuBuffer indexBuffer, VertexFormat.IndexType indexType) {
        this.indexBuffer = indexBuffer;
        this.indexType = indexType;
    }

    @Override
    public void drawIndexed(int firstIndex, int index, int indexCount, int primCount) {
        if (this.closed) {
            throw new IllegalStateException("Can't use a closed render pass");
        } else {
            this.encoder.executeDraw(this, firstIndex, index, indexCount, this.indexType, primCount);
        }
    }

    @Override
    public <T> void drawMultipleIndexed(
        Collection<RenderPass.Draw<T>> draws,
        @Nullable GpuBuffer indexBuffer,
        @Nullable VertexFormat.IndexType indexType,
        Collection<String> uniformNames,
        T userData
    ) {
        if (this.closed) {
            throw new IllegalStateException("Can't use a closed render pass");
        } else {
            this.encoder.executeDrawMultiple(this, draws, indexBuffer, indexType, uniformNames, userData);
        }
    }

    @Override
    public void draw(int firstIndex, int indexCount) {
        if (this.closed) {
            throw new IllegalStateException("Can't use a closed render pass");
        } else {
            this.encoder.executeDraw(this, firstIndex, 0, indexCount, null, 1);
        }
    }

    @Override
    public void close() {
        if (!this.closed) {
            if (this.pushedDebugGroups > 0) {
                throw new IllegalStateException("Render pass had debug groups left open!");
            }

            this.closed = true;
            this.encoder.finishRenderPass();
        }
    }
}
