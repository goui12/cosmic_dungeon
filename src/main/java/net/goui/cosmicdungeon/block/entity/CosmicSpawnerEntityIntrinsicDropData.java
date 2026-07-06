package net.goui.cosmicdungeon.block.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class CosmicSpawnerEntityIntrinsicDropData {
    public static final String ROOT_KEY = "cosmicdungeon:spawner_intrinsic_drops";
    private static final int DATA_VERSION = 2;
    private static final String KEY_VERSION = "version";
    private static final String KEY_RULES = "rules";
    private static final String KEY_ID = "id";
    private static final String KEY_ITEM = "item";
    private static final String KEY_CHANCE = "chance";
    private static final String KEY_COUNT = "count";
    private static final String KEY_KIND = "kind";

    private CosmicSpawnerEntityIntrinsicDropData() {}

    public static void write(Entity entity, Map<String, CosmicSpawnerPreset.IntrinsicDropRule> rules) {
        if (rules == null || rules.isEmpty()) { entity.getPersistentData().remove(ROOT_KEY); return; }
        CompoundTag root = new CompoundTag();
        root.putInt(KEY_VERSION, DATA_VERSION);
        ListTag list = new ListTag();
        for (CosmicSpawnerPreset.IntrinsicDropRule rule : rules.values()) {
            if (rule == null || rule.itemId() == null) continue;
            CompoundTag entry = new CompoundTag();
            entry.putString(KEY_ID, rule.id());
            entry.putString(KEY_ITEM, rule.itemId().toString());
            entry.putFloat(KEY_CHANCE, clamp(rule.chance()));
            entry.putInt(KEY_COUNT, CosmicSpawnerPreset.clampCount(rule.count()));
            entry.putString(KEY_KIND, rule.kind().name());
            list.add(entry);
        }
        root.put(KEY_RULES, list);
        entity.getPersistentData().put(ROOT_KEY, root);
    }

    public static Optional<Map<String, CosmicSpawnerPreset.IntrinsicDropRule>> readRules(Entity entity) {
        CompoundTag persistentData = entity.getPersistentData();
        if (!persistentData.contains(ROOT_KEY)) return Optional.empty();
        CompoundTag root = persistentData.getCompoundOrEmpty(ROOT_KEY);
        if (root.isEmpty()) return Optional.empty();
        Map<String, CosmicSpawnerPreset.IntrinsicDropRule> out = new LinkedHashMap<>();
        ListTag rules = root.getListOrEmpty(KEY_RULES);
        for (int i = 0; i < rules.size(); i++) {
            CompoundTag entry = rules.getCompoundOrEmpty(i);
            ResourceLocation itemId = ResourceLocation.tryParse(entry.getStringOr(KEY_ITEM, ""));
            if (itemId == null) continue;
            String id = entry.getStringOr(KEY_ID, "entity_" + i);
            float chance = clamp(entry.getFloatOr(KEY_CHANCE, 1.0f));
            int count = entry.getIntOr(KEY_COUNT, 1);
            CosmicSpawnerPreset.IntrinsicDropRule.Kind kind;
            try { kind = CosmicSpawnerPreset.IntrinsicDropRule.Kind.valueOf(entry.getStringOr(KEY_KIND, CosmicSpawnerPreset.IntrinsicDropRule.Kind.UNKNOWN_CONFIGURED.name())); }
            catch (IllegalArgumentException ex) { kind = CosmicSpawnerPreset.IntrinsicDropRule.Kind.UNKNOWN_CONFIGURED; }
            CosmicSpawnerPreset.IntrinsicDropRule rule = new CosmicSpawnerPreset.IntrinsicDropRule(id, itemId, chance, count, kind);
            out.put(rule.id(), rule);
        }
        return Optional.of(out);
    }

    private static float clamp(float chance) { return Math.max(0f, Math.min(1f, chance)); }
}
