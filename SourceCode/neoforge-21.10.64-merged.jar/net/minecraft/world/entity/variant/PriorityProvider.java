package net.minecraft.world.entity.variant;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.util.RandomSource;

public interface PriorityProvider<Context, Condition extends PriorityProvider.SelectorCondition<Context>> {
    List<PriorityProvider.Selector<Context, Condition>> selectors();

    static <C, T> Stream<T> select(Stream<T> elements, Function<T, PriorityProvider<C, ?>> entryGetter, C context) {
        List<PriorityProvider.UnpackedEntry<C, T>> list = new ArrayList<>();
        elements.forEach(
            p_399951_ -> {
                PriorityProvider<C, ?> priorityprovider = entryGetter.apply((T)p_399951_);

                for (PriorityProvider.Selector<C, ?> selector : priorityprovider.selectors()) {
                    list.add(
                        new PriorityProvider.UnpackedEntry<>(
                            (T)p_399951_,
                            selector.priority(),
                            DataFixUtils.orElseGet(
                                (Optional<? extends PriorityProvider.SelectorCondition<C>>)selector.condition(), PriorityProvider.SelectorCondition::alwaysTrue
                            )
                        )
                    );
                }
            }
        );
        list.sort(PriorityProvider.UnpackedEntry.HIGHEST_PRIORITY_FIRST);
        Iterator<PriorityProvider.UnpackedEntry<C, T>> iterator = list.iterator();
        int i = Integer.MIN_VALUE;

        while (iterator.hasNext()) {
            PriorityProvider.UnpackedEntry<C, T> unpackedentry = iterator.next();
            if (unpackedentry.priority < i) {
                iterator.remove();
            } else if (unpackedentry.condition.test(context)) {
                i = unpackedentry.priority;
            } else {
                iterator.remove();
            }
        }

        return list.stream().map(PriorityProvider.UnpackedEntry::entry);
    }

    static <C, T> Optional<T> pick(Stream<T> elements, Function<T, PriorityProvider<C, ?>> entryGetter, RandomSource random, C context) {
        List<T> list = select(elements, entryGetter, context).toList();
        return Util.getRandomSafe(list, random);
    }

    static <Context, Condition extends PriorityProvider.SelectorCondition<Context>> List<PriorityProvider.Selector<Context, Condition>> single(
        Condition condition, int priority
    ) {
        return List.of(new PriorityProvider.Selector<>(condition, priority));
    }

    static <Context, Condition extends PriorityProvider.SelectorCondition<Context>> List<PriorityProvider.Selector<Context, Condition>> alwaysTrue(
        int priority
    ) {
        return List.of(new PriorityProvider.Selector<>(Optional.empty(), priority));
    }

    public record Selector<Context, Condition extends PriorityProvider.SelectorCondition<Context>>(Optional<Condition> condition, int priority) {
        public Selector(Condition p_400069_, int p_400304_) {
            this(Optional.of(p_400069_), p_400304_);
        }

        public Selector(int p_399666_) {
            this(Optional.empty(), p_399666_);
        }

        public static <Context, Condition extends PriorityProvider.SelectorCondition<Context>> Codec<PriorityProvider.Selector<Context, Condition>> codec(
            Codec<Condition> conditionCodec
        ) {
            return RecordCodecBuilder.create(
                p_399853_ -> p_399853_.group(
                        conditionCodec.optionalFieldOf("condition").forGetter(PriorityProvider.Selector::condition),
                        Codec.INT.fieldOf("priority").forGetter(PriorityProvider.Selector::priority)
                    )
                    .apply(p_399853_, PriorityProvider.Selector::new)
            );
        }
    }

    @FunctionalInterface
    public interface SelectorCondition<C> extends Predicate<C> {
        static <C> PriorityProvider.SelectorCondition<C> alwaysTrue() {
            return p_400277_ -> true;
        }
    }

    public record UnpackedEntry<C, T>(T entry, int priority, PriorityProvider.SelectorCondition<C> condition) {
        public static final Comparator<PriorityProvider.UnpackedEntry<?, ?>> HIGHEST_PRIORITY_FIRST = Comparator.<PriorityProvider.UnpackedEntry<?, ?>>comparingInt(
                PriorityProvider.UnpackedEntry::priority
            )
            .reversed();
    }
}
