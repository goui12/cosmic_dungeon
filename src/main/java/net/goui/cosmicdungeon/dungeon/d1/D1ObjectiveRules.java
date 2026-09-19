package net.goui.cosmicdungeon.dungeon.d1;

import java.util.Map;

/** Pure rules, shared with offline regression checks. No Minecraft/client state. */
public final class D1ObjectiveRules {
    private D1ObjectiveRules() {}
    public static int strike(Map<String, Long> bells, String bell, long now, long windowTicks) {
        bells.entrySet().removeIf(e -> e.getValue() > now || now - e.getValue() > windowTicks);
        bells.put(bell, now);
        return bells.size();
    }
    public static int roundedLesserBlooms(int collected) {
        if (collected <= 0) return 0;
        long rounded = ((long) collected + 4L) / 5L * 5L;
        return (int) Math.min(Integer.MAX_VALUE, rounded);
    }
    public static long saturatingAdd(long total, long amount) {
        if (amount <= 0) return Math.max(0, total);
        return Long.MAX_VALUE - Math.max(0, total) < amount ? Long.MAX_VALUE : Math.max(0, total) + amount;
    }
}
