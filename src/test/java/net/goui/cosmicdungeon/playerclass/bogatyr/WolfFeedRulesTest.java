package net.goui.cosmicdungeon.playerclass.bogatyr;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class WolfFeedRulesTest {
    @Test void FoodPriorityHonorsAbilityOnlyBonesAndCheapFoodBeforeRawAndCooked(){
        assertEquals(0,WolfFeedRules.tier("minecraft:bone"));assertEquals(0,WolfFeedRules.tier("minecraft:rotten_flesh"));
        for(String food:new String[]{"beef","chicken","porkchop","mutton","rabbit"})assertEquals(1,WolfFeedRules.tier("minecraft:"+food));
        for(String food:new String[]{"cooked_beef","cooked_chicken","cooked_porkchop","cooked_mutton","cooked_rabbit"})assertEquals(2,WolfFeedRules.tier("minecraft:"+food));
        assertEquals(2,WolfFeedRules.tier("other:bone"));assertEquals(10,WolfFeedRules.LIMIT);
    }
}
