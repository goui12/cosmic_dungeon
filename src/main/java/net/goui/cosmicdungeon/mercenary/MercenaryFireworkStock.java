package net.goui.cosmicdungeon.mercenary;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Virtual ammunition: no loot/material costs, and no offline production backlog. */
public record MercenaryFireworkStock(int count, int remaining) {
    public static final int BASE_INTERVAL=600, MIN_INTERVAL=100, MAX_STOCK=32772;
    public static final Codec<MercenaryFireworkStock> CODEC=RecordCodecBuilder.create(i->i.group(
            Codec.intRange(0,MAX_STOCK).fieldOf("count").forGetter(MercenaryFireworkStock::count),
            Codec.intRange(0,BASE_INTERVAL).fieldOf("remaining").forGetter(MercenaryFireworkStock::remaining)
    ).apply(i,MercenaryFireworkStock::new));
    public MercenaryFireworkStock {
        if(count<0||count>MAX_STOCK||remaining<0||remaining>BASE_INTERVAL)
            throw new IllegalArgumentException("Invalid firework stock");
    }
    public static MercenaryFireworkStock initial(){return new MercenaryFireworkStock(5,BASE_INTERVAL);}
    private static int level(int level){return Math.clamp(level,1,65536);}
    public static int capacity(int level){return 5+(level(level)-1)/2;}
    public static int interval(int level){
        return Math.max(MIN_INTERVAL,(int)Math.ceil(BASE_INTERVAL/(1+.06*(level(level)-1))));
    }
    public MercenaryFireworkStock advance(int ticks,int level){
        if(ticks<=0)return this;
        int interval=interval(level);
        // Keep existing stock if a counter is reduced; only new stock obeys current capacity.
        if(count>=capacity(level))return new MercenaryFireworkStock(count,interval);
        int next=Math.max(0,Math.min(remaining,interval)-Math.min(ticks,BASE_INTERVAL));
        return next==0?new MercenaryFireworkStock(count+1,interval):new MercenaryFireworkStock(count,next);
    }
    public MercenaryFireworkStock spend(){
        if(count==0)throw new IllegalStateException("No fireworks");
        return new MercenaryFireworkStock(count-1,remaining);
    }
}
