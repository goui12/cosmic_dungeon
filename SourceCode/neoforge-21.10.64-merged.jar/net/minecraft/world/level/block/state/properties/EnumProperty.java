package net.minecraft.world.level.block.state.properties;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.util.StringRepresentable;

public final class EnumProperty<T extends Enum<T> & StringRepresentable> extends Property<T> {
    private final List<T> values;
    /**
     * Map of names to Enum values
     */
    private final Map<String, T> names;
    private final int[] ordinalToIndex;

    private EnumProperty(String name, Class<T> clazz, List<T> values) {
        super(name, clazz);
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Trying to make empty EnumProperty '" + name + "'");
        } else {
            this.values = List.copyOf(values);
            T[] at = clazz.getEnumConstants();
            this.ordinalToIndex = new int[at.length];

            for (T t : at) {
                this.ordinalToIndex[t.ordinal()] = values.indexOf(t);
            }

            Builder<String, T> builder = ImmutableMap.builder();

            for (T t1 : values) {
                String s = t1.getSerializedName();
                builder.put(s, t1);
            }

            this.names = builder.buildOrThrow();
        }
    }

    @Override
    public List<T> getPossibleValues() {
        return this.values;
    }

    @Override
    public Optional<T> getValue(String value) {
        return Optional.ofNullable(this.names.get(value));
    }

    /**
     * @return the name for the given value.
     */
    public String getName(T value) {
        return value.getSerializedName();
    }

    public int getInternalIndex(T value) {
        return this.ordinalToIndex[value.ordinal()];
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else {
            return other instanceof EnumProperty<?> enumproperty && super.equals(other) ? this.values.equals(enumproperty.values) : false;
        }
    }

    @Override
    public int generateHashCode() {
        int i = super.generateHashCode();
        return 31 * i + this.values.hashCode();
    }

    /**
     * Create a new EnumProperty with all Enum constants of the given class.
     */
    public static <T extends Enum<T> & StringRepresentable> EnumProperty<T> create(String name, Class<T> clazz) {
        return create(name, clazz, p_187560_ -> true);
    }

    /**
     * Create a new EnumProperty with all Enum constants of the given class that match the given Predicate.
     */
    public static <T extends Enum<T> & StringRepresentable> EnumProperty<T> create(String name, Class<T> clazz, Predicate<T> filter) {
        return create(name, clazz, Arrays.<T>stream(clazz.getEnumConstants()).filter(filter).collect(Collectors.toList()));
    }

    /**
     * Create a new EnumProperty with the specified values
     */
    @SafeVarargs
    public static <T extends Enum<T> & StringRepresentable> EnumProperty<T> create(String name, Class<T> clazz, T... values) {
        return create(name, clazz, List.of(values));
    }

    public static <T extends Enum<T> & StringRepresentable> EnumProperty<T> create(String name, Class<T> clazz, List<T> values) {
        return new EnumProperty<>(name, clazz, values);
    }
}
