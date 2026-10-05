package net.goui.cosmicdungeon.mercenary;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/** A fixed-size bank, not a queued cast list. Native autosave persists it with the entity. */
record MercenaryLightningState(int hits,float damage,Optional<UUID> excluded,int cooldown) {
    static final int CAST_TICKS=20;
    static final Codec<MercenaryLightningState> CODEC=RecordCodecBuilder.create(i->i.group(
        Codec.intRange(0,Integer.MAX_VALUE).fieldOf("hits").forGetter(MercenaryLightningState::hits),
        Codec.floatRange(0,Float.MAX_VALUE).fieldOf("damage").forGetter(MercenaryLightningState::damage),
        UUIDUtil.CODEC.optionalFieldOf("excluded").forGetter(MercenaryLightningState::excluded),
        Codec.intRange(0,CAST_TICKS).fieldOf("cooldown").forGetter(MercenaryLightningState::cooldown)
    ).apply(i,MercenaryLightningState::new));
    MercenaryLightningState {
        hits=Math.max(0,hits);
        damage=Float.isFinite(damage)?Math.max(0,damage):0;
        excluded=java.util.Objects.requireNonNull(excluded);
        cooldown=Math.clamp(cooldown,0,CAST_TICKS);
    }
    static MercenaryLightningState initial(){return new MercenaryLightningState(0,0,Optional.empty(),0);}
    static int threshold(int level){return 51-Math.clamp(level,1,46);}
    boolean ready(int level){return hits>=threshold(level)&&damage>0&&cooldown==0;}
    MercenaryLightningState advance(int ticks){return new MercenaryLightningState(hits,damage,excluded,Math.max(0,cooldown-Math.max(0,ticks)));}
    MercenaryLightningState primary(float dealt,UUID victim){
        return dealt>0&&Float.isFinite(dealt)?new MercenaryLightningState(increment(hits,1),dealt,Optional.of(victim),cooldown):this;
    }
    MercenaryLightningState secondary(){return new MercenaryLightningState(increment(hits,1),damage,excluded,cooldown);}
    MercenaryLightningState spend(int level){
        if(!ready(level))throw new IllegalStateException("Lightning is not charged");
        return new MercenaryLightningState(hits-threshold(level),damage,excluded,CAST_TICKS);
    }
    MercenaryLightningState refund(int cost){return new MercenaryLightningState(increment(hits,cost),damage,excluded,cooldown);}
    private static int increment(int value,int amount){return (int)Math.min(Integer.MAX_VALUE,(long)value+Math.max(0,amount));}
}
