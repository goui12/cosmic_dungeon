package net.minecraft.util.valueproviders;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;

public class WeightedListInt extends IntProvider {
    public static final MapCodec<WeightedListInt> CODEC = RecordCodecBuilder.mapCodec(
        p_392953_ -> p_392953_.group(WeightedList.nonEmptyCodec(IntProvider.CODEC).fieldOf("distribution").forGetter(p_392952_ -> p_392952_.distribution))
            .apply(p_392953_, WeightedListInt::new)
    );
    private final WeightedList<IntProvider> distribution;
    private final int minValue;
    private final int maxValue;

    public WeightedListInt(WeightedList<IntProvider> distribution) {
        this.distribution = distribution;
        int i = Integer.MAX_VALUE;
        int j = Integer.MIN_VALUE;

        for (Weighted<IntProvider> weighted : distribution.unwrap()) {
            int k = weighted.value().getMinValue();
            int l = weighted.value().getMaxValue();
            i = Math.min(i, k);
            j = Math.max(j, l);
        }

        this.minValue = i;
        this.maxValue = j;
    }

    @Override
    public int sample(RandomSource random) {
        return this.distribution.getRandomOrThrow(random).sample(random);
    }

    @Override
    public int getMinValue() {
        return this.minValue;
    }

    @Override
    public int getMaxValue() {
        return this.maxValue;
    }

    @Override
    public IntProviderType<?> getType() {
        return IntProviderType.WEIGHTED_LIST;
    }
}
