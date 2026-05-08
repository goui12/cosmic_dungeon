package net.minecraft.util.random;

import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;
import net.minecraft.Util;
import net.minecraft.util.RandomSource;

public class WeightedRandom {
    private WeightedRandom() {
    }

    public static <T> int getTotalWeight(List<T> elements, ToIntFunction<T> weightGetter) {
        long i = 0L;

        for (T t : elements) {
            i += weightGetter.applyAsInt(t);
        }

        if (i > 2147483647L) {
            throw new IllegalArgumentException("Sum of weights must be <= 2147483647");
        } else {
            return (int)i;
        }
    }

    public static <T> Optional<T> getRandomItem(RandomSource random, List<T> elements, int totalWeight, ToIntFunction<T> weightGetter) {
        if (totalWeight < 0) {
            throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException("Negative total weight in getRandomItem"));
        } else if (totalWeight == 0) {
            return Optional.empty();
        } else {
            int i = random.nextInt(totalWeight);
            return getWeightedItem(elements, i, weightGetter);
        }
    }

    public static <T> Optional<T> getWeightedItem(List<T> elements, int index, ToIntFunction<T> weightGetter) {
        for (T t : elements) {
            index -= weightGetter.applyAsInt(t);
            if (index < 0) {
                return Optional.of(t);
            }
        }

        return Optional.empty();
    }

    public static <T> Optional<T> getRandomItem(RandomSource random, List<T> elements, ToIntFunction<T> weightGetter) {
        return getRandomItem(random, elements, getTotalWeight(elements, weightGetter), weightGetter);
    }
}
