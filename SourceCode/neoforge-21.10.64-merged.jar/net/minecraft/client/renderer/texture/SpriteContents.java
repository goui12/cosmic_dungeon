package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.client.resources.metadata.animation.AnimationFrame;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.util.ARGB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SpriteContents implements Stitcher.Entry, AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    final ResourceLocation name;
    final int width;
    final int height;
    private final NativeImage originalImage;
    public NativeImage[] byMipLevel;
    @Nullable
    final SpriteContents.AnimatedTexture animatedTexture;
    private final List<MetadataSectionType.WithValue<?>> additionalMetadata;

    public SpriteContents(ResourceLocation name, FrameSize size, NativeImage originalImage) {
        this(name, size, originalImage, Optional.empty(), List.of());
    }

    public SpriteContents(
        ResourceLocation name,
        FrameSize size,
        NativeImage originalImage,
        Optional<AnimationMetadataSection> animationMetadata,
        List<MetadataSectionType.WithValue<?>> additionalMetadata
    ) {
        this.name = name;
        this.width = size.width();
        this.height = size.height();
        this.additionalMetadata = additionalMetadata;
        this.animatedTexture = animationMetadata.<SpriteContents.AnimatedTexture>map(
                p_389349_ -> this.createAnimatedTexture(size, originalImage.getWidth(), originalImage.getHeight(), p_389349_)
            )
            .orElse(null);
        this.originalImage = originalImage;
        this.byMipLevel = new NativeImage[]{this.originalImage};
    }

    public NativeImage getOriginalImage() {
        return this.originalImage;
    }

    public void increaseMipLevel(int mipLevel) {
        try {
            this.byMipLevel = MipmapGenerator.generateMipLevels(this.byMipLevel, mipLevel);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Generating mipmaps for frame");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Frame being iterated");
            crashreportcategory.setDetail("Sprite name", this.name);
            crashreportcategory.setDetail("Sprite size", () -> this.width + " x " + this.height);
            crashreportcategory.setDetail("Sprite frames", () -> this.getFrameCount() + " frames");
            crashreportcategory.setDetail("Mipmap levels", mipLevel);
            crashreportcategory.setDetail("Original image size", () -> this.originalImage.getWidth() + "x" + this.originalImage.getHeight());
            throw new ReportedException(crashreport);
        }
    }

    private int getFrameCount() {
        return this.animatedTexture != null ? this.animatedTexture.frames.size() : 1;
    }

    public boolean isAnimated() {
        return this.getFrameCount() > 1;
    }

    @Nullable
    private SpriteContents.AnimatedTexture createAnimatedTexture(FrameSize frameSize, int width, int height, AnimationMetadataSection metadata) {
        int i = width / frameSize.width();
        int j = height / frameSize.height();
        int k = i * j;
        int l = metadata.defaultFrameTime();
        List<SpriteContents.FrameInfo> list;
        if (metadata.frames().isEmpty()) {
            list = new ArrayList<>(k);

            for (int i1 = 0; i1 < k; i1++) {
                list.add(new SpriteContents.FrameInfo(i1, l));
            }
        } else {
            List<AnimationFrame> list1 = metadata.frames().get();
            list = new ArrayList<>(list1.size());

            for (AnimationFrame animationframe : list1) {
                list.add(new SpriteContents.FrameInfo(animationframe.index(), animationframe.timeOr(l)));
            }

            int j1 = 0;
            IntSet intset = new IntOpenHashSet();

            for (Iterator<SpriteContents.FrameInfo> iterator = list.iterator(); iterator.hasNext(); j1++) {
                SpriteContents.FrameInfo spritecontents$frameinfo = iterator.next();
                boolean flag = true;
                if (spritecontents$frameinfo.time <= 0) {
                    LOGGER.warn("Invalid frame duration on sprite {} frame {}: {}", this.name, j1, spritecontents$frameinfo.time);
                    flag = false;
                }

                if (spritecontents$frameinfo.index < 0 || spritecontents$frameinfo.index >= k) {
                    LOGGER.warn("Invalid frame index on sprite {} frame {}: {}", this.name, j1, spritecontents$frameinfo.index);
                    flag = false;
                }

                if (flag) {
                    intset.add(spritecontents$frameinfo.index);
                } else {
                    iterator.remove();
                }
            }

            int[] aint = IntStream.range(0, k).filter(p_251185_ -> !intset.contains(p_251185_)).toArray();
            if (aint.length > 0) {
                LOGGER.warn("Unused frames in sprite {}: {}", this.name, Arrays.toString(aint));
            }
        }

        return list.size() <= 1 ? null : new SpriteContents.AnimatedTexture(List.copyOf(list), i, metadata.interpolatedFrames());
    }

    void upload(int x, int y, int sourceX, int sourceY, NativeImage[] images, GpuTexture texture) {
        for (int i = 0; i < this.byMipLevel.length; i++) {
            // NeoForge: Skip uploading if the texture would be made invalid by mip level
            if ((this.width >> i) <= 0 || (this.height >> i) <= 0) break;
            RenderSystem.getDevice()
                .createCommandEncoder()
                .writeToTexture(
                    texture, images[i], i, 0, x >> i, y >> i, this.width >> i, this.height >> i, sourceX >> i, sourceY >> i
                );
        }
    }

    @Override
    public int width() {
        return this.width;
    }

    @Override
    public int height() {
        return this.height;
    }

    @Override
    public ResourceLocation name() {
        return this.name;
    }

    public IntStream getUniqueFrames() {
        return this.animatedTexture != null ? this.animatedTexture.getUniqueFrames() : IntStream.of(1);
    }

    @Nullable
    public SpriteTicker createTicker() {
        return this.animatedTexture != null ? this.animatedTexture.createTicker() : null;
    }

    public <T> Optional<T> getAdditionalMetadata(MetadataSectionType<T> sectionType) {
        for (MetadataSectionType.WithValue<?> withvalue : this.additionalMetadata) {
            Optional<T> optional = withvalue.unwrapToType(sectionType);
            if (optional.isPresent()) {
                return optional;
            }
        }

        return Optional.empty();
    }

    @Override
    public void close() {
        for (NativeImage nativeimage : this.byMipLevel) {
            nativeimage.close();
        }
    }

    @Override
    public String toString() {
        return "SpriteContents{name=" + this.name + ", frameCount=" + this.getFrameCount() + ", height=" + this.height + ", width=" + this.width + "}";
    }

    public boolean isTransparent(int frame, int x, int y) {
        int i = x;
        int j = y;
        if (this.animatedTexture != null) {
            i = x + this.animatedTexture.getFrameX(frame) * this.width;
            j = y + this.animatedTexture.getFrameY(frame) * this.height;
        }

        return ARGB.alpha(this.originalImage.getPixel(i, j)) == 0;
    }

    public void uploadFirstFrame(int x, int y, GpuTexture texture) {
        if (this.animatedTexture != null) {
            this.animatedTexture.uploadFirstFrame(x, y, texture);
        } else {
            this.upload(x, y, 0, 0, this.byMipLevel, texture);
        }
    }

    @OnlyIn(Dist.CLIENT)
    class AnimatedTexture {
        final List<SpriteContents.FrameInfo> frames;
        private final int frameRowSize;
        private final boolean interpolateFrames;

        AnimatedTexture(List<SpriteContents.FrameInfo> frames, int frameRowSize, boolean interpolateFrames) {
            this.frames = frames;
            this.frameRowSize = frameRowSize;
            this.interpolateFrames = interpolateFrames;
        }

        int getFrameX(int frameIndex) {
            return frameIndex % this.frameRowSize;
        }

        int getFrameY(int frameIndex) {
            return frameIndex / this.frameRowSize;
        }

        void uploadFrame(int x, int y, int frameIndex, GpuTexture texture) {
            int i = this.getFrameX(frameIndex) * SpriteContents.this.width;
            int j = this.getFrameY(frameIndex) * SpriteContents.this.height;
            SpriteContents.this.upload(x, y, i, j, SpriteContents.this.byMipLevel, texture);
        }

        public SpriteTicker createTicker() {
            return SpriteContents.this.new Ticker(this, this.interpolateFrames ? SpriteContents.this.new InterpolationData() : null);
        }

        public void uploadFirstFrame(int x, int y, GpuTexture texture) {
            this.uploadFrame(x, y, this.frames.get(0).index, texture);
        }

        public IntStream getUniqueFrames() {
            return this.frames.stream().mapToInt(p_249981_ -> p_249981_.index).distinct();
        }
    }

    @OnlyIn(Dist.CLIENT)
    record FrameInfo(int index, int time) {
    }

    @OnlyIn(Dist.CLIENT)
    final class InterpolationData implements AutoCloseable {
        private final NativeImage[] activeFrame = new NativeImage[SpriteContents.this.byMipLevel.length];

        InterpolationData() {
            for (int i = 0; i < this.activeFrame.length; i++) {
                int j = SpriteContents.this.width >> i;
                int k = SpriteContents.this.height >> i;
                // Forge: Guard against invalid texture size, because we allow generating mipmaps regardless of texture sizes
                this.activeFrame[i] = new NativeImage(Math.max(1, j), Math.max(1, k), false);
            }
        }

        void uploadInterpolatedFrame(int x, int y, SpriteContents.Ticker ticker, GpuTexture texture) {
            SpriteContents.AnimatedTexture spritecontents$animatedtexture = ticker.animationInfo;
            List<SpriteContents.FrameInfo> list = spritecontents$animatedtexture.frames;
            SpriteContents.FrameInfo spritecontents$frameinfo = list.get(ticker.frame);
            float f = (float)ticker.subFrame / spritecontents$frameinfo.time;
            int i = spritecontents$frameinfo.index;
            int j = list.get((ticker.frame + 1) % list.size()).index;
            if (i != j) {
                for (int k = 0; k < this.activeFrame.length; k++) {
                    int l = SpriteContents.this.width >> k;
                    int i1 = SpriteContents.this.height >> k;
                    // Forge: Guard against invalid texture size, because we allow generating mipmaps regardless of texture sizes
                    if (l < 1 || i1 < 1)
                        continue;

                    for (int j1 = 0; j1 < i1; j1++) {
                        for (int k1 = 0; k1 < l; k1++) {
                            int l1 = this.getPixel(spritecontents$animatedtexture, i, k, k1, j1);
                            int i2 = this.getPixel(spritecontents$animatedtexture, j, k, k1, j1);
                            this.activeFrame[k].setPixel(k1, j1, ARGB.lerp(f, l1, i2));
                        }
                    }
                }

                SpriteContents.this.upload(x, y, 0, 0, this.activeFrame, texture);
                if (SharedConstants.DEBUG_DUMP_INTERPOLATED_TEXTURE_FRAMES) {
                    try {
                        Path path = TextureUtil.getDebugTexturePath();
                        Path path1 = path.resolve(SpriteContents.this.name.toDebugFileName());
                        Files.createDirectories(path1);

                        for (int j2 = 0; j2 < this.activeFrame.length; j2++) {
                            this.activeFrame[j2].writeToFile(path1.resolve(SpriteContents.this.name.toDebugFileName() + "_" + j2 + "_" + i + "_" + j + ".png"));
                        }
                    } catch (IOException ioexception) {
                    }
                }
            }
        }

        private int getPixel(SpriteContents.AnimatedTexture animatedTexture, int frameIndex, int mipLevel, int x, int y) {
            return SpriteContents.this.byMipLevel[mipLevel]
                .getPixel(
                    x + (animatedTexture.getFrameX(frameIndex) * SpriteContents.this.width >> mipLevel),
                    y + (animatedTexture.getFrameY(frameIndex) * SpriteContents.this.height >> mipLevel)
                );
        }

        @Override
        public void close() {
            for (NativeImage nativeimage : this.activeFrame) {
                nativeimage.close();
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    class Ticker implements SpriteTicker {
        int frame;
        int subFrame;
        final SpriteContents.AnimatedTexture animationInfo;
        @Nullable
        private final SpriteContents.InterpolationData interpolationData;

        Ticker(SpriteContents.AnimatedTexture animationInfo, @Nullable SpriteContents.InterpolationData interpolationData) {
            this.animationInfo = animationInfo;
            this.interpolationData = interpolationData;
        }

        @Override
        public void tickAndUpload(int x, int y, GpuTexture texture) {
            this.subFrame++;
            SpriteContents.FrameInfo spritecontents$frameinfo = this.animationInfo.frames.get(this.frame);
            if (this.subFrame >= spritecontents$frameinfo.time) {
                int i = spritecontents$frameinfo.index;
                this.frame = (this.frame + 1) % this.animationInfo.frames.size();
                this.subFrame = 0;
                int j = this.animationInfo.frames.get(this.frame).index;
                if (i != j) {
                    this.animationInfo.uploadFrame(x, y, j, texture);
                }
            } else if (this.interpolationData != null) {
                this.interpolationData.uploadInterpolatedFrame(x, y, this, texture);
            }
        }

        @Override
        public void close() {
            if (this.interpolationData != null) {
                this.interpolationData.close();
            }
        }
    }
}
