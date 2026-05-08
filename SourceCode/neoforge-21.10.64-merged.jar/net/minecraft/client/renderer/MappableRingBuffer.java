package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.function.Supplier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MappableRingBuffer implements AutoCloseable {
    private static final int BUFFER_COUNT = 3;
    private final GpuBuffer[] buffers = new GpuBuffer[3];
    private final GpuFence[] fences = new GpuFence[3];
    private final int size;
    private int current = 0;

    public MappableRingBuffer(Supplier<String> label, int usage, int size) {
        GpuDevice gpudevice = RenderSystem.getDevice();
        if ((usage & 1) == 0 && (usage & 2) == 0) {
            throw new IllegalArgumentException("MappableRingBuffer requires at least one of USAGE_MAP_READ or USAGE_MAP_WRITE");
        } else {
            for (int i = 0; i < 3; i++) {
                int j = i;
                this.buffers[i] = gpudevice.createBuffer(() -> label.get() + " #" + j, usage, size);
                this.fences[i] = null;
            }

            this.size = size;
        }
    }

    public int size() {
        return this.size;
    }

    public GpuBuffer currentBuffer() {
        GpuFence gpufence = this.fences[this.current];
        if (gpufence != null) {
            gpufence.awaitCompletion(Long.MAX_VALUE);
            gpufence.close();
            this.fences[this.current] = null;
        }

        return this.buffers[this.current];
    }

    public void rotate() {
        if (this.fences[this.current] != null) {
            this.fences[this.current].close();
        }

        this.fences[this.current] = RenderSystem.getDevice().createCommandEncoder().createFence();
        this.current = (this.current + 1) % 3;
    }

    @Override
    public void close() {
        for (int i = 0; i < 3; i++) {
            this.buffers[i].close();
            if (this.fences[i] != null) {
                this.fences[i].close();
            }
        }
    }
}
