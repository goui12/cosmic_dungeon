package net.goui.cosmicdungeon.playerclass.api;

/**
 * Canonical IDs for all dungeoneer classes.
 * These strings are stored in player persistent data and sent over the network.
 */
public final class DungeoneerClassIds {
    private DungeoneerClassIds() {}

    public static final String NONE        = "none";

    public static final String METALMANCER = "metalmancer";
    public static final String JUDICATOR   = "judicator";
    public static final String DRAGOON     = "dragoon";
    public static final String DEADEYE     = "deadeye";     // if you want exact casing change now
    public static final String PYROCLAST   = "pyroclast";
    public static final String THEURGIST   = "theurgist";
    public static final String VENEFEX     = "venefex";
    public static final String BOGATYR     = "bogatyr";
}
