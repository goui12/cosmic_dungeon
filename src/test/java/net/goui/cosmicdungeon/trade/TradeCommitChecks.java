package net.goui.cosmicdungeon.trade;
import com.mojang.serialization.Codec;
import net.goui.cosmicdungeon.economy.*;
import net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairTransactions;
import net.minecraft.nbt.*;
import java.util.*;
public final class TradeCommitChecks {
    private static int checks;
    private static final UUID A=new UUID(0,1),B=new UUID(0,2),TX=new UUID(0,3),SESSION=new UUID(0,4);
    private static final AccountTransfer.Terms TERMS=new AccountTransfer.Terms(A,B,30,7,"player_trade",12,"fixture");
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static void reject(Runnable r,String why){boolean failed=false;try{r.run();}catch(RuntimeException expected){failed=true;}check(failed,why);}
    @SuppressWarnings("unchecked") private static Codec<PlayerCurrencyData> codec()throws Exception{
        var f=PlayerCurrencyData.class.getDeclaredField("CODEC");f.setAccessible(true);return (Codec<PlayerCurrencyData>)f.get(null);
    }
    private static CompoundTag item(String id,int count){var t=new CompoundTag();t.putString("id",id);t.putInt("count",count);
        var c=new CompoundTag();var nested=new CompoundTag();nested.putByteArray("exact",new byte[]{-1,127,0});c.put("minecraft:custom_data",nested);t.put("components",c);return t;}
    private static CompoundTag custody(UUID owner,UUID peer,String offer,String cursor){
        var c=TradeCustodyImages.open(SESSION,owner,peer,12);var offers=(ListTag)c.get("offers");offers.set(8,item(offer,2));
        return TradeCustodyImages.bind(TradeCustodyImages.live(c,offers,item(cursor,3)),TX);
    }
    private static TradeCommitPlan plan(){return new TradeCommitPlan(A,B,custody(A,B,"minecraft:iron_sword","minecraft:apple"),custody(B,A,"minecraft:diamond_sword","minecraft:carrot"),false,false);}
    private static PlayerCurrencyData fresh(Codec<PlayerCurrencyData> codec){var d=codec.parse(NbtOps.INSTANCE,new CompoundTag()).getOrThrow();
        d.setBalanceTrace(A,100);d.setBalanceTrace(B,50);d.setCapacityTrace(A,1000);d.setCapacityTrace(B,1000);d.reserve(TX,TERMS,1);d.prepareTrade(TX,plan());return d;}
    private static CompoundTag encoded(Codec<PlayerCurrencyData> codec,PlayerCurrencyData d){return (CompoundTag)codec.encodeStart(NbtOps.INSTANCE,d).getOrThrow();}
    private static CompoundTag player(CompoundTag custody){var p=new CompoundTag();p.put("custody",custody);p.put("returns",new ListTag());return p;}
    private static CompoundTag apply(TradeCommitPlan plan,UUID owner,String outcome,CompoundTag before){
        var p=before.copy();if(RepairTransactions.receiptMatches(p.getCompoundOrEmpty("receipt"),owner,TX,outcome))return p;
        var next=plan.project(owner,p.getCompoundOrEmpty("custody"),outcome.equals(AccountTransfer.COMMITTED));
        for(var item:TradeCustodyImages.held(next))((ListTag)p.get("returns")).add(item);
        p.remove("custody");p.put("receipt",RepairTransactions.receiptImage(owner,TX,outcome));return p;
    }
    private static int count(CompoundTag p,String id){int count=0;for(var t:(ListTag)p.get("returns"))if(((CompoundTag)t).getStringOr("id","").equals(id))count+=((CompoundTag)t).getIntOr("count",0);return count;}
    private record Cut(String label,CompoundTag money,CompoundTag first,CompoundTag second){}
    private static Cut cut(String label,CompoundTag money,CompoundTag first,CompoundTag second){return new Cut(label,money.copy(),first.copy(),second.copy());}
    public static void main(String[] args)throws Exception{
        var codec=codec();var d=fresh(codec);var p=plan();
        check(d.pendingTrade(A).orElseThrow().equals(TX)&&d.pendingTrade(B).orElseThrow().equals(TX),"Both unresolved owners indexed");
        check(codec.parse(NbtOps.INSTANCE,encoded(codec,d)).getOrThrow().tradePlan(TX).orElseThrow().equals(p),"Native account codec preserves exact item evidence");
        reject(()->d.acknowledgeTrade(TX,A),"Undecided trade cannot be acknowledged");
        var reserved=encoded(codec,d);var first=player(p.firstBefore());var second=player(p.secondBefore());var cuts=new ArrayList<Cut>();
        cuts.add(cut("prepared",reserved,first,second));
        var committed=d.commitTrade(TX,2);check(committed.status().equals(AccountTransfer.COMMITTED),"Both balances commit in one receipt");
        check(d.getBalanceTrace(A)==77&&d.getBalanceTrace(B)==73,"Whole incoming/outgoing values applied exactly");
        check(d.getBalanceTrace(A)+d.getBalanceTrace(B)==150,"Player trade conserves Trace supply");
        cuts.add(cut("commit not saved",reserved,first,second));var money=encoded(codec,d);cuts.add(cut("commit saved",money,first,second));
        var firstAfter=apply(p,A,AccountTransfer.COMMITTED,first);cuts.add(cut("first projection not saved",money,first,second));
        first=firstAfter;cuts.add(cut("first saved",money,first,second));d.acknowledgeTrade(TX,A);cuts.add(cut("first ack not saved",money,first,second));
        money=encoded(codec,d);cuts.add(cut("first ack saved",money,first,second));
        var secondAfter=apply(p,B,AccountTransfer.COMMITTED,second);cuts.add(cut("second projection not saved",money,first,second));
        second=secondAfter;cuts.add(cut("second saved",money,first,second));d.acknowledgeTrade(TX,B);cuts.add(cut("second ack not saved",money,first,second));
        money=encoded(codec,d);cuts.add(cut("both acknowledged",money,first,second));
        var directory=java.nio.file.Files.createTempDirectory("trade-cuts-");
        try{for(var cut:cuts){
            var mf=directory.resolve("money.dat");var af=directory.resolve("a.dat");var bf=directory.resolve("b.dat");
            NbtIo.writeCompressed(cut.money(),mf);NbtIo.writeCompressed(cut.first(),af);NbtIo.writeCompressed(cut.second(),bf);
            var loaded=codec.parse(NbtOps.INSTANCE,NbtIo.readCompressed(mf,NbtAccounter.unlimitedHeap())).getOrThrow();
            var a=NbtIo.readCompressed(af,NbtAccounter.unlimitedHeap());var b=NbtIo.readCompressed(bf,NbtAccounter.unlimitedHeap());
            var decision=loaded.transfer(TX).orElseThrow();if(decision.reserved())decision=loaded.cancelTransfer(TX,TERMS,3,"restart");
            boolean success=decision.status().equals(AccountTransfer.COMMITTED);var recovered=loaded.tradePlan(TX).orElse(p);
            if(loaded.pendingTrade(A).isPresent()){a=apply(recovered,A,decision.status(),a);loaded.acknowledgeTrade(TX,A);}
            if(loaded.pendingTrade(B).isPresent()){b=apply(recovered,B,decision.status(),b);loaded.acknowledgeTrade(TX,B);}
            check(loaded.getBalanceTrace(A)==(success?77:100)&&loaded.getBalanceTrace(B)==(success?73:50),cut.label()+": atomic money outcome");
            check(count(a,success?"minecraft:diamond_sword":"minecraft:iron_sword")==2,cut.label()+": first correct offer once");
            check(count(b,success?"minecraft:iron_sword":"minecraft:diamond_sword")==2,cut.label()+": second correct offer once");
            check(count(a,"minecraft:apple")==3&&count(a,"minecraft:carrot")==0,cut.label()+": first own cursor");
            check(count(b,"minecraft:carrot")==3&&count(b,"minecraft:apple")==0,cut.label()+": second own cursor");
            check(loaded.pendingTrade(A).isEmpty()&&loaded.pendingTrade(B).isEmpty(),cut.label()+": pending indices clear");
            check(apply(recovered,A,decision.status(),a).equals(a)&&apply(recovered,B,decision.status(),b).equals(b),cut.label()+": participant replay no extra items");
            var replay=codec.parse(NbtOps.INSTANCE,encoded(codec,loaded)).getOrThrow();
            check(replay.pendingTrade(A).isEmpty()&&replay.pendingTrade(B).isEmpty(),cut.label()+": acknowledgements survive restart");
            long before=replay.getBalanceTrace(A);replay.commitTrade(TX,5);check(replay.getBalanceTrace(A)==before,cut.label()+": terminal money replay does not change balance");
        }}finally{for(String name:List.of("money.dat","a.dat","b.dat"))java.nio.file.Files.deleteIfExists(directory.resolve(name));java.nio.file.Files.deleteIfExists(directory);}
        var mismatch=p.firstBefore();((CompoundTag)((ListTag)mismatch.get("offers")).get(8)).putInt("count",5);
        reject(()->p.project(A,mismatch,true),"Changed offered count held for review");
        reject(()->p.project(A,new CompoundTag(),true),"Absent owner evidence cannot create replacement gear");
        reject(()->p.project(new UUID(8,8),p.firstBefore(),true),"Foreign owner cannot claim");
        check(p.project(A,p.firstBefore(),false).equals(p.firstBefore()),"Cancellation returns exact originals");
        check(TradeFinalizationService.validate(20,5,100,50,100,100,true,true)==TradeFinalizationService.Result.SUCCESS,"Currency-only preflight");
        check(TradeFinalizationService.validate(0,0,0,0,0,0,true,true)==TradeFinalizationService.Result.SUCCESS,"Item-only preflight");
        check(TradeFinalizationService.validate(10,10,100,100,0,0,true,true)==TradeFinalizationService.Result.CANNOT_RECEIVE_CURRENCY,"Outgoing does not net away full incoming-capacity gate");
        check(TradeFinalizationService.validate(0,0,100,100,100,100,false,true)==TradeFinalizationService.Result.NOT_ENOUGH_INVENTORY_SPACE,"Full inventory rejects before mutation");
        check(TradeFinalizationService.validate(101,0,100,100,100,100,true,true)==TradeFinalizationService.Result.INSUFFICIENT_BALANCE,"Insufficient full offered payment rejected");
        System.out.println(checks+" trade commit and interrupted-save checks passed");
    }
}
