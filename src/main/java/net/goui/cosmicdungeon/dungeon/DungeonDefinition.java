package net.goui.cosmicdungeon.dungeon;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

public record DungeonDefinition(
        String id,
        ResourceKey<Level> primaryDimension,
        List<ResourceKey<Level>> dimensions
) {
    public DungeonDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(primaryDimension, "primaryDimension");

        LinkedHashSet<ResourceKey<Level>> ordered = new LinkedHashSet<>();
        ordered.add(primaryDimension);
        if (dimensions != null) {
            ordered.addAll(dimensions);
        }

        dimensions = List.copyOf(new ArrayList<>(ordered));
    }

    public boolean containsDimension(ResourceKey<Level> dim) {
        return dim != null && dimensions.contains(dim);
    }

    public List<String> dimensionIds() {
        return dimensions.stream()
                .map(k -> k.location().toString())
                .toList();
    }
}