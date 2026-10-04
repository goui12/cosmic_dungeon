package net.goui.cosmicdungeon.playerclass.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Map;

/** Bounded, independent class skills; balance defaults are delegated, not lore. */
public final class ClassSkillRules {
    private ClassSkillRules() {}
    public static final Map<String, Double> DAMAGE = Map.ofEntries(
            Map.entry("bogatyr.sword", .25), Map.entry("bogatyr.bow", .20),
            Map.entry("dragoon.trident", .20), Map.entry("judicator.mace", .25),
            Map.entry("judicator.bow", .20), Map.entry("pyroclast.sword", .25),
            Map.entry("pyroclast.bow", .20), Map.entry("pyroclast.crossbow", .15),
            Map.entry("theurgist.mace", .25), Map.entry("theurgist.bow", .20),
            Map.entry("theurgist.potions", .25), Map.entry("venefex.sword", .25),
            Map.entry("venefex.bow", .15), Map.entry("deadeye.sword", .25),
            Map.entry("deadeye.bow", .20), Map.entry("metalmancer.resonance", .20));
    public static final Codec<Map<String, Integer>> CODEC = Codec.unboundedMap(Codec.STRING,
            Codec.intRange(0, Integer.MAX_VALUE)).validate(values ->
            values.size() <= DAMAGE.size() && DAMAGE.keySet().containsAll(values.keySet())
                    ? DataResult.success(Map.copyOf(values)) : DataResult.error(() -> "Unknown class skill"));
    public static boolean known(String cls, String skill) { return DAMAGE.containsKey(cls + "." + skill); }
    public static boolean melee(String skill) {
        return skill.equals("sword") || skill.equals("mace") || skill.equals("trident");
    }
    public static long threshold(int level, int base, int step) {
        if (level < 0 || level > 100 || base < 1 || step < 0) throw new IllegalArgumentException("Invalid skill curve");
        return (long) base * level + (long) step * level * (level - 1) / 2;
    }
    public static int level(int xp, int cap, int base, int step) {
        int result = 0;
        while (result < cap && threshold(result + 1, base, step) <= xp) result++;
        return result;
    }
    public static int addXp(int old, int amount, long cap) {
        if (old < 0 || amount < 0 || cap < 0) throw new IllegalArgumentException("Negative skill XP");
        // Lowered balance caps suspend earning; never erase already earned progress.
        return (int) Math.max(old, Math.min(Integer.MAX_VALUE, Math.min(cap, (long) old + amount)));
    }
    public static double bonus(int level, int cap, double maximum) {
        return maximum * Math.clamp(level / (double) Math.max(1, cap), 0, 1);
    }
    public static int budget(int spent, int requested, int cap) {
        return Math.min(Math.max(0, requested), Math.max(0, cap - Math.max(0, spent)));
    }
    public static boolean usefulBuff(boolean beneficial, Integer previousAmplifier, int incomingAmplifier) {
        return beneficial && (previousAmplifier == null || incomingAmplifier > previousAmplifier);
    }
    public static boolean cooldownReady(long now, long previous, int interval) {
        return previous < 0 || now >= previous && now - previous >= interval;
    }
}
