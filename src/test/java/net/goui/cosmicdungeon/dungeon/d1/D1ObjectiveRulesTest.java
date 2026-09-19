package net.goui.cosmicdungeon.dungeon.d1;

import java.util.HashMap;

public final class D1ObjectiveRulesTest {
    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
    }
    public static void main(String[] args) {
        var first = new HashMap<String, Long>();
        var second = new HashMap<String, Long>();
        for (int i = 0; i < 10; i++) D1ObjectiveRules.strike(first, "same", i, 40);
        check(first.size() == 1, "Repeated bell is only one distinct bell");
        for (int i = 0; i < 6; i++) D1ObjectiveRules.strike(second, "bell" + i, 100 + i, 40);
        check(second.size() == 6 && first.size() == 1, "Instances cannot share bells");
        check(D1ObjectiveRules.strike(second, "later", 146, 40) == 1, "Expired bells do not count");
        check(D1ObjectiveRules.strike(second, "restart", 10, 40) == 1, "Clock rollback cannot reuse future rings");
        var boundary = new HashMap<String, Long>();
        for (int i=0;i<6;i++) D1ObjectiveRules.strike(boundary,"bell"+i,i*8,40);
        check(boundary.size()==6,"Six unique rings spanning exactly two seconds qualify");
        check(D1ObjectiveRules.strike(boundary,"bell5",41,40)==5,"One tick beyond window excludes first bell");
        check(D1ObjectiveRules.roundedLesserBlooms(0) == 0, "No rounding with zero collected");
        check(D1ObjectiveRules.roundedLesserBlooms(1) == 5, "One rounds to five");
        check(D1ObjectiveRules.roundedLesserBlooms(5) == 5, "Exact multiple remains unchanged");
        check(D1ObjectiveRules.roundedLesserBlooms(6) == 10, "Six rounds to ten");
        check(D1ObjectiveRules.roundedLesserBlooms(Integer.MAX_VALUE) == Integer.MAX_VALUE, "Rounding cannot overflow");
        check(D1ObjectiveRules.saturatingAdd(Long.MAX_VALUE - 1, 5) == Long.MAX_VALUE, "Lifetime counter cannot overflow");
        check(D1ObjectiveRules.saturatingAdd(50, -2) == 50, "Lifetime counter never decreases");
        System.out.println("13 D1 objective regression checks passed");
    }
}
