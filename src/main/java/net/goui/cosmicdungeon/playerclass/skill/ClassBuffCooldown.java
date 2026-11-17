package net.goui.cosmicdungeon.playerclass.skill;

import java.util.List;

/** Additive absolute game-time deadlines in existing run storage; malformed entries fail closed. */
public final class ClassBuffCooldown {
    private ClassBuffCooldown(){}
    public static long deadline(List<String> values){
        if(values.isEmpty())return 0;
        if(values.size()!=1)return Long.MAX_VALUE;
        try{long value=Long.parseLong(values.getFirst());return value>=0?value:Long.MAX_VALUE;}
        catch(NumberFormatException invalid){return Long.MAX_VALUE;}
    }
    public static int remaining(long deadline,long now){
        if(now<0)return ClassBuff.COOLDOWN;
        if(deadline<=now)return 0;
        return (int)Math.min(ClassBuff.COOLDOWN,deadline-now);
    }
    public static boolean ready(long deadline,long now){
        return now>=0&&now<=Long.MAX_VALUE-ClassBuff.COOLDOWN&&deadline!=Long.MAX_VALUE&&deadline<=now;
    }
    public static String display(int ticks){long seconds=(Math.max(0,ticks)+19L)/20;return seconds/60+":"+String.format(java.util.Locale.ROOT,"%02d",seconds%60);}
}
