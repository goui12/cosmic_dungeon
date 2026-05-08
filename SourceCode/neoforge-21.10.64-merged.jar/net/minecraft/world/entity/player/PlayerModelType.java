package net.minecraft.world.entity.player;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Objects;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum PlayerModelType implements StringRepresentable {
    SLIM("slim", "slim"),
    WIDE("wide", "default");

    public static final Codec<PlayerModelType> CODEC = StringRepresentable.fromEnum(PlayerModelType::values);
    private static final Function<String, PlayerModelType> NAME_LOOKUP = StringRepresentable.createNameLookup(values(), p_448975_ -> p_448975_.legacyServicesId);
    public static final StreamCodec<ByteBuf, PlayerModelType> STREAM_CODEC = ByteBufCodecs.BOOL
        .map(p_446575_ -> p_446575_ ? SLIM : WIDE, p_446951_ -> p_446951_ == SLIM);
    private final String id;
    private final String legacyServicesId;

    private PlayerModelType(String id, String legacyServicesId) {
        this.id = id;
        this.legacyServicesId = legacyServicesId;
    }

    public static PlayerModelType byLegacyServicesName(@Nullable String name) {
        return Objects.requireNonNullElse(NAME_LOOKUP.apply(name), WIDE);
    }

    @Override
    public String getSerializedName() {
        return this.id;
    }
}
