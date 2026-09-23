package net.goui.cosmicdungeon.achievement.d1;

/** D71/D74: instance members share credit. Death alone does not remove membership. */
public final class SharedAchievementRules {
    private SharedAchievementRules() {}
    public static boolean eligible(boolean member, boolean completedExit, boolean outsideInventory,
                                   boolean developer, boolean spectator, boolean online, boolean inside) {
        return member && !completedExit && !outsideInventory && !developer && !spectator && (!online || inside);
    }
}
