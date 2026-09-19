package net.goui.cosmicdungeon.economy;
import java.util.*;
/** Pure allocation rule; stable party slots remain the rotation authority when eligibility changes. */
public final class D1RewardRules {
    public record Split(Map<UUID,Long> shares,int nextCursor){}
    private D1RewardRules(){}
    public static Split split(long pool,List<UUID> order,Set<UUID> eligible,int cursor){
        var shares=new LinkedHashMap<UUID,Long>();
        if(pool<0||order.isEmpty()||eligible.isEmpty())return new Split(Map.of(),0);
        for(UUID id:order)if(eligible.contains(id))shares.put(id,0L);
        if(shares.isEmpty())return new Split(Map.of(),0);
        long whole=pool/shares.size(),left=pool%shares.size();
        shares.replaceAll((id,old)->whole);
        int next=Math.floorMod(cursor,order.size());
        while(left>0){
            UUID id=order.get(next);next=(next+1)%order.size();
            if(shares.containsKey(id)){shares.put(id,shares.get(id)+1);left--;}
        }
        return new Split(Map.copyOf(shares),next);
    }
}
