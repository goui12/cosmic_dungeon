package net.goui.cosmicdungeon.playerclass.dragoon.repair;
import java.math.BigInteger;
/** Server-side quote arithmetic; client deltas cannot overflow a whole-Trace fee. */
public final class RepairQuoteRules {
    private RepairQuoteRules(){}
    public static long adjustFee(long current,long denomination,int delta,long available){
        if(current<0||denomination<=0||available<0)throw new IllegalArgumentException("Invalid fee bounds");
        return BigInteger.valueOf(current).add(BigInteger.valueOf(denomination).multiply(BigInteger.valueOf(delta)))
                .max(BigInteger.ZERO).min(BigInteger.valueOf(available)).longValueExact();
    }
    public static boolean expired(long now,long deadline){return now>=deadline;}
    public static boolean poll(boolean active,long now,long due){return active&&(now%20==0||now>=due);}

}
