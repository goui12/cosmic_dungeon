package net.goui.cosmicdungeon.dungeon;

/** Facts are collected again on the server immediately before ordinary travel.
 * Journaled entry, Chop inventory swaps and cleanup own their separate transitions. */
public final class DungeonTravelRules {
    private DungeonTravelRules() {}
    public record Member(long runId, boolean active, boolean member, boolean exited,
                         boolean starting, boolean sealed, boolean outsideInventory, boolean inside,
                         boolean alive, boolean spectator, boolean developer, boolean blocked, boolean eligibleClass) {}
    public static boolean inside(Member f) {
        return f != null && f.runId > 0 && f.active && f.member && !f.exited && !f.starting
                && !f.sealed && !f.outsideInventory && f.inside && f.alive && !f.spectator
                && !f.developer && !f.blocked && f.eligibleClass;
    }
    public static boolean riftBoundary(boolean insideD1, boolean dungeonTarget, boolean reset, boolean developer) {
        return developer || !insideD1 || (dungeonTarget ? !reset : reset);
    }
    public static boolean village(boolean namedVillage, boolean unlocked, boolean developer) {
        return !namedVillage || unlocked || developer;
    }
    public static boolean villageName(String name) {
        return name != null && switch (name.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "main_village", "village", "main village" -> true;
            default -> false;
        };
    }
}
