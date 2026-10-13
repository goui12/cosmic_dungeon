package net.goui.cosmicdungeon.economy;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Explicit supported rewards; F6 additions approved by Cameron on 2026-10-13. */
public final class D1MobRewardCatalog {
    private static final List<String> LEGACY = List.of(
            "minecraft:zombie=common",
            "minecraft:skeleton=common",
            "minecraft:husk=tough",
            "minecraft:drowned=common",
            "minecraft:spider=common",
            "minecraft:cave_spider=weak",
            "minecraft:creeper=tough",
            "minecraft:stray=tough",
            "minecraft:bogged=tough",
            "minecraft:enderman=tough",
            "minecraft:witch=elite",
            "minecraft:silverfish=weak",
            "minecraft:blaze=tough",
            "minecraft:wither_skeleton=tough",
            "minecraft:ghast=elite",
            "minecraft:piglin=common",
            "minecraft:piglin_brute=elite",
            "minecraft:zombified_piglin=common",
            "minecraft:hoglin=tough",
            "minecraft:magma_cube=common",
            "minecraft:slime=weak",
            "minecraft:endermite=weak",
            "minecraft:guardian=tough",
            "minecraft:elder_guardian=miniboss",
            "minecraft:warden=boss",
            "minecraft:wither=boss",
            "minecraft:ender_dragon=boss");
    private static final List<String> APPROVED_ADDITIONS = List.of(
            "minecraft:vex=weak",
            "minecraft:zombie_villager=common",
            "minecraft:pillager=common",
            "minecraft:phantom=common",
            "cosmicdungeon:goblin_ambusher=common",
            "cosmicdungeon:cthonian_gnawling=common",
            "minecraft:shulker=tough",
            "minecraft:breeze=tough",
            "minecraft:vindicator=tough",
            "minecraft:zoglin=tough",
            "cosmicdungeon:magma_glob=tough",
            "cosmicdungeon:crystal_creeper=tough",
            "minecraft:creaking=elite",
            "minecraft:evoker=elite",
            "minecraft:illusioner=elite",
            "cosmicdungeon:stone_warden=elite",
            "minecraft:giant=miniboss",
            "minecraft:ravager=miniboss");
    private static final Map<String, String> ADDITIONS = parse(APPROVED_ADDITIONS);
    private static final Map<String, String> REGISTRATIONS;
    private static final List<String> DEFAULTS;
    static {
        Map<String, String> all = new LinkedHashMap<>(parse(LEGACY));
        all.putAll(ADDITIONS);
        REGISTRATIONS = Collections.unmodifiableMap(all);
        DEFAULTS = all.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toList();
    }

    private D1MobRewardCatalog() {}

    public static List<String> defaults() { return DEFAULTS; }
    public static Map<String, String> additions() { return ADDITIONS; }
    public static Map<String, String> registrations() { return REGISTRATIONS; }

    private static Map<String, String> parse(List<String> entries) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String entry : entries) {
            String[] pair = entry.split("=", 2);
            if (result.putIfAbsent(pair[0], pair[1]) != null)
                throw new IllegalStateException("Duplicate default reward: " + pair[0]);
        }
        return Collections.unmodifiableMap(result);
    }
}
