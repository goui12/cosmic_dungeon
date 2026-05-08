package net.minecraft.advancements.critereon;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record DataComponentMatchers(DataComponentExactPredicate exact, Map<DataComponentPredicate.Type<?>, DataComponentPredicate> partial)
    implements Predicate<DataComponentGetter> {
    public static final DataComponentMatchers ANY = new DataComponentMatchers(DataComponentExactPredicate.EMPTY, Map.of());
    public static final MapCodec<DataComponentMatchers> CODEC = RecordCodecBuilder.mapCodec(
        p_411060_ -> p_411060_.group(
                DataComponentExactPredicate.CODEC.optionalFieldOf("components", DataComponentExactPredicate.EMPTY).forGetter(DataComponentMatchers::exact),
                DataComponentPredicate.CODEC.optionalFieldOf("predicates", Map.of()).forGetter(DataComponentMatchers::partial)
            )
            .apply(p_411060_, DataComponentMatchers::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, DataComponentMatchers> STREAM_CODEC = StreamCodec.composite(
        DataComponentExactPredicate.STREAM_CODEC,
        DataComponentMatchers::exact,
        DataComponentPredicate.STREAM_CODEC,
        DataComponentMatchers::partial,
        DataComponentMatchers::new
    );

    public boolean test(DataComponentGetter componentGetter) {
        if (!this.exact.test(componentGetter)) {
            return false;
        } else {
            for (DataComponentPredicate datacomponentpredicate : this.partial.values()) {
                if (!datacomponentpredicate.matches(componentGetter)) {
                    return false;
                }
            }

            return true;
        }
    }

    public boolean isEmpty() {
        return this.exact.isEmpty() && this.partial.isEmpty();
    }

    public static class Builder {
        private DataComponentExactPredicate exact = DataComponentExactPredicate.EMPTY;
        private final ImmutableMap.Builder<DataComponentPredicate.Type<?>, DataComponentPredicate> partial = ImmutableMap.builder();

        private Builder() {
        }

        public static DataComponentMatchers.Builder components() {
            return new DataComponentMatchers.Builder();
        }

        public <T extends DataComponentPredicate> DataComponentMatchers.Builder partial(DataComponentPredicate.Type<T> type, T value) {
            this.partial.put(type, value);
            return this;
        }

        public DataComponentMatchers.Builder exact(DataComponentExactPredicate exact) {
            this.exact = exact;
            return this;
        }

        public DataComponentMatchers build() {
            return new DataComponentMatchers(this.exact, this.partial.buildOrThrow());
        }
    }
}
