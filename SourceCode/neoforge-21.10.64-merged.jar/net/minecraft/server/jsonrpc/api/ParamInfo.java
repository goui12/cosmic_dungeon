package net.minecraft.server.jsonrpc.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;

public record ParamInfo(String name, Schema schema, boolean required) {
    public static final MapCodec<ParamInfo> CODEC = RecordCodecBuilder.mapCodec(
        p_451632_ -> p_451632_.group(
                Codec.STRING.fieldOf("name").forGetter(ParamInfo::name),
                Schema.CODEC.fieldOf("schema").forGetter(ParamInfo::schema),
                Codec.BOOL.fieldOf("required").forGetter(ParamInfo::required)
            )
            .apply(p_451632_, ParamInfo::new)
    );

    public ParamInfo(String p_442876_, Schema p_443570_) {
        this(p_442876_, p_443570_, true);
    }
}
