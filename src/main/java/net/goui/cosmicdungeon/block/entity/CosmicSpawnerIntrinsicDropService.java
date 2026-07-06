package net.goui.cosmicdungeon.block.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CosmicSpawnerIntrinsicDropService {
    private CosmicSpawnerIntrinsicDropService() {}

    public static Display buildDisplay(MinecraftServer server, ResourceLocation entityTypeId, CosmicSpawnerPreset preset) {
        CosmicSpawnerLootTableSummary summary = CosmicSpawnerLootTableInspector.inspect(server, entityTypeId);
        Map<ResourceLocation, CosmicSpawnerLootTableSummary.ChanceDisplay> defaults = new LinkedHashMap<>();
        for (CosmicSpawnerLootTableSummary.DefaultDrop drop : summary.defaultDrops()) defaults.put(drop.itemId(), drop.chance());

        List<Row> rows = new ArrayList<>();
        if (preset != null) {
            for (CosmicSpawnerPreset.IntrinsicDropRule rule : preset.getConfiguredIntrinsicDropRuleList()) {
                rows.add(Row.configured(rule, defaults.get(rule.itemId())));
            }
        }
        for (var entry : defaults.entrySet()) {
            boolean configured = preset != null && !preset.getConfiguredIntrinsicDropRules(entry.getKey()).isEmpty();
            if (!configured) rows.add(Row.defaultOnly(entry.getKey(), entry.getValue()));
        }
        rows.sort(Comparator.comparing(Row::itemId).thenComparing(r -> r.ruleId() == null ? "" : r.ruleId()));
        return new Display(summary, rows);
    }

    public static CosmicSpawnerLootTableSummary.ChanceDisplay findDefaultChance(MinecraftServer server, ResourceLocation entityTypeId, ResourceLocation itemId) {
        if (entityTypeId == null || itemId == null) return null;
        CosmicSpawnerLootTableSummary summary = CosmicSpawnerLootTableInspector.inspect(server, entityTypeId);
        for (CosmicSpawnerLootTableSummary.DefaultDrop drop : summary.defaultDrops()) if (itemId.equals(drop.itemId())) return drop.chance();
        return null;
    }

    public record Display(CosmicSpawnerLootTableSummary summary, List<Row> rows) {}

    public record Row(ResourceLocation itemId, String ruleId, CosmicSpawnerLootTableSummary.ChanceDisplay defaultChance,
                      Float overrideChance, Integer count, CosmicSpawnerPreset.IntrinsicDropRule.Kind kind) {
        static Row defaultOnly(ResourceLocation itemId, CosmicSpawnerLootTableSummary.ChanceDisplay defaultChance) {
            return new Row(itemId, null, defaultChance, null, null, null);
        }
        static Row configured(CosmicSpawnerPreset.IntrinsicDropRule rule, CosmicSpawnerLootTableSummary.ChanceDisplay defaultChance) {
            return new Row(rule.itemId(), rule.id(), defaultChance, rule.chance(), rule.count(), rule.kind());
        }
        public boolean hasDefault() { return defaultChance != null; }
        public boolean hasOverride() { return overrideChance != null; }
        public boolean customAdded() { return !hasDefault() && hasOverride(); }
        public boolean overriddenDefault() { return hasDefault() && hasOverride(); }
    }
}
