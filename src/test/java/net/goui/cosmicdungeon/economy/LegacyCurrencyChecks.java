package net.goui.cosmicdungeon.economy;

import com.mojang.serialization.Codec;
import net.goui.cosmicdungeon.vendor.CommerceCustodyImages;
import net.minecraft.nbt.*;
import java.nio.file.*;
import java.util.*;

/** Real account CODEC/native NBT and pure legacy policy; no server/client launch. */
public final class LegacyCurrencyChecks {
    private static final UUID A=new UUID(0,3201),B=new UUID(0,3202),C=new UUID(0,3203);
    private static int checks,images;
    private static void check(boolean ok,String name){checks++;if(!ok)throw new AssertionError(name);}
    private interface Work {void run()throws Exception;}
    private static void reject(Work action,String name){boolean failed=false;try{action.run();}catch(Exception expected){failed=true;}check(failed,name);}
    @SuppressWarnings("unchecked") private static Codec<PlayerCurrencyData> codec()throws Exception {
        var field=PlayerCurrencyData.class.getDeclaredField("CODEC");field.setAccessible(true);return (Codec<PlayerCurrencyData>)field.get(null);
    }
    private static CompoundTag encode(PlayerCurrencyData data)throws Exception {
        return (CompoundTag)codec().encodeStart(NbtOps.INSTANCE,data).getOrThrow();
    }
    private static PlayerCurrencyData decode(CompoundTag data)throws Exception{return codec().parse(NbtOps.INSTANCE,data).getOrThrow();}
    private static PlayerCurrencyData disk(PlayerCurrencyData data,Path file)throws Exception {
        var image=new CompoundTag();image.put("data",encode(data));NbtIo.writeCompressed(image,file);
        var loaded=decode(NbtIo.readCompressed(file,NbtAccounter.create(64L*1024*1024)).getCompoundOrEmpty("data"));
        check(encode(loaded).equals(encode(data)),"native account round trip preserves every field");images++;return loaded;
    }
    private static void intervals() {
        var retired=new RetiredRewardRuns();
        check(retired.retire(2)&&retired.retire(4)&&retired.rangeCount()==2,"unretired gap remains open");
        check(!retired.contains(1)&&!retired.contains(3)&&!retired.contains(5),"no guessed high-water mark");
        check(retired.retire(3)&&retired.rangeCount()==1,"bridge merges exact adjacent runs");
        check(retired.retire(1)&&retired.contains(4)&&retired.rangeCount()==1,"left adjacency");
        check(!retired.retire(2),"repeated retirement is idempotent");
        check(retired.retire(Long.MAX_VALUE)&&retired.retire(Long.MAX_VALUE-1),"maximum range no overflow");
        check(retired.contains(Long.MAX_VALUE)&&!retired.contains(Long.MAX_VALUE-2),"maximum exact containment");
        var copy=RetiredRewardRuns.load(retired.save());
        check(copy.save().equals(retired.save()),"native interval shape round trip");
        reject(()->retired.retire(0),"zero run cannot retire");reject(()->retired.retire(-1),"negative run cannot retire");
        check(copy.blocks("mob:2:old-format",0),"old suffix retained");
        check(copy.blocks("mob:7:x",2),"declared retired run cannot be hidden by mismatched ID");
        check(!copy.blocks("inn:2:x",2),"other workflows not retired with mob receipts");
        for(String id:List.of("mob:","mob:x:uuid","mob:-1:uuid","mob:0:uuid","mob:01:uuid","mob:9223372036854775808:uuid","inn:2:x"))
            check(RetiredRewardRuns.transactionRun(id)==0,"unrecognized prefix does not invent retirement");
        var future=copy.save();future.putInt("schema",2);reject(()->RetiredRewardRuns.load(future),"future retirement schema rejected");
        var wrong=copy.save();wrong.putString("intervals","damaged");reject(()->RetiredRewardRuns.load(wrong),"wrong interval type rejected");
        for(long[] values:new long[][]{{0,1},{2,1},{-2,-1}}) {
            var root=new CompoundTag();root.putInt("schema",1);var list=new ListTag();var row=new CompoundTag();
            row.putLong("first",values[0]);row.putLong("last",values[1]);list.add(row);root.put("intervals",list);
            reject(()->RetiredRewardRuns.load(root),"invalid interval endpoints");
        }
        var overlap=new CompoundTag();overlap.putInt("schema",1);var list=new ListTag();
        for(long[] ends:new long[][]{{2,4},{4,5}}){var row=new CompoundTag();row.putLong("first",ends[0]);row.putLong("last",ends[1]);list.add(row);}
        overlap.put("intervals",list);reject(()->RetiredRewardRuns.load(overlap),"overlap rejected");
        var random=new Random(32);var expected=new HashSet<Long>();var actual=new RetiredRewardRuns();
        for(int i=0;i<300;i++){
            long run=1+random.nextInt(1000);check(actual.retire(run)==expected.add(run),"random retirement idempotence");
        }
        actual=RetiredRewardRuns.load(actual.save());
        for(long run=1;run<=1000;run++)check(actual.contains(run)==expected.contains(run),"exact holes survive interval compaction");
    }
    private static void policies() {
        for(var denomination:CurrencyDenomination.values()) {
            String item="cosmicdungeon:attunement_"+denomination.id();
            check(LegacyCurrencyPolicy.classify(item,false)==LegacyCurrencyPolicy.Kind.LEGACY_REVIEW,"old registry needs review");
            check(LegacyCurrencyPolicy.classify(item,true)==LegacyCurrencyPolicy.Kind.MANAGED_DEATH,"death marker stays on managed path");
            check(LegacyCurrencyPolicy.nominalTrace(item,64)==denomination.traceValue()*64,"nominal count only");
            reject(()->LegacyCurrencyPolicy.nominalTrace(item,0),"zero count cannot grant");
            reject(()->LegacyCurrencyPolicy.nominalTrace(item,-1),"negative count cannot grant");
            if(denomination.traceValue()>1)reject(()->LegacyCurrencyPolicy.nominalTrace(item,Long.MAX_VALUE),"overflow evidence rejected");
        }
        for(String item:List.of("minecraft:gold_ingot","minecraft:iron_ingot","other:attunement_trace",
                "cosmicdungeon:attunement_future","cosmicdungeon:raw_farrows_chop","cosmicdungeon:attunement_trace_extra")) {
            check(LegacyCurrencyPolicy.classify(item,false)==LegacyCurrencyPolicy.Kind.ORDINARY,"no material/name inference");
            reject(()->LegacyCurrencyPolicy.nominalTrace(item,1),"unknown denomination has no nominal credit");
        }
    }
    private static void account(Path dir)throws Exception {
        var old=new CompoundTag();var balances=new CompoundTag();balances.putLong(A.toString(),12345);old.put("balances",balances);
        var legacy=decode(old);
        check(legacy.getBalanceTrace(A)==12345,"old balance retained without retirement extension");
        check(legacy.receiptRetentionSummary().getIntOr("retired_run_intervals",-1)==0,"old missing history not invented");
        var d=decode(new CompoundTag());
        check(d.change(A,"A",7,"dungeon_mob","mob",31,"mob:31:one",false)==7,"live reward commits");
        check(d.change(A,"A",2,"dungeon_mob","mob",32,"mob:32:two",false)==2,"adjacent active run commits");
        d.change(A,"A",1,"first_trace","",0,"first_trace",false);
        d.change(A,"A",1,"system_reward","",0,"inn:retained",false);
        var pre=disk(d,dir.resolve("before.dat"));
        d.clearRunReceipts(31);
        var post=disk(d,dir.resolve("after.dat"));
        long balance=post.getBalanceTrace(A);var evidence=encode(post).getCompoundOrEmpty("ledger").copy();
        check(!encode(post).getCompoundOrEmpty("receipts").contains("mob:31:one|"+A),"retired itemized mob row released");
        check(post.hasReceipt("mob:31:one",A),"retired run still answers replay guard");
        check(post.change(A,"A",7,"dungeon_mob","mob",31,"mob:31:one",false)==-1,"post-retirement old reward cannot replay");
        check(post.change(B,"B",7,"dungeon_mob","mob",31,"mob:31:new",false)==-1,"new owner/old run cannot receive historical share");
        check(post.getBalanceTrace(A)==balance&&post.getBalanceTrace(B)==0,"retirement never changes balances");
        check(encode(post).getCompoundOrEmpty("ledger").equals(evidence),"repeat blocked without multiplying historical ledger rows");
        check(post.change(A,"A",99,"dungeon_mob","mob",32,"mob:32:two",false)==2,"active run original receipt still retained");
        check(post.getBalanceTrace(A)==balance,"active receipt preserves original result without paying twice");
        check(pre.change(A,"A",7,"dungeon_mob","mob",31,"mob:31:one",false)==7&&pre.getBalanceTrace(A)==balance,
                "interruption before retirement persists old receipt guard");
        check(post.hasReceipt("first_trace",A)&&post.hasReceipt("inn:retained",A),"lifetime/Inn receipts never pruned");
        var beforeRepeat=encode(post);post.clearRunReceipts(31);
        check(encode(post).equals(beforeRepeat),"retirement retry no additional save evidence");
        reject(()->post.clearRunReceipts(0),"invalid clear cannot remove receipts");
        post.clearRunReceipts(33);
        check(post.change(A,"A",3,"dungeon_mob","mob",32,"mob:32:new",false)==3,"gap run remains available");
        post.clearRunReceipts(32);
        check(post.receiptRetentionSummary().getIntOr("retired_run_intervals",0)==1,"known adjacent finished runs compact once");
        var reloaded=disk(post,dir.resolve("compacted.dat"));
        check(reloaded.change(A,"A",2,"dungeon_mob","mob",32,"mob:32:new",false)==-1,"compacted replay blocked after native restart");
        check(reloaded.supplySnapshot().getStringOr("difference","").equals("0"),"supply retained through retirement");
        var corrupt=encode(reloaded);corrupt.getCompoundOrEmpty("retired_reward_runs").putInt("schema",99);
        reject(()->decode(corrupt),"future account extension held for recovery");

        var money=decode(new CompoundTag());money.setBalanceTrace(A,100);
        var terms=new AccountTransfer.Terms(A,B,10,0,"player_trade",31,"exact legacy item evidence");
        UUID committed=new UUID(0,3210),cancelled=new UUID(0,3211),pending=new UUID(0,3212),operation=new UUID(0,3213);
        money.reserve(committed,terms,1);money.commitTransfer(committed,terms,2);
        money.reserve(cancelled,terms,3);money.cancelTransfer(cancelled,terms,4,"reviewed cancellation");
        money.reserve(pending,terms,5);
        var plan=CommerceCustodyImages.create(operation,C,31,new ListTag(),new ListTag(),new CompoundTag());
        money.reserveOperation(operation,C,5,"vendor_sale","review",31,plan,1);money.prepareOperation(operation);
        money.decideOperation(operation,true,2);money.acknowledgeOperation(operation,C);
        var operationBefore=money.operation(operation).orElseThrow();
        money.clearRunReceipts(31);
        var restored=disk(money,dir.resolve("retained.dat"));
        check(restored.transfer(committed).orElseThrow().status().equals(AccountTransfer.COMMITTED),"terminal transfer retained");
        check(restored.transfer(cancelled).orElseThrow().status().equals(AccountTransfer.CANCELLED),"cancelled transfer retained");
        check(restored.transfer(pending).orElseThrow().reserved()&&restored.reservedDebit(A)==10,"pending reservation and hold retained");
        check(restored.operation(operation).orElseThrow().equals(operationBefore),"acknowledged operation retained");
        long savedBalance=restored.getBalanceTrace(A),savedCredit=restored.getBalanceTrace(C);
        restored.commitTransfer(committed,terms,6);restored.decideOperation(operation,true,7);
        check(restored.getBalanceTrace(A)==savedBalance&&restored.getBalanceTrace(C)==savedCredit,"retained terminal IDs cannot repay");
        check(restored.operation(operation).orElseThrow().plan().isEmpty(),"settled item image compacted but receipt survives");
        var original=encode(restored);var summary=restored.receiptRetentionSummary();summary.putInt("operation_receipts",0);
        check(encode(restored).equals(original),"read-only summary cannot mutate account");
        check(restored.supplySnapshot().getStringOr("difference","").equals("0"),"mixed retained evidence reconciles supply");
    }
    public static void main(String[] args)throws Exception {
        intervals();policies();
        Path dir=Files.createTempDirectory("cosmic-legacy-currency-");
        try {account(dir);}
        finally {try(var files=Files.list(dir)){for(var file:files.toList())Files.deleteIfExists(file);}Files.deleteIfExists(dir);}
        System.out.println(checks+" legacy currency and receipt retention checks passed; "+images+" native account images");
    }
}
