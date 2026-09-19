package net.goui.cosmicdungeon.playerclass.bogatyr;

import java.util.Collection;

/** Wolf (Internal), 2026-04-25. Pure feeding and saved-roster decisions. */
public final class WolfCareRules {
    private WolfCareRules() {}
    /** Fraction of remaining juvenile age, rounded up to one server tick; never adds adulthood cooldown. */
    public static int growthTicks(int age, double fraction) {
        if (age >= 0 || !Double.isFinite(fraction) || fraction <= 0) return 0;
        long remaining = -(long) age;
        return (int)Math.min(Integer.MAX_VALUE, Math.min(remaining, (long)Math.ceil(remaining * Math.min(1, fraction))));
    }
    public static boolean healthy(float health, float maximum) {
        return Float.isFinite(health) && Float.isFinite(maximum) && maximum > 0 && health >= maximum;
    }
    /** Count saved UUIDs, including unloaded companions. Lowering the cap never deletes existing pets. */
    public static boolean hasRoom(Collection<String> roster, int cap) {
        return roster.size() < Math.max(0, cap);
    }
}
