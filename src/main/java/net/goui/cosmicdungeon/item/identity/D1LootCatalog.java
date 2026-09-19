package net.goui.cosmicdungeon.item.identity;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** D1 only. Source 1fX1UbC6cG_cnN2auDo_1ascDY24yjFy9pIfL4qe3-_g, 2026-08-28.
 * Identities describe authored vanilla stacks; this class never manufactures or renames equipment.
 */
public final class D1LootCatalog {
    public record Entry(String id, String name, String baseItem, long defaultPurchaseTrace) {}
    private static final List<Entry> ENTRIES = List.of(
            new Entry("ranseur_of_the_fallen_dragoon", "Ranseur of the Fallen Dragoon", "minecraft:trident", 779L),
            new Entry("recovered_spyglass", "Recovered Spyglass", "minecraft:spyglass", 900L),
            new Entry("perforated_chestplate", "Perforated Chestplate", "minecraft:diamond_chestplate", 976L),
            new Entry("salvaged_leggings", "Salvaged Leggings", "minecraft:diamond_leggings", 951L),
            new Entry("discarded_helm", "Discarded Helm", "minecraft:diamond_helmet", 930L),
            new Entry("resoled_boots", "Resoled Boots", "minecraft:diamond_boots", 910L),
            new Entry("last_resort", "Last Resort", "minecraft:diamond_sword", 676L),
            new Entry("squared_mallet", "Squared Mallet", "minecraft:mace", 717L),
            new Entry("brutes_key", "Brute's Key", "minecraft:diamond_pickaxe", 848L),
            new Entry("lash_of_the_crumbling_front", "Lash of the Crumbling Front", "minecraft:bow", 348L),
            new Entry("fibril", "Fibril", "minecraft:bow", 332L),
            new Entry("triptych", "Triptych", "minecraft:crossbow", 406L),
            new Entry("web_cautery", "Web Cautery", "minecraft:diamond_sword", 651L),
            new Entry("can_opener", "Can Opener", "minecraft:mace", 666L),
            new Entry("skeleton_key", "Skeleton Key", "minecraft:iron_pickaxe", 695L),
            new Entry("dead_reckoning", "Dead Reckoning", "minecraft:bow", 300L),
            new Entry("loophole", "Loophole", "minecraft:bow", 294L),
            new Entry("traitors_enfilade", "Traitor's Enfilade", "minecraft:crossbow", 415L),
            new Entry("cold_comfort", "Cold Comfort", "minecraft:golden_sword", 267L),
            new Entry("the_adjuster", "The Adjuster", "minecraft:golden_pickaxe", 230L),
            new Entry("limb_lopper", "Limb Lopper", "minecraft:golden_axe", 283L),
            new Entry("severance_pay", "Severance Pay", "minecraft:golden_axe", 280L),
            new Entry("second_thought", "Second Thought", "minecraft:golden_sword", 234L)
    );
    private static final Map<String, Entry> BY_ID = ENTRIES.stream()
            .collect(Collectors.toUnmodifiableMap(Entry::id, Function.identity()));
    private D1LootCatalog() {}
    public static List<Entry> entries() { return ENTRIES; }
    public static Entry find(String id) { return id == null ? null : BY_ID.get(id); }
    public static Set<String> ids() { return BY_ID.keySet(); }
}
