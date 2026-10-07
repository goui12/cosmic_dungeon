package net.goui.cosmicdungeon.playerclass.bogatyr;

import net.goui.cosmicdungeon.network.BogatyrPayloads.Kind;

/** Paid care policy is independent of native entity side effects. No admission cap is imposed. */
public final class WolfCommandRules {
    private WolfCommandRules(){}
    public static int unitCost(Kind kind){return switch(kind){case BREED,HEAL->5;case SUMMON->30;case REGROUP->1;};}
    public static int count(Kind kind,int eligible,int kibble){
        if(eligible<0||kibble<0||kibble>600)throw new IllegalArgumentException("Invalid command budget");
        if(kind==Kind.REGROUP)return eligible<=kibble?eligible:0;
        return Math.min(kind==Kind.SUMMON?Math.min(1,eligible):eligible,kibble/unitCost(kind));
    }
    public static boolean breed(int age,boolean inLove,float health,float maximum){
        return age==0&&!inLove&&WolfCareRules.healthy(health,maximum);
    }
    public static boolean injured(float health,float maximum){
        return Float.isFinite(health)&&Float.isFinite(maximum)&&health>0&&maximum>health;
    }
}
