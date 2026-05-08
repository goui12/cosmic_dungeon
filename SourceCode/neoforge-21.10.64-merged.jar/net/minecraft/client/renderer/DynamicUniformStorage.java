package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class DynamicUniformStorage<T extends DynamicUniformStorage.DynamicUniform> implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final List<MappableRingBuffer> oldBuffers = new ArrayList<>();
    private final int blockSize;
    private MappableRingBuffer ringBuffer;
    private int nextBlock;
    private int capacity;
    @Nullable
    private T lastUniform;
    private final String label;

    public DynamicUniformStorage(String label, int blockSize, int capacity) {
        GpuDevice gpudevice = RenderSystem.getDevice();
        this.blockSize = Mth.roundToward(blockSize, gpudevice.getUniformOffsetAlignment());
        this.capacity = Mth.smallestEncompassingPowerOfTwo(capacity);
        this.nextBlock = 0;
        this.ringBuffer = new MappableRingBuffer(() -> label + " x" + this.blockSize, 130, this.blockSize * this.capacity);
        this.label = label;
    }

    public void endFrame() {
        this.nextBlock = 0;
        this.lastUniform = null;
        this.ringBuffer.rotate();
        if (!this.oldBuffers.isEmpty()) {
            for (MappableRingBuffer mappableringbuffer : this.oldBuffers) {
                mappableringbuffer.close();
            }

            this.oldBuffers.clear();
        }
    }

    private void resizeBuffers(int newSize) {
        this.capacity = newSize;
        this.nextBlock = 0;
        this.lastUniform = null;
        this.oldBuffers.add(this.ringBuffer);
        this.ringBuffer = new MappableRingBuffer(() -> this.label + " x" + this.blockSize, 130, this.blockSize * this.capacity);
    }

    public GpuBufferSlice writeUniform(T uniform) {
        if (this.lastUniform != null && this.lastUniform.equals(uniform)) {
            return this.ringBuffer.currentBuffer().slice((this.nextBlock - 1) * this.blockSize, this.blockSize);
        } else {
            if (this.nextBlock >= this.capacity) {
                int i = this.capacity * 2;
                LOGGER.info("Resizing {}, capacity limit of {} reached during a single frame. New capacity will be {}.", this.label, this.capacity, i);
                this.resizeBuffers(i);
            }

            int j = this.nextBlock * this.blockSize;

            try (GpuBuffer.MappedView gpubuffer$mappedview = RenderSystem.getDevice()
                    .createCommandEncoder()
                    .mapBuffer(this.ringBuffer.currentBuffer().slice(j, this.blockSize), false, true)) {
                uniform.write(gpubuffer$mappedview.data());
            }

            this.nextBlock++;
            this.lastUniform = uniform;
            return this.ringBuffer.currentBuffer().slice(j, this.blockSize);
        }
    }

    public GpuBufferSlice[] writeUniforms(T[] uniforms) {
        if (uniforms.length == 0) {
            return new GpuBufferSlice[0];
        } else {
            if (this.nextBlock + uniforms.length > this.capacity) {
                int i = Mth.smallestEncompassingPowerOfTwo(Math.max(this.capacity + 1, uniforms.length));
                LOGGER.info("Resizing {}, capacity limit of {} reached during a single frame. New capacity will be {}.", this.label, this.capacity, i);
                this.resizeBuffers(i);
            }

            int k = this.nextBlock * this.blockSize;
            GpuBufferSlice[] agpubufferslice = new GpuBufferSlice[uniforms.length];

            try (GpuBuffer.MappedView gpubuffer$mappedview = RenderSystem.getDevice()
                    .createCommandEncoder()
                    .mapBuffer(this.ringBuffer.currentBuffer().slice(k, uniforms.length * this.blockSize), false, true)) {
                ByteBuffer bytebuffer = gpubuffer$mappedview.data();

                for (int j = 0; j < uniforms.length; j++) {
                    T t = uniforms[j];
                    agpubufferslice[j] = this.ringBuffer.currentBuffer().slice(k + j * this.blockSize, this.blockSize);
                    bytebuffer.position(j * this.blockSize);
                    t.write(bytebuffer);
                }
            }

            this.nextBlock += uniforms.length;
            this.lastUniform = uniforms[uniforms.length - 1];
            return agpubufferslice;
        }
    }

    @Override
    public void close() {
        for (MappableRingBuffer mappableringbuffer : this.oldBuffers) {
            mappableringbuffer.close();
        }

        this.ringBuffer.close();
    }

    @OnlyIn(Dist.CLIENT)
    public interface DynamicUniform {
        void write(ByteBuffer buffer);
    }
}
