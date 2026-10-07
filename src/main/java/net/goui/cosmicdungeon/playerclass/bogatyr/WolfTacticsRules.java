package net.goui.cosmicdungeon.playerclass.bogatyr;

import java.util.Objects;

/** Pure ordering and boundaries shared by advanced wolf goals and their cadence. */
final class WolfTacticsRules {
    private WolfTacticsRules(){}
    static int compareStrategic(boolean rangedA,double maxHpA,String idA,
                                boolean rangedB,double maxHpB,String idB){
        int ranged=Boolean.compare(rangedB,rangedA);
        if(ranged!=0)return ranged;
        int health=Double.compare(validMaxHealth(maxHpB),validMaxHealth(maxHpA));
        return health!=0?health:Objects.requireNonNull(idA).compareTo(Objects.requireNonNull(idB));
    }
    private static double validMaxHealth(double value){return Double.isFinite(value)&&value>0?value:0;}
    static boolean inside(double distanceSquared){
        return Double.isFinite(distanceSquared)&&distanceSquared>=0&&distanceSquared<=256;
    }
    static boolean critical(float health){return Float.isFinite(health)&&health>0&&health<=6;}
    static boolean healDue(long now,long since){
        return now>=since&&since<=Long.MAX_VALUE-100&&now>=since+100;
    }
}
