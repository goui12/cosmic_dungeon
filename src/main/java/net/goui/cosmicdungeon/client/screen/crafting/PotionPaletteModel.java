package net.goui.cosmicdungeon.client.screen.crafting;

import java.util.*;
import net.goui.cosmicdungeon.network.PotionCraftPayloads.Entry;

/** One tile per effect family; variants never invent an unavailable server recipe. */
public final class PotionPaletteModel {
    public enum Variant {
        NORMAL("I", "Normal potency"), STRONG("II", "Stronger potency"), EXTENDED("+", "Extended duration");
        public final String label, description;
        Variant(String label, String description) { this.label = label; this.description = description; }
    }
    private PotionPaletteModel() {}
    public static Variant variant(String id) {
        String path = id.substring(id.indexOf(':') + 1);
        return path.startsWith("strong_") ? Variant.STRONG : path.startsWith("long_") ? Variant.EXTENDED : Variant.NORMAL;
    }
    public static String family(String id) {
        int start = id.indexOf(':') + 1;
        return switch (variant(id)) {
            case STRONG -> id.substring(0, start) + id.substring(start + 7);
            case EXTENDED -> id.substring(0, start) + id.substring(start + 5);
            default -> id;
        };
    }
    public static List<Entry> visible(List<Entry> entries, Variant variant) {
        var families = new TreeMap<String, EnumMap<Variant, Entry>>();
        for (var entry : entries)
            families.computeIfAbsent(family(entry.targetId()), ignored -> new EnumMap<>(Variant.class))
                    .put(variant(entry.targetId()), entry);
        var result = new ArrayList<Entry>();
        for (var family : families.values()) {
            var selected = family.get(variant);
            if (selected == null) selected = family.get(Variant.NORMAL);
            if (selected == null) selected = family.values().iterator().next();
            result.add(selected);
        }
        return List.copyOf(result);
    }
    public static int count(Entry entry) { return entry.own() > 0 ? entry.own() : entry.shared(); }
    public static int color(Entry entry) { return entry.own() > 0 ? 0xFFFFFFFF : entry.shared() > 0 ? 0xFF66B6FF : 0xFFA51F2B; }
    public static int background(Entry entry) { return entry.own() > 0 ? 0xFF345D3A : entry.shared() > 0 ? 0xFF303B48 : 0xFFD6A0A4; }
    public static String source(Entry entry) {
        return entry.own() > 0 ? "This item can be crafted using materials in your own inventory."
                : entry.shared() > 0 ? "This item can be crafted using group shared materials in other players' inventories."
                : "Missing materials or brewing fuel. A red 0 cannot be crafted.";
    }
}
