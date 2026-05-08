package net.minecraft.client.renderer.block.model.multipart;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@FunctionalInterface
@OnlyIn(Dist.CLIENT)
public interface Condition {
    Codec<Condition> CODEC = Codec.recursive(
        "condition",
        p_404078_ -> {
            Codec<CombinedCondition> codec = Codec.simpleMap(
                    CombinedCondition.Operation.CODEC, p_404078_.listOf(), StringRepresentable.keys(CombinedCondition.Operation.values())
                )
                .codec()
                .comapFlatMap(p_404075_ -> {
                    if (p_404075_.size() != 1) {
                        return DataResult.error(() -> "Invalid map size for combiner condition, expected exactly one element");
                    } else {
                        Entry<CombinedCondition.Operation, List<Condition>> entry = p_404075_.entrySet().iterator().next();
                        return DataResult.success(new CombinedCondition(entry.getKey(), entry.getValue()));
                    }
                }, p_404079_ -> Map.of(p_404079_.operation(), p_404079_.terms()));
            return Codec.either(codec, KeyValueCondition.CODEC)
                .flatComapMap(p_404077_ -> p_404077_.map(p_404074_ -> p_404074_, p_404080_ -> p_404080_), p_404076_ -> {
                    return switch (p_404076_) {
                        case CombinedCondition combinedcondition -> DataResult.success(Either.left(combinedcondition));
                        case KeyValueCondition keyvaluecondition -> DataResult.success(Either.right(keyvaluecondition));
                        default -> DataResult.error(() -> "Unrecognized condition");
                    };
                });
        }
    );

    <O, S extends StateHolder<O, S>> Predicate<S> instantiate(StateDefinition<O, S> stateDefinition);
}
