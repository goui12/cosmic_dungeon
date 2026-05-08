package net.minecraft.core.component;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public final class DataComponentExactPredicate implements Predicate<DataComponentGetter> {
    public static final Codec<DataComponentExactPredicate> CODEC = DataComponentType.VALUE_MAP_CODEC
        .xmap(
            p_399796_ -> new DataComponentExactPredicate(p_399796_.entrySet().stream().map(TypedDataComponent::fromEntryUnchecked).collect(Collectors.toList())),
            p_399915_ -> p_399915_.expectedComponents
                .stream()
                .filter(p_399578_ -> !p_399578_.type().isTransient())
                .collect(Collectors.toMap(TypedDataComponent::type, TypedDataComponent::value))
        );
    public static final StreamCodec<RegistryFriendlyByteBuf, DataComponentExactPredicate> STREAM_CODEC = TypedDataComponent.STREAM_CODEC
        .apply(ByteBufCodecs.list())
        .map(DataComponentExactPredicate::new, p_399906_ -> p_399906_.expectedComponents);
    public static final DataComponentExactPredicate EMPTY = new DataComponentExactPredicate(List.of());
    private final List<TypedDataComponent<?>> expectedComponents;

    DataComponentExactPredicate(List<TypedDataComponent<?>> expectedComponents) {
        this.expectedComponents = expectedComponents;
    }

    public static DataComponentExactPredicate.Builder builder() {
        return new DataComponentExactPredicate.Builder();
    }

    public static <T> DataComponentExactPredicate expect(DataComponentType<T> component, T value) {
        return new DataComponentExactPredicate(List.of(new TypedDataComponent<>(component, value)));
    }

    public static DataComponentExactPredicate allOf(DataComponentMap map) {
        return new DataComponentExactPredicate(ImmutableList.copyOf(map));
    }

    public static DataComponentExactPredicate someOf(DataComponentMap map, DataComponentType<?>... types) {
        DataComponentExactPredicate.Builder datacomponentexactpredicate$builder = new DataComponentExactPredicate.Builder();

        for (DataComponentType<?> datacomponenttype : types) {
            TypedDataComponent<?> typeddatacomponent = map.getTyped(datacomponenttype);
            if (typeddatacomponent != null) {
                datacomponentexactpredicate$builder.expect(typeddatacomponent);
            }
        }

        return datacomponentexactpredicate$builder.build();
    }

    public boolean isEmpty() {
        return this.expectedComponents.isEmpty();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof DataComponentExactPredicate datacomponentexactpredicate
            && this.expectedComponents.equals(datacomponentexactpredicate.expectedComponents);
    }

    @Override
    public int hashCode() {
        return this.expectedComponents.hashCode();
    }

    @Override
    public String toString() {
        return this.expectedComponents.toString();
    }

    public boolean test(DataComponentGetter componentGetter) {
        for (TypedDataComponent<?> typeddatacomponent : this.expectedComponents) {
            Object object = componentGetter.get(typeddatacomponent.type());
            if (!Objects.equals(typeddatacomponent.value(), object)) {
                return false;
            }
        }

        return true;
    }

    public boolean alwaysMatches() {
        return this.expectedComponents.isEmpty();
    }

    public DataComponentPatch asPatch() {
        DataComponentPatch.Builder datacomponentpatch$builder = DataComponentPatch.builder();

        for (TypedDataComponent<?> typeddatacomponent : this.expectedComponents) {
            datacomponentpatch$builder.set(typeddatacomponent);
        }

        return datacomponentpatch$builder.build();
    }

    public static class Builder {
        private final List<TypedDataComponent<?>> expectedComponents = new ArrayList<>();

        Builder() {
        }

        public <T> DataComponentExactPredicate.Builder expect(TypedDataComponent<T> component) {
            return this.expect(component.type(), component.value());
        }

        public <T> DataComponentExactPredicate.Builder expect(DataComponentType<? super T> component, T value) {
            for (TypedDataComponent<?> typeddatacomponent : this.expectedComponents) {
                if (typeddatacomponent.type() == component) {
                    throw new IllegalArgumentException("Predicate already has component of type: '" + component + "'");
                }
            }

            this.expectedComponents.add(new TypedDataComponent<>(component, value));
            return this;
        }

        public DataComponentExactPredicate build() {
            return new DataComponentExactPredicate(List.copyOf(this.expectedComponents));
        }
    }
}
