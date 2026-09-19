package net.goui.cosmicdungeon.playerclass.bogatyr;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import java.util.UUID;

public final class BogatyrDirectoryChecks {
    private static int checks;
    private static void check(boolean yes,String label){checks++;if(!yes)throw new AssertionError(label);}
    @SuppressWarnings("unchecked") private static <T> Codec<T> codec(Class<T> type)throws Exception{
        var field=type.getDeclaredField("CODEC");field.setAccessible(true);return (Codec<T>)field.get(null);
    }
    public static void main(String[] args)throws Exception{
        var codec=codec(BogatyrCompanionData.class);
        var data=codec.parse(JsonOps.INSTANCE,new JsonObject()).getOrThrow();
        UUID owner=UUID.randomUUID(),peer=UUID.randomUUID(),wolf=UUID.randomUUID();
        String inside="cosmicdungeon:dungeon_instance_1",outside="minecraft:overworld";
        check(data.count(owner)==0,"Old absent directory loads without inventing pets");
        check(data.preserveLegacy(wolf,owner,31,inside),"Legacy roster is retained before reset");
        check(data.count(owner)==1,"Unknown-location wolf still occupies its slot");
        check(!data.find(wolf).orElseThrow().located(),"No guessed location for an unloaded legacy wolf");
        check(!data.inDimension(inside).isEmpty(),"Unknown legacy identity blocks world replacement");
        check(!data.preserveLegacy(wolf,peer,32,outside),"Another roster cannot steal an existing identity");
        check(data.find(wolf).orElseThrow().owner().equals(owner),"Existing owner remains authoritative");
        var actual=new BogatyrCompanionData.Companion(wolf,owner,31,inside,124L,true);
        check(data.remember(actual),"Observed live wolf resolves its location");
        check(!data.remember(actual),"Repeated unchanged tick does not dirty the directory");
        check(data.count(owner)==1,"Same UUID cannot add another slot");
        var loaded=codec.parse(JsonOps.INSTANCE,codec.encodeStart(JsonOps.INSTANCE,data).getOrThrow()).getOrThrow();
        check(loaded.find(wolf).orElseThrow().equals(actual),"Identity and precise location survive save");
        var objectives=codec(D1RunData.class).parse(JsonOps.INSTANCE,new JsonObject()).getOrThrow();
        objectives.recordUnique(31,"wolves:"+owner,wolf.toString());objectives.clearRun(31);
        check(loaded.count(owner)==1,"Run counter cleanup cannot erase permanent ownership");
        check(loaded.inDimension(inside).size()==1,"Reset guard remains after objective cleanup");
        check(loaded.inDimension("cosmicdungeon:dungeon_instance_2").isEmpty(),"Unrelated party world is not blocked");
        check(!loaded.died(wolf,peer),"Wrong owner cannot release another companion");
        check(loaded.count(owner)==1,"Rejected removal preserves pack cap");
        var moved=new BogatyrCompanionData.Companion(wolf,owner,31,outside,987L,true);
        loaded.remember(moved);
        check(loaded.inDimension(inside).isEmpty(),"Verified location update clears the old dimension hold");
        check(loaded.inDimension(outside).size()==1,"Move retains exact UUID in the destination");
        check(loaded.count(owner)==1,"Cross-dimension location change is not a new pet");
        loaded.remember(new BogatyrCompanionData.Companion(wolf,peer,32,outside,987L,true));
        check(loaded.count(owner)==0&&loaded.count(peer)==1,"Observed ownership transfer updates both indices");
        check(loaded.forOwner(owner).isEmpty(),"Previous owner cannot inspect the transferred pet");
        var reloaded=codec.parse(JsonOps.INSTANCE,codec.encodeStart(JsonOps.INSTANCE,loaded).getOrThrow()).getOrThrow();
        check(reloaded.count(peer)==1&&reloaded.forOwner(peer).getFirst().wolf().equals(wolf),"Owner index rebuilds from saved truth");
        check(!reloaded.died(wolf,owner),"Stale owner death event cannot remove transferred record");
        check(reloaded.died(wolf,peer),"Observed actual death releases the correct slot");
        check(!reloaded.died(wolf,peer),"Repeated removal is idempotent");
        check(reloaded.inDimension(outside).isEmpty()&&reloaded.count(peer)==0,"Death clears both reset hold and cap");
        var afterDeath=codec.parse(JsonOps.INSTANCE,codec.encodeStart(JsonOps.INSTANCE,reloaded).getOrThrow()).getOrThrow();
        check(afterDeath.find(wolf).isEmpty(),"Directory contains no resurrection image after death");
        var duplicate=codec.encodeStart(JsonOps.INSTANCE,loaded).getOrThrow().deepCopy().getAsJsonObject();
        var array=duplicate.getAsJsonArray("companions");array.add(array.get(0).deepCopy());
        boolean rejected=false;
        try{rejected=codec.parse(JsonOps.INSTANCE,duplicate).error().isPresent();}
        catch(RuntimeException expected){rejected=true;}
        check(rejected,"Duplicate UUID save cannot silently choose a conflicting owner");
        var malformed=codec.encodeStart(JsonOps.INSTANCE,loaded).getOrThrow().deepCopy().getAsJsonObject();
        malformed.getAsJsonArray("companions").get(0).getAsJsonObject().addProperty("wolf","invalid");
        check(codec.parse(JsonOps.INSTANCE,malformed).error().isPresent(),"Malformed identity requires recovery instead of deletion");
        UUID physical=UUID.randomUUID(),bondA=UUID.randomUUID(),bondB=UUID.randomUUID();
        var separated=codec.parse(JsonOps.INSTANCE,new JsonObject()).getOrThrow();
        var a=new BogatyrCompanionData.Companion(bondA,owner,90,inside,100,true,physical);
        var b=new BogatyrCompanionData.Companion(bondB,peer,91,"cosmicdungeon:dungeon_instance_2",200,true,physical);
        separated.remember(a);separated.remember(b);
        check(separated.count(owner)==1&&separated.count(peer)==1,"Copied native UUIDs retain independent bonds and owners");
        var round=codec.parse(JsonOps.INSTANCE,codec.encodeStart(JsonOps.INSTANCE,separated).getOrThrow()).getOrThrow();
        check(round.find(bondA).orElseThrow().entityUuid().equals(physical),"Physical lookup UUID survives restart");
        check(round.find(bondB).orElseThrow().dimension().equals(b.dimension()),"Copied wolf retains separate dimension");
        var legacy=codec.encodeStart(JsonOps.INSTANCE,data).getOrThrow().deepCopy().getAsJsonObject();
        legacy.getAsJsonArray("companions").get(0).getAsJsonObject().remove("entity_uuid");
        var oldSave=codec.parse(JsonOps.INSTANCE,legacy).getOrThrow();
        check(oldSave.find(wolf).orElseThrow().entityUuid().equals(wolf),"Old save defaults physical UUID to legacy identity");
        check(WolfIdentity.matches(a,owner,physical,inside,false,false),"Same live location may refresh");
        check(!WolfIdentity.matches(a,peer,physical,inside,false,false),"Observed copied wolf cannot steal owner");
        check(!WolfIdentity.matches(a,owner,UUID.randomUUID(),inside,false,false),"Another physical entity cannot overwrite bond");
        check(!WolfIdentity.matches(a,owner,physical,outside,false,false),"Unproven dimension change cannot overwrite source");
        check(WolfIdentity.matches(a,owner,physical,outside,true,false),"Actual native dimension departure permits movement");
        check(!WolfIdentity.matches(a,peer,physical,outside,true,false),"Departure permit cannot transfer ownership");
        check(WolfIdentity.matches(a,owner,bondA,outside,false,true),"Exact archived delivery permits restored native UUID");
        check(!WolfIdentity.matches(a,peer,bondA,outside,false,true),"Delivery proof never overrides owner");
        separated.identityHold(b.dimension(),physical);
        var heldSave=codec.parse(JsonOps.INSTANCE,codec.encodeStart(JsonOps.INSTANCE,separated).getOrThrow()).getOrThrow();
        check(heldSave.identityHeld(b.dimension(),physical),"Identity quarantine survives restart");
        check(heldSave.dimensionHeld(b.dimension()),"Quarantined copied dimension cannot reset");
        check(!heldSave.dimensionHeld(inside),"Unrelated source dimension remains distinct");
        check(!heldSave.identityHeld(b.dimension(),bondA),"Quarantine is physical identity specific");
        check(oldSave.activeCount(owner)==1,"Legacy saves without archive fields retain their actual active pet");
        check(heldSave.activeCount(owner)==1&&heldSave.activeCount(peer)==1,"Conflicting/unloaded identities retain conservative active slots");
        check(round.forOwner(owner).stream().allMatch(c->c.owner().equals(owner)),"Roster pages start from exact owner index");
        System.out.println(checks+" Bogatyr permanent-directory checks passed");
    }
}
