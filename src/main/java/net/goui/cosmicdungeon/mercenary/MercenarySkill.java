package net.goui.cosmicdungeon.mercenary;

import java.util.List;

/** The requested independent mercenary skills; player weapon progression is separate. */
public enum MercenarySkill {
    POSITIVE_POTIONS("positive_potions", "theurgist", "Positive Potions"),
    NEGATIVE_POTIONS("negative_potions", "theurgist", "Negative Potions"),
    WOLVES("wolves", "bogatyr", "Wolves"),
    FIREWORKS("fireworks", "pyroclast", "Fireworks"),
    CHAIN_LIGHTNING("chain_lightning", "dragoon", "Chain Lightning");

    private final String id, classId, title;
    MercenarySkill(String id, String classId, String title) {
        this.id=id; this.classId=classId; this.title=title;
    }
    public String id() { return id; }
    public String title() { return title; }
    public boolean supports(MercenaryContract contract) {
        return contract!=null && classId.equals(contract.classId());
    }
    public static List<MercenarySkill> forContract(MercenaryContract contract) {
        return java.util.Arrays.stream(values()).filter(s->s.supports(contract)).toList();
    }
    public static MercenarySkill fromId(String id) {
        for (var skill:values()) if (skill.id.equals(id)) return skill;
        throw new IllegalArgumentException("Unknown mercenary skill");
    }
    /** Level 1 needs zero successes; level 5 needs 1+2+3+4=10. */
    public static long threshold(int level) {
        if (level<1 || level>65537) throw new IllegalArgumentException("Invalid mercenary level");
        return (long)level*(level-1)/2;
    }
    public static int level(int successes) {
        if (successes<0) throw new IllegalArgumentException("Negative mercenary progress");
        int low=1, high=65537;
        while (low+1<high) {
            int middle=(low+high)>>>1;
            if (threshold(middle)<=successes) low=middle; else high=middle;
        }
        return low;
    }
    public static int advance(int successes) {
        if (successes<0) throw new IllegalArgumentException("Negative mercenary progress");
        return successes==Integer.MAX_VALUE ? successes : successes+1;
    }
    public static String description(String id, int successes) {
        var skill=fromId(id);
        int level=level(successes);
        return skill.title()+" - Level "+level+" ("+(successes-threshold(level))+"/"+level+")";
    }
}
