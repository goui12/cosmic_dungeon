package net.minecraft.network.chat;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapDecoder;
import com.mojang.serialization.MapEncoder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.NbtContents;
import net.minecraft.network.chat.contents.ObjectContents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.ScoreContents;
import net.minecraft.network.chat.contents.SelectorContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.GsonHelper;

public class ComponentSerialization {
    public static final Codec<Component> CODEC = Codec.recursive("Component", ComponentSerialization::createCodec);
    public static final StreamCodec<RegistryFriendlyByteBuf, Component> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);
    public static final StreamCodec<RegistryFriendlyByteBuf, Optional<Component>> OPTIONAL_STREAM_CODEC = STREAM_CODEC.apply(ByteBufCodecs::optional);
    public static final StreamCodec<RegistryFriendlyByteBuf, Component> TRUSTED_STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistriesTrusted(CODEC);
    public static final StreamCodec<RegistryFriendlyByteBuf, Optional<Component>> TRUSTED_OPTIONAL_STREAM_CODEC = TRUSTED_STREAM_CODEC.apply(
        ByteBufCodecs::optional
    );
    public static final StreamCodec<ByteBuf, Component> TRUSTED_CONTEXT_FREE_STREAM_CODEC = ByteBufCodecs.fromCodecTrusted(CODEC);

    public static Codec<Component> flatRestrictedCodec(final int maxSize) {
        return new Codec<Component>() {
            @Override
            public <T> DataResult<Pair<Component, T>> decode(DynamicOps<T> ops, T input) {
                return ComponentSerialization.CODEC
                    .decode(ops, input)
                    .flatMap(
                        p_392612_ -> this.isTooLarge(ops, p_392612_.getFirst())
                            ? DataResult.error(() -> "Component was too large: greater than max size " + maxSize)
                            : DataResult.success((Pair<Component, T>)p_392612_)
                    );
            }

            public <T> DataResult<T> encode(Component input, DynamicOps<T> ops, T value) {
                return ComponentSerialization.CODEC.encodeStart(ops, input);
            }

            private <T> boolean isTooLarge(DynamicOps<T> ops, Component component) {
                DataResult<JsonElement> dataresult = ComponentSerialization.CODEC.encodeStart(asJsonOps(ops), component);
                return dataresult.isSuccess() && GsonHelper.encodesLongerThan(dataresult.getOrThrow(), maxSize);
            }

            private static <T> DynamicOps<JsonElement> asJsonOps(DynamicOps<T> ops) {
                return (DynamicOps<JsonElement>)(ops instanceof RegistryOps<T> registryops ? registryops.withParent(JsonOps.INSTANCE) : JsonOps.INSTANCE);
            }
        };
    }

    private static MutableComponent createFromList(List<Component> components) {
        MutableComponent mutablecomponent = components.get(0).copy();

        for (int i = 1; i < components.size(); i++) {
            mutablecomponent.append(components.get(i));
        }

        return mutablecomponent;
    }

    public static <T> MapCodec<T> createLegacyComponentMatcher(
        ExtraCodecs.LateBoundIdMapper<String, MapCodec<? extends T>> mapper, Function<T, MapCodec<? extends T>> encoderGetter, String fieldName
    ) {
        MapCodec<T> mapcodec = new ComponentSerialization.FuzzyCodec<>(mapper.values(), encoderGetter);
        MapCodec<T> mapcodec1 = mapper.codec(Codec.STRING).dispatchMap(fieldName, encoderGetter, p_443511_ -> p_443511_);
        MapCodec<T> mapcodec2 = new ComponentSerialization.StrictEither<>(fieldName, mapcodec1, mapcodec);
        return ExtraCodecs.orCompressed(mapcodec2, mapcodec1);
    }

    private static Codec<Component> createCodec(Codec<Component> p_codec) {
        ExtraCodecs.LateBoundIdMapper<String, MapCodec<? extends ComponentContents>> lateboundidmapper = new ExtraCodecs.LateBoundIdMapper<>();
        bootstrap(lateboundidmapper);
        MapCodec<ComponentContents> mapcodec = createLegacyComponentMatcher(lateboundidmapper, ComponentContents::codec, "type");
        Codec<Component> codec = RecordCodecBuilder.create(
            p_337494_ -> p_337494_.group(
                    mapcodec.forGetter(Component::getContents),
                    ExtraCodecs.nonEmptyList(p_codec.listOf()).optionalFieldOf("extra", List.of()).forGetter(Component::getSiblings),
                    Style.Serializer.MAP_CODEC.forGetter(Component::getStyle)
                )
                .apply(p_337494_, MutableComponent::new)
        );
        return Codec.either(Codec.either(Codec.STRING, ExtraCodecs.nonEmptyList(p_codec.listOf())), codec)
            .xmap(
                p_304547_ -> p_304547_.map(
                    p_304568_ -> p_304568_.map(Component::literal, ComponentSerialization::createFromList), p_304887_ -> (Component)p_304887_
                ),
                p_304501_ -> {
                    String s = p_304501_.tryCollapseToString();
                    return s != null ? Either.left(Either.left(s)) : Either.right(p_304501_);
                }
            );
    }

    private static void bootstrap(ExtraCodecs.LateBoundIdMapper<String, MapCodec<? extends ComponentContents>> mapper) {
        mapper.put("text", PlainTextContents.MAP_CODEC);
        mapper.put("translatable", TranslatableContents.MAP_CODEC);
        mapper.put("keybind", KeybindContents.MAP_CODEC);
        mapper.put("score", ScoreContents.MAP_CODEC);
        mapper.put("selector", SelectorContents.MAP_CODEC);
        mapper.put("nbt", NbtContents.MAP_CODEC);
        mapper.put("object", ObjectContents.MAP_CODEC);
        mapper.put("neoforge:inserting", net.neoforged.neoforge.common.util.InsertingContents.MAP_CODEC);
    }

    static class FuzzyCodec<T> extends MapCodec<T> {
        private final Collection<MapCodec<? extends T>> codecs;
        private final Function<T, ? extends MapEncoder<? extends T>> encoderGetter;

        public FuzzyCodec(Collection<MapCodec<? extends T>> codecs, Function<T, ? extends MapEncoder<? extends T>> encoderGetter) {
            this.codecs = codecs;
            this.encoderGetter = encoderGetter;
        }

        @Override
        public <S> DataResult<T> decode(DynamicOps<S> ops, MapLike<S> input) {
            for (MapDecoder<? extends T> mapdecoder : this.codecs) {
                DataResult<? extends T> dataresult = mapdecoder.decode(ops, input);
                if (dataresult.result().isPresent()) {
                    return (DataResult<T>)dataresult;
                }
            }

            return DataResult.error(() -> "No matching codec found");
        }

        @Override
        public <S> RecordBuilder<S> encode(T input, DynamicOps<S> ops, RecordBuilder<S> prefix) {
            MapEncoder<T> mapencoder = (MapEncoder<T>)this.encoderGetter.apply(input);
            return mapencoder.encode(input, ops, prefix);
        }

        @Override
        public <S> Stream<S> keys(DynamicOps<S> ops) {
            return this.codecs.stream().flatMap(p_304401_ -> p_304401_.keys(ops)).distinct();
        }

        @Override
        public String toString() {
            return "FuzzyCodec[" + this.codecs + "]";
        }
    }

    static class StrictEither<T> extends MapCodec<T> {
        private final String typeFieldName;
        private final MapCodec<T> typed;
        private final MapCodec<T> fuzzy;

        public StrictEither(String typeFieldName, MapCodec<T> typed, MapCodec<T> fuzzy) {
            this.typeFieldName = typeFieldName;
            this.typed = typed;
            this.fuzzy = fuzzy;
        }

        @Override
        public <O> DataResult<T> decode(DynamicOps<O> ops, MapLike<O> input) {
            return input.get(this.typeFieldName) != null ? this.typed.decode(ops, input) : this.fuzzy.decode(ops, input);
        }

        @Override
        public <O> RecordBuilder<O> encode(T input, DynamicOps<O> ops, RecordBuilder<O> prefix) {
            return this.fuzzy.encode(input, ops, prefix);
        }

        @Override
        public <T1> Stream<T1> keys(DynamicOps<T1> ops) {
            return Stream.concat(this.typed.keys(ops), this.fuzzy.keys(ops)).distinct();
        }
    }
}
