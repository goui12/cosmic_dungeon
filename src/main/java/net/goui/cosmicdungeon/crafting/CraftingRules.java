package net.goui.cosmicdungeon.crafting;

import java.util.*;
import java.util.regex.Pattern;

/** Immutable policy: recipe IDs authorize production, never item display names. */
public record CraftingRules(boolean enabled, Map<String, Set<String>> players, Set<String> automation) {
    public static final List<String> DEFAULT_PLAYERS = List.of(
            "minecraft:fermented_spider_eye|theurgist", "minecraft:magma_cream|theurgist",
            "minecraft:glistering_melon_slice|theurgist");
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern CLASS = Pattern.compile("[a-z0-9_]+");
    public CraftingRules {
        Map<String, Set<String>> copy = new HashMap<>();
        players.forEach((id, classes) -> copy.put(id, Set.copyOf(classes)));
        players = Map.copyOf(copy); automation = Set.copyOf(automation);
    }
    public static boolean recipeEntry(Object value) {
        return value instanceof String text && text.length() <= 256 && ID.matcher(text).matches();
    }
    public static boolean playerEntry(Object value) {
        if (!(value instanceof String text)) return false;
        String[] parts = text.split("\\|", -1);
        return parts.length == 2 && recipeEntry(parts[0]) && parts[1].length() <= 64
                && (parts[1].equals("*") || CLASS.matcher(parts[1]).matches());
    }
    public static CraftingRules parse(boolean enabled, List<? extends String> players, List<? extends String> automated) {
        if (players.size() > 4096 || automated.size() > 4096)
            throw new IllegalArgumentException("Crafting allowlists are limited to 4096 entries each");
        Map<String, Set<String>> parsed = new HashMap<>();
        for (String entry : players) {
            if (!playerEntry(entry)) throw new IllegalArgumentException("Invalid player recipe entry: " + entry);
            String[] parts = entry.split("\\|");
            parsed.computeIfAbsent(parts[0], key -> new HashSet<>()).add(parts[1]);
        }
        Set<String> machines = new HashSet<>();
        for (String entry : automated) {
            if (!recipeEntry(entry)) throw new IllegalArgumentException("Invalid automated recipe ID: " + entry);
            machines.add(entry);
        }
        return new CraftingRules(enabled, parsed, machines);
    }
    public boolean player(String id, String classId) {
        var classes = players.getOrDefault(id, Set.of());
        return !enabled || classes.contains("*") || (classId != null && classes.contains(classId));
    }
    public boolean automated(String id) { return !enabled || automation.contains(id); }
    public boolean known(String id) { return !enabled || players.containsKey(id) || automation.contains(id); }
}
