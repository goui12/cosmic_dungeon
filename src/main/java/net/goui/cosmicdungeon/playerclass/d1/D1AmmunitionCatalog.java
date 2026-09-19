package net.goui.cosmicdungeon.playerclass.d1;

import java.util.*;

/**
 * D1 identity signatures, not combat modifiers (those stay in CosmicDungeon.config).
 * Sources: Theurgist 1l9ox2pQUSPy0_J3h7ljPOaVOFtMFkoHq_rFSGK4iqeM (2026-04-05),
 * Judicator 1cY_czWEYbUEg_EQmaSANhOFe326gTDVKfL9XaEFGmQo and Venefex
 * 1JXqPdwWxateRMGpAuoV1ub8asNL7iMeyrBtzqTuUwm8 (2026-04-04), Pyroclast
 * 16FD3wxi-Uen_DRzItDHdSrSZvkGNwa_r-ZeUYiswxoE (2026-03-26).
 */
public final class D1AmmunitionCatalog {
    public record Entry(String id, String name, String baseItem, String potionFamily, int stars,
                        Set<String> classes) {
        public Entry { classes = Set.copyOf(classes); }
        public boolean matches(Facts facts) {
            if (!baseItem.equals(facts.item())) return false;
            if (stars > 0) return stars == facts.stars() && !facts.customEffects();
            if (potionFamily.isEmpty()) return !facts.customEffects() && facts.potion().isEmpty();
            // Retain previously supported authored vanilla family variants; the configured
            // ability defines power/duration. Do not rewrite a player's potion component.
            return !facts.customEffects() && Set.of("minecraft:" + potionFamily,
                    "minecraft:long_" + potionFamily, "minecraft:strong_" + potionFamily).contains(facts.potion());
        }
    }
    /** Null marker means absent. Empty/unknown markers are explicit evidence and cannot fall back. */
    public record Facts(String item, String marker, String name, String potion, boolean customEffects, int stars) {}
    private static final List<Entry> ENTRIES = List.of(
            arrow("mending_sting", "Mending Sting", "regeneration", "theurgist"),
            arrow("verdant_jolt", "Verdant Jolt", "regeneration", "theurgist"),
            arrow("scintilla_vitalis", "Scintilla Vitalis", "healing", "theurgist", "judicator"),
            arrow("lux_vitalis", "Lux Vitalis", "healing", "theurgist"),
            arrow("ebonsight", "Ebonsight", "night_vision", "judicator"),
            new Entry("vielpiercer", "Vielpiercer", "minecraft:spectral_arrow", "", 0, Set.of("judicator")),
            arrow("tree_viper", "Venom of the Tree Viper", "poison", "venefex"),
            arrow("pestis", "Arrow of Pestis", "weakness", "venefex"),
            arrow("vapours", "Arrow of Vapours", "slowness", "venefex"),
            arrow("spicule_breach", "Spicule Breach", "harming", "venefex"),
            arrow("bushmaster", "Venom of the Bushmaster", "poison", "venefex"),
            arrow("fer_de_lance", "Venom of the Fer-de-Lance", "poison", "venefex"),
            arrow("black_bubo", "Arrow of the Black Bubo", "weakness", "venefex"),
            arrow("melancholia", "Scytel of Melancholia", "slowness", "venefex"),
            arrow("deathly_stupor", "Bodkin of Deathly Stupor", "slowness", "venefex"),
            arrow("spicule_rend", "Spicule Rend", "harming", "venefex"),
            new Entry("cinderbite", "Cinderbite", "minecraft:firework_rocket", "", 4, Set.of("pyroclast")),
            new Entry("cindermaul", "Cindermaul", "minecraft:firework_rocket", "", 5, Set.of("pyroclast")));
    private static final Map<String, Entry> BY_ID = new LinkedHashMap<>();
    private static final Map<String, String> ALIASES = new HashMap<>();
    // Only these four existing registered items intrinsically identify D1 ammunition.
    // Their unmodified registered TippedArrowItem defaults need no potion component.
    private static final Set<String> REGISTERED = Set.of("vielpiercer", "scintilla_vitalis", "lux_vitalis", "ebonsight");
    static {
        for (var e : ENTRIES) {
            BY_ID.put(e.id(), e); alias(e.id(), e.id()); alias(e.name(), e.id());
        }
        alias("Arrow of the Vapours", "vapours"); alias("Arrow of Black Bubo", "black_bubo");
        alias("Arrow of Melancholia", "melancholia"); alias("Arrow of Deathly Stupor", "deathly_stupor");
        alias("Spicule of Breach", "spicule_breach"); alias("Spicule of Rend", "spicule_rend");
        alias("Arrow of Mending Sting", "mending_sting"); alias("Arrow of Verdant Jolt", "verdant_jolt");
        alias("Veilpiercer", "vielpiercer");
    }
    private D1AmmunitionCatalog() {}
    private static Entry arrow(String id, String name, String potion, String... classes) {
        return new Entry(id, name, "minecraft:tipped_arrow", potion, 0, Set.of(classes));
    }
    private static String normalize(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
    private static void alias(String name, String id) { ALIASES.put(normalize(name), id); }
    public static Entry find(String id) { return id == null ? null : BY_ID.get(id); }
    public static List<Entry> entries() { return ENTRIES; }
    public static Set<String> ids() { return Collections.unmodifiableSet(BY_ID.keySet()); }
    public static boolean candidate(String item) {
        return item.equals("minecraft:tipped_arrow") || item.equals("minecraft:spectral_arrow")
                || item.equals("minecraft:firework_rocket")
                || item.startsWith("cosmicdungeon:") && REGISTERED.contains(item.substring("cosmicdungeon:".length()));
    }
    public static String identify(Facts facts) {
        if (!candidate(facts.item())) return null;
        String registered = facts.item().startsWith("cosmicdungeon:")
                ? facts.item().substring("cosmicdungeon:".length()) : "";
        if (REGISTERED.contains(registered))
            return facts.marker() == null || registered.equals(facts.marker()) ? registered : null;
        String id = facts.marker() != null ? facts.marker() : ALIASES.get(normalize(facts.name()));
        var entry = find(id);
        return entry != null && entry.matches(facts) ? id : null;
    }
    public static boolean canAdopt(Facts facts, String id) {
        var entry = find(id);
        String existing = identify(facts);
        return facts.marker() == null && entry != null && entry.matches(facts)
                && (existing == null || existing.equals(id));
    }
    public static boolean bindingAllowed(String id, boolean present, boolean complete, String classId,
                                         Integer dungeon, Integer tier) {
        var entry = find(id);
        return entry != null && (!present || complete && classId != null && entry.classes().contains(classId)
                && Integer.valueOf(1).equals(dungeon) && (Integer.valueOf(3).equals(tier) || Integer.valueOf(4).equals(tier)));
    }
    // TODO(M55/M63/M64-M69/M72, authored TEST inventory): Q&A D79 prohibits rewriting chest
    // contents. Verify actual template stacks against docs/ai/D1_BATCH_33_MAPPINGS.md without
    // changing names, quantities or enchantments. Pyro chest doc 1CQTFJrQyW8YNvU9pIaJZcFEHGSTrVjA7jQS0aTTYMNg
    // (2026-04-04) contains Cinderkiss/Cinderbight but supplies no approved payload equivalence;
    // do not invent aliases. Judicator chest Lux does not establish a Judicator healing ability.
    // D2+ Gusting Bolt/conduits/rockets stay deferred; do not infer them from a shared potion.
}
