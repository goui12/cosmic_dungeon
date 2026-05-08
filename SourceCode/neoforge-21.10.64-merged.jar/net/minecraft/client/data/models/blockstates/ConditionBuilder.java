package net.minecraft.client.data.models.blockstates;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.renderer.block.model.multipart.Condition;
import net.minecraft.client.renderer.block.model.multipart.KeyValueCondition;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ConditionBuilder {
    private final Builder<String, KeyValueCondition.Terms> terms = ImmutableMap.builder();

    private <T extends Comparable<T>> void putValue(Property<T> property, KeyValueCondition.Terms terms) {
        this.terms.put(property.getName(), terms);
    }

    public final <T extends Comparable<T>> ConditionBuilder term(Property<T> property, T value) {
        this.putValue(property, new KeyValueCondition.Terms(List.of(new KeyValueCondition.Term(property.getName(value), false))));
        return this;
    }

    @SafeVarargs
    public final <T extends Comparable<T>> ConditionBuilder term(Property<T> property, T value, T... otherValues) {
        List<KeyValueCondition.Term> list = Stream.concat(Stream.of(value), Stream.of(otherValues))
            .map(property::getName)
            .sorted()
            .distinct()
            .map(p_405408_ -> new KeyValueCondition.Term(p_405408_, false))
            .toList();
        this.putValue(property, new KeyValueCondition.Terms(list));
        return this;
    }

    public final <T extends Comparable<T>> ConditionBuilder negatedTerm(Property<T> property, T value) {
        this.putValue(property, new KeyValueCondition.Terms(List.of(new KeyValueCondition.Term(property.getName(value), true))));
        return this;
    }

    public Condition build() {
        return new KeyValueCondition(this.terms.buildOrThrow());
    }
}
