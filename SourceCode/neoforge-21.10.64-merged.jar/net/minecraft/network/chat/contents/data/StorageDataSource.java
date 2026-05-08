package net.minecraft.network.chat.contents.data;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public record StorageDataSource(ResourceLocation id) implements DataSource {
    public static final MapCodec<StorageDataSource> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_442589_ -> p_442589_.group(ResourceLocation.CODEC.fieldOf("storage").forGetter(StorageDataSource::id)).apply(p_442589_, StorageDataSource::new)
    );

    @Override
    public Stream<CompoundTag> getData(CommandSourceStack p_443094_) {
        CompoundTag compoundtag = p_443094_.getServer().getCommandStorage().get(this.id);
        return Stream.of(compoundtag);
    }

    @Override
    public MapCodec<StorageDataSource> codec() {
        return MAP_CODEC;
    }

    @Override
    public String toString() {
        return "storage=" + this.id;
    }
}
