package net.minecraft.server.jsonrpc.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

public record MethodInfo(String description, Optional<ParamInfo> params, Optional<ResultInfo> result) {
    public static final Codec<Optional<ParamInfo>> PARAMS_CODEC = ParamInfo.CODEC
        .codec()
        .listOf()
        .xmap(p_451629_ -> p_451629_.stream().findAny(), p_451630_ -> p_451630_.map(List::of).orElse(List.of()));
    public static final MapCodec<MethodInfo> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_451631_ -> p_451631_.group(
                Codec.STRING.fieldOf("description").forGetter(MethodInfo::description),
                PARAMS_CODEC.fieldOf("params").forGetter(MethodInfo::params),
                ResultInfo.CODEC.codec().optionalFieldOf("result").forGetter(MethodInfo::result)
            )
            .apply(p_451631_, MethodInfo::new)
    );

    public MethodInfo(String p_442898_, @Nullable ParamInfo p_451419_, @Nullable ResultInfo p_450926_) {
        this(p_442898_, Optional.ofNullable(p_451419_), Optional.ofNullable(p_450926_));
    }

    public MethodInfo.Named named(ResourceLocation name) {
        return new MethodInfo.Named(name, this);
    }

    public record Named(ResourceLocation name, MethodInfo contents) {
        public static final Codec<MethodInfo.Named> CODEC = RecordCodecBuilder.create(
            p_451380_ -> p_451380_.group(
                    ResourceLocation.CODEC.fieldOf("name").forGetter(MethodInfo.Named::name), MethodInfo.MAP_CODEC.forGetter(MethodInfo.Named::contents)
                )
                .apply(p_451380_, MethodInfo.Named::new)
        );
    }
}
