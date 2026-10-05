package net.goui.cosmicdungeon.mercenary;

/** Bounded cadence and quality; skills can grow without creating a tick/particle flood. */
public final class MercenaryPotionBalance {
    private MercenaryPotionBalance() {}
    public static double qualityChance(int level) {
        int value=Math.max(1,level);
        return Math.min(.95,value<=10 ? .05+(value-1)*(.4/9) : .45+(value-10)*(.5/15));
    }
    public static int cadence(int base,int level,int minimum) {
        return Math.max(minimum,(int)Math.ceil(base/(1+.06*(Math.max(1,level)-1))));
    }
    static final int STOCK=6, PREPARE=10, GLOBAL_GAP=20;
    static final String GLOBAL="theurgist:throw";
    static String throwKey(MercenarySkill skill){return "theurgist:throw:"+skill.id();}
    static String stockKey(boolean positive,boolean stand){
        return "theurgist:stock:"+(positive?"positive":"negative")+(stand?":stand":"");
    }
}
