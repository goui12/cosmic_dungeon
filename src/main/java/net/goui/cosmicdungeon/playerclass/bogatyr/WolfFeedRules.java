package net.goui.cosmicdungeon.playerclass.bogatyr;
import java.util.Set;
/** Deterministic item priority, independent of wolf/native side effects. */
public final class WolfFeedRules {
    public static final int LIMIT=10;
    private static final Set<String> RAW=Set.of("minecraft:beef","minecraft:chicken","minecraft:porkchop","minecraft:mutton","minecraft:rabbit");
    private WolfFeedRules(){}
    public static int tier(String id){
        if(id.equals("minecraft:bone")||id.equals("minecraft:rotten_flesh"))return 0;
        return RAW.contains(id)?1:2;
    }
}
