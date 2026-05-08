package net.minecraft.world.level.storage.loot.providers.number;

import com.google.common.collect.Sets;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Set;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.storage.loot.LootContext;

/**
 * A number provider which generates a random number based on a binomial distribution.
 */
public record BinomialDistributionGenerator(NumberProvider n, NumberProvider p) implements NumberProvider {
    public static final MapCodec<BinomialDistributionGenerator> CODEC = RecordCodecBuilder.mapCodec(
        p_299136_ -> p_299136_.group(
                NumberProviders.CODEC.fieldOf("n").forGetter(BinomialDistributionGenerator::n),
                NumberProviders.CODEC.fieldOf("p").forGetter(BinomialDistributionGenerator::p)
            )
            .apply(p_299136_, BinomialDistributionGenerator::new)
    );

    @Override
    public LootNumberProviderType getType() {
        return NumberProviders.BINOMIAL;
    }

    @Override
    public int getInt(LootContext lootContext) {
        int i = this.n.getInt(lootContext);
        float f = this.p.getFloat(lootContext);
        RandomSource randomsource = lootContext.getRandom();
        int j = 0;

        for (int k = 0; k < i; k++) {
            if (randomsource.nextFloat() < f) {
                j++;
            }
        }

        return j;
    }

    @Override
    public float getFloat(LootContext lootContext) {
        return this.getInt(lootContext);
    }

    public static BinomialDistributionGenerator binomial(int n, float p) {
        return new BinomialDistributionGenerator(ConstantValue.exactly(n), ConstantValue.exactly(p));
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Sets.union(this.n.getReferencedContextParams(), this.p.getReferencedContextParams());
    }
}
