package net.goui.cosmicdungeon.leaderboard;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
/** Small custom counters extend the existing lifetime store; native statistics stay native. */
public record LifetimeActivity(String name,Map<String,Long> counts){
    public static final Set<String> KEYS=Set.of("doors_unlocked","hostile_kills","lesser_harvests","logins","dimension_changes");
    public static final LifetimeActivity EMPTY=new LifetimeActivity("",Map.of());
    public static final Codec<LifetimeActivity> CODEC=RecordCodecBuilder.create(i->i.group(
        Codec.STRING.optionalFieldOf("name","").forGetter(LifetimeActivity::name),
        Codec.unboundedMap(Codec.STRING,Codec.LONG).optionalFieldOf("counts",Map.of()).forGetter(LifetimeActivity::counts)
    ).apply(i,LifetimeActivity::new));
    public LifetimeActivity{
        if(name==null||name.length()>16||counts.values().stream().anyMatch(n->n<0))
            throw new IllegalArgumentException("Invalid lifetime activity");
        counts=Map.copyOf(counts);
    }
    public LifetimeActivity named(String value){return new LifetimeActivity(value,counts);}
    public LifetimeActivity add(String key,long amount){
        if(!KEYS.contains(key)||amount<0)throw new IllegalArgumentException("Invalid activity");
        if(amount==0)return this;
        var next=new HashMap<>(counts);long old=counts.getOrDefault(key,0L);
        next.put(key,old>Long.MAX_VALUE-amount?Long.MAX_VALUE:old+amount);
        return new LifetimeActivity(name,next);
    }
}
