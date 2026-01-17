package net.goui.cosmicdungeon.playerclass.api;

import java.util.List;
import java.util.Set;

/**
 * Canonical string constants for class IDs.
 * Keep all hard-coded IDs here so they stay consistent across NBT, packets, etc.
 */
public final class ClassKeys {
    private ClassKeys() {}

    // Base "no class" value
    public static final String CLASS_ID_NONE = "none";

    // The 8 playable classes
    public static final String CLASS_ID_BOGATYR     = "bogatyr";
    public static final String CLASS_ID_DEADEYE     = "deadeye";
    public static final String CLASS_ID_DRAGOON     = "dragoon";
    public static final String CLASS_ID_JUDICATOR   = "judicator";
    public static final String CLASS_ID_METALMANCER = "metalmancer";
    public static final String CLASS_ID_PYROCLAST   = "pyroclast";
    public static final String CLASS_ID_THEURGIST   = "theurgist";
    public static final String CLASS_ID_VENEFEX     = "venefex";

    /** Ordered list for UI. */
    public static final List<String> ORDERED = List.of(
            CLASS_ID_NONE,
            CLASS_ID_BOGATYR,
            CLASS_ID_DEADEYE,
            CLASS_ID_DRAGOON,
            CLASS_ID_JUDICATOR,
            CLASS_ID_METALMANCER,
            CLASS_ID_PYROCLAST,
            CLASS_ID_THEURGIST,
            CLASS_ID_VENEFEX
    );

    /** Fast membership check. */
    private static final Set<String> KNOWN = Set.copyOf(ORDERED);

    /** Clamp a potentially-invalid / hacked / typo class id to a known value. */
    public static String clamp(String classId) {
        if (classId == null || classId.isBlank()) return CLASS_ID_NONE;
        return KNOWN.contains(classId) ? classId : CLASS_ID_NONE;
    }
}
