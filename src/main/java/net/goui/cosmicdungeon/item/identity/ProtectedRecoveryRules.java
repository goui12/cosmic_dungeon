package net.goui.cosmicdungeon.item.identity;

/** A recovery item belongs to an inventory context, not merely to an online player. */
public final class ProtectedRecoveryRules {
    private ProtectedRecoveryRules() {}
    public enum End { KEEP, RELEASE_OUTSIDE, DISCARD }
    public static long scope(long trackedRun, boolean outsideInventory) {
        return trackedRun > 0 && !outsideInventory ? trackedRun : 0;
    }
    public static boolean claimable(long savedScope, long currentScope, boolean resetPending) {
        return !resetPending && savedScope >= 0 && savedScope == currentScope;
    }
    public static End finish(long savedScope, long endingRun, boolean successfulD1) {
        if (endingRun <= 0 || savedScope != endingRun) return End.KEEP;
        return successfulD1 ? End.RELEASE_OUTSIDE : End.DISCARD;
    }
}
