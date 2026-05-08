package net.minecraft.client.renderer.texture.atlas;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.client.renderer.texture.atlas.sources.DirectoryLister;
import net.minecraft.client.renderer.texture.atlas.sources.PalettedPermutations;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.client.renderer.texture.atlas.sources.SourceFilter;
import net.minecraft.client.renderer.texture.atlas.sources.Unstitcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpriteSources {
    private static final ExtraCodecs.LateBoundIdMapper<ResourceLocation, MapCodec<? extends SpriteSource>> ID_MAPPER = new ExtraCodecs.LateBoundIdMapper<>();
    public static final Codec<SpriteSource> CODEC = ID_MAPPER.codec(ResourceLocation.CODEC).dispatch(SpriteSource::codec, p_399350_ -> p_399350_);
    public static final Codec<List<SpriteSource>> FILE_CODEC = net.neoforged.neoforge.common.conditions.ConditionalOps.decodeListWithElementConditions(CODEC).fieldOf("sources").codec();

    public static void bootstrap() {
        ID_MAPPER.put(ResourceLocation.withDefaultNamespace("single"), SingleFile.MAP_CODEC);
        ID_MAPPER.put(ResourceLocation.withDefaultNamespace("directory"), DirectoryLister.MAP_CODEC);
        ID_MAPPER.put(ResourceLocation.withDefaultNamespace("filter"), SourceFilter.MAP_CODEC);
        ID_MAPPER.put(ResourceLocation.withDefaultNamespace("unstitch"), Unstitcher.MAP_CODEC);
        ID_MAPPER.put(ResourceLocation.withDefaultNamespace("paletted_permutations"), PalettedPermutations.MAP_CODEC);

        net.neoforged.fml.ModLoader.postEvent(new net.neoforged.neoforge.client.event.RegisterSpriteSourcesEvent(ID_MAPPER));
    }
}
