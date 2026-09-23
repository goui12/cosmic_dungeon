package net.goui.cosmicdungeon.economy;

import net.minecraft.nbt.CompoundTag;
import java.math.BigInteger;
import java.util.List;

/** Bounded recent UTC-day summaries; full immutable history remains in ledger pages. */
public final class EconomyReports {
    public static final int RECENT_DAYS = 32;
    private EconomyReports() {}

    private static void add(CompoundTag target, String key, long amount) {
        target.putString(key, new BigInteger(target.getStringOr(key, "0")).add(BigInteger.valueOf(amount)).toString());
    }

    public static CompoundTag daily(CompoundTag previous, CompoundTag row, long adjustment) {
        var days = previous.copy();
        long day = Math.floorDiv(row.getLongOr("timestamp", 0), 86_400_000L);
        long newest = Math.max(day, days.keySet().stream().mapToLong(Long::parseLong).max().orElse(day));
        // A clock rollback never removes newer evidence or creates another daily reporting window.
        if (day < newest - RECENT_DAYS + 1) return days;
        String key = Long.toString(day);
        var summary = days.getCompoundOrEmpty(key).copy();
        String category = row.getStringOr("category", "");
        add(summary, category, adjustment);
        var byType = summary.getCompoundOrEmpty("by_type").copy();
        String type = row.getStringOr("type", "");
        var activity = byType.getCompoundOrEmpty(type).copy();
        boolean committed = row.getStringOr("status", "").equals(AccountTransfer.COMMITTED);
        add(activity, committed ? "committed_rows" : "attempt_rows", 1);
        add(activity, "trace", adjustment);
        byType.put(type, activity);
        summary.put("by_type", byType);
        add(summary, "cap_rejected_trace", row.getCompoundOrEmpty("details").getLongOr("cap_rejected_trace", 0));
        days.put(key, summary);
        for (String old : List.copyOf(days.keySet())) if (Long.parseLong(old) < newest - RECENT_DAYS + 1) days.remove(old);
        return days;
    }

    public static CompoundTag finalReviews(CompoundTag previous, CompoundTag row, long maximum) {
        var reviews = previous;
        long before = row.getLongOr("before", 0), after = row.getLongOr("after", 0);
        if (after > before && before < maximum && after >= maximum
                && row.getStringOr("status", "").equals(AccountTransfer.COMMITTED)) {
            String owner = row.getStringOr("player", "");
            if (!reviews.contains(owner)) {
                reviews = previous.copy(); var evidence = row.copy(); evidence.remove("details"); reviews.put(owner, evidence);
            }
        }
        return reviews;
    }
}
