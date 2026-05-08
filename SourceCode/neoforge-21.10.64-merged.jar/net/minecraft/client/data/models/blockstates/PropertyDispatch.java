package net.minecraft.client.data.models.blockstates;

import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function5;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.renderer.block.model.VariantMutator;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class PropertyDispatch<V> {
    private final Map<PropertyValueList, V> values = new HashMap<>();

    protected void putValue(PropertyValueList properties, V value) {
        V v = this.values.put(properties, value);
        if (v != null) {
            throw new IllegalStateException("Value " + properties + " is already defined");
        }
    }

    Map<PropertyValueList, V> getEntries() {
        this.verifyComplete();
        return Map.copyOf(this.values);
    }

    private void verifyComplete() {
        List<Property<?>> list = this.getDefinedProperties();
        Stream<PropertyValueList> stream = Stream.of(PropertyValueList.EMPTY);

        for (Property<?> property : list) {
            stream = stream.flatMap(p_405250_ -> property.getAllValues().map(p_405250_::extend));
        }

        List<PropertyValueList> list1 = stream.filter(p_405347_ -> !this.values.containsKey(p_405347_)).toList();
        if (!list1.isEmpty()) {
            throw new IllegalStateException("Missing definition for properties: " + list1);
        }
    }

    abstract List<Property<?>> getDefinedProperties();

    public static <T1 extends Comparable<T1>> PropertyDispatch.C1<MultiVariant, T1> initial(Property<T1> property) {
        return new PropertyDispatch.C1<>(property);
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>> PropertyDispatch.C2<MultiVariant, T1, T2> initial(
        Property<T1> property1, Property<T2> property2
    ) {
        return new PropertyDispatch.C2<>(property1, property2);
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>> PropertyDispatch.C3<MultiVariant, T1, T2, T3> initial(
        Property<T1> property1, Property<T2> property2, Property<T3> property3
    ) {
        return new PropertyDispatch.C3<>(property1, property2, property3);
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>> PropertyDispatch.C4<MultiVariant, T1, T2, T3, T4> initial(
        Property<T1> property1, Property<T2> property2, Property<T3> property3, Property<T4> property4
    ) {
        return new PropertyDispatch.C4<>(property1, property2, property3, property4);
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>> PropertyDispatch.C5<MultiVariant, T1, T2, T3, T4, T5> initial(
        Property<T1> property1, Property<T2> property2, Property<T3> property3, Property<T4> property4, Property<T5> property5
    ) {
        return new PropertyDispatch.C5<>(property1, property2, property3, property4, property5);
    }

    public static <T1 extends Comparable<T1>> PropertyDispatch.C1<VariantMutator, T1> modify(Property<T1> property) {
        return new PropertyDispatch.C1<>(property);
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>> PropertyDispatch.C2<VariantMutator, T1, T2> modify(
        Property<T1> property1, Property<T2> property2
    ) {
        return new PropertyDispatch.C2<>(property1, property2);
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>> PropertyDispatch.C3<VariantMutator, T1, T2, T3> modify(
        Property<T1> property1, Property<T2> property2, Property<T3> property3
    ) {
        return new PropertyDispatch.C3<>(property1, property2, property3);
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>> PropertyDispatch.C4<VariantMutator, T1, T2, T3, T4> modify(
        Property<T1> property1, Property<T2> property2, Property<T3> property3, Property<T4> property4
    ) {
        return new PropertyDispatch.C4<>(property1, property2, property3, property4);
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>> PropertyDispatch.C5<VariantMutator, T1, T2, T3, T4, T5> modify(
        Property<T1> property1, Property<T2> property2, Property<T3> property3, Property<T4> property4, Property<T5> property5
    ) {
        return new PropertyDispatch.C5<>(property1, property2, property3, property4, property5);
    }

    /**
     * Creates a property dispatch on a single blockstate property that can be used to apply a {@link net.neoforged.neoforge.client.model.generators.blockstate.UnbakedMutator}.
     */
    public static <T1 extends Comparable<T1>> PropertyDispatch.C1<net.neoforged.neoforge.client.model.generators.blockstate.UnbakedMutator, T1> modifyUnbaked(Property<T1> p1) {
        return new PropertyDispatch.C1<>(p1);
    }

    /**
     * Creates a property dispatch on two blockstate properties that can be used to apply a {@link net.neoforged.neoforge.client.model.generators.blockstate.UnbakedMutator}.
     */
    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>> PropertyDispatch.C2<net.neoforged.neoforge.client.model.generators.blockstate.UnbakedMutator, T1, T2> modifyUnbaked(
        Property<T1> p1, Property<T2> p2
    ) {
        return new PropertyDispatch.C2<>(p1, p2);
    }

    /**
     * Creates a property dispatch on three blockstate properties that can be used to apply a {@link net.neoforged.neoforge.client.model.generators.blockstate.UnbakedMutator}.
     */
    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>> PropertyDispatch.C3<net.neoforged.neoforge.client.model.generators.blockstate.UnbakedMutator, T1, T2, T3> modifyUnbaked(
        Property<T1> p1, Property<T2> p2, Property<T3> p3
    ) {
        return new PropertyDispatch.C3<>(p1, p2, p3);
    }

    /**
     * Creates a property dispatch on four blockstate properties that can be used to apply a {@link net.neoforged.neoforge.client.model.generators.blockstate.UnbakedMutator}.
     */
    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>> PropertyDispatch.C4<net.neoforged.neoforge.client.model.generators.blockstate.UnbakedMutator, T1, T2, T3, T4> modifyUnbaked(
        Property<T1> p1, Property<T2> p2, Property<T3> p3, Property<T4> p4
    ) {
        return new PropertyDispatch.C4<>(p1, p2, p3, p4);
    }

    /**
     * Creates a property dispatch on five blockstate properties that can be used to apply a {@link net.neoforged.neoforge.client.model.generators.blockstate.UnbakedMutator}.
     */
    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>> PropertyDispatch.C5<net.neoforged.neoforge.client.model.generators.blockstate.UnbakedMutator, T1, T2, T3, T4, T5> modifyUnbaked(
        Property<T1> p1, Property<T2> p2, Property<T3> p3, Property<T4> p4, Property<T5> p5
    ) {
        return new PropertyDispatch.C5<>(p1, p2, p3, p4, p5);
    }

    @OnlyIn(Dist.CLIENT)
    public static class C1<V, T1 extends Comparable<T1>> extends PropertyDispatch<V> {
        private final Property<T1> property1;

        C1(Property<T1> property1) {
            this.property1 = property1;
        }

        @Override
        public List<Property<?>> getDefinedProperties() {
            return List.of(this.property1);
        }

        public PropertyDispatch.C1<V, T1> select(T1 property, V value) {
            PropertyValueList propertyvaluelist = PropertyValueList.of(this.property1.value(property));
            this.putValue(propertyvaluelist, value);
            return this;
        }

        public PropertyDispatch<V> generate(Function<T1, V> generator) {
            this.property1.getPossibleValues().forEach(p_403957_ -> this.select((T1)p_403957_, generator.apply((T1)p_403957_)));
            return this;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class C2<V, T1 extends Comparable<T1>, T2 extends Comparable<T2>> extends PropertyDispatch<V> {
        private final Property<T1> property1;
        private final Property<T2> property2;

        C2(Property<T1> property1, Property<T2> property2) {
            this.property1 = property1;
            this.property2 = property2;
        }

        @Override
        public List<Property<?>> getDefinedProperties() {
            return List.of(this.property1, this.property2);
        }

        public PropertyDispatch.C2<V, T1, T2> select(T1 property1, T2 property2, V value) {
            PropertyValueList propertyvaluelist = PropertyValueList.of(this.property1.value(property1), this.property2.value(property2));
            this.putValue(propertyvaluelist, value);
            return this;
        }

        public PropertyDispatch<V> generate(BiFunction<T1, T2, V> generator) {
            this.property1
                .getPossibleValues()
                .forEach(
                    p_388697_ -> this.property2
                        .getPossibleValues()
                        .forEach(p_403960_ -> this.select((T1)p_388697_, (T2)p_403960_, generator.apply((T1)p_388697_, (T2)p_403960_)))
                );
            return this;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class C3<V, T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>> extends PropertyDispatch<V> {
        private final Property<T1> property1;
        private final Property<T2> property2;
        private final Property<T3> property3;

        C3(Property<T1> property1, Property<T2> property2, Property<T3> property3) {
            this.property1 = property1;
            this.property2 = property2;
            this.property3 = property3;
        }

        @Override
        public List<Property<?>> getDefinedProperties() {
            return List.of(this.property1, this.property2, this.property3);
        }

        public PropertyDispatch.C3<V, T1, T2, T3> select(T1 property1, T2 property2, T3 property3, V value) {
            PropertyValueList propertyvaluelist = PropertyValueList.of(
                this.property1.value(property1), this.property2.value(property2), this.property3.value(property3)
            );
            this.putValue(propertyvaluelist, value);
            return this;
        }

        public PropertyDispatch<V> generate(Function3<T1, T2, T3, V> generator) {
            this.property1
                .getPossibleValues()
                .forEach(
                    p_388004_ -> this.property2
                        .getPossibleValues()
                        .forEach(
                            p_387043_ -> this.property3
                                .getPossibleValues()
                                .forEach(
                                    p_403964_ -> this.select(
                                        (T1)p_388004_, (T2)p_387043_, (T3)p_403964_, generator.apply((T1)p_388004_, (T2)p_387043_, (T3)p_403964_)
                                    )
                                )
                        )
                );
            return this;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class C4<V, T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>>
        extends PropertyDispatch<V> {
        private final Property<T1> property1;
        private final Property<T2> property2;
        private final Property<T3> property3;
        private final Property<T4> property4;

        C4(Property<T1> property1, Property<T2> property2, Property<T3> property3, Property<T4> property4) {
            this.property1 = property1;
            this.property2 = property2;
            this.property3 = property3;
            this.property4 = property4;
        }

        @Override
        public List<Property<?>> getDefinedProperties() {
            return List.of(this.property1, this.property2, this.property3, this.property4);
        }

        public PropertyDispatch.C4<V, T1, T2, T3, T4> select(T1 property1, T2 property2, T3 property3, T4 property4, V value) {
            PropertyValueList propertyvaluelist = PropertyValueList.of(
                this.property1.value(property1), this.property2.value(property2), this.property3.value(property3), this.property4.value(property4)
            );
            this.putValue(propertyvaluelist, value);
            return this;
        }

        public PropertyDispatch<V> generate(Function4<T1, T2, T3, T4, V> generator) {
            this.property1
                .getPossibleValues()
                .forEach(
                    p_386818_ -> this.property2
                        .getPossibleValues()
                        .forEach(
                            p_388830_ -> this.property3
                                .getPossibleValues()
                                .forEach(
                                    p_387951_ -> this.property4
                                        .getPossibleValues()
                                        .forEach(
                                            p_403969_ -> this.select(
                                                (T1)p_386818_,
                                                (T2)p_388830_,
                                                (T3)p_387951_,
                                                (T4)p_403969_,
                                                generator.apply((T1)p_386818_, (T2)p_388830_, (T3)p_387951_, (T4)p_403969_)
                                            )
                                        )
                                )
                        )
                );
            return this;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class C5<V, T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>>
        extends PropertyDispatch<V> {
        private final Property<T1> property1;
        private final Property<T2> property2;
        private final Property<T3> property3;
        private final Property<T4> property4;
        private final Property<T5> property5;

        C5(Property<T1> property1, Property<T2> property2, Property<T3> property3, Property<T4> property4, Property<T5> property5) {
            this.property1 = property1;
            this.property2 = property2;
            this.property3 = property3;
            this.property4 = property4;
            this.property5 = property5;
        }

        @Override
        public List<Property<?>> getDefinedProperties() {
            return List.of(this.property1, this.property2, this.property3, this.property4, this.property5);
        }

        public PropertyDispatch.C5<V, T1, T2, T3, T4, T5> select(T1 property1, T2 property2, T3 property3, T4 property4, T5 property5, V value) {
            PropertyValueList propertyvaluelist = PropertyValueList.of(
                this.property1.value(property1),
                this.property2.value(property2),
                this.property3.value(property3),
                this.property4.value(property4),
                this.property5.value(property5)
            );
            this.putValue(propertyvaluelist, value);
            return this;
        }

        public PropertyDispatch<V> generate(Function5<T1, T2, T3, T4, T5, V> generator) {
            this.property1
                .getPossibleValues()
                .forEach(
                    p_388135_ -> this.property2
                        .getPossibleValues()
                        .forEach(
                            p_388174_ -> this.property3
                                .getPossibleValues()
                                .forEach(
                                    p_387752_ -> this.property4
                                        .getPossibleValues()
                                        .forEach(
                                            p_388362_ -> this.property5
                                                .getPossibleValues()
                                                .forEach(
                                                    p_403975_ -> this.select(
                                                        (T1)p_388135_,
                                                        (T2)p_388174_,
                                                        (T3)p_387752_,
                                                        (T4)p_388362_,
                                                        (T5)p_403975_,
                                                        generator.apply((T1)p_388135_, (T2)p_388174_, (T3)p_387752_, (T4)p_388362_, (T5)p_403975_)
                                                    )
                                                )
                                        )
                                )
                        )
                );
            return this;
        }
    }
}
