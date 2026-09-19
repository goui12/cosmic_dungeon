package net.goui.cosmicdungeon.playerclass.bogatyr;

import net.minecraft.nbt.CompoundTag;
import java.util.List;

public final class BogatyrBehaviourChecks {
    private static int checks;
    private static void check(boolean yes,String label){checks++;if(!yes)throw new AssertionError(label);}
    public static void main(String[] args){
        check(WolfBehaviourRules.threatRank(true,true)==0,"Skeleton ranged threat keeps highest source priority");
        check(WolfBehaviourRules.threatRank(true,false)<WolfBehaviourRules.threatRank(false,true),"Skeleton precedes other ranged");
        check(WolfBehaviourRules.threatRank(false,true)<WolfBehaviourRules.threatRank(false,false),"Other ranged precedes melee");
        check(WolfBehaviourRules.refresh(0,Long.MIN_VALUE,20),"First owner observation may query");
        check(!WolfBehaviourRules.refresh(119,100,20),"Owner query stays cached before interval");
        check(WolfBehaviourRules.refresh(120,100,20),"Exact poll boundary refreshes");
        check(WolfBehaviourRules.refresh(99,100,20),"Clock rollback cannot strand cached targets");
        check(!WolfBehaviourRules.refresh(100,100,0),"Invalid zero interval still permits only one query per tick");
        check(WolfBehaviourRules.refresh(101,100,0),"Defensive interval minimum is one tick");
        check(!WolfBehaviourRules.expired(Long.MAX_VALUE,0),"Zero duration always means permanent");
        check(WolfBehaviourRules.activeTick(400,0,false)==400,"Permanent setting pauses any existing counter");
        check(WolfBehaviourRules.activeTick(400,1,true)==400,"Archive or explicit pause consumes no lifetime");
        check(WolfBehaviourRules.activeTick(400,1,false)==401,"Loaded active companion advances once per tick");
        check(WolfBehaviourRules.activeTick(-20,1,false)==1,"Malformed negative counter cannot extend lifetime");
        check(WolfBehaviourRules.activeTick(Long.MAX_VALUE,1,false)==Long.MAX_VALUE,"Counter saturates instead of overflow");
        check(!WolfBehaviourRules.expired(1199,1),"One minute is not expired a tick early");
        check(WolfBehaviourRules.expired(1200,1),"One active minute is 1200 ticks");
        check(WolfBehaviourRules.expired(2400,1),"Lowering a positive duration uses accumulated active time");
        check(!WolfBehaviourRules.expired(1200,2),"Raising duration preserves existing elapsed time");
        check(!WolfBehaviourRules.expired(-1,1),"Invalid negative elapsed does not force expiration");
        check(!WolfBehaviourRules.expired(630719999L,525600),"Maximum one-year setting has no integer overflow");
        check(WolfBehaviourRules.expired(630720000L,525600),"Maximum duration expires at exact long boundary");
        long elapsed=0;
        for(int i=0;i<600;i++)elapsed=WolfBehaviourRules.activeTick(elapsed,1,false);
        var nativeSave=new CompoundTag();nativeSave.putLong(BogatyrDuration.ACTIVE_TICKS,elapsed);
        long restored=nativeSave.copy().getLongOr(BogatyrDuration.ACTIVE_TICKS,0);
        check(restored==600,"Native entity save retains half-minute counter");
        for(int i=0;i<600;i++)restored=WolfBehaviourRules.activeTick(restored,1,true);
        check(restored==600,"Archived time does not silently spend active lifetime");
        for(int i=0;i<600;i++)restored=WolfBehaviourRules.activeTick(restored,1,false);
        check(WolfBehaviourRules.expired(restored,1),"Reloaded active lifetime reaches same deadline");
        System.out.println(checks+" Bogatyr targeting and duration checks passed");
    }
}
