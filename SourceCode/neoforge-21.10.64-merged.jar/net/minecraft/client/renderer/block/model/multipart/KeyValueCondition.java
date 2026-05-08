package net.minecraft.client.renderer.block.model.multipart;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.Util;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public record KeyValueCondition(Map<String, KeyValueCondition.Terms> tests) implements Condition {
    static final Logger LOGGER = LogUtils.getLogger();
    public static final Codec<KeyValueCondition> CODEC = ExtraCodecs.nonEmptyMap(Codec.unboundedMap(Codec.STRING, KeyValueCondition.Terms.CODEC))
        .xmap(KeyValueCondition::new, KeyValueCondition::tests);

    @Override
    public <O, S extends StateHolder<O, S>> Predicate<S> instantiate(StateDefinition<O, S> stateDefinition) {
        List<Predicate<S>> list = new ArrayList<>(this.tests.size());
        this.tests.forEach((p_404083_, p_404084_) -> list.add(instantiate(stateDefinition, p_404083_, p_404084_)));
        return Util.allOf(list);
    }

    private static <O, S extends StateHolder<O, S>> Predicate<S> instantiate(
        StateDefinition<O, S> stateDefinition, String p_property, KeyValueCondition.Terms terms
    ) {
        Property<?> property = stateDefinition.getProperty(p_property);
        if (property == null) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "Unknown property '%s' on '%s'", p_property, stateDefinition.getOwner()));
        } else {
            return terms.instantiate(stateDefinition.getOwner(), property);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record Term(String value, boolean negated) {
        private static final String NEGATE = "!";

        public Term(String value, boolean negated) {
            if (value.isEmpty()) {
                throw new IllegalArgumentException("Empty term");
            } else {
                this.value = value;
                this.negated = negated;
            }
        }

        public static KeyValueCondition.Term parse(String text) {
            return text.startsWith("!") ? new KeyValueCondition.Term(text.substring(1), true) : new KeyValueCondition.Term(text, false);
        }

        @Override
        public String toString() {
            return this.negated ? "!" + this.value : this.value;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record Terms(List<KeyValueCondition.Term> entries) {
        private static final char SEPARATOR = '|';
        private static final Joiner JOINER = Joiner.on('|');
        private static final Splitter SPLITTER = Splitter.on('|');
        private static final Codec<String> LEGACY_REPRESENTATION_CODEC = Codec.either(Codec.INT, Codec.BOOL)
            .flatComapMap(
                p_405478_ -> p_405478_.map(String::valueOf, String::valueOf), p_405596_ -> DataResult.error(() -> "This codec can't be used for encoding")
            );
        public static final Codec<KeyValueCondition.Terms> CODEC = Codec.withAlternative(Codec.STRING, LEGACY_REPRESENTATION_CODEC)
            .comapFlatMap(KeyValueCondition.Terms::parse, KeyValueCondition.Terms::toString);

        public Terms(List<KeyValueCondition.Term> entries) {
            if (entries.isEmpty()) {
                throw new IllegalArgumentException("Empty value for property");
            } else {
                this.entries = entries;
            }
        }

        public static DataResult<KeyValueCondition.Terms> parse(String text) {
            List<KeyValueCondition.Term> list = SPLITTER.splitToStream(text).map(KeyValueCondition.Term::parse).toList();
            if (list.isEmpty()) {
                return DataResult.error(() -> "Empty value for property");
            } else {
                for (KeyValueCondition.Term keyvaluecondition$term : list) {
                    if (keyvaluecondition$term.value.isEmpty()) {
                        return DataResult.error(() -> "Empty term in value '" + text + "'");
                    }
                }

                return DataResult.success(new KeyValueCondition.Terms(list));
            }
        }

        @Override
        public String toString() {
            return JOINER.join(this.entries);
        }

        public <O, S extends StateHolder<O, S>, T extends Comparable<T>> Predicate<S> instantiate(O owner, Property<T> property) {
            Predicate<T> predicate = Util.anyOf(Lists.transform(this.entries, p_405722_ -> this.instantiate(owner, property, p_405722_)));
            List<T> list = new ArrayList<>(property.getPossibleValues());
            int i = list.size();
            list.removeIf(predicate.negate());
            int j = list.size();
            if (j == 0) {
                KeyValueCondition.LOGGER.warn("Condition {} for property {} on {} is always false", this, property.getName(), owner);
                return p_405630_ -> false;
            } else {
                int k = i - j;
                if (k == 0) {
                    KeyValueCondition.LOGGER.warn("Condition {} for property {} on {} is always true", this, property.getName(), owner);
                    return p_404757_ -> true;
                } else {
                    boolean flag;
                    List<T> list1;
                    if (j <= k) {
                        flag = false;
                        list1 = list;
                    } else {
                        flag = true;
                        List<T> list2 = new ArrayList<>(property.getPossibleValues());
                        list2.removeIf(predicate);
                        list1 = list2;
                    }

                    if (list1.size() == 1) {
                        T t = (T)list1.getFirst();
                        return p_404713_ -> {
                            T t1 = p_404713_.getValue(property);
                            return t.equals(t1) ^ flag;
                        };
                    } else {
                        return p_405532_ -> {
                            T t1 = p_405532_.getValue(property);
                            return list1.contains(t1) ^ flag;
                        };
                    }
                }
            }
        }

        private <T extends Comparable<T>> T getValueOrThrow(Object owner, Property<T> property, String value) {
            Optional<T> optional = property.getValue(value);
            if (optional.isEmpty()) {
                throw new RuntimeException(
                    String.format(Locale.ROOT, "Unknown value '%s' for property '%s' on '%s' in '%s'", value, property, owner, this)
                );
            } else {
                return optional.get();
            }
        }

        private <T extends Comparable<T>> Predicate<T> instantiate(Object owner, Property<T> property, KeyValueCondition.Term term) {
            T t = this.getValueOrThrow(owner, property, term.value);
            return term.negated ? p_405370_ -> !p_405370_.equals(t) : p_404808_ -> p_404808_.equals(t);
        }
    }
}
