package net.goui.cosmicdungeon.region.quest;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class RegionQuestRegistry {
    private static final Map<String, RegionQuestHandler> HANDLERS = new LinkedHashMap<>();

    static {
        register(new PlantFlagsRegionQuestHandler());
    }

    private RegionQuestRegistry() {}

    public static void register(RegionQuestHandler handler) {
        if (handler == null) throw new IllegalArgumentException("Region quest handler cannot be null.");
        String name = normalize(handler.questName());
        if (name.isBlank()) throw new IllegalArgumentException("Region quest name cannot be blank.");
        if (name.contains(" ")) throw new IllegalArgumentException("Region quest name must be a single command word: " + name);
        RegionQuestHandler previous = HANDLERS.putIfAbsent(name, handler);
        if (previous != null) throw new IllegalArgumentException("Duplicate region quest handler: " + name);
    }

    public static Optional<RegionQuestHandler> get(String questName) {
        return Optional.ofNullable(HANDLERS.get(normalize(questName)));
    }

    public static Collection<String> questNames() {
        return HANDLERS.keySet();
    }

    public static String knownQuestList() {
        return String.join(", ", HANDLERS.keySet());
    }

    private static String normalize(String questName) {
        return questName == null ? "" : questName.toLowerCase(Locale.ROOT);
    }
}
