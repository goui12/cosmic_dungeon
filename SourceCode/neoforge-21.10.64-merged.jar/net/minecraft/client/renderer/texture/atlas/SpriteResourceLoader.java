package net.minecraft.client.renderer.texture.atlas;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@FunctionalInterface
@OnlyIn(Dist.CLIENT)
public interface SpriteResourceLoader {
    Logger LOGGER = LogUtils.getLogger();

    static SpriteResourceLoader create(Set<MetadataSectionType<?>> sectionTypes) {
        return (p_432311_, p_432312_, constructor) -> {
            Optional<AnimationMetadataSection> optional;
            List<MetadataSectionType.WithValue<?>> list;
            try {
                ResourceMetadata resourcemetadata = p_432312_.metadata();
                optional = resourcemetadata.getSection(AnimationMetadataSection.TYPE);
                list = resourcemetadata.getTypedSections(sectionTypes);
            } catch (Exception exception) {
                LOGGER.error("Unable to parse metadata from {}", p_432311_, exception);
                return null;
            }

            NativeImage nativeimage;
            try (InputStream inputstream = p_432312_.open()) {
                nativeimage = NativeImage.read(inputstream);
            } catch (IOException ioexception) {
                LOGGER.error("Using missing texture, unable to load {}", p_432311_, ioexception);
                return null;
            }

            FrameSize framesize;
            if (optional.isPresent()) {
                framesize = optional.get().calculateFrameSize(nativeimage.getWidth(), nativeimage.getHeight());
                if (!Mth.isMultipleOf(nativeimage.getWidth(), framesize.width()) || !Mth.isMultipleOf(nativeimage.getHeight(), framesize.height())) {
                    LOGGER.error(
                        "Image {} size {},{} is not multiple of frame size {},{}",
                        p_432311_,
                        nativeimage.getWidth(),
                        nativeimage.getHeight(),
                        framesize.width(),
                        framesize.height()
                    );
                    nativeimage.close();
                    return null;
                }
            } else {
                framesize = new FrameSize(nativeimage.getWidth(), nativeimage.getHeight());
            }

            return constructor.create(p_432311_, framesize, nativeimage, optional, list);
        };
    }

    @Nullable
    default SpriteContents loadSprite(ResourceLocation location, Resource resource) {
        return loadSprite(location, resource, SpriteContents::new);
    }

    @Nullable
    SpriteContents loadSprite(ResourceLocation location, Resource resource, net.neoforged.neoforge.client.textures.SpriteContentsConstructor constructor);
}
