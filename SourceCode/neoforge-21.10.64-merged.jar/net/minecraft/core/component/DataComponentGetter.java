package net.minecraft.core.component;

import javax.annotation.Nullable;

public interface DataComponentGetter {
    @Nullable
    <T> T get(DataComponentType<? extends T> component);

    default <T> T getOrDefault(DataComponentType<? extends T> component, T defaultValue) {
        T t = this.get(component);
        return t != null ? t : defaultValue;
    }

    @Nullable
    default <T> TypedDataComponent<T> getTyped(DataComponentType<T> component) {
        T t = this.get(component);
        return t != null ? new TypedDataComponent<>(component, t) : null;
    }

    // Neo: Utility for modded component types, to remove the need to invoke '.value()'
    @Nullable
    default <T> T get(java.util.function.Supplier<? extends DataComponentType<? extends T>> componentType) {
        return get(componentType.get());
    }

    default <T> T getOrDefault(java.util.function.Supplier<? extends DataComponentType<? extends T>> componentType, T value) {
        return getOrDefault(componentType.get(), value);
    }

    default <T> boolean has(java.util.function.Supplier<? extends DataComponentType<? extends T>> componentType) {
        return get(componentType) != null;
    }

    default boolean has(DataComponentType<?> componentType) {
        return get(componentType) != null;
    }
}
