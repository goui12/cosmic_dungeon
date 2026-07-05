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
    private static final int DATA_VERSION = 1;
    private static final String KEY_VERSION = "version";
    private static final String KEY_RULES = "rules";
    private static final String KEY_ITEM = "item";
    private static final String KEY_CHANCE = "chance";
    private static final String KEY_KIND = "kind";

    private CosmicSpawnerEntityIntrinsicDropData() {}

    public static void write(Entity entity, Map<ResourceLocation, CosmicSpawnerPreset.IntrinsicDropRule> rules) {
        if (rules == null || rules.isEmpty()) {
            entity.getPersistentData().remove(ROOT_KEY);
            return;
        }

        CompoundTag root = new CompoundTag();
        root.putInt(KEY_VERSION, DATA_VERSION);
        ListTag list = new ListTag();
        for (CosmicSpawnerPreset.IntrinsicDropRule rule : rules.values()) {
            if (rule == null || rule.itemId() == null) continue;
            CompoundTag entry = new CompoundTag();
            entry.putString(KEY_ITEM, rule.itemId().toString());
            entry.putFloat(KEY_CHANCE, clamp(rule.chance()));
            entry.putString(KEY_KIND, rule.kind().name());
            list.add(entry);
        }
        root.put(KEY_RULES, list);
        entity.getPersistentData().put(ROOT_KEY, root);
    }

    public static Optional<Map<ResourceLocation, Float>> readChances(Entity entity) {
        CompoundTag persistentData = entity.getPersistentData();
        if (!persistentData.contains(ROOT_KEY)) return Optional.empty();

        CompoundTag root = persistentData.getCompoundOrEmpty(ROOT_KEY);
        if (root.isEmpty()) return Optional.empty();

        Map<ResourceLocation, Float> out = new LinkedHashMap<>();
        ListTag rules = root.getListOrEmpty(KEY_RULES);
        for (int i = 0; i < rules.size(); i++) {
            CompoundTag entry = rules.getCompoundOrEmpty(i);
            ResourceLocation itemId = ResourceLocation.tryParse(entry.getStringOr(KEY_ITEM, ""));
            if (itemId != null) {
                out.put(itemId, clamp(entry.getFloatOr(KEY_CHANCE, 1.0f)));
            }
        }
        return Optional.of(out);
    }

    private static float clamp(float chance) {
        return Math.max(0f, Math.min(1f, chance));
    }
}
