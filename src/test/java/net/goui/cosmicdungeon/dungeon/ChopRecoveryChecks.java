package net.goui.cosmicdungeon.dungeon;

import com.mojang.serialization.Codec;
import net.goui.cosmicdungeon.item.identity.ItemAuthoringPlan;
import net.minecraft.nbt.*;
import java.util.*;
import java.util.function.Consumer;

/** Production review validation and compressed-NBT interruption fixtures; no server bootstrap. */
public final class ChopRecoveryChecks {
    private static int checks;
    private static final UUID OWNER=new UUID(0,3001),OTHER=new UUID(0,3002),TOKEN=new UUID(0,3003),DEV=new UUID(0,3004);
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private interface Work {void run()throws Exception;}
    private static void reject(Work work,String message){boolean failed=false;try{work.run();}catch(Exception expected){failed=true;}check(failed,message);}
    private static CompoundTag item(int slot,String id,int count) {
        var t=new CompoundTag();t.putByte("Slot",(byte)slot);t.putString("id",id);t.putInt("count",count);
        var components=new CompoundTag();var custom=new CompoundTag();custom.putString("authored","keep exact");
        var nested=new ListTag();var note=new CompoundTag();note.putString("note","nested original");nested.add(note);custom.put("nested",nested);
        components.put("minecraft:custom_data",custom);components.putInt("minecraft:damage",17);t.put("components",components);return t;
    }
    private static CompoundTag inventory(boolean cooked,boolean bound) {
        var result=new CompoundTag();var items=new ListTag();
        var chop=item(5,cooked?"cosmicdungeon:farrows_chop":"cosmicdungeon:raw_farrows_chop",1);
        var components=chop.getCompoundOrEmpty("components");components.put("cosmicdungeon:coordinates",new IntArrayTag(new int[]{3,70,-9}));
        if(bound) {
            components.putString("cosmicdungeon:chop_owner",OWNER.toString());components.putString("cosmicdungeon:chop_token",TOKEN.toString());
            var target=new CompoundTag();target.putString("owner",OWNER.toString());target.putLong("run_id",29);
            target.putString("dimension","cosmicdungeon:d1_instance_1");target.putDouble("x",3);target.putDouble("y",70);target.putDouble("z",-9);
            target.putFloat("yaw",2);target.putFloat("pitch",3);components.put("cosmicdungeon:dungeon_return_target",target);
        }
        items.add(item(0,"minecraft:paper",64));items.add(chop);items.add(item(40,"minecraft:shield",1));result.put("Items",items);return result;
    }
    private static CompoundTag chosen(CompoundTag inventory){return (CompoundTag)inventory.getListOrEmpty("Items").get(1);}
    private static ChopTravelPlan draft(boolean cooked,boolean bound) {
        var before=inventory(cooked,bound);var after=before.copy();var item=chosen(after);
        item.putString("id","cosmicdungeon:raw_farrows_chop");var c=item.getCompoundOrEmpty("components");
        c.remove("cosmicdungeon:coordinates");c.remove("cosmicdungeon:dungeon_return_target");
        c.putString("cosmicdungeon:chop_owner",OWNER.toString());c.putString("cosmicdungeon:chop_token",TOKEN.toString());
        var pose=ChopTravelPlan.pose("minecraft:overworld",1.25,64,-9,20,3);
        return ChopTravelPlan.create(OWNER,0,bound?"refresh":"adopt",before,after,pose,pose,new CompoundTag(),new CompoundTag(),
                bound?ChopOwnershipData.entryImage(new ChopOwnershipData.Entry(TOKEN.toString(),29,false)):new CompoundTag(),
                ChopOwnershipData.issuedImage(TOKEN),null);
    }
    private static void altered(Consumer<CompoundTag> change,String message) {
        var image=draft(true,true).image();change.accept(image);reject(()->new ChopTravelPlan(image).reviewed(DEV,5),message);
    }
    private static ChopRecoveryReview.Evidence evidence(int count,int copies,int ender,UUID itemOwner,UUID token,
            ChopOwnershipData.Entry ownership,UUID returnOwner,long run,boolean retained,boolean stored,boolean foreignToken) {
        return new ChopRecoveryReview.Evidence(OWNER,count,copies,ender,itemOwner,token,ownership,returnOwner,run,retained,stored,foreignToken);
    }
    private static void denied(ChopRecoveryReview.Evidence e,String message){check(!ChopRecoveryReview.rejection(e).isEmpty(),message);}
    @SuppressWarnings("unchecked") private static <T> Codec<T> codec(Class<T> type)throws Exception{
        var field=type.getDeclaredField("CODEC");field.setAccessible(true);return (Codec<T>)field.get(null);
    }
    private static void indexes()throws Exception {
        var codec=codec(DungeonInventoryEscrowData.class);var data=codec.parse(NbtOps.INSTANCE,new CompoundTag()).getOrThrow();
        var original=inventory(true,true);var a=new DungeonInventoryEscrowData.Entry(29,OWNER,original,new CompoundTag(),true);
        var b=new DungeonInventoryEscrowData.Entry(30,OWNER,new CompoundTag(),original,false);
        check(!data.hasOwner(OWNER),"Empty legacy shape has no phantom owner");
        data.put(a);data.put(a);check(data.hasOwner(OWNER),"Replacing escrow does not duplicate owner count");
        data.put(b);data.put(new DungeonInventoryEscrowData.Entry(31,OTHER,new CompoundTag(),original,false));
        data=codec.parse(NbtOps.INSTANCE,codec.encodeStart(NbtOps.INSTANCE,data).getOrThrow()).getOrThrow();
        check(data.hasOwner(OWNER)&&data.hasOwner(OTHER),"Old escrow rows rebuild both owner indexes");
        check(data.get(29,OWNER).orElseThrow().dungeonInventory().equals(original),"Owner index preserves native escrow image");
        data.remove(29,OWNER);check(data.hasOwner(OWNER),"Second orphan escrow still blocks adoption");
        data.remove(29,OWNER);check(data.hasOwner(OWNER),"Missing removal cannot lower remaining count");
        data.remove(30,OWNER);check(!data.hasOwner(OWNER)&&data.hasOwner(OTHER),"Only final owner row releases gate");
        var round=codec.parse(NbtOps.INSTANCE,codec.encodeStart(NbtOps.INSTANCE,data).getOrThrow()).getOrThrow();
        check(!round.hasOwner(OWNER)&&round.hasOwner(OTHER),"Owner index requires no new persisted schema");
        var ownership=codec(ChopOwnershipData.class).parse(NbtOps.INSTANCE,new CompoundTag()).getOrThrow();ownership.issue(OWNER,TOKEN);
        check(!ownership.tokenClaimedByOther(OWNER,TOKEN)&&ownership.tokenClaimedByOther(OTHER,TOKEN),"Another recorded token owner blocks adoption");
        check(!ownership.tokenClaimedByOther(OTHER,null),"Untagged item has no invented token conflict");
    }
    public static void main(String[] args)throws Exception {
        checks=0;var owned=new ChopOwnershipData.Entry(TOKEN.toString(),29,false);
        check(ChopRecoveryReview.rejection(evidence(1,1,0,null,null,null,null,0,false,false,false)).isEmpty(),"One untagged orphan can be explicitly assigned");
        check(ChopRecoveryReview.rejection(evidence(1,1,0,OWNER,TOKEN,owned,OWNER,29,false,false,false)).isEmpty(),"Matching old ownership and vanished return recover");
        check(ChopRecoveryReview.rejection(evidence(1,1,0,OWNER,TOKEN,null,OWNER,29,false,false,false)).isEmpty(),"Missing ownership row preserves self-owned token");
        for(int count:new int[]{0,2,64})denied(evidence(count,1,0,null,null,null,null,0,false,false,false),"Preserve count "+count);
        for(int copies:new int[]{0,2,36})denied(evidence(1,copies,0,null,null,null,null,0,false,false,false),"Do not choose among "+copies+" stacks");
        denied(evidence(1,1,1,null,null,null,null,0,false,false,false),"Ender Chest copy remains intact");
        denied(evidence(1,1,0,OTHER,TOKEN,null,null,0,false,false,false),"Foreign item owner preserved");
        denied(evidence(1,1,0,OWNER,null,null,null,0,false,false,false),"Partial owner held");
        denied(evidence(1,1,0,null,TOKEN,null,null,0,false,false,false),"Partial token held");
        denied(evidence(1,1,0,OWNER,TOKEN,owned,OTHER,29,false,false,false),"Foreign return owner held");
        denied(evidence(1,1,0,OWNER,TOKEN,owned,OWNER,0,false,false,false),"Zero return run held");
        denied(evidence(1,1,0,null,null,null,null,29,false,false,false),"Incomplete return target held");
        denied(evidence(1,1,0,OWNER,TOKEN,owned,OWNER,30,false,false,false),"Conflicting run IDs held");
        denied(evidence(1,1,0,null,null,owned,null,0,false,false,false),"Existing ownership cannot be guessed");
        denied(evidence(1,1,0,OWNER,OTHER,owned,null,0,false,false,false),"Different item token held");
        denied(evidence(1,1,0,OWNER,TOKEN,new ChopOwnershipData.Entry(TOKEN.toString(),0,true),null,0,false,false,false),"Pending Raw delivery not overwritten");
        denied(evidence(1,1,0,null,null,null,null,0,true,false,false),"Retained run must settle");
        denied(evidence(1,1,0,null,null,null,null,0,false,true,false),"Orphan escrow or stored belongings must settle");
        denied(evidence(1,1,0,OWNER,TOKEN,null,null,0,false,false,true),"Cross-owner token collision held");
        for(boolean cooked:new boolean[]{false,true})for(boolean bound:new boolean[]{false,true}) {
            var plan=draft(cooked,bound).reviewed(DEV,5);
            var round=ChopTravelPlan.CODEC.parse(NbtOps.INSTANCE,ChopTravelPlan.CODEC.encodeStart(NbtOps.INSTANCE,plan).getOrThrow()).getOrThrow();
            check(round.image().equals(plan.image()),"Exact native review codec round trip");
            check(round.receipt().getCompoundOrEmpty("review").equals(plan.tag("review")),"Receipt retains exact original and developer evidence");
            check(!plan.committed()&&plan.commit().committed(),"Review cannot skip reservation");
            var copy=plan.tag("review");copy.remove("before_item");check(plan.tag("review").contains("before_item"),"Review evidence immutable");
            var receipt=plan.commit().receipt();receipt.remove("review");check(!plan.commit().receipted(receipt),"Missing review cannot acknowledge recovery");
            var snapshot=new CompoundTag();snapshot.put("inventory",plan.tag("before"));snapshot.put("ownership",plan.tag("ownership_before"));
            snapshot.put("ender",inventory(false,false));snapshot.put("pose",plan.tag("source"));
            var review=new ItemAuthoringPlan<>(plan.id().toString(),100,snapshot.copy(),snapshot.copy());
            check(review.accepts(plan.id().toString(),99,true,snapshot,false,CompoundTag::equals),"Exact review before expiry accepted");
            for(String field:List.of("inventory","ownership","ender","pose")) {
                var changed=snapshot.copy();changed.getCompoundOrEmpty(field).putInt("changed",1);
                check(!review.accepts(plan.id().toString(),99,true,changed,false,CompoundTag::equals),"Changed "+field+" rejects stale preview");
            }
            check(!review.accepts(plan.id().toString(),100,true,snapshot,false,CompoundTag::equals),"Exact expiry rejects");
            check(!review.accepts(OTHER.toString(),99,true,snapshot,false,CompoundTag::equals),"Wrong token rejects");
            check(!review.accepts(plan.id().toString(),99,false,snapshot,false,CompoundTag::equals),"Changed session rejects");
            checks+=ChopTravelChecks.reviewedInterruptions(plan);
        }
        altered(t->chosen(t.getCompoundOrEmpty("after")).putInt("count",2),"Cannot grant an extra Chop");
        altered(t->chosen(t.getCompoundOrEmpty("before")).putInt("count",64),"Cannot consume overstack");
        altered(t->chosen(t.getCompoundOrEmpty("after")).getCompoundOrEmpty("components").putInt("minecraft:damage",0),"Damage cannot disappear");
        altered(t->chosen(t.getCompoundOrEmpty("after")).getCompoundOrEmpty("components").remove("minecraft:custom_data"),"Nested authored data cannot disappear");
        altered(t->((CompoundTag)t.getCompoundOrEmpty("after").getListOrEmpty("Items").get(0)).putInt("count",1),"Other stack counts stay exact");
        altered(t->((CompoundTag)t.getCompoundOrEmpty("after").getListOrEmpty("Items").get(2)).putString("id","minecraft:air"),"Offhand stays exact");
        altered(t->chosen(t.getCompoundOrEmpty("after")).getCompoundOrEmpty("components").putString("cosmicdungeon:chop_owner",OTHER.toString()),"Cannot transfer ownership");
        altered(t->chosen(t.getCompoundOrEmpty("before")).getCompoundOrEmpty("components").putString("cosmicdungeon:chop_owner",OTHER.toString()),"Foreign original cannot be reassigned");
        altered(t->chosen(t.getCompoundOrEmpty("after")).getCompoundOrEmpty("components").putString("cosmicdungeon:chop_token",OTHER.toString()),"Item and authority share token");
        altered(t->t.put("ownership_after",ChopOwnershipData.issuedImage(OTHER)),"Authority cannot diverge from item");
        altered(t->{chosen(t.getCompoundOrEmpty("after")).getCompoundOrEmpty("components").putString("cosmicdungeon:chop_token",OTHER.toString());
            t.put("ownership_after",ChopOwnershipData.issuedImage(OTHER));},"Even matching output images cannot rotate an existing token");
        altered(t->chosen(t.getCompoundOrEmpty("after")).getCompoundOrEmpty("components").put("cosmicdungeon:coordinates",new IntArrayTag(new int[]{1,2,3})),"Old return coordinates must clear");
        altered(t->{var more=item(7,"cosmicdungeon:raw_farrows_chop",1);t.getCompoundOrEmpty("before").getListOrEmpty("Items").add(more);t.getCompoundOrEmpty("after").getListOrEmpty("Items").add(more.copy());},"Duplicate Chop cannot be selected");
        altered(t->t.getCompoundOrEmpty("before").getListOrEmpty("Items").add(chosen(t.getCompoundOrEmpty("before")).copy()),"Duplicate selected slot rejected");
        var future=draft(true,true).reviewed(DEV,5).image();future.getCompoundOrEmpty("review").putInt("version",99);
        reject(()->new ChopTravelPlan(future),"Future review version held");
        var mismatch=draft(true,true).reviewed(DEV,5).image();mismatch.getCompoundOrEmpty("review").remove("ownership_before");
        reject(()->new ChopTravelPlan(mismatch),"Partial evidence rejected");
        var plan=draft(true,true);reject(()->plan.reviewed(DEV,40),"Ordinary-slot review cannot edit armor/offhand");
        indexes();
        System.out.println("Chop legacy review checks passed: "+checks);
    }
}
