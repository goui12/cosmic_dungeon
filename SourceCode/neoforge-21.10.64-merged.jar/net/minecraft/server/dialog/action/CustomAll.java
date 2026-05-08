package net.minecraft.server.dialog.action;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Map;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.resources.ResourceLocation;

public record CustomAll(ResourceLocation id, Optional<CompoundTag> additions) implements Action {
    public static final MapCodec<CustomAll> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_428385_ -> p_428385_.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(CustomAll::id), CompoundTag.CODEC.optionalFieldOf("additions").forGetter(CustomAll::additions)
            )
            .apply(p_428385_, CustomAll::new)
    );

    @Override
    public MapCodec<CustomAll> codec() {
        return MAP_CODEC;
    }

    @Override
    public Optional<ClickEvent> createAction(Map<String, Action.ValueGetter> p_428540_) {
        CompoundTag compoundtag = this.additions.<CompoundTag>map(CompoundTag::copy).orElseGet(CompoundTag::new);
        p_428540_.forEach((p_428406_, p_428563_) -> compoundtag.put(p_428406_, p_428563_.asTag()));
        return Optional.of(new ClickEvent.Custom(this.id, Optional.of(compoundtag)));
    }
}
