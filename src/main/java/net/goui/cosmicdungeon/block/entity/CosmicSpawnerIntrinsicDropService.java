package net.goui.cosmicdungeon.block.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CosmicSpawnerIntrinsicDropService {
    private CosmicSpawnerIntrinsicDropService() {}

    public static Display buildDisplay(MinecraftServer server, ResourceLocation entityTypeId, CosmicSpawnerPreset preset) {
        CosmicSpawnerLootTableSummary summary = CosmicSpawnerLootTableInspector.inspect(server, entityTypeId);
        Map<ResourceLocation, RowBuilder> rows = new LinkedHashMap<>();

        for (CosmicSpawnerLootTableSummary.DefaultDrop drop : summary.defaultDrops()) {
            rows.computeIfAbsent(drop.itemId(), RowBuilder::new).defaultChance = drop.chance();
        }

        if (preset != null) {
            for (var entry : preset.getConfiguredIntrinsicDropRules().entrySet()) {
                rows.computeIfAbsent(entry.getKey(), RowBuilder::new).overrideChance = entry.getValue().chance();
            }
        }

        List<Row> outputRows = rows.values().stream()
                .map(RowBuilder::build)
                .sorted(Comparator.comparing(Row::itemId))
                .toList();
        return new Display(summary, outputRows);
    }

    public static CosmicSpawnerLootTableSummary.ChanceDisplay findDefaultChance(MinecraftServer server, ResourceLocation entityTypeId, ResourceLocation itemId) {
        if (entityTypeId == null || itemId == null) return null;
        CosmicSpawnerLootTableSummary summary = CosmicSpawnerLootTableInspector.inspect(server, entityTypeId);
        for (CosmicSpawnerLootTableSummary.DefaultDrop drop : summary.defaultDrops()) {
            if (itemId.equals(drop.itemId())) return drop.chance();
        }
        return null;
    }

    public record Display(CosmicSpawnerLootTableSummary summary, List<Row> rows) {}

    public record Row(ResourceLocation itemId, CosmicSpawnerLootTableSummary.ChanceDisplay defaultChance, Float overrideChance) {
        public boolean hasDefault() { return defaultChance != null; }
        public boolean hasOverride() { return overrideChance != null; }
        public boolean customAdded() { return !hasDefault() && hasOverride(); }
        public boolean overriddenDefault() { return hasDefault() && hasOverride(); }
    }

    private static final class RowBuilder {
        private final ResourceLocation itemId;
        private CosmicSpawnerLootTableSummary.ChanceDisplay defaultChance;
        private Float overrideChance;

        private RowBuilder(ResourceLocation itemId) {
            this.itemId = itemId;
        }

        private Row build() {
            return new Row(itemId, defaultChance, overrideChance);
        }
    }
}
