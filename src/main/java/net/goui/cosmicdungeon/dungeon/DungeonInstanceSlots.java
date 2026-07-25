package net.goui.cosmicdungeon.dungeon;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.server.MinecraftServer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Fixed, datapack-registered physical worlds leased as ten logical dungeon slots. */
public final class DungeonInstanceSlots {
    public static final int SLOT_COUNT = 10;

    private DungeonInstanceSlots() {}

    public static ResourceKey<Level> primary(int slot) {
        validate(slot);
        return dimension("dungeon_instance_" + twoDigits(slot));
    }

    public static ResourceKey<Level> nether(int slot) {
        validate(slot);
        return dimension("dungeon_instance_" + twoDigits(slot) + "_nether");
    }

    public static List<ResourceKey<Level>> allDimensions(int slot) {
        return List.of(primary(slot), nether(slot));
    }

    public static List<String> dimensionIds(int slot) {
        return allDimensions(slot).stream().map(key -> key.location().toString()).toList();
    }

    public static Map<ResourceKey<Level>, ResourceKey<Level>> mapping(DungeonDefinition definition, int slot) {
        validate(slot);
        LinkedHashMap<ResourceKey<Level>, ResourceKey<Level>> result = new LinkedHashMap<>();
        result.put(definition.primaryDimension(), primary(slot));
        for (ResourceKey<Level> template : definition.dimensions()) {
            if (template.equals(definition.primaryDimension())) continue;
            if (template.location().getPath().endsWith("_nether")) {
                result.put(template, nether(slot));
            } else {
                throw new IllegalArgumentException("No physical subdimension is reserved for template " + template.location());
            }
        }
        return Map.copyOf(result);
    }

    public static Optional<Integer> slotOf(ResourceKey<Level> dimension) {
        if (dimension == null || !CosmicDungeonMod.MOD_ID.equals(dimension.location().getNamespace())) return Optional.empty();
        String path = dimension.location().getPath();
        if (!path.startsWith("dungeon_instance_") || path.length() < "dungeon_instance_00".length()) return Optional.empty();
        try {
            int slot = Integer.parseInt(path.substring("dungeon_instance_".length(), "dungeon_instance_".length() + 2));
            return slot >= 1 && slot <= SLOT_COUNT ? Optional.of(slot) : Optional.empty();
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    public static Optional<ResourceKey<Level>> translateTemplate(DungeonDefinition definition,
                                                                  int slot,
                                                                  ResourceKey<Level> templateDimension) {
        return Optional.ofNullable(mapping(definition, slot).get(templateDimension));
    }

    public static ResourceKey<Level> templateDimensionForPhysical(MinecraftServer server, ResourceKey<Level> physical) {
        if (server == null || physical == null || slotOf(physical).isEmpty()) return physical;
        DungeonRunRegistryData.RunRecord run = DungeonRunRegistryData.get(server).findRunForInstanceDimension(physical).orElse(null);
        DungeonDefinition definition = run == null ? null : DungeonDefinitions.byId(run.dungeonId()).orElse(null);
        if (definition == null) return physical;
        for (Map.Entry<ResourceKey<Level>, ResourceKey<Level>> entry : mapping(definition, run.instanceSlot()).entrySet()) {
            if (entry.getValue().equals(physical)) return entry.getKey();
        }
        return physical;
    }

    private static ResourceKey<Level> dimension(String path) {
        return ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, path));
    }

    private static String twoDigits(int slot) {
        return slot < 10 ? "0" + slot : Integer.toString(slot);
    }

    private static void validate(int slot) {
        if (slot < 1 || slot > SLOT_COUNT) {
            throw new IllegalArgumentException("Dungeon instance slot must be 1-" + SLOT_COUNT + ": " + slot);
        }
    }
}
