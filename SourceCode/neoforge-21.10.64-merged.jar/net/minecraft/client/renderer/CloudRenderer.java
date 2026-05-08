package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import javax.annotation.Nullable;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class CloudRenderer extends SimplePreparableReloadListener<Optional<CloudRenderer.TextureData>> implements AutoCloseable {
    private static final int FLAG_INSIDE_FACE = 16;
    private static final int FLAG_USE_TOP_COLOR = 32;
    private static final int MAX_RADIUS_CHUNKS = 128;
    private static final float CELL_SIZE_IN_BLOCKS = 12.0F;
    private static final int UBO_SIZE = new Std140SizeCalculator().putVec4().putVec3().putVec3().get();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/clouds.png");
    private static final float BLOCKS_PER_SECOND = 0.6F;
    private static final long EMPTY_CELL = 0L;
    private static final int COLOR_OFFSET = 4;
    private static final int NORTH_OFFSET = 3;
    private static final int EAST_OFFSET = 2;
    private static final int SOUTH_OFFSET = 1;
    private static final int WEST_OFFSET = 0;
    private boolean needsRebuild = true;
    private int prevCellX = Integer.MIN_VALUE;
    private int prevCellZ = Integer.MIN_VALUE;
    private CloudRenderer.RelativeCameraPos prevRelativeCameraPos = CloudRenderer.RelativeCameraPos.INSIDE_CLOUDS;
    @Nullable
    private CloudStatus prevType;
    @Nullable
    private CloudRenderer.TextureData texture;
    private int quadCount = 0;
    private final RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
    private final MappableRingBuffer ubo = new MappableRingBuffer(() -> "Cloud UBO", 130, UBO_SIZE);
    @Nullable
    private MappableRingBuffer utb;

    /**
     * Performs any reloading that can be done off-thread, such as file IO
     */
    protected Optional<CloudRenderer.TextureData> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        try {
            Optional optional;
            try (
                InputStream inputstream = resourceManager.open(TEXTURE_LOCATION);
                NativeImage nativeimage = NativeImage.read(inputstream);
            ) {
                int i = nativeimage.getWidth();
                int j = nativeimage.getHeight();
                long[] along = new long[i * j];

                for (int k = 0; k < j; k++) {
                    for (int l = 0; l < i; l++) {
                        int i1 = nativeimage.getPixel(l, k);
                        if (isCellEmpty(i1)) {
                            along[l + k * i] = 0L;
                        } else {
                            boolean flag = isCellEmpty(nativeimage.getPixel(l, Math.floorMod(k - 1, j)));
                            boolean flag1 = isCellEmpty(nativeimage.getPixel(Math.floorMod(l + 1, j), k));
                            boolean flag2 = isCellEmpty(nativeimage.getPixel(l, Math.floorMod(k + 1, j)));
                            boolean flag3 = isCellEmpty(nativeimage.getPixel(Math.floorMod(l - 1, j), k));
                            along[l + k * i] = packCellData(i1, flag, flag1, flag2, flag3);
                        }
                    }
                }

                optional = Optional.of(new CloudRenderer.TextureData(along, i, j));
            }

            return optional;
        } catch (IOException ioexception) {
            LOGGER.error("Failed to load cloud texture", (Throwable)ioexception);
            return Optional.empty();
        }
    }

    private static int getSizeForCloudDistance(int cloudDistance) {
        int i = 4;
        int j = (cloudDistance + 1) * 2 * (cloudDistance + 1) * 2 / 2;
        int k = j * 4 + 54;
        return k * 3;
    }

    protected void apply(Optional<CloudRenderer.TextureData> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        this.texture = object.orElse(null);
        this.needsRebuild = true;
    }

    private static boolean isCellEmpty(int color) {
        return ARGB.alpha(color) < 10;
    }

    private static long packCellData(int color, boolean northEmpty, boolean eastEmpty, boolean southEmpty, boolean westEmpty) {
        return (long)color << 4 | (northEmpty ? 1 : 0) << 3 | (eastEmpty ? 1 : 0) << 2 | (southEmpty ? 1 : 0) << 1 | (westEmpty ? 1 : 0) << 0;
    }

    private static boolean isNorthEmpty(long cellData) {
        return (cellData >> 3 & 1L) != 0L;
    }

    private static boolean isEastEmpty(long cellData) {
        return (cellData >> 2 & 1L) != 0L;
    }

    private static boolean isSouthEmpty(long cellData) {
        return (cellData >> 1 & 1L) != 0L;
    }

    private static boolean isWestEmpty(long cellData) {
        return (cellData >> 0 & 1L) != 0L;
    }

    public void render(int cloudColor, CloudStatus cloudStatus, float height, Vec3 cameraPosition, float ticks) {
        if (this.texture != null) {
            int i = Math.min(Minecraft.getInstance().options.cloudRange().get(), 128) * 16;
            int j = Mth.ceil(i / 12.0F);
            int k = getSizeForCloudDistance(j);
            if (this.utb == null || this.utb.currentBuffer().size() != k) {
                if (this.utb != null) {
                    this.utb.close();
                }

                this.utb = new MappableRingBuffer(() -> "Cloud UTB", 258, k);
            }

            float f = (float)(height - cameraPosition.y);
            float f1 = f + 4.0F;
            CloudRenderer.RelativeCameraPos cloudrenderer$relativecamerapos;
            if (f1 < 0.0F) {
                cloudrenderer$relativecamerapos = CloudRenderer.RelativeCameraPos.ABOVE_CLOUDS;
            } else if (f > 0.0F) {
                cloudrenderer$relativecamerapos = CloudRenderer.RelativeCameraPos.BELOW_CLOUDS;
            } else {
                cloudrenderer$relativecamerapos = CloudRenderer.RelativeCameraPos.INSIDE_CLOUDS;
            }

            double d0 = cameraPosition.x + ticks * 0.030000001F;
            double d1 = cameraPosition.z + 3.96F;
            double d2 = this.texture.width * 12.0;
            double d3 = this.texture.height * 12.0;
            d0 -= Mth.floor(d0 / d2) * d2;
            d1 -= Mth.floor(d1 / d3) * d3;
            int l = Mth.floor(d0 / 12.0);
            int i1 = Mth.floor(d1 / 12.0);
            float f2 = (float)(d0 - l * 12.0F);
            float f3 = (float)(d1 - i1 * 12.0F);
            boolean flag = cloudStatus == CloudStatus.FANCY;
            RenderPipeline renderpipeline = flag ? RenderPipelines.CLOUDS : RenderPipelines.FLAT_CLOUDS;
            if (this.needsRebuild
                || l != this.prevCellX
                || i1 != this.prevCellZ
                || cloudrenderer$relativecamerapos != this.prevRelativeCameraPos
                || cloudStatus != this.prevType) {
                this.needsRebuild = false;
                this.prevCellX = l;
                this.prevCellZ = i1;
                this.prevRelativeCameraPos = cloudrenderer$relativecamerapos;
                this.prevType = cloudStatus;
                this.utb.rotate();

                try (GpuBuffer.MappedView gpubuffer$mappedview = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .mapBuffer(this.utb.currentBuffer(), false, true)) {
                    this.buildMesh(cloudrenderer$relativecamerapos, gpubuffer$mappedview.data(), l, i1, flag, j);
                    this.quadCount = gpubuffer$mappedview.data().position() / 3;
                }
            }

            if (this.quadCount != 0) {
                try (GpuBuffer.MappedView gpubuffer$mappedview1 = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .mapBuffer(this.ubo.currentBuffer(), false, true)) {
                    Std140Builder.intoBuffer(gpubuffer$mappedview1.data())
                        .putVec4(ARGB.redFloat(cloudColor), ARGB.greenFloat(cloudColor), ARGB.blueFloat(cloudColor), 1.0F)
                        .putVec3(-f2, f, -f3)
                        .putVec3(12.0F, 4.0F, 12.0F);
                }

                GpuBufferSlice gpubufferslice = RenderSystem.getDynamicUniforms()
                    .writeTransform(RenderSystem.getModelViewMatrix(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f(), 0.0F);
                RenderTarget rendertarget = Minecraft.getInstance().getMainRenderTarget();
                RenderTarget rendertarget1 = Minecraft.getInstance().levelRenderer.getCloudsTarget();
                RenderSystem.AutoStorageIndexBuffer rendersystem$autostorageindexbuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
                GpuBuffer gpubuffer = rendersystem$autostorageindexbuffer.getBuffer(6 * this.quadCount);
                GpuTextureView gputextureview;
                GpuTextureView gputextureview1;
                if (rendertarget1 != null) {
                    gputextureview = rendertarget1.getColorTextureView();
                    gputextureview1 = rendertarget1.getDepthTextureView();
                } else {
                    gputextureview = rendertarget.getColorTextureView();
                    gputextureview1 = rendertarget.getDepthTextureView();
                }

                try (RenderPass renderpass = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(() -> "Clouds", gputextureview, OptionalInt.empty(), gputextureview1, OptionalDouble.empty())) {
                    renderpass.setPipeline(renderpipeline);
                    RenderSystem.bindDefaultUniforms(renderpass);
                    renderpass.setUniform("DynamicTransforms", gpubufferslice);
                    renderpass.setIndexBuffer(gpubuffer, rendersystem$autostorageindexbuffer.type());
                    renderpass.setUniform("CloudInfo", this.ubo.currentBuffer());
                    renderpass.setUniform("CloudFaces", this.utb.currentBuffer());
                    renderpass.drawIndexed(0, 0, 6 * this.quadCount, 1);
                }
            }
        }
    }

    private void buildMesh(CloudRenderer.RelativeCameraPos relativeCameraPos, ByteBuffer buffer, int cellX, int cellZ, boolean fancyClouds, int size) {
        if (this.texture != null) {
            long[] along = this.texture.cells;
            int i = this.texture.width;
            int j = this.texture.height;

            for (int k = 0; k <= 2 * size; k++) {
                for (int l = -k; l <= k; l++) {
                    int i1 = k - Math.abs(l);
                    if (i1 >= 0 && i1 <= size && l * l + i1 * i1 <= size * size) {
                        if (i1 != 0) {
                            this.tryBuildCell(relativeCameraPos, buffer, cellX, cellZ, fancyClouds, l, i, -i1, j, along);
                        }

                        this.tryBuildCell(relativeCameraPos, buffer, cellX, cellZ, fancyClouds, l, i, i1, j, along);
                    }
                }
            }
        }
    }

    private void tryBuildCell(
        CloudRenderer.RelativeCameraPos relativeCameraPos,
        ByteBuffer buffer,
        int cellX,
        int cellZ,
        boolean fancyClouds,
        int x,
        int width,
        int z,
        int height,
        long[] cells
    ) {
        int i = Math.floorMod(cellX + x, width);
        int j = Math.floorMod(cellZ + z, height);
        long k = cells[i + j * width];
        if (k != 0L) {
            if (fancyClouds) {
                this.buildExtrudedCell(relativeCameraPos, buffer, x, z, k);
            } else {
                this.buildFlatCell(buffer, x, z);
            }
        }
    }

    private void buildFlatCell(ByteBuffer buffer, int cellX, int cellZ) {
        this.encodeFace(buffer, cellX, cellZ, Direction.DOWN, 32);
    }

    private void encodeFace(ByteBuffer buffer, int cellX, int cellZ, Direction face, int offset) {
        int i = face.get3DDataValue() | offset;
        i |= (cellX & 1) << 7;
        i |= (cellZ & 1) << 6;
        buffer.put((byte)(cellX >> 1)).put((byte)(cellZ >> 1)).put((byte)i);
    }

    private void buildExtrudedCell(CloudRenderer.RelativeCameraPos relativeCameraPos, ByteBuffer buffer, int cellX, int cellZ, long cellData) {
        if (relativeCameraPos != CloudRenderer.RelativeCameraPos.BELOW_CLOUDS) {
            this.encodeFace(buffer, cellX, cellZ, Direction.UP, 0);
        }

        if (relativeCameraPos != CloudRenderer.RelativeCameraPos.ABOVE_CLOUDS) {
            this.encodeFace(buffer, cellX, cellZ, Direction.DOWN, 0);
        }

        if (isNorthEmpty(cellData) && cellZ > 0) {
            this.encodeFace(buffer, cellX, cellZ, Direction.NORTH, 0);
        }

        if (isSouthEmpty(cellData) && cellZ < 0) {
            this.encodeFace(buffer, cellX, cellZ, Direction.SOUTH, 0);
        }

        if (isWestEmpty(cellData) && cellX > 0) {
            this.encodeFace(buffer, cellX, cellZ, Direction.WEST, 0);
        }

        if (isEastEmpty(cellData) && cellX < 0) {
            this.encodeFace(buffer, cellX, cellZ, Direction.EAST, 0);
        }

        boolean flag = Math.abs(cellX) <= 1 && Math.abs(cellZ) <= 1;
        if (flag) {
            for (Direction direction : Direction.values()) {
                this.encodeFace(buffer, cellX, cellZ, direction, 16);
            }
        }
    }

    public void markForRebuild() {
        this.needsRebuild = true;
    }

    public void endFrame() {
        this.ubo.rotate();
    }

    @Override
    public void close() {
        this.ubo.close();
        if (this.utb != null) {
            this.utb.close();
        }
    }

    @OnlyIn(Dist.CLIENT)
    static enum RelativeCameraPos {
        ABOVE_CLOUDS,
        INSIDE_CLOUDS,
        BELOW_CLOUDS;
    }

    @OnlyIn(Dist.CLIENT)
    public record TextureData(long[] cells, int width, int height) {
    }
}
