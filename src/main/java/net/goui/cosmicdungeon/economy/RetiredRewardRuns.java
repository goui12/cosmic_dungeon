package net.goui.cosmicdungeon.economy;

import net.minecraft.nbt.*;
import java.util.*;

/** Exact completed-run intervals. Never infer retirement from a highest-seen run number. */
public final class RetiredRewardRuns {
    private final NavigableMap<Long,Long> ranges=new TreeMap<>();
    public boolean contains(long run) {
        var entry=ranges.floorEntry(run);
        return run>0 && entry!=null && run<=entry.getValue();
    }
    public int rangeCount(){return ranges.size();}
    public boolean retire(long run) {
        if(run<=0)throw new IllegalArgumentException("Only a positive retired run can release receipts");
        if(contains(run))return false;
        long first=run,last=run;
        var lower=ranges.floorEntry(run);
        if(lower!=null && lower.getValue()==run-1){first=lower.getKey();ranges.remove(lower.getKey());}
        var upper=ranges.ceilingEntry(run);
        if(upper!=null && run<Long.MAX_VALUE && upper.getKey()==run+1){last=upper.getValue();ranges.remove(upper.getKey());}
        ranges.put(first,last);return true;
    }
    /** Parse only the run prefix; old suffix formats are preserved, not reinterpreted. */
    public static long transactionRun(String transaction) {
        if(transaction==null || !transaction.startsWith("mob:"))return 0;
        int end=transaction.indexOf(':',4);if(end<0)return 0;
        String value=transaction.substring(4,end);
        try {
            long run=Long.parseLong(value);
            return run>0 && Long.toString(run).equals(value) ? run : 0;
        } catch(NumberFormatException invalid){return 0;}
    }
    public boolean blocks(String transaction,long declaredRun) {
        return transaction!=null && transaction.startsWith("mob:")
                && (contains(declaredRun)||contains(transactionRun(transaction)));
    }
    public CompoundTag save() {
        var root=new CompoundTag();root.putInt("schema",1);
        var intervals=new ListTag();
        ranges.forEach((first,last)->{
            var row=new CompoundTag();row.putLong("first",first);row.putLong("last",last);intervals.add(row);
        });
        root.put("intervals",intervals);return root;
    }
    public static RetiredRewardRuns load(CompoundTag root) {
        var result=new RetiredRewardRuns();
        if(root.isEmpty())return result; // Optional extension absent in older accounts.
        if(root.getIntOr("schema",0)!=1 || !(root.get("intervals") instanceof ListTag intervals))
            throw new IllegalArgumentException("Retired reward runs require schema review");
        long previous=0;
        for(var value:intervals) {
            if(!(value instanceof CompoundTag row) || !(row.get("first") instanceof LongTag)
                    || !(row.get("last") instanceof LongTag))
                throw new IllegalArgumentException("Invalid retired reward interval");
            long first=row.getLongOr("first",0),last=row.getLongOr("last",0);
            if(first<=0 || last<first || first<=previous || previous>0 && first==previous+1)
                throw new IllegalArgumentException("Overlapping, adjacent or unordered reward intervals");
            result.ranges.put(first,last);previous=last;
        }
        return result;
    }
    // TODO(M03, older recovery evidence): missing intervals do not prove that an old run is
    // active or unpaid. Batches before32 removed some mob receipts without a retirement record.
    // Do not reconstruct payouts, guessed high-water marks or old runs from balances/names.
    // Reconcile complete account, run, player, entity and ledger backups on licensed TEST.
}
