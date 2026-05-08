package com.mojang.blaze3d;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.jtracy.TracyClient;
import java.util.OptionalInt;
import net.minecraft.client.renderer.RenderPipelines;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TracyFrameCapture implements AutoCloseable {
    private static final int MAX_WIDTH = 320;
    private static final int MAX_HEIGHT = 180;
    private static final int BYTES_PER_PIXEL = 4;
    private int targetWidth;
    private int targetHeight;
    private int width;
    private int height;
    private GpuTexture frameBuffer;
    private GpuTextureView frameBufferView;
    private GpuBuffer pixelbuffer;
    private int lastCaptureDelay;
    private boolean capturedThisFrame;
    private TracyFrameCapture.Status status = TracyFrameCapture.Status.WAITING_FOR_CAPTURE;

    public TracyFrameCapture() {
        this.width = 320;
        this.height = 180;
        GpuDevice gpudevice = RenderSystem.getDevice();
        this.frameBuffer = gpudevice.createTexture("Tracy Frame Capture", 10, TextureFormat.RGBA8, this.width, this.height, 1, 1);
        this.frameBufferView = gpudevice.createTextureView(this.frameBuffer);
        this.pixelbuffer = gpudevice.createBuffer(() -> "Tracy Frame Capture buffer", 9, this.width * this.height * 4);
    }

    private void resize(int width, int height) {
        float f = (float)width / height;
        if (width > 320) {
            width = 320;
            height = (int)(320.0F / f);
        }

        if (height > 180) {
            width = (int)(180.0F * f);
            height = 180;
        }

        width = width / 4 * 4;
        height = height / 4 * 4;
        if (this.width != width || this.height != height) {
            this.width = width;
            this.height = height;
            GpuDevice gpudevice = RenderSystem.getDevice();
            this.frameBuffer.close();
            this.frameBuffer = gpudevice.createTexture("Tracy Frame Capture", 10, TextureFormat.RGBA8, width, height, 1, 1);
            this.frameBufferView.close();
            this.frameBufferView = gpudevice.createTextureView(this.frameBuffer);
            this.pixelbuffer.close();
            this.pixelbuffer = gpudevice.createBuffer(() -> "Tracy Frame Capture buffer", 9, width * height * 4);
        }
    }

    public void capture(RenderTarget renderTarget) {
        if (this.status == TracyFrameCapture.Status.WAITING_FOR_CAPTURE && !this.capturedThisFrame && renderTarget.getColorTexture() != null) {
            this.capturedThisFrame = true;
            if (renderTarget.width != this.targetWidth || renderTarget.height != this.targetHeight) {
                this.targetWidth = renderTarget.width;
                this.targetHeight = renderTarget.height;
                this.resize(this.targetWidth, this.targetHeight);
            }

            this.status = TracyFrameCapture.Status.WAITING_FOR_COPY;
            CommandEncoder commandencoder = RenderSystem.getDevice().createCommandEncoder();

            try (RenderPass renderpass = RenderSystem.getDevice()
                    .createCommandEncoder()
                    .createRenderPass(() -> "Tracy blit", this.frameBufferView, OptionalInt.empty())) {
                renderpass.setPipeline(RenderPipelines.TRACY_BLIT);
                renderpass.bindSampler("InSampler", renderTarget.getColorTextureView());
                renderpass.draw(0, 3);
            }

            commandencoder.copyTextureToBuffer(this.frameBuffer, this.pixelbuffer, 0, () -> this.status = TracyFrameCapture.Status.WAITING_FOR_UPLOAD, 0);
            this.lastCaptureDelay = 0;
        }
    }

    public void upload() {
        if (this.status == TracyFrameCapture.Status.WAITING_FOR_UPLOAD) {
            this.status = TracyFrameCapture.Status.WAITING_FOR_CAPTURE;

            try (GpuBuffer.MappedView gpubuffer$mappedview = RenderSystem.getDevice().createCommandEncoder().mapBuffer(this.pixelbuffer, true, false)) {
                TracyClient.frameImage(gpubuffer$mappedview.data(), this.width, this.height, this.lastCaptureDelay, true);
            }
        }
    }

    public void endFrame() {
        this.lastCaptureDelay++;
        this.capturedThisFrame = false;
        TracyClient.markFrame();
    }

    @Override
    public void close() {
        this.frameBuffer.close();
        this.frameBufferView.close();
        this.pixelbuffer.close();
    }

    @OnlyIn(Dist.CLIENT)
    static enum Status {
        WAITING_FOR_CAPTURE,
        WAITING_FOR_COPY,
        WAITING_FOR_UPLOAD;
    }
}
