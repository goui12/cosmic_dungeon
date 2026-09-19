package net.goui.cosmicdungeon.economy;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.*;
import java.util.*;

/** Native account serialization at interruption boundaries, without launching a game server. */
public final class DeathCurrencyChecks {
    private static int checks;
    private static final UUID A=new UUID(0,2601),B=new UUID(0,2602),C=new UUID(0,2603),ID=new UUID(0,2626);
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private interface Work {void run() throws Exception;}
    private static void reject(Work action,String why){boolean failed=false;try{action.run();}catch(Exception expected){failed=true;}check(failed,why);}
    @SuppressWarnings("unchecked") private static Codec<PlayerCurrencyData> codec()throws Exception{
        var field=PlayerCurrencyData.class.getDeclaredField("CODEC");field.setAccessible(true);return (Codec<PlayerCurrencyData>)field.get(null);
    }
    private static CompoundTag encode(PlayerCurrencyData data)throws Exception{return (CompoundTag)codec().encodeStart(NbtOps.INSTANCE,data).getOrThrow();}
    private static PlayerCurrencyData reload(PlayerCurrencyData data)throws Exception{return codec().parse(NbtOps.INSTANCE,encode(data)).getOrThrow();}
    private static PlayerCurrencyData accounts(long amount)throws Exception{
        var old=new CompoundTag();var balances=new CompoundTag();balances.putLong(A.toString(),amount);old.put("balances",balances);
        return codec().parse(NbtOps.INSTANCE,old).getOrThrow();
    }
    private static CompoundTag image(UUID id,int age,int lifespan){
        var tag=new CompoundTag();tag.store("UUID",net.minecraft.core.UUIDUtil.CODEC,id);
        tag.putString("death_projection_id",id.toString());var custom=new CompoundTag();custom.putString(DeathCurrencyRecord.MARKER,id.toString());
        tag.put("NeoForgeData",custom);tag.putShort("Age",(short)age);tag.putInt("Lifespan",lifespan);tag.putShort("PickupDelay",(short)10);tag.putShort("Health",(short)5);
        var pos=new ListTag();pos.add(DoubleTag.valueOf(1.25));pos.add(DoubleTag.valueOf(64));pos.add(DoubleTag.valueOf(-4.75));tag.put("Pos",pos);
        var item=new CompoundTag();item.putString("id","cosmicdungeon:attunement_trace");item.putInt("count",1);
        var components=new CompoundTag();components.put("minecraft:custom_data",custom.copy());item.put("components",components);tag.put("Item",item);
        var opaque=new CompoundTag();opaque.putString("other_mod_data","preserve");tag.put("unrelated",opaque);return tag;
    }
    private static DeathCurrencyRecord death(UUID id,UUID owner,long balance,long run){
        return new DeathCurrencyRecord(id,owner,balance,DeathCurrencyRecord.loss(balance,20,.02,1),run,1720000000000L,"minecraft:overworld",image(id,0,6000),false);
    }
    private static void supply(PlayerCurrencyData data,long balances,long drops,String why){
        var report=data.supplySnapshot();
        check(report.getStringOr("balances","").equals(Long.toString(balances)),why+" balances");
        check(report.getStringOr("active_death_drops","").equals(Long.toString(drops)),why+" active value");
        check(report.getStringOr("difference","").equals("0"),why+" no unexplained supply");
    }
    public static void main(String[] args)throws Exception{
        long[][] examples={{0,0},{19,0},{20,1},{49,1},{50,1},{99,1},{100,2},{12345,246},{100000000,2000000},{Long.MAX_VALUE,184467440737095516L}};
        for(var example:examples)check(DeathCurrencyRecord.loss(example[0],20,.02,1)==example[1],"Canonical integer loss "+example[0]);
        check(DeathCurrencyRecord.loss(3,0,1.0,100)==3,"Configured minimum cannot create a negative balance");
        check(DeathCurrencyRecord.loss(100,20,0,0)==0,"Developer may disable loss");
        check(DeathCurrencyRecord.loss(Long.MAX_VALUE,0,1,0)==Long.MAX_VALUE,"Full fraction remains exact at long maximum");
        reject(()->DeathCurrencyRecord.loss(100,20,Double.NaN,1),"NaN rejected");
        reject(()->DeathCurrencyRecord.loss(100,20,Double.POSITIVE_INFINITY,1),"Infinite fraction rejected");
        reject(()->DeathCurrencyRecord.loss(100,20,-.01,1),"Negative fraction rejected");
        reject(()->DeathCurrencyRecord.loss(-1,20,.02,1),"Negative balance rejected");
        var record=death(ID,A,12345,7);var original=record.image();
        check(DeathCurrencyRecord.load(record.save()).equals(record),"Native death record round trip");
        original.putShort("Age",(short)5999);check(record.image().getShortOr("Age",(short)-1)==0,"Image accessor cannot mutate authoritative state");
        var checkpoint=record.activate().snapshot("minecraft:the_nether",image(ID,4123,9000));
        var loaded=DeathCurrencyRecord.load(checkpoint.save());
        check(loaded.image().getShortOr("Age",(short)-1)==4123&&loaded.image().getIntOr("Lifespan",-1)==9000,"Server item age/lifespan survive checkpoint without reset");
        check(loaded.dimension().equals("minecraft:the_nether")&&loaded.image().getCompoundOrEmpty("unrelated").equals(record.image().getCompoundOrEmpty("unrelated")),"Portal destination and unrelated native data retained");
        var infinite=image(ID,0,6000);infinite.putShort("Age",(short)-32768);
        reject(()->new DeathCurrencyRecord(ID,A,12345,246,7,1,"minecraft:overworld",infinite,true),"Infinite lifetime not accepted");
        var ownerOnly=image(ID,0,6000);ownerOnly.store("Owner",net.minecraft.core.UUIDUtil.CODEC,A);
        reject(()->new DeathCurrencyRecord(ID,A,12345,246,7,1,"minecraft:overworld",ownerOnly,true),"Drop cannot become owner-only");
        var wrong=image(new UUID(0,9999),0,6000);
        reject(()->new DeathCurrencyRecord(ID,A,12345,246,7,1,"minecraft:overworld",wrong,true),"Native UUID bound to transaction");
        for(String key:List.of("Age","Lifespan","PickupDelay","Item","UUID","Pos","NeoForgeData")){
            var broken=image(ID,0,6000);broken.remove(key);
            reject(()->new DeathCurrencyRecord(ID,A,12345,246,7,1,"minecraft:overworld",broken,true),"Missing image "+key+" held");
        }
        var legacy=accounts(12345);check(legacy.deathIds().isEmpty(),"Old account shape migrates without active drops");supply(legacy,12345,0,"Legacy baseline");
        check(legacy.prepareDeath(record),"First death intent saved");supply(legacy,12345,0,"Pending intent has not debited");
        check(legacy.pendingDeath(A)&&!legacy.prepareDeath(record),"Repeated intent is idempotent");
        check(legacy.change(A,"A",-1,"debit","",0,"spend-pending",false)==-1,"Pending death blocks spending");
        reject(()->legacy.setBalanceTrace(A,20000),"Pending death blocks administrative overwrite");
        reject(()->legacy.prepareDeath(death(new UUID(0,2627),A,12345,7)),"Next death waits for existing intent");
        var prepared=reload(legacy);
        check(prepared.commitDeath(ID),"Startup resolves persisted intent without owner online");supply(prepared,12099,246,"Committed debit and drop");
        check(!prepared.commitDeath(ID)&&!prepared.prepareDeath(record),"Repeated death never debits twice");
        prepared.setCapacityTrace(B,245);
        check(!prepared.collectDeath(ID,B,1),"Insufficient capacity rejects the complete amount");supply(prepared,12099,246,"Capacity refusal retains value");
        prepared.setCapacityTrace(B,246);
        check(prepared.collectDeath(ID,B,2),"Any eligible player can collect the full value");supply(prepared,12345,0,"Pickup transfers supply");
        check(prepared.getBalanceTrace(B)==246&&!prepared.collectDeath(ID,C,3),"Second collector cannot duplicate pickup");
        check(!prepared.destroyDeath(ID,"despawn_after_pickup",4),"Removed projection cannot sink collected money");
        var collected=reload(prepared);
        check(!collected.prepareDeath(record)&&!collected.commitDeath(ID)&&!collected.collectDeath(ID,A,5),"Restart after collection retains compact death identity");
        check(collected.supplySnapshot().getStringOr("transfer","").equals("492"),"Debit and pickup report gross transfers");
        // Each cut represents the last durable account image; replay recovery after every cut.
        for(int cut=0;cut<=4;cut++){
            var data=accounts(12345);
            if(cut>=1)data.prepareDeath(record);
            if(cut>=2)data.commitDeath(ID);
            if(cut>=3)data.snapshotDeath(ID,"minecraft:the_nether",image(ID,4123,9000));
            if(cut>=4)data.collectDeath(ID,B,6);
            data=reload(data);
            if(cut==0)data.prepareDeath(record);
            data.commitDeath(ID);
            if(cut<4)check(data.collectDeath(ID,B,7),"Recovered collection at save cut "+cut);
            check(!data.collectDeath(ID,C,8),"Competing/replayed collection at cut "+cut);
            supply(data,12345,0,"Cut "+cut);
            check(data.getBalanceTrace(A)==12099&&data.getBalanceTrace(B)==246,"Exactly one debit and one recipient at cut "+cut);
        }
        var destroyed=accounts(12345);destroyed.prepareDeath(record);destroyed.commitDeath(ID);
        destroyed=reload(destroyed);check(destroyed.destroyDeath(ID,"native_discarded",9),"Ordinary despawn destroys active value");
        supply(destroyed,12099,0,"Despawn sink");
        check(destroyed.supplySnapshot().getStringOr("sink","").equals("246")&&destroyed.getBalanceTrace(A)==12099,"Despawn never debits original owner again");
        destroyed=reload(destroyed);check(!destroyed.destroyDeath(ID,"replay",10)&&!destroyed.collectDeath(ID,B,11),"Despawn replay cannot mint or double-sink");
        var zero=accounts(19);var none=death(ID,A,19,7);zero.prepareDeath(none);zero.commitDeath(ID);
        check(zero.deathIds().isEmpty()&&zero.deathSeen(A,ID),"Zero loss records the death without a world drop");supply(reload(zero),19,0,"Below threshold");
        var reserved=accounts(12345);var trade=new UUID(0,2650);var terms=new AccountTransfer.Terms(A,B,12000,0,"player_trade",7,"fixture");
        reserved.reserve(trade,terms,1);reserved.prepareDeath(record);reserved=reload(reserved);
        check(reserved.commitDeath(ID)&&reserved.transfer(trade).orElseThrow().status().equals(AccountTransfer.CANCELLED),"Death cancels undecided trade reservation after restart");
        check(reserved.reservedDebit(A)==0&&reserved.reservedCredit(B)==0,"Both reservation indexes released");supply(reserved,12099,246,"Cancelled trade then death");
        var broken=encode(legacy);broken.getCompoundOrEmpty("death_currency").putInt("schema",99);
        reject(()->codec().parse(NbtOps.INSTANCE,broken).getOrThrow(),"Future death schema held");
        var mismatched=encode(legacy);mismatched.getCompoundOrEmpty("balances").putLong(A.toString(),1);
        reject(()->codec().parse(NbtOps.INSTANCE,mismatched).getOrThrow(),"Partial restore with mismatched pending balance rejected");
        var missing=record.save();missing.remove("amount");reject(()->DeathCurrencyRecord.load(missing),"Missing amount never becomes zero");
        var emptyExtension=encode(legacy);emptyExtension.put("death_currency",new CompoundTag());
        reject(()->codec().parse(NbtOps.INSTANCE,emptyExtension).getOrThrow(),"Present empty extension cannot masquerade as old account");
        var wrongVisual=image(ID,0,6000);wrongVisual.getCompoundOrEmpty("Item").putString("id","minecraft:diamond");
        reject(()->new DeathCurrencyRecord(ID,A,12345,246,7,1,"minecraft:overworld",wrongVisual,true),"Foreign item cannot replace logical Trace");
        var stacked=image(ID,0,6000);stacked.getCompoundOrEmpty("Item").putInt("count",2);
        reject(()->new DeathCurrencyRecord(ID,A,12345,246,7,1,"minecraft:overworld",stacked,true),"Logical value never becomes multiple items");
        var wrongMarker=image(ID,0,6000);wrongMarker.getCompoundOrEmpty("Item").getCompoundOrEmpty("components").getCompoundOrEmpty("minecraft:custom_data").remove(DeathCurrencyRecord.MARKER);
        reject(()->new DeathCurrencyRecord(ID,A,12345,246,7,1,"minecraft:overworld",wrongMarker,true),"Unmarked inventory currency cannot become a death projection");
        // Two deaths in different runs: ending one run destroys only its own remaining logical value.
        var runs=accounts(10000);var first=death(ID,A,10000,7);runs.prepareDeath(first);runs.commitDeath(ID);
        var second=death(new UUID(0,2627),A,9800,8);runs.prepareDeath(second);runs.commitDeath(second.id());
        check(runs.destroyDeath(ID,"dungeon_dimension_reset",12),"Old run reset removes its own active value");
        check(runs.deathDrop(second.id()).isPresent()&&runs.deathSeen(A,second.id()),"Another run's drop and life remain");
        supply(reload(runs),9604,196,"Run-scoped destruction");
        System.out.println(checks+" death loss, recovery, native-image and supply checks passed");
    }
}
