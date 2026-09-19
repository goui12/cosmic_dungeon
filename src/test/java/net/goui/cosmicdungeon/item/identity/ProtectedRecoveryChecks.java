package net.goui.cosmicdungeon.item.identity;

import net.minecraft.nbt.*;

import static net.goui.cosmicdungeon.item.identity.ProtectedRecoveryRules.End.*;

public final class ProtectedRecoveryChecks {
    private static int checks;
    private static void check(boolean ok, String label) { checks++; if (!ok) throw new AssertionError(label); }
    public static void main(String[] args) {
        check(ProtectedRecoveryRules.scope(0, false) == 0, "Outside player belongings");
        check(ProtectedRecoveryRules.scope(42, false) == 42, "Dungeon inventory tied to its instance");
        check(ProtectedRecoveryRules.scope(42, true) == 0, "Village escrow is outside even while roster tracks run");
        for (long saved : new long[]{-1, 0, 42, 99}) {
            for (long current : new long[]{0, 42, 99}) {
                check(ProtectedRecoveryRules.claimable(saved, current, false) == (saved >= 0 && saved == current),
                        "No cross-inventory or stale-run claims " + saved + "/" + current);
                check(!ProtectedRecoveryRules.claimable(saved, current, true), "Pending reset blocks every claim");
            }
        }
        check(ProtectedRecoveryRules.finish(42, 42, true) == RELEASE_OUTSIDE, "Successful run retains overflow");
        check(ProtectedRecoveryRules.finish(42, 42, false) == DISCARD, "Failure removes dungeon-issued overflow");
        check(ProtectedRecoveryRules.finish(0, 42, false) == KEEP, "Failure preserves Village/outside belongings");
        check(ProtectedRecoveryRules.finish(0, 42, true) == KEEP, "Success preserves existing outside stash");
        check(ProtectedRecoveryRules.finish(99, 42, false) == KEEP, "Another run's state cannot be discarded");
        check(ProtectedRecoveryRules.finish(-1, 42, false) == KEEP, "Malformed scope preserved for diagnosis");
        check(ProtectedRecoveryRules.finish(0, 0, false) == KEEP, "Invalid reset cannot erase outside items");
        check(ProtectedRecoveryRules.finish(42, 0, true) == KEEP, "Invalid completion cannot release old run items");
        long releasedScope = 0;
        check(ProtectedRecoveryRules.finish(releasedScope, 42, true) == KEEP, "Repeated completion is idempotent");
        check(!ProtectedRecoveryRules.claimable(releasedScope, 43, false), "Completed loot cannot enter later run through recovery");
        var components = new CompoundTag(); components.putString("minecraft:custom_name", "Dad's authored gear");
        components.putInt("minecraft:damage", 17);
        var item = new CompoundTag(); item.putString("id", "minecraft:iron_ingot"); item.putInt("count", 64);
        item.put("components", components);
        var entry = new CompoundTag(); entry.put("item", item); entry.putLong("run", 42);
        for (int remaining = 1; remaining <= 64; remaining++) {
            var partial = ProtectedRecoveryEntries.remainder(entry, remaining);
            check(partial.getCompoundOrEmpty("item").getIntOr("count", 0) == remaining, "Exact partial quantity " + remaining);
            check(partial.getCompoundOrEmpty("item").getCompoundOrEmpty("components").equals(components),
                    "Every authored component survives " + remaining);
        }
        check(entry.getCompoundOrEmpty("item").getIntOr("count", 0) == 64, "Original image was not mutated");
        for (int bad : new int[]{-1, 0, 65}) {
            boolean rejected = false;
            try { ProtectedRecoveryEntries.remainder(entry, bad); } catch (IllegalArgumentException expected) { rejected = true; }
            check(rejected, "Impossible remainder rejected before mutation " + bad);
        }
        var outside = entry.copy(); outside.putLong("run", 0);
        var future = new CompoundTag(); future.putString("unknown", "preserve me");
        var pending = new CompoundTag(); pending.put("run-item", entry); pending.put("outside-item", outside);
        pending.put("future", future);
        check(ProtectedRecoveryEntries.hasClaimable(pending, 42), "Run stash blocks additional protected lifting");
        check(ProtectedRecoveryEntries.hasClaimable(pending, 0), "Outside stash is claimable outside");
        check(!ProtectedRecoveryEntries.hasClaimable(pending, 99), "Old outside/run stash does not block a new dungeon loadout");
        var failed = ProtectedRecoveryEntries.finish(pending, 42, false);
        check(!failed.contains("run-item") && failed.getCompoundOrEmpty("outside-item").equals(outside), "Failure removes only current run");
        check(failed.getCompoundOrEmpty("future").equals(future), "Malformed/future record is retained");
        check(pending.contains("run-item"), "Finish operates on a copy");
        var success = ProtectedRecoveryEntries.finish(pending, 42, true);
        check(success.getCompoundOrEmpty("run-item").getLongOr("run", -1) == 0, "Success releases into outside context");
        check(success.getCompoundOrEmpty("run-item").getCompoundOrEmpty("item").equals(item), "Success preserves full item image");
        check(ProtectedRecoveryEntries.finish(success, 42, true).equals(success), "Repeated finish cannot duplicate records");
        check(ProtectedRecoveryEntries.finish(success, 42, false).equals(success), "Late repeated cleanup cannot erase released recovery");
        check(ProtectedRecoveryEntries.finish(pending, 99, false).equals(pending), "Wrong run leaves every record byte-equivalent");
        System.out.println(checks + " protected recovery checks passed");
    }
}
