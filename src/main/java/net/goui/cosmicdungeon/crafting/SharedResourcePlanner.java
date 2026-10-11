package net.goui.cosmicdungeon.crafting;

import java.util.*;

/** Pure, bounded resource allocation. Counts describe disjoint ingredient pools, never item authorization. */
public final class SharedResourcePlanner {
    public static final int MAX_OWNERS=6, MAX_KEYS=16, MAX_UNITS=64, MAX_CAPACITY=64;
    private SharedResourcePlanner() {}

    public record Stock(UUID owner,long donated,Map<String,Integer> counts) {
        public Stock {
            Objects.requireNonNull(owner);
            if(donated<0)throw new IllegalArgumentException("Negative donation rank");
            counts=SharedResourcePlanner.counts(counts,false);
        }
    }
    public record Debit(UUID owner,String ingredient,int count) {
        public Debit {
            Objects.requireNonNull(owner);key(ingredient);
            if(count<1||count>MAX_UNITS)throw new IllegalArgumentException("Invalid debit count");
        }
    }
    public record Plan(boolean shared,List<Debit> debits,Map<UUID,Long> donatedAfter,long sequence) {
        public Plan {
            debits=List.copyOf(debits);donatedAfter=Map.copyOf(donatedAfter);
            if(sequence<0||shared!=!donatedAfter.isEmpty())throw new IllegalArgumentException("Invalid allocation decision");
            int units=0;var seen=new HashSet<Allocation>();
            for(var debit:debits) {
                units=Math.addExact(units,debit.count());
                if(!seen.add(new Allocation(debit.owner(),debit.ingredient())))throw new IllegalArgumentException("Duplicate debit");
            }
            if(units<1||units>MAX_UNITS||donatedAfter.size()>MAX_OWNERS)
                throw new IllegalArgumentException("Allocation exceeds recipe bounds");
            for(var entry:donatedAfter.entrySet())
                if(entry.getValue()<0||entry.getValue()>sequence||debits.stream().noneMatch(d->d.owner().equals(entry.getKey())))
                    throw new IllegalArgumentException("Donation rank has no debit");
        }
    }
    private record Allocation(UUID owner,String ingredient) {}

    /**
     * Uses the crafter's stock first, then distributes each missing unit to the least-recent donor
     * who actually has that ingredient. Failed allocation cannot change stock or donation history.
     * Only actual donors appear in donatedAfter; own material use never advances donation history.
     */
    public static Plan plan(UUID crafter,Map<String,Integer> requirements,List<Stock> available,long sequence) {
        Objects.requireNonNull(crafter);
        if(sequence<0)throw new IllegalArgumentException("Negative donation sequence");
        var needed=counts(requirements,true);var stocks=stocks(available);
        if(stocks.stream().anyMatch(stock->stock.donated()>sequence))
            throw new IllegalArgumentException("Donation sequence trails participant history");
        if(capacityOf(needed,stocks)==0)return null;

        var remaining=new HashMap<UUID,Map<String,Integer>>();
        var ranks=new HashMap<UUID,Long>();long nextSequence=sequence;
        for(var stock:stocks) {
            remaining.put(stock.owner(),new HashMap<>(stock.counts()));
            ranks.put(stock.owner(),stock.donated());
        }
        var allocated=new LinkedHashMap<Allocation,Integer>();
        var changedRanks=new LinkedHashMap<UUID,Long>();
        for(var requirement:needed.entrySet()) {
            String ingredient=requirement.getKey();int missing=requirement.getValue();
            var own=remaining.get(crafter);
            int self=own==null?0:Math.min(missing,own.getOrDefault(ingredient,0));
            if(self>0) {
                allocated.put(new Allocation(crafter,ingredient),self);
                own.put(ingredient,own.get(ingredient)-self);missing-=self;
            }
            while(missing-->0) {
                UUID chosen=null;
                for(var stock:stocks) {
                    UUID owner=stock.owner();
                    if(owner.equals(crafter)||remaining.get(owner).getOrDefault(ingredient,0)==0)continue;
                    if(chosen==null||ranks.get(owner)<ranks.get(chosen)
                            ||ranks.get(owner).equals(ranks.get(chosen))&&owner.toString().compareTo(chosen.toString())<0)
                        chosen=owner;
                }
                if(chosen==null)return null;
                nextSequence=Math.incrementExact(nextSequence);
                ranks.put(chosen,nextSequence);changedRanks.put(chosen,nextSequence);
                var inventory=remaining.get(chosen);inventory.put(ingredient,inventory.get(ingredient)-1);
                allocated.merge(new Allocation(chosen,ingredient),1,Math::addExact);
            }
        }
        var debits=new ArrayList<Debit>();
        allocated.forEach((allocation,count)->debits.add(new Debit(allocation.owner(),allocation.ingredient(),count)));
        return new Plan(!changedRanks.isEmpty(),debits,changedRanks,nextSequence);
    }

    /** Alternative craft count, capped at64; it does not reserve resources for any recipe. */
    public static int capacity(Map<String,Integer> requirements,List<Stock> available) {
        return capacityOf(counts(requirements,true),stocks(available));
    }
    private static int capacityOf(Map<String,Integer> requirements,List<Stock> stocks) {
        long result=MAX_CAPACITY;
        for(var requirement:requirements.entrySet()) {
            long total=0;
            for(var stock:stocks)total+=stock.counts().getOrDefault(requirement.getKey(),0);
            result=Math.min(result,total/requirement.getValue());
        }
        return (int)result;
    }
    private static List<Stock> stocks(List<Stock> available) {
        var result=List.copyOf(available);
        if(result.size()>MAX_OWNERS||result.stream().map(Stock::owner).distinct().count()!=result.size())
            throw new IllegalArgumentException("Invalid owner roster");
        return result;
    }
    private static Map<String,Integer> counts(Map<String,Integer> source,boolean requirements) {
        Objects.requireNonNull(source);
        if(source.size()>MAX_KEYS||requirements&&source.isEmpty())throw new IllegalArgumentException("Invalid ingredient count");
        var result=new TreeMap<String,Integer>();long total=0;
        for(var entry:source.entrySet()) {
            key(entry.getKey());Integer count=entry.getValue();
            if(count==null||count<0||requirements&&count==0)throw new IllegalArgumentException("Invalid ingredient quantity");
            result.put(entry.getKey(),count);total+=count;
        }
        if(requirements&&total>MAX_UNITS)throw new IllegalArgumentException("Recipe exceeds unit limit");
        return Collections.unmodifiableMap(result);
    }
    private static void key(String key) {
        if(key==null||key.isBlank()||key.length()>128)throw new IllegalArgumentException("Invalid ingredient key");
    }
}
