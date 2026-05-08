package net.minecraft.client.renderer.block.model.multipart;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.Util;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record CombinedCondition(CombinedCondition.Operation operation, List<Condition> terms) implements Condition {
    @Override
    public <O, S extends StateHolder<O, S>> Predicate<S> instantiate(StateDefinition<O, S> stateDefinition) {
        return this.operation.apply(Lists.transform(this.terms, p_405506_ -> p_405506_.instantiate(stateDefinition)));
    }

    @OnlyIn(Dist.CLIENT)
    public static enum Operation implements StringRepresentable {
        AND("AND") {
            @Override
            public <V> Predicate<V> apply(List<Predicate<V>> p_405733_) {
                return Util.allOf(p_405733_);
            }
        },
        OR("OR") {
            @Override
            public <V> Predicate<V> apply(List<Predicate<V>> p_404677_) {
                return Util.anyOf(p_404677_);
            }
        };

        public static final Codec<CombinedCondition.Operation> CODEC = StringRepresentable.fromEnum(CombinedCondition.Operation::values);
        private final String name;

        Operation(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public abstract <V> Predicate<V> apply(List<Predicate<V>> conditions);
    }
}
