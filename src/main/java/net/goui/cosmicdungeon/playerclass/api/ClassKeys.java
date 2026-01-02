package net.goui.cosmicdungeon.playerclass.api;

/**
 * Canonical string constants for class IDs.
 * Keep all hard-coded IDs here so they stay consistent across NBT, packets, etc.
 */
public final class ClassKeys {
    private ClassKeys() {}

    // Base "no class" value
    public static final String CLASS_ID_NONE = "none";

    // Classes
    public static final String CLASS_ID_METALMANCER = "metalmancer";

    // TODO: future classes
    // public static final String CLASS_ID_JUDICATOR = "judicator";
    // public static final String CLASS_ID_DRAGOON   = "dragoon";
}
