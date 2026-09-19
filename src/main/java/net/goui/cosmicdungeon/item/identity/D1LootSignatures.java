package net.goui.cosmicdungeon.item.identity;

import java.util.*;

/** New named-adoption evidence only; never rewrites equipment or invalidates an existing receipt.
 * Source: Dungeon Dropped Gear and Items, 1fX1UbC6cG_cnN2auDo_1ascDY24yjFy9pIfL4qe3-_g,
 * edited 2026-08-28. Exact applied enchantments, including absence of extras/curses.
 * These are item definitions, not combat modifiers or price overrides.
 */
public final class D1LootSignatures {
    private static final Map<String, Map<String,Integer>> ENCHANTMENTS = Map.ofEntries(
            Map.entry("ranseur_of_the_fallen_dragoon", Map.of("minecraft:channeling", 1, "minecraft:impaling", 4, "minecraft:loyalty", 3, "minecraft:unbreaking", 3)),
            Map.entry("recovered_spyglass", Map.of()),
            Map.entry("perforated_chestplate", Map.of("minecraft:fire_protection", 1)),
            Map.entry("salvaged_leggings", Map.of("minecraft:blast_protection", 1)),
            Map.entry("discarded_helm", Map.of("minecraft:thorns", 1)),
            Map.entry("resoled_boots", Map.of("minecraft:feather_falling", 1)),
            Map.entry("last_resort", Map.of("minecraft:sharpness", 2, "minecraft:unbreaking", 2)),
            Map.entry("squared_mallet", Map.of("minecraft:wind_burst", 1, "minecraft:smite", 3, "minecraft:fire_aspect", 1)),
            Map.entry("brutes_key", Map.of("minecraft:efficiency", 4, "minecraft:unbreaking", 1)),
            Map.entry("lash_of_the_crumbling_front", Map.of("minecraft:power", 4, "minecraft:punch", 2, "minecraft:infinity", 1)),
            Map.entry("fibril", Map.of("minecraft:power", 4, "minecraft:infinity", 1)),
            Map.entry("triptych", Map.of("minecraft:multishot", 1, "minecraft:quick_charge", 2)),
            Map.entry("web_cautery", Map.of("minecraft:bane_of_arthropods", 4, "minecraft:fire_aspect", 1)),
            Map.entry("can_opener", Map.of("minecraft:wind_burst", 1, "minecraft:breach", 2, "minecraft:fire_aspect", 1)),
            Map.entry("skeleton_key", Map.of("minecraft:efficiency", 3)),
            Map.entry("dead_reckoning", Map.of("minecraft:power", 3, "minecraft:flame", 1)),
            Map.entry("loophole", Map.of("minecraft:power", 2, "minecraft:punch", 1, "minecraft:infinity", 1)),
            Map.entry("traitors_enfilade", Map.of("minecraft:piercing", 2, "minecraft:quick_charge", 1)),
            Map.entry("cold_comfort", Map.of("minecraft:smite", 3, "minecraft:sweeping_edge", 1, "minecraft:knockback", 1)),
            Map.entry("the_adjuster", Map.of("minecraft:efficiency", 2)),
            Map.entry("limb_lopper", Map.of("minecraft:sharpness", 1, "minecraft:efficiency", 2, "minecraft:unbreaking", 1)),
            Map.entry("severance_pay", Map.of("minecraft:sharpness", 2, "minecraft:efficiency", 2)),
            Map.entry("second_thought", Map.of("minecraft:fire_aspect", 2, "minecraft:knockback", 1, "minecraft:unbreaking", 1)));
    private D1LootSignatures() {}
    public static Map<String,Integer> expected(String id) { return id == null ? null : ENCHANTMENTS.get(id); }
    public static boolean matches(String id, String item, Map<String,Integer> enchantments) {
        var entry = D1LootCatalog.find(id);
        return entry != null && entry.baseItem().equals(item) && expected(id).equals(enchantments);
    }
    // TODO(M72/M103, authored drop binding): the source specifies 6 bosses/12 entries with a
    // combined 10% per boss and 10 standard spawners/11 entries at 1% per mob (4-5 mobs each).
    // It does NOT identify each placed spawner UUID, template or preset-to-item assignment.
    // Verify those stable identities in TEST before any adoption. Keep independent rolls and
    // the 1.04-1.15 expected total; do not invent a guaranteed drop or hard instance cap.
    // Future configured chances belong in CosmicDungeon.config; prices already live in
    // all_vendors_prices.config. No world scan, reroll, spawner replacement or saved-data edit here.
}
