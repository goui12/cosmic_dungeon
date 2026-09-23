package net.goui.cosmicdungeon.economy;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class WealthReviewChecks {
    private static final UUID A=new UUID(0,3101),B=new UUID(0,3102);
    private static final String DEV="developer:"+new UUID(0,3103);
    private static int checks,nativeImages;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private interface Work {void run()throws Exception;}
    private static void reject(Work work,String why){boolean failed=false;try{work.run();}catch(Exception expected){failed=true;}check(failed,why);}
    @SuppressWarnings("unchecked") private static Codec<PlayerCurrencyData> codec()throws Exception{
        var f=PlayerCurrencyData.class.getDeclaredField("CODEC");f.setAccessible(true);return (Codec<PlayerCurrencyData>)f.get(null);
    }
    private static CompoundTag encode(PlayerCurrencyData data)throws Exception{return (CompoundTag)codec().encodeStart(NbtOps.INSTANCE,data).getOrThrow();}
    private static PlayerCurrencyData decode(CompoundTag tag)throws Exception{return codec().parse(NbtOps.INSTANCE,tag).getOrThrow();}
    private static CompoundTag legacy(long first,long second){
        var root=new CompoundTag();var balances=new CompoundTag();balances.putLong(A.toString(),first);balances.putLong(B.toString(),second);root.put("balances",balances);return root;
    }
    private static CompoundTag row(long before,long after,String status){
        var details=new CompoundTag();details.putString("large_item_plan","not copied into the notification");
        return EconomyLedger.row("reward",A,"Named Owner","mob_reward",after-before,before,after,"mob",31,status,1_800_000_000_000L,details);
    }
    private static WealthReviewState crossing(){
        return new WealthReviewState().capture(row(0,100_000_000,"committed"),new long[]{500_000,80_000_000,100_000_000},100_000_000);
    }
    private static PlayerCurrencyData nativeRoundTrip(PlayerCurrencyData data,Path file)throws Exception{
        var root=new CompoundTag();root.put("data",encode(data));NbtIo.writeCompressed(root,file);
        var read=NbtIo.readCompressed(file,NbtAccounter.create(64L*1024*1024));
        var restored=decode(read.getCompoundOrEmpty("data"));nativeImages++;
        check(encode(restored).equals(encode(data)),"Exact account, inbox and ledger native image");
        return restored;
    }
    private static void model(){
        var inbox=crossing();
        check(inbox.pendingCount()==3&&inbox.openFinalCount()==1,"First credit creates three durable reviews including final");
        check(inbox.capture(row(0,100_000_000,"committed"),new long[]{500_000,80_000_000,100_000_000},100_000_000)==inbox,"Crossing replay preserves first evidence");
        check(new WealthReviewState().capture(row(0,100_000_000,"reserved"),new long[]{500_000},100_000_000).pendingCount()==0,"Reservations never notify");
        check(new WealthReviewState().capture(row(100_000_000,1,"committed"),new long[]{500_000},100_000_000).pendingCount()==0,"Debits never create crossings");
        check(new WealthReviewState().capture(row(500_000,500_001,"committed"),new long[]{500_000},100_000_000).pendingCount()==0,"Already-at-threshold credit is not a crossing");
        check(new WealthReviewState().capture(row(0,500_000,"committed"),new long[]{500_000,500_000,500_000},500_000).pendingCount()==1,"Equal configured thresholds deduplicate");
        var n=inbox.find(A,500_000).orElseThrow();
        check(n.evidence().getStringOr("name","").equals("Named Owner")&&n.evidence().getStringOr("transaction","").equals("reward")
                &&n.evidence().getLongOr("before",-1)==0&&n.evidence().getLongOr("after",0)==100_000_000,"Original named transaction and amounts retained");
        check(n.evidence().getCompoundOrEmpty("details").isEmpty(),"Notification excludes large item custody images");
        var leaked=n.evidence();leaked.putLong("after",0);
        check(n.evidence().getLongOr("after",0)==100_000_000,"Evidence is defensively copied");
        var ack=inbox.decide(A,500_000,0,"ack",DEV,"",10);
        check(ack.pendingCount()==2&&ack.find(A,500_000).orElseThrow().revision()==1,"Acknowledgment removes delivery only");
        check(ack.decide(A,500_000,0,"ack",DEV,"",11)==ack,"Identical uncertain-write retry is idempotent");
        reject(()->ack.decide(A,500_000,0,"resolve",DEV,"checked",12),"Stale competing reviewer cannot overwrite acknowledgment");
        reject(()->ack.decide(A,500_000,0,"ack","console","",12),"Different actor cannot claim stale identical request");
        var resolved=ack.decide(A,100_000_000,0,"resolve","console","Verified source of wealth",20);
        check(resolved.openFinalCount()==0&&resolved.pendingCount()==1,"Resolution closes final review and acknowledges notice");
        check(resolved.openFinalCount(500_000)==1,"Changed maximum includes the existing lower-threshold open review without deleting historical decisions");
        var reopened=resolved.decide(A,100_000_000,1,"reopen",DEV,"New information",30);
        check(reopened.openFinalCount()==1&&reopened.pendingCount()==2,"Reopen creates fresh delivery without replacing evidence");
        check(reopened.find(A,100_000_000).orElseThrow().sequence()>3,"Reopened review reaches already-connected developer cursor");
        check(reopened.find(A,100_000_000).orElseThrow().evidence().equals(inbox.find(A,100_000_000).orElseThrow().evidence()),"First evidence survives decisions");
        reject(()->inbox.decide(A,500_000,0,"resolve",DEV," ",10),"Blank resolution rejected");
        reject(()->inbox.decide(A,500_000,0,"resolve",DEV,"x".repeat(513),10),"Oversized audit note rejected");
        reject(()->inbox.decide(A,500_000,0,"ack","operator","",10),"Unbound actor rejected");
        reject(()->inbox.decide(B,500_000,0,"ack",DEV,"",10),"Foreign owner rejected");
        reject(()->inbox.decide(A,500_000,0,"reopen",DEV,"already open",10),"Open review cannot be reopened");
        check(WealthReviewState.load(reopened.save()).save().equals(reopened.save()),"Review schema round trip");
        for(String missing:List.of("schema","sequence","entries")){
            var damaged=inbox.save();damaged.remove(missing);reject(()->WealthReviewState.load(damaged),"Incomplete inbox held: "+missing);
        }
        var future=inbox.save();future.putInt("schema",99);reject(()->WealthReviewState.load(future),"Future schema held");
        var fraction=inbox.save();fraction.putDouble("sequence",3.5);reject(()->WealthReviewState.load(fraction),"Fractional sequence cannot be silently truncated");
        for(String missing:List.of("owner","threshold","final","origin","evidence","sequence","revision","acknowledged","resolved","action","actor","note","expected_revision","decision_time")){
            var damaged=inbox.save();damaged.getCompoundOrEmpty("entries").getCompoundOrEmpty(A+"|500000").remove(missing);
            reject(()->WealthReviewState.load(damaged),"Incomplete notice held: "+missing);
        }
        var duplicate=inbox.save();duplicate.getCompoundOrEmpty("entries").getCompoundOrEmpty(A+"|80000000").putLong("sequence",1);
        reject(()->WealthReviewState.load(duplicate),"Duplicate delivery sequences held");
        var foreign=inbox.save();foreign.getCompoundOrEmpty("entries").getCompoundOrEmpty(A+"|500000").getCompoundOrEmpty("evidence").putString("player",B.toString());
        reject(()->WealthReviewState.load(foreign),"Foreign evidence held");
        var falseCrossing=inbox.save();falseCrossing.getCompoundOrEmpty("entries").getCompoundOrEmpty(A+"|500000").getCompoundOrEmpty("evidence").putLong("before",500000);
        reject(()->WealthReviewState.load(falseCrossing),"Impossible crossing held");
        check(inbox.pending(1,1).getFirst().sequence()==2&&inbox.owner(A,500_000,1).getFirst().threshold()==80_000_000,"Bounded stable pagination");
    }
    private static void delivery(){
        var inbox=crossing();var delivery=new WealthReviewDelivery<String>();var sent=new ArrayList<String>();var proofs=new AtomicInteger();
        java.util.function.BooleanSupplier pass=()->{proofs.incrementAndGet();return true;};
        java.util.function.BiConsumer<String,WealthReviewState.Notice> send=(who,n)->sent.add(who+":"+n.sequence());
        check(delivery.deliver(List.of(),inbox,2,true,pass,send)==0&&proofs.get()==1,"Offline developers do not lose persisted notices");
        check(delivery.deliver(List.of("first","second"),inbox,2,false,()->false,send)==0&&sent.isEmpty(),"Failed persistence sends nothing and advances no cursor");
        check(delivery.deliver(List.of("first","second"),inbox,2,false,pass,send)==2&&sent.equals(List.of("first:1","second:1")),"Global budget distributes delivery fairly");
        check(delivery.deliver(List.of("first","second"),inbox,2,false,pass,send)==2&&sent.get(2).equals("first:2"),"Next poll advances both developers");
        check(delivery.deliver(List.of("first","second"),inbox,2,false,pass,send)==2,"Last pending notice delivered to both");
        int before=proofs.get();
        check(delivery.deliver(List.of("first","second"),inbox,2,false,pass,send)==0&&proofs.get()==before,"No repeated chat or disk writes once live cursors catch up");
        check(inbox.pendingCount()==3,"Chat delivery never acknowledges durable inbox");
        delivery.forget("first");
        check(delivery.deliver(List.of("first","second"),inbox,1,false,pass,send)==1&&sent.getLast().equals("first:1"),"Reconnect replays unacknowledged evidence");
        var ack=inbox.decide(A,500_000,0,"ack",DEV,"",10);delivery.forget("first");
        check(delivery.deliver(List.of("first","second"),ack,1,false,pass,send)==1&&sent.getLast().equals("first:2"),"Acknowledged notice stays suppressed on reconnect");
        var restarted=new WealthReviewDelivery<String>();
        check(restarted.deliver(List.of("first"),WealthReviewState.load(ack.save()),1,false,pass,send)==1&&sent.getLast().equals("first:2"),"Server restart restores pending durable notices");
        var interrupted=new WealthReviewDelivery<String>();
        reject(()->interrupted.deliver(List.of("first"),inbox,1,false,pass,(who,n)->{throw new IllegalStateException("network");}),"Send failure is visible");
        check(interrupted.deliver(List.of("first"),inbox,1,false,pass,send)==1&&sent.getLast().equals("first:1"),"Send failure cannot advance its cursor");
        var diskRetry=new WealthReviewDelivery<String>();int previous=proofs.get();
        diskRetry.deliver(List.of(),inbox,1,true,()->false,send);
        diskRetry.deliver(List.of(),inbox,1,false,pass,send);
        check(proofs.get()==previous+1,"Failed legacy-observation write retries even without online developers");
    }
    private static void account(Path dir)throws Exception{
        var capped=legacy(120_000_000,0);var overrides=new CompoundTag();overrides.putLong(A.toString(),150_000_000);capped.put("capacity_overrides",overrides);
        var d=decode(capped);check(d.wealthReviews().pendingCount()==0,"Loading an old account does not invent historical crossings");
        boolean first=d.observeLegacyWealth(1,1000);
        check(first|d.observeLegacyWealth(1,1001),"Bounded startup discovers offline old account in either owner order");
        check(d.wealthReviews().pendingCount()==3&&d.getBalanceTrace(A)==120_000_000&&d.getCapacityTrace(A)==150_000_000,"Over-cap legacy balance and explicit capacity remain intact");
        check(d.wealthReviews().find(A,100_000_000).orElseThrow().origin().equals("legacy_balance"),"Old cap status explicitly labeled observation");
        check(d.finalReviews().getCompoundOrEmpty(A.toString()).getStringOr("review_origin","").equals("legacy_balance"),"Legacy final-review API also exposes honest origin");
        check(d.supplySnapshot().getStringOr("difference","").equals("0")&&d.supplySnapshot().getStringOr("generation","").equals("0"),"Observations neither mint nor sink money");
        check(!d.observeLegacyWealth(2,2000)&&!d.observeWealth(A,2000),"Legacy replay cannot duplicate evidence");
        var source=d.wealthReviews().find(A,100_000_000).orElseThrow().evidence();
        check(source.getLongOr("before",0)==120_000_000&&source.getLongOr("after",0)==120_000_000&&source.getLongOr("requested_trace",-1)==0,"Observation never fabricates original earning amount");
        d=nativeRoundTrip(d,dir.resolve("old-cap.nbt"));
        long amount=d.getBalanceTrace(A);
        check(d.decideWealth(A,100_000_000,0,"resolve",DEV,"Approved existing balance",3000),"Developer resolution changes state");
        var committed=encode(d);
        check(!d.decideWealth(A,100_000_000,0,"resolve",DEV,"Approved existing balance",3001)&&encode(d).equals(committed),"Identical review retry adds no ledger row");
        check(d.getBalanceTrace(A)==amount&&d.getCapacityTrace(A)==150_000_000,"Resolution does not confiscate money or change capacity");
        d.change(A,"A",-1,"vendor_retail","vendor",0,"spend",false);
        check(d.getBalanceTrace(A)==amount-1,"Reviewed over-cap owner can still spend");
        check(d.wealthReviews().find(A,100_000_000).orElseThrow().resolved(),"Spending cannot clear or reopen review");
        d=nativeRoundTrip(d,dir.resolve("resolved.nbt"));
        var immediate=decode(legacy(100_000_000,0));immediate.clear(A);
        check(immediate.wealthReviews().pendingCount()==3&&immediate.getBalanceTrace(A)==0,"Immediate old-account reset cannot erase its unobserved cap");
        check(immediate.wealthReviews().find(A,100_000_000).orElseThrow().evidence().getLongOr("before",0)==100_000_000,"Legacy evidence captures pre-reset balance");
        var markers=legacy(1,0);var list=new ListTag();list.add(StringTag.valueOf(A+"|500000"));markers.put("wealth_crossings",list);
        var firstCredit=decode(markers);firstCredit.change(A,"A",499_999,"mob_reward","mob",0,"credit-before-observer",false);
        check(firstCredit.wealthReviews().find(A,500_000).orElseThrow().origin().equals("legacy_marker"),"A new crossing cannot replace unknown old first-notification evidence");
        var old=decode(markers);old.observeWealth(A,4000);
        check(old.wealthReviews().pendingCount()==1&&old.wealthReviews().find(A,500_000).orElseThrow().origin().equals("legacy_marker"),"Old offline-delivery marker is reviewable after balance falls");
        check(old.wealthReviews().find(A,500_000).orElseThrow().evidence().getLongOr("before",0)==1,"Unknown historical amount not invented from legacy marker");
        check(encode(old).getListOrEmpty("wealth_crossings").equals(list),"Original legacy marker preserved");
        var configChanged=decode(encode(old));long previousMaximum=D1EconomyConfig.WEALTH_MAX.get();
        try{
            D1EconomyConfig.WEALTH_MAX.set(1L);
            configChanged.decideWealth(A,500_000,0,"ack",DEV,"",4500);
            check(configChanged.wealthReviews().find(A,1).isEmpty()&&!configChanged.finalReviews().contains(A.toString()),
                    "Review-only decision cannot create a legacy final row while discarding its paired notice");
            check(configChanged.observeWealth(A,4501)&&configChanged.wealthReviews().find(A,1).isPresent()
                    &&configChanged.finalReviews().contains(A.toString()),"Changed maximum observation saves both pieces together");
            check(configChanged.wealthReviews().find(A,500_000).orElseThrow().acknowledged(),"Later observation retains the operator acknowledgment");
        }finally{D1EconomyConfig.WEALTH_MAX.set(previousMaximum);}
        var bad=markers.copy();var badList=new ListTag();badList.add(StringTag.valueOf("bad-marker"));bad.put("wealth_crossings",badList);
        reject(()->decode(bad),"Malformed old markers cannot silently disappear");
        var historical=decode(legacy(0,0));historical.setBalanceTrace(A,100_000_000);historical.change(A,"A",-10,"vendor_retail","vendor",0,"spend",false);
        var oldImage=encode(historical);oldImage.remove("wealth_review");
        var recovered=decode(oldImage);recovered.observeWealth(A,5000);
        check(recovered.wealthReviews().find(A,100_000_000).orElseThrow().origin().equals("legacy_final"),"Existing final evidence is reused after spending");
        check(recovered.wealthReviews().find(A,100_000_000).orElseThrow().evidence().getLongOr("before",-1)==0,"Original final crossing preserved");
        var transfer=decode(legacy(499_999,10));var tx=UUID.randomUUID();var terms=new AccountTransfer.Terms(B,A,1,0,"player_trade",0,"review");
        transfer.reserve(tx,terms,1);check(transfer.wealthReviews().pendingCount()==0,"Transfer reservation cannot notify");
        transfer.commitTransfer(tx,terms,2);
        check(transfer.wealthReviews().find(A,500_000).orElseThrow().evidence().getStringOr("type","").equals("player_trade"),"Transfer path captures reviews without CurrencyAudit callback");
        var image=encode(transfer);transfer.commitTransfer(tx,terms,3);
        check(encode(transfer).equals(image),"Transfer retry cannot duplicate notices");
        // Paired ledger write: first owner's crossing must roll back when the second row hits the outbox limit.
        var blocked=decode(legacy(499_999,10));var pair=new AccountTransfer.Terms(A,B,0,1,"player_trade",0,"review");var pairId=UUID.randomUUID();
        blocked.reserve(pairId,pair,1);var full=encode(blocked);var ledger=full.getCompoundOrEmpty("ledger");var outbox=ledger.getCompoundOrEmpty("outbox");
        for(long i=3;i<=4095;i++)outbox.put(Long.toString(i),row(0,1,"committed"));ledger.putLong("sequence",4095);
        blocked=decode(full);var before=encode(blocked);var held=blocked;
        reject(()->held.commitTransfer(pairId,pair,2),"Second ledger row may reject an otherwise valid transfer");
        check(encode(blocked).equals(before)&&blocked.wealthReviews().pendingCount()==0,"Failed paired transaction restores balance, receipt, ledger and first-owner review together");
        // The same capacity failure must preserve an unacknowledged inbox and reject the operator decision.
        var pending=encode(old);var pendingLedger=pending.getCompoundOrEmpty("ledger");var pendingOutbox=new CompoundTag();
        for(long i=1;i<=4096;i++)pendingOutbox.put(Long.toString(i),row(0,1,"committed"));
        pendingLedger.put("outbox",pendingOutbox);pendingLedger.putLong("sequence",4096);
        var decisionBlocked=decode(pending);var saved=encode(decisionBlocked);
        reject(()->decisionBlocked.decideWealth(A,500_000,0,"ack",DEV,"",6000),"Full ledger prevents unrecorded review decisions");
        check(encode(decisionBlocked).equals(saved),"Failed decision leaves entire native account unchanged");
        for(int cut=0;cut<6;cut++){
            var run=decode(legacy(499_999,0));
            run=nativeRoundTrip(run,dir.resolve("cut-"+cut+"-before.nbt"));
            if(cut==0)run=nativeRoundTrip(run,dir.resolve("cut-"+cut+"-load.nbt"));
            run.change(A,"A",1,"mob_reward","mob",0,"mob:31:"+cut,false);
            if(cut==1)run=nativeRoundTrip(run,dir.resolve("cut-"+cut+"-credit.nbt"));
            check(run.wealthReviews().pendingCount()==1,"Committed balance and first notification are one image at cut "+cut);
            run.decideWealth(A,500_000,0,"ack",DEV,"",7000);
            if(cut==2)run=nativeRoundTrip(run,dir.resolve("cut-"+cut+"-ack.nbt"));
            run.decideWealth(A,500_000,1,"resolve",DEV,"Verified",7100);
            if(cut==3)run=nativeRoundTrip(run,dir.resolve("cut-"+cut+"-resolve.nbt"));
            run.decideWealth(A,500_000,2,"reopen","console","Recheck",7200);
            if(cut==4)run=nativeRoundTrip(run,dir.resolve("cut-"+cut+"-reopen.nbt"));
            run=nativeRoundTrip(run,dir.resolve("cut-"+cut+"-final.nbt"));
            var finalImage=encode(run);
            check(!run.decideWealth(A,500_000,2,"reopen","console","Recheck",7300)&&encode(run).equals(finalImage),"Restart decision retry preserves exact image at cut "+cut);
            check(run.getBalanceTrace(A)==500_000&&run.wealthReviews().pendingCount()==1,"Money and pending reopened review preserved at cut "+cut);
            var decisions=finalImage.getCompoundOrEmpty("ledger").getCompoundOrEmpty("outbox");
            check(decisions.size()==4,"Exactly one earning and three operator ledger decisions at cut "+cut);
        }
    }
    public static void main(String[] args)throws Exception{
        model();delivery();Path dir=Files.createTempDirectory("wealth-review-");
        try{account(dir);}finally{try(var files=Files.list(dir)){for(var file:files.toList())Files.deleteIfExists(file);}Files.deleteIfExists(dir);}
        System.out.println(checks+" wealth review checks passed; "+nativeImages+" native compressed account images");
    }
}
