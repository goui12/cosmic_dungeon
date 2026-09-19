package net.goui.cosmicdungeon.transaction;

/** One player's inventory removal and receipt must share ONE authoritative save snapshot.
 * Never use this protocol for payments/escrow spread across multiple files or owners. */
public final class SingleItemCommit {
    private SingleItemCommit() {}
    public enum Result { COMMITTED, RECOVERY_REQUIRED }
    public interface Storage { boolean saveAndVerify() throws Exception; }
    public static Result finish(Storage storage, Runnable award) {
        try {
            if (!storage.saveAndVerify()) return Result.RECOVERY_REQUIRED;
        } catch (Exception failure) {
            return Result.RECOVERY_REQUIRED;
        }
        // A crash here leaves a committed receipt. Login projects the award; never consumes again.
        award.run();
        return Result.COMMITTED;
    }
}
