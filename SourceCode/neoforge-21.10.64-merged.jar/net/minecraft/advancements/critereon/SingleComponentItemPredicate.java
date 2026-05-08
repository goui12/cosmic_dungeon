package net.minecraft.advancements.critereon;

import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.predicates.DataComponentPredicate;

public interface SingleComponentItemPredicate<T> extends DataComponentPredicate {
    @Override
    default boolean matches(DataComponentGetter componentGetter) {
        T t = componentGetter.get(this.componentType());
        return t != null && this.matches(t);
    }

    DataComponentType<T> componentType();

    boolean matches(T value);
}
