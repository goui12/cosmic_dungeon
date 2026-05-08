package net.minecraft.core.component;

import java.util.stream.Stream;
import javax.annotation.Nullable;

public interface DataComponentHolder extends DataComponentGetter, net.neoforged.neoforge.common.extensions.IDataComponentHolderExtension {
    DataComponentMap getComponents();

    @Nullable
    @Override
    default <T> T get(DataComponentType<? extends T> component) {
        return this.getComponents().get(component);
    }

    default <T> Stream<T> getAllOfType(Class<? extends T> type) {
        return this.getComponents()
            .stream()
            .map(TypedDataComponent::value)
            .filter(p_366823_ -> type.isAssignableFrom(p_366823_.getClass()))
            .map(p_366685_ -> (T)p_366685_);
    }

    @Override
    default <T> T getOrDefault(DataComponentType<? extends T> component, T defaultValue) {
        return this.getComponents().getOrDefault(component, defaultValue);
    }

    default boolean has(DataComponentType<?> component) {
        return this.getComponents().has(component);
    }
}
