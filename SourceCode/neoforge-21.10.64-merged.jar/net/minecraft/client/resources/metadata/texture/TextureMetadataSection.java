package net.minecraft.client.resources.metadata.texture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record TextureMetadataSection(boolean blur, boolean clamp) {
    public static final boolean DEFAULT_BLUR = false;
    public static final boolean DEFAULT_CLAMP = false;
    public static final Codec<TextureMetadataSection> CODEC = RecordCodecBuilder.create(
        p_389522_ -> p_389522_.group(
                Codec.BOOL.optionalFieldOf("blur", false).forGetter(TextureMetadataSection::blur),
                Codec.BOOL.optionalFieldOf("clamp", false).forGetter(TextureMetadataSection::clamp)
            )
            .apply(p_389522_, TextureMetadataSection::new)
    );
    public static final MetadataSectionType<TextureMetadataSection> TYPE = new MetadataSectionType<>("texture", CODEC);
}
