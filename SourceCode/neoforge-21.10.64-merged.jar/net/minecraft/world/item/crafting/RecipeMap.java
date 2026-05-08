package net.minecraft.world.item.crafting;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class RecipeMap {
    public static final RecipeMap EMPTY = new RecipeMap(ImmutableMultimap.of(), Map.of());
    private Multimap<RecipeType<?>, RecipeHolder<?>> byType;
    private final Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> byKey;

    private RecipeMap(Multimap<RecipeType<?>, RecipeHolder<?>> byType, Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> byKey) {
        this.byType = byType;
        this.byKey = byKey;
    }

    public static RecipeMap create(Iterable<RecipeHolder<?>> recipes) {
        Builder<RecipeType<?>, RecipeHolder<?>> builder = ImmutableMultimap.builder();
        com.google.common.collect.ImmutableMap.Builder<ResourceKey<Recipe<?>>, RecipeHolder<?>> builder1 = ImmutableMap.builder();

        for (RecipeHolder<?> recipeholder : recipes) {
            builder.put(recipeholder.value().getType(), recipeholder);
            builder1.put(recipeholder.id(), recipeholder);
        }

        return new RecipeMap(builder.build(), builder1.build());
    }

    // Neo: handle ordering according to any recipe priorities
    public void order(it.unimi.dsi.fastutil.objects.Object2IntMap<ResourceKey<net.minecraft.world.item.crafting.Recipe<?>>> recipePriorities) {
        it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap<java.util.List<RecipeHolder<?>>> priorityBuilder = new it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap<>();
        com.google.common.collect.LinkedListMultimap<RecipeType<?>, RecipeHolder<?>> finalBuilder = com.google.common.collect.LinkedListMultimap.create();

        for (RecipeHolder<?> recipeholder : this.byKey.values()) {
            int priority = recipePriorities.getOrDefault(recipeholder.id(), 0);
            priorityBuilder.computeIfAbsent(priority, p -> new java.util.ArrayList<>()).add(recipeholder);
        }

        for (var list : priorityBuilder.reversed().values()) {
            for (RecipeHolder<?> recipeHolder : list) {
                finalBuilder.put(recipeHolder.value().getType(), recipeHolder);
            }
        }

        this.byType = ImmutableMultimap.copyOf(finalBuilder);
    }

    public <I extends RecipeInput, T extends Recipe<I>> Collection<RecipeHolder<T>> byType(RecipeType<T> type) {
        return (Collection<RecipeHolder<T>>)(Collection<?>)this.byType.get(type);
    }

    public Collection<RecipeHolder<?>> values() {
        return this.byKey.values();
    }

    @Nullable
    public RecipeHolder<?> byKey(ResourceKey<Recipe<?>> key) {
        return this.byKey.get(key);
    }

    public <I extends RecipeInput, T extends Recipe<I>> Stream<RecipeHolder<T>> getRecipesFor(RecipeType<T> type, I input, Level level) {
        return input.isEmpty() ? Stream.empty() : this.byType(type).stream().filter(p_380352_ -> p_380352_.value().matches(input, level));
    }
}
