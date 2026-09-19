package net.goui.cosmicdungeon.economy;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.*;
import java.nio.file.*;
import java.util.*;

public final class EconomyLedgerChecks {
    private static int checks;
    private static final UUID A=new UUID(0,61),B=new UUID(0,62);
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private interface Work {void run() throws Exception;}
    private static void reject(Work action,String why){boolean failed=false;try{action.run();}catch(Exception expected){failed=true;}check(failed,why);}
    @SuppressWarnings("unchecked") private static Codec<PlayerCurrencyData> codec()throws Exception{
        var field=PlayerCurrencyData.class.getDeclaredField("CODEC");field.setAccessible(true);return (Codec<PlayerCurrencyData>)field.get(null);
    }
    private static CompoundTag row(long id){return EconomyLedger.row("tx-"+id,A,"A","mob_reward",3,10,13,"mob",7,"committed",id,new CompoundTag());}
    public static void main(String[] args)throws Exception{
        Path dir=Files.createTempDirectory("economy-ledger-");
        try{
            var rows=new TreeMap<Long,CompoundTag>();for(long i=1;i<=260;i++)rows.put(i,row(i));
            EconomyLedger.archive(dir,rows);
            try(var files=Files.list(dir)){check(files.count()==3,"Rows split into bounded pages");}
            for(long i:List.of(1L,127L,128L,255L,256L,260L)){
                var page=NbtIo.readCompressed(dir.resolve(String.format(Locale.ROOT,"%016x.nbt",i/EconomyLedger.PAGE_SIZE)),NbtAccounter.unlimitedHeap());
                check(page.getCompoundOrEmpty(Long.toString(i)).equals(rows.get(i)),"Exact sequence "+i+" survives native serialization");
            }
            var file=dir.resolve("0000000000000000.nbt");byte[] before=Files.readAllBytes(file);
            EconomyLedger.archive(dir,rows);check(Arrays.equals(before,Files.readAllBytes(file)),"Archive replay preserves identical page bytes");
            var conflict=row(1);conflict.putLong("after",14);
            reject(()->EconomyLedger.archive(dir,Map.of(1L,conflict)),"Conflicting duplicate sequence held");
            check(Arrays.equals(before,Files.readAllBytes(file)),"Conflicting archive leaves original evidence");
            reject(()->EconomyLedger.archive(dir,Map.of(-1L,row(1))),"Negative sequence rejected");
            var invalid=row(2);invalid.remove("before");reject(()->EconomyLedger.archive(dir,Map.of(2L,invalid)),"Incomplete ledger row rejected");
            Files.write(file,new byte[]{1,2,3});
            reject(()->EconomyLedger.archive(dir,Map.of(1L,row(1))),"Unreadable original page never replaced by empty ledger");
            check(Arrays.equals(Files.readAllBytes(file),new byte[]{1,2,3}),"Damaged original retained for review");
        }finally{try(var files=Files.list(dir)){for(var p:files.toList())Files.deleteIfExists(p);}Files.deleteIfExists(dir);}
        var c=codec();var legacy=new CompoundTag();var balances=new CompoundTag();balances.putLong(A.toString(),100);balances.putLong(B.toString(),50);legacy.put("balances",balances);
        var d=c.parse(NbtOps.INSTANCE,legacy).getOrThrow();
        check(d.supplySnapshot().getStringOr("baseline","").equals("150"),"Existing balances become migration baseline once");
        d.change(A,"A",10,"mob_reward","mob",7,"reward",false);
        d.change(A,"A",-5,"vendor_retail","vendor",7,"retail",false);
        var id=new UUID(0,63);var terms=new AccountTransfer.Terms(A,B,30,7,"player_trade",7,"fixture");
        d.reserve(id,terms,1);d.commitTransfer(id,terms,2);
        var report=d.supplySnapshot();
        check(report.getStringOr("generation","").equals("10"),"Only new rewards count as generation");
        check(report.getStringOr("sink","").equals("5"),"Retail is a sink");
        check(report.getStringOr("transfer","").equals("37"),"Two-way trades count gross transfer without minting");
        check(report.getStringOr("balances","").equals("155")&&report.getStringOr("difference","").equals("0"),"Supply accounts for generation, sinks and transfers");
        long before=d.getBalanceTrace(A);d.commitTransfer(id,terms,3);
        check(d.getBalanceTrace(A)==before&&d.supplySnapshot().getStringOr("transfer","").equals("37"),"Decision replay cannot duplicate financial totals");
        var encoded=(CompoundTag)c.encodeStart(NbtOps.INSTANCE,d).getOrThrow();
        check(c.parse(NbtOps.INSTANCE,encoded).getOrThrow().supplySnapshot().equals(report),"Ledger summary and baseline survive account codec");
        var percent=c.parse(NbtOps.INSTANCE,new CompoundTag()).getOrThrow();
        for(int i=1;i<=10;i++)percent.setBalanceTrace(new UUID(0,100+i),i*10);
        check(percent.supplySnapshot().getLongOr("p50",-1)==50,"Nearest-rank 50th percentile uses current balances");
        check(percent.supplySnapshot().getLongOr("p90",-1)==90&&percent.supplySnapshot().getLongOr("p99",-1)==100,"90th/99th percentiles");
        check(percent.supplySnapshot().getDoubleOr("median_trace",-1)==55.0,"Even-population median averages middle accounts");
        var days=new CompoundTag();
        for(int day=1;day<=40;day++){var r=row(day);r.putLong("timestamp",day*86_400_000L);days=EconomyReports.daily(days,r,3);}
        check(days.size()==EconomyReports.RECENT_DAYS&&!days.contains("8")&&days.contains("9")&&days.contains("40"),"Recent-day memory stays bounded");
        check(days.getCompoundOrEmpty("40").getStringOr("generation","").equals("3"),"Daily generation separate from lifetime");
        var rewind=row(100);rewind.putLong("timestamp",1);
        check(EconomyReports.daily(days,rewind,3).equals(days),"Clock rollback preserves newer daily evidence");
        var review=c.parse(NbtOps.INSTANCE,new CompoundTag()).getOrThrow();review.setBalanceTrace(A,99_999_999);
        review.change(A,"A",1,"mob_reward","mob",7,"reach-cap",false);
        check(review.finalReviews().contains(A.toString()),"Cap crossing creates persistent final review");
        review.change(A,"A",-1,"vendor_retail","vendor",7,"debit-at-cap",false);
        check(review.getBalanceTrace(A)==99_999_999&&review.finalReviews().contains(A.toString()),"Final review permits debits and persists after spending");
        review.change(A,"A",10,"mob_reward","mob",7,"partial-cap",true);
        var today=review.supplySnapshot().getCompoundOrEmpty("recent_days").getCompoundOrEmpty(Long.toString(Math.floorDiv(System.currentTimeMillis(),86_400_000L)));
        check(today.getStringOr("cap_rejected_trace","").equals("9"),"Partial reward overflow recorded daily");
        check(!review.finalReviews().getCompoundOrEmpty(A.toString()).contains("details"),"Review entry stays compact");
        check(review.claimDailyReport(10)&&!review.claimDailyReport(10)&&!review.claimDailyReport(9)&&review.claimDailyReport(11),"Daily report marker survives repeats and clock rollback");
        var snap=(CompoundTag)c.encodeStart(NbtOps.INSTANCE,review).getOrThrow();var reloaded=c.parse(NbtOps.INSTANCE,snap).getOrThrow();
        check(reloaded.finalReviews().equals(review.finalReviews())&&!reloaded.claimDailyReport(11),"Review state and reporting marker persist");
        check(EconomyLedger.category("death_debit",-3).equals("transfer")&&EconomyLedger.category("death_pickup",3).equals("transfer")&&EconomyLedger.category("death_despawn",-3).equals("sink"),"Future death accounting categories keep destruction separate");
        System.out.println(checks+" ledger archive, supply and review checks passed");
    }
}
