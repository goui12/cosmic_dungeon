package net.goui.cosmicdungeon.playerclass.api;

import java.util.Set;

/**
 * Central allow-list for all class IDs.
 * This is used to clamp inputs and prevent unrecognized IDs.
 */
public final class DungeoneerClassRegistry {
    private DungeoneerClassRegistry() {}

    private static final Set<String> ALL = Set.of(
            DungeoneerClassIds.NONE,
            DungeoneerClassIds.METALMANCER,
            DungeoneerClassIds.JUDICATOR,
            DungeoneerClassIds.DRAGOON,
            DungeoneerClassIds.DEADEYE,
            DungeoneerClassIds.PYROCLAST,
            DungeoneerClassIds.THEURGIST,
            DungeoneerClassIds.VENEFEX,
            DungeoneerClassIds.BOGATYR
    );

    public static boolean isValid(String id) {
        return id != null && ALL.contains(id);
    }

    public static String clamp(String id) {
        if (id == null) return DungeoneerClassIds.NONE;
        return isValid(id) ? id : DungeoneerClassIds.NONE;
    }

    public static Set<String> all() {
        return ALL;
    }
}
