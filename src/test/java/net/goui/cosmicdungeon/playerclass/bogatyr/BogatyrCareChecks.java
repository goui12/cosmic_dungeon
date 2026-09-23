package net.goui.cosmicdungeon.playerclass.bogatyr;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import java.util.List;
import java.util.UUID;

public final class BogatyrCareChecks {
    private static int checks;
    private static void check(boolean yes,String label){checks++;if(!yes)throw new AssertionError(label);}
    @SuppressWarnings("unchecked")
    public static void main(String[] args)throws Exception{
        check(WolfCareRules.growthTicks(-24000,.1)==2400,"Twenty-minute pup gains exactly two minutes");
        check(WolfCareRules.growthTicks(-21600,.1)==2160,"Second item uses remaining juvenile age");
        check(WolfCareRules.growthTicks(-199,.1)==20,"Partial seconds round to a tick, not zero");
        check(WolfCareRules.growthTicks(-1,.1)==1,"Last tick can complete growth");
        check(WolfCareRules.growthTicks(-500,0)==0,"Zero modifier disables acceleration");
        check(WolfCareRules.growthTicks(0,.1)==0,"Adult cannot acquire a growth cooldown");
        check(WolfCareRules.growthTicks(6000,.1)==0,"Breeding cooldown is not juvenile growth");
        check(WolfCareRules.growthTicks(-500,1)==500,"Full-growth override stops at adulthood");
        check(WolfCareRules.growthTicks(-500,2)==500,"Oversized override is bounded");
        check(WolfCareRules.growthTicks(Integer.MIN_VALUE,.1)==214748365,"Malformed extreme age cannot overflow");
        for(double fraction:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY})
            check(WolfCareRules.growthTicks(-500,fraction)==0,"Invalid growth fraction ignored");
        check(WolfCareRules.healthy(20,20),"Full health may breed");
        check(!WolfCareRules.healthy(19.99f,20),"Even slightly injured wolf must heal first");
        check(!WolfCareRules.healthy(0,20),"Dead wolf cannot breed");
        check(!WolfCareRules.healthy(Float.NaN,20),"Invalid health cannot breed");
        check(!WolfCareRules.healthy(20,Float.POSITIVE_INFINITY),"Invalid maximum cannot breed");
        check(!WolfCareRules.healthy(0,0),"Zero maximum cannot breed");
        var field=D1RunData.class.getDeclaredField("CODEC");field.setAccessible(true);
        Codec<D1RunData> codec=(Codec<D1RunData>)field.get(null);
        D1RunData data=codec.parse(JsonOps.INSTANCE,new JsonObject()).getOrThrow();
        UUID owner=UUID.randomUUID(),peer=UUID.randomUUID();
        String key="wolves:"+owner, pet=UUID.randomUUID().toString();
        data.recordUnique(71,key,pet);
        check(!data.recordUnique(71,key,pet),"Reloading a wolf UUID never occupies another slot");
        for(int i=1;i<5;i++)data.recordUnique(71,key,UUID.randomUUID().toString());
        check(!WolfCareRules.hasRoom(data.values(71,key),5),"Full five-member saved roster rejects sixth");
        var saved=codec.encodeStart(JsonOps.INSTANCE,data).getOrThrow();
        var loaded=codec.parse(JsonOps.INSTANCE,saved).getOrThrow();
        check(!WolfCareRules.hasRoom(loaded.values(71,key),5),"Restart and unloaded wolves cannot bypass cap");
        check(WolfCareRules.hasRoom(loaded.values(71,"wolves:"+peer),5),"Another owner has an independent cap");
        check(WolfCareRules.hasRoom(loaded.values(72,key),5),"Different run is independent until companion transfer");
        check(!WolfCareRules.hasRoom(loaded.values(71,key),2),"Lowered cap stops new pets without deleting old ones");
        check(loaded.values(71,key).size()==5,"Existing over-cap pets are preserved");
        loaded.removeUnique(71,key,pet);
        check(WolfCareRules.hasRoom(loaded.values(71,key),5),"Actual death releases the exact slot");
        check(!WolfCareRules.hasRoom(List.of(),0),"Zero cap permits no acquisition");
        System.out.println(checks+" Bogatyr feeding and saved-roster checks passed");
    }
}
