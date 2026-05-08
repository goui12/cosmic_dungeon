package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

@OnlyIn(Dist.CLIENT)
public class CachedPerspectiveProjectionMatrixBuffer implements AutoCloseable {
    private final GpuBuffer buffer;
    private final GpuBufferSlice bufferSlice;
    private final float zNear;
    private final float zFar;
    private int width;
    private int height;
    private float fov;

    public CachedPerspectiveProjectionMatrixBuffer(String label, float zNear, float zFar) {
        this.zNear = zNear;
        this.zFar = zFar;
        GpuDevice gpudevice = RenderSystem.getDevice();
        this.buffer = gpudevice.createBuffer(() -> "Projection matrix UBO " + label, 136, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
        this.bufferSlice = this.buffer.slice(0, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
    }

    public GpuBufferSlice getBuffer(int width, int height, float fov) {
        if (this.width != width || this.height != height || this.fov != fov) {
            Matrix4f matrix4f = this.createProjectionMatrix(width, height, fov);

            try (MemoryStack memorystack = MemoryStack.stackPush()) {
                ByteBuffer bytebuffer = Std140Builder.onStack(memorystack, RenderSystem.PROJECTION_MATRIX_UBO_SIZE).putMat4f(matrix4f).get();
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer.slice(), bytebuffer);
            }

            this.width = width;
            this.height = height;
            this.fov = fov;
        }

        return this.bufferSlice;
    }

    private Matrix4f createProjectionMatrix(int width, int height, float fov) {
        return new Matrix4f().perspective(fov * (float) (Math.PI / 180.0), (float)width / height, this.zNear, this.zFar);
    }

    @Override
    public void close() {
        this.buffer.close();
    }
}
