package net.goui.cosmicdungeon.item.identity;

import java.util.Optional;
import java.util.regex.Pattern;

/** Versioned immutable value, written only by trusted server issuance/developer adoption.
 * Unknown strings remain stored by the component codec and fail closed at use; no item is deleted.
 */
public record ItemProvenance(String origin, String itemId, String baseItem) {
    public static final String LOOT = "dungeon_loot", RETAIL = "vendor_retail";
    private static final Pattern BASE = Pattern.compile("minecraft:[a-z0-9_/]+");
    public ItemProvenance {
        if ((!LOOT.equals(origin) && !RETAIL.equals(origin)) || itemId == null
                || baseItem == null || !BASE.matcher(baseItem).matches())
            throw new IllegalArgumentException("Invalid item provenance");
        if (!itemId.isEmpty()) {
            var entry = D1LootCatalog.find(itemId);
            if (!LOOT.equals(origin) || entry == null || !entry.baseItem().equals(baseItem))
                throw new IllegalArgumentException("Unknown named identity or mismatched vanilla item");
        }
    }
    public String encode() { return "1|" + origin + "|" + itemId + "|" + baseItem; }
    public boolean matches(String actualItem) { return baseItem.equals(actualItem); }
    public static Optional<ItemProvenance> parse(String raw) {
        if (raw == null || raw.length() > 192) return Optional.empty();
        var fields = raw.split("\\|", -1);
        if (fields.length != 4 || !fields[0].equals("1")) return Optional.empty();
        try { return Optional.of(new ItemProvenance(fields[1], fields[2], fields[3])); }
        catch (IllegalArgumentException malformed) { return Optional.empty(); }
    }
}
