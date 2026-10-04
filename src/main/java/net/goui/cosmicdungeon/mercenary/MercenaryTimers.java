package net.goui.cosmicdungeon.mercenary;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;

/** Remaining active ticks: unloading/offline time never grants a production backlog. */
public record MercenaryTimers(int brew, int fallback, Map<String,Integer> potions) {
    private static final Codec<Integer> TICKS=Codec.intRange(0,1728000);
    public static final Codec<MercenaryTimers> CODEC=RecordCodecBuilder.create(i->i.group(
        TICKS.optionalFieldOf("brew",400).forGetter(MercenaryTimers::brew),
        TICKS.optionalFieldOf("fallback",3600).forGetter(MercenaryTimers::fallback),
        Codec.unboundedMap(Codec.STRING,TICKS).optionalFieldOf("potions",Map.of()).forGetter(MercenaryTimers::potions)
    ).apply(i,MercenaryTimers::new));
    public MercenaryTimers {
        if(brew<0||fallback<0||brew>1728000||fallback>1728000||potions.size()>128
                ||potions.values().stream().anyMatch(v->v<0||v>1728000))
            throw new IllegalArgumentException("Invalid mercenary timers");
        potions=Map.copyOf(potions);
    }
    public MercenaryTimers advance(int ticks){
        var next=new HashMap<String,Integer>();
        potions.forEach((k,v)->{if(v>ticks)next.put(k,v-ticks);});
        return new MercenaryTimers(Math.max(0,brew-ticks),Math.max(0,fallback-ticks),next);
    }
    public boolean ready(String potion){return potions.getOrDefault(potion,0)==0;}
    public MercenaryTimers used(String potion,int ticks){
        var next=new HashMap<>(potions);
        if(!next.containsKey(potion)&&next.size()>=128)return this;
        next.put(potion,ticks);return new MercenaryTimers(brew,fallback,next);
    }
}
