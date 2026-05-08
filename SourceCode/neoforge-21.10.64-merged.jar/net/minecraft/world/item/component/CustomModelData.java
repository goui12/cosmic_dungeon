package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import io.netty.buffer.ByteBuf;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

public record CustomModelData(List<Float> floats, List<Boolean> flags, List<String> strings, List<Integer> colors) {
    public static final CustomModelData EMPTY = new CustomModelData(List.of(), List.of(), List.of(), List.of());
    public static final Codec<CustomModelData> CODEC = RecordCodecBuilder.create(
        p_387502_ -> p_387502_.group(
                Codec.FLOAT.listOf().optionalFieldOf("floats", List.of()).forGetter(CustomModelData::floats),
                Codec.BOOL.listOf().optionalFieldOf("flags", List.of()).forGetter(CustomModelData::flags),
                Codec.STRING.listOf().optionalFieldOf("strings", List.of()).forGetter(CustomModelData::strings),
                ExtraCodecs.RGB_COLOR_CODEC.listOf().optionalFieldOf("colors", List.of()).forGetter(CustomModelData::colors)
            )
            .apply(p_387502_, CustomModelData::new)
    );
    public static final StreamCodec<ByteBuf, CustomModelData> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT.apply(ByteBufCodecs.list()),
        CustomModelData::floats,
        ByteBufCodecs.BOOL.apply(ByteBufCodecs.list()),
        CustomModelData::flags,
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
        CustomModelData::strings,
        ByteBufCodecs.INT.apply(ByteBufCodecs.list()),
        CustomModelData::colors,
        CustomModelData::new
    );

    @Nullable
    private static <T> T getSafe(List<T> values, int index) {
        return index >= 0 && index < values.size() ? values.get(index) : null;
    }

    @Nullable
    public Float getFloat(int index) {
        return getSafe(this.floats, index);
    }

    @Nullable
    public Boolean getBoolean(int index) {
        return getSafe(this.flags, index);
    }

    @Nullable
    public String getString(int index) {
        return getSafe(this.strings, index);
    }

    @Nullable
    public Integer getColor(int index) {
        return getSafe(this.colors, index);
    }
}
