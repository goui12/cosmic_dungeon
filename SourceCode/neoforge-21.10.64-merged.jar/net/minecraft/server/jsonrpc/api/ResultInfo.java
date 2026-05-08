package net.minecraft.server.jsonrpc.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;

public record ResultInfo(String name, Schema schema) {
    public static final MapCodec<ResultInfo> CODEC = RecordCodecBuilder.mapCodec(
        p_451633_ -> p_451633_.group(Codec.STRING.fieldOf("name").forGetter(ResultInfo::name), Schema.CODEC.fieldOf("schema").forGetter(ResultInfo::schema))
            .apply(p_451633_, ResultInfo::new)
    );
}
