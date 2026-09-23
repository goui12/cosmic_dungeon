package net.goui.cosmicdungeon.achievement.d1;
import java.util.*;
/** One simultaneous sample of distinct eligible characters, never an accumulated run count. */
public final class PiglinDisguiseRules {
    private PiglinDisguiseRules() {}
    public record Character(UUID owner, boolean participant, boolean inCamp, boolean piglinHead) {}
    public static boolean qualifies(Collection<Character> sample, int required) {
        if (sample == null || required < 1 || required > 6) return false;
        var owners = new HashSet<UUID>();
        for (var c : sample) if (c != null && c.owner != null && c.participant && c.inCamp && c.piglinHead) owners.add(c.owner);
        return owners.size() >= required;
    }
}
