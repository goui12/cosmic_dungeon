package net.minecraft.world.item.enchantment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public record ConditionalEffect<T>(T effect, Optional<LootItemCondition> requirements) {
    public static Codec<LootItemCondition> conditionCodec(ContextKeySet contextKeySet) {
        return LootItemCondition.DIRECT_CODEC
            .validate(
                p_421408_ -> {
                    ProblemReporter.Collector problemreporter$collector = new ProblemReporter.Collector();
                    ValidationContext validationcontext = new ValidationContext(problemreporter$collector, contextKeySet);
                    p_421408_.validate(validationcontext);
                    return !problemreporter$collector.isEmpty()
                        ? DataResult.error(() -> "Validation error in enchantment effect condition: " + problemreporter$collector.getReport())
                        : DataResult.success(p_421408_);
                }
            );
    }

    public static <T> Codec<ConditionalEffect<T>> codec(Codec<T> codec, ContextKeySet contextKeySet) {
        return RecordCodecBuilder.create(
            p_380864_ -> p_380864_.group(
                    codec.fieldOf("effect").forGetter(ConditionalEffect::effect),
                    conditionCodec(contextKeySet).optionalFieldOf("requirements").forGetter(ConditionalEffect::requirements)
                )
                .apply(p_380864_, ConditionalEffect::new)
        );
    }

    public boolean matches(LootContext context) {
        return this.requirements.isEmpty() ? true : this.requirements.get().test(context);
    }
}
