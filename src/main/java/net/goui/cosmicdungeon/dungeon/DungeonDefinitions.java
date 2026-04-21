package net.goui.cosmicdungeon.dungeon;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class DungeonDefinitions {
    private DungeonDefinitions() {}

    private static ResourceKey<Level> dim(String path) {
        return ResourceKey.create(
                Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, path)
        );
    }

    public static final DungeonDefinition DUNGEON_1 = new DungeonDefinition(
            "dungeon_1",
            dim("dungeon_1"),
            List.of(dim("dungeon_1"), dim("dungeon_1_nether"))
    );

    public static final DungeonDefinition DUNGEON_2 = new DungeonDefinition(
            "dungeon_2",
            dim("dungeon_2"),
            List.of(dim("dungeon_2"))
    );

    public static final DungeonDefinition DUNGEON_3 = new DungeonDefinition(
            "dungeon_3",
            dim("dungeon_3"),
            List.of(dim("dungeon_3"))
    );

    public static final DungeonDefinition DUNGEON_4 = new DungeonDefinition(
            "dungeon_4",
            dim("dungeon_4"),
            List.of(dim("dungeon_4"))
    );

    public static final DungeonDefinition DUNGEON_5 = new DungeonDefinition(
            "dungeon_5",
            dim("dungeon_5"),
            List.of(dim("dungeon_5"))
    );

    private static final List<DungeonDefinition> ALL = List.of(
            DUNGEON_1, DUNGEON_2, DUNGEON_3, DUNGEON_4, DUNGEON_5
    );

    private static final Map<String, DungeonDefinition> BY_ID = buildById();

    private static Map<String, DungeonDefinition> buildById() {
        Map<String, DungeonDefinition> out = new LinkedHashMap<>();
        for (DungeonDefinition def : ALL) {
            out.put(def.id().toLowerCase(Locale.ROOT), def);
        }
        return Map.copyOf(out);
    }

    public static List<DungeonDefinition> all() {
        return ALL;
    }

    public static Optional<DungeonDefinition> byId(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        return Optional.ofNullable(BY_ID.get(id.trim().toLowerCase(Locale.ROOT)));
    }

    public static Optional<DungeonDefinition> byDimension(ResourceKey<Level> dim) {
        if (dim == null) return Optional.empty();
        for (DungeonDefinition def : ALL) {
            if (def.containsDimension(dim)) {
                return Optional.of(def);
            }
        }
        return Optional.empty();
    }

    public static Optional<DungeonDefinition> resolve(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();

        String s = raw.trim().toLowerCase(Locale.ROOT);

        DungeonDefinition byId = BY_ID.get(s);
        if (byId != null) return Optional.of(byId);

        ResourceLocation rl = s.contains(":")
                ? ResourceLocation.tryParse(s)
                : ResourceLocation.tryBuild(CosmicDungeonMod.MOD_ID, s);

        if (rl == null) return Optional.empty();

        for (DungeonDefinition def : ALL) {
            if (def.id().equalsIgnoreCase(s)) return Optional.of(def);
            for (ResourceKey<Level> key : def.dimensions()) {
                if (key.location().equals(rl)) return Optional.of(def);
                if (key.location().getPath().equalsIgnoreCase(s)) return Optional.of(def);
            }
        }

        return Optional.empty();
    }

    public static List<String> suggestedDungeonTargets() {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (DungeonDefinition def : ALL) {
            out.add(def.id());
            for (ResourceKey<Level> key : def.dimensions()) {
                out.add(key.location().getPath());
            }
        }
        return List.copyOf(out);
    }
}