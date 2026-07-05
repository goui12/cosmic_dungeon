package net.goui.cosmicdungeon.block.entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootTable;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.util.Optional;

public final class CosmicSpawnerLootTableInspector {
    private static final Logger LOGGER = LogUtils.getLogger();

    private CosmicSpawnerLootTableInspector() {}

    public static CosmicSpawnerLootTableSummary inspect(MinecraftServer server, ResourceLocation entityTypeId) {
        ResourceLocation actualEntityId = actualEntityTypeId(entityTypeId);
        Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(actualEntityId);
        if (type.isEmpty()) {
            CosmicSpawnerLootTableSummary missing = new CosmicSpawnerLootTableSummary(actualEntityId, null, null);
            missing.setMissingReason("Unknown entity type " + actualEntityId + ".");
            return missing;
        }

        Optional<net.minecraft.resources.ResourceKey<LootTable>> lootKey = type.get().getDefaultLootTable();
        if (lootKey.isEmpty()) {
            CosmicSpawnerLootTableSummary missing = new CosmicSpawnerLootTableSummary(actualEntityId, null, null);
            missing.setMissingReason("No intrinsic loot table found for " + actualEntityId + ".");
            return missing;
        }

        ResourceLocation lootTableId = lootKey.get().location();
        ResourceLocation resourceId = lootTableResourceId(lootTableId);
        CosmicSpawnerLootTableSummary summary = new CosmicSpawnerLootTableSummary(actualEntityId, lootTableId, resourceId);

        try {
            var resource = server.getResourceManager().getResource(resourceId);
            if (resource.isEmpty()) {
                summary.setMissingReason("No active loot table resource found for " + actualEntityId + " (" + resourceId + ").");
                return summary;
            }
            try (BufferedReader reader = resource.get().openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                readPools(root, summary);
            }
            if (!summary.hasDefaultDrops()) {
                summary.setMissingReason("Loot table " + lootTableId + " has no direct item entries that can be displayed.");
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to inspect Cosmic Spawner loot table {} for {}", lootTableId, actualEntityId, e);
            summary.setMissingReason("Could not inspect active loot table " + lootTableId + ".");
        }
        return summary;
    }

    public static ResourceLocation actualEntityTypeId(ResourceLocation entityTypeId) {
        return CosmicSpawnerPreset.ILLAGER_CAPTAIN_ID.equals(entityTypeId)
                ? ResourceLocation.withDefaultNamespace("pillager")
                : entityTypeId;
    }

    private static ResourceLocation lootTableResourceId(ResourceLocation lootTableId) {
        return ResourceLocation.fromNamespaceAndPath(lootTableId.getNamespace(), "loot_table/" + lootTableId.getPath() + ".json");
    }

    private static void readPools(JsonObject root, CosmicSpawnerLootTableSummary summary) {
        JsonArray pools = array(root, "pools");
        if (pools == null) return;
        for (JsonElement poolElement : pools) {
            if (!poolElement.isJsonObject()) continue;
            JsonObject pool = poolElement.getAsJsonObject();
            CosmicSpawnerLootTableSummary.ChanceDisplay poolChance = poolChance(pool);
            JsonArray entries = array(pool, "entries");
            if (entries != null) readEntries(entries, pool, poolChance, summary);
        }
    }

    private static void readEntries(JsonArray entries, JsonObject parent, CosmicSpawnerLootTableSummary.ChanceDisplay parentChance, CosmicSpawnerLootTableSummary summary) {
        int totalWeight = 0;
        for (JsonElement entryElement : entries) {
            if (!entryElement.isJsonObject()) continue;
            JsonObject entry = entryElement.getAsJsonObject();
            if (isItemEntry(entry)) totalWeight += Math.max(1, intOr(entry, "weight", 1));
        }

        for (JsonElement entryElement : entries) {
            if (!entryElement.isJsonObject()) continue;
            JsonObject entry = entryElement.getAsJsonObject();
            String type = stringOr(entry, "type", "");
            if (isItemEntry(entry)) {
                ResourceLocation itemId = ResourceLocation.tryParse(stringOr(entry, "name", ""));
                if (itemId == null) continue;
                CosmicSpawnerLootTableSummary.ChanceDisplay chance = entryChance(parent, entry, parentChance, totalWeight);
                summary.addDefaultDrop(itemId, chance);
            } else if (type.endsWith(":alternatives") || type.endsWith(":sequence") || type.endsWith(":group")) {
                JsonArray children = array(entry, "children");
                if (children != null) readEntries(children, entry, parentChance, summary);
            }
        }
    }

    private static CosmicSpawnerLootTableSummary.ChanceDisplay poolChance(JsonObject pool) {
        if (hasCondition(pool, "killed_by_player") && hasCondition(pool, "random_chance_with")) {
            return CosmicSpawnerLootTableSummary.ChanceDisplay.complex("rare/player/looting");
        }
        Float conditionChance = randomChance(pool);
        if (conditionChance != null) return CosmicSpawnerLootTableSummary.ChanceDisplay.percent(conditionChance);
        if (hasConditions(pool)) return CosmicSpawnerLootTableSummary.ChanceDisplay.complex("conditional");
        return CosmicSpawnerLootTableSummary.ChanceDisplay.percent(1f);
    }

    private static CosmicSpawnerLootTableSummary.ChanceDisplay entryChance(JsonObject pool, JsonObject entry, CosmicSpawnerLootTableSummary.ChanceDisplay poolChance, int totalWeight) {
        if (hasCondition(entry, "killed_by_player") || hasCondition(entry, "random_chance_with")) {
            return CosmicSpawnerLootTableSummary.ChanceDisplay.complex("rare/player/looting");
        }
        if (poolChance != null && !poolChance.numeric()) return poolChance;
        if (hasConditions(entry)) return CosmicSpawnerLootTableSummary.ChanceDisplay.complex("conditional");

        float chance = poolChance != null && poolChance.chance() != null ? poolChance.chance() : 1f;
        if (totalWeight > 0) chance *= Math.max(1, intOr(entry, "weight", 1)) / (float) totalWeight;

        CosmicSpawnerLootTableSummary.ChanceDisplay countChance = countFunctionChance(entry);
        if (countChance != null) {
            if (!countChance.numeric()) return countChance;
            chance *= countChance.chance();
        }
        return CosmicSpawnerLootTableSummary.ChanceDisplay.percent(chance);
    }

    private static CosmicSpawnerLootTableSummary.ChanceDisplay countFunctionChance(JsonObject entry) {
        JsonArray functions = array(entry, "functions");
        if (functions == null) return null;
        for (JsonElement functionElement : functions) {
            if (!functionElement.isJsonObject()) continue;
            JsonObject function = functionElement.getAsJsonObject();
            if (!stringOr(function, "function", "").endsWith(":set_count")) continue;
            JsonElement count = function.get("count");
            if (count == null) return CosmicSpawnerLootTableSummary.ChanceDisplay.complex("complex count");
            if (count.isJsonPrimitive() && count.getAsJsonPrimitive().isNumber()) {
                return CosmicSpawnerLootTableSummary.ChanceDisplay.percent(count.getAsFloat() > 0f ? 1f : 0f);
            }
            if (!count.isJsonObject()) return CosmicSpawnerLootTableSummary.ChanceDisplay.complex("complex count");
            JsonObject countObject = count.getAsJsonObject();
            float min = floatOr(countObject, "min", 0f);
            float max = floatOr(countObject, "max", min);
            if (max <= 0f) return CosmicSpawnerLootTableSummary.ChanceDisplay.percent(0f);
            if (min >= 1f) return CosmicSpawnerLootTableSummary.ChanceDisplay.percent(1f);
            if (min == 0f && max > 0f && Math.floor(max) == max) {
                return CosmicSpawnerLootTableSummary.ChanceDisplay.percent(max / (max + 1f));
            }
            return CosmicSpawnerLootTableSummary.ChanceDisplay.complex("complex count");
        }
        return null;
    }

    private static boolean isItemEntry(JsonObject entry) {
        return stringOr(entry, "type", "").endsWith(":item") && entry.has("name");
    }

    private static boolean hasConditions(JsonObject object) {
        JsonArray conditions = array(object, "conditions");
        return conditions != null && !conditions.isEmpty();
    }

    private static boolean hasCondition(JsonObject object, String needle) {
        JsonArray conditions = array(object, "conditions");
        if (conditions == null) return false;
        for (JsonElement conditionElement : conditions) {
            if (!conditionElement.isJsonObject()) continue;
            if (stringOr(conditionElement.getAsJsonObject(), "condition", "").contains(needle)) return true;
        }
        return false;
    }

    private static Float randomChance(JsonObject object) {
        JsonArray conditions = array(object, "conditions");
        if (conditions == null) return null;
        for (JsonElement conditionElement : conditions) {
            if (!conditionElement.isJsonObject()) continue;
            JsonObject condition = conditionElement.getAsJsonObject();
            if (stringOr(condition, "condition", "").endsWith(":random_chance") && condition.has("chance")) {
                return floatOr(condition, "chance", 1f);
            }
        }
        return null;
    }

    private static JsonArray array(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    private static String stringOr(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : fallback;
    }

    private static int intOr(JsonObject object, String key, int fallback) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber() ? element.getAsInt() : fallback;
    }

    private static float floatOr(JsonObject object, String key, float fallback) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber() ? element.getAsFloat() : fallback;
    }
}
