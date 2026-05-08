package net.minecraft.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;

public interface StringRepresentable {
    int PRE_BUILT_MAP_THRESHOLD = 16;

    String getSerializedName();

    static <E extends Enum<E> & StringRepresentable> StringRepresentable.EnumCodec<E> fromEnum(Supplier<E[]> elementsSupplier) {
        return fromEnumWithMapping(elementsSupplier, p_304817_ -> p_304817_);
    }

    static <E extends Enum<E> & StringRepresentable> StringRepresentable.EnumCodec<E> fromEnumWithMapping(
        Supplier<E[]> enumValues, Function<String, String> keyFunction
    ) {
        E[] ae = (E[])enumValues.get();
        Function<String, E> function = createNameLookup(ae, p_448911_ -> keyFunction.apply(p_448911_.getSerializedName()));
        return new StringRepresentable.EnumCodec<>(ae, function);
    }

    static <T extends StringRepresentable> Codec<T> fromValues(Supplier<T[]> valuesSupplier) {
        T[] at = (T[])valuesSupplier.get();
        Function<String, T> function = createNameLookup(at);
        ToIntFunction<T> tointfunction = Util.createIndexLookup(Arrays.asList(at));
        return new StringRepresentable.StringRepresentableCodec<>(at, function, tointfunction);
    }

    static <T extends StringRepresentable> Function<String, T> createNameLookup(T[] values) {
        return createNameLookup(values, StringRepresentable::getSerializedName);
    }

    static <T> Function<String, T> createNameLookup(T[] values, Function<T, String> serializer) {
        if (values.length > 16) {
            Map<String, T> map = Arrays.<T>stream(values).collect(Collectors.toMap(serializer, p_449196_ -> (T)p_449196_));
            return p_448913_ -> p_448913_ == null ? null : map.get(p_448913_);
        } else {
            return p_448916_ -> {
                for (T t : values) {
                    if (serializer.apply(t).equals(p_448916_)) {
                        return t;
                    }
                }

                return null;
            };
        }
    }

    static Keyable keys(final StringRepresentable[] serializables) {
        return new Keyable() {
            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return Arrays.stream(serializables).map(StringRepresentable::getSerializedName).map(ops::createString);
            }
        };
    }

    public static class EnumCodec<E extends Enum<E> & StringRepresentable> extends StringRepresentable.StringRepresentableCodec<E> {
        private final Function<String, E> resolver;

        public EnumCodec(E[] values, Function<String, E> resolver) {
            super(values, resolver, p_216454_ -> p_216454_.ordinal());
            this.resolver = resolver;
        }

        @Nullable
        public E byName(@Nullable String name) {
            return this.resolver.apply(name);
        }

        public E byName(@Nullable String name, E defaultValue) {
            return Objects.requireNonNullElse(this.byName(name), defaultValue);
        }

        public E byName(@Nullable String name, Supplier<? extends E> defaultValue) {
            return Objects.requireNonNullElseGet(this.byName(name), defaultValue);
        }
    }

    public static class StringRepresentableCodec<S extends StringRepresentable> implements Codec<S> {
        private final Codec<S> codec;

        public StringRepresentableCodec(S[] values, Function<String, S> nameLookup, ToIntFunction<S> indexLookup) {
            this.codec = ExtraCodecs.orCompressed(
                Codec.stringResolver(StringRepresentable::getSerializedName, nameLookup),
                ExtraCodecs.idResolverCodec(indexLookup, p_304986_ -> p_304986_ >= 0 && p_304986_ < values.length ? values[p_304986_] : null, -1)
            );
        }

        @Override
        public <T> DataResult<Pair<S, T>> decode(DynamicOps<T> ops, T value) {
            return this.codec.decode(ops, value);
        }

        public <T> DataResult<T> encode(S input, DynamicOps<T> ops, T prefix) {
            return this.codec.encode(input, ops, prefix);
        }
    }
}
