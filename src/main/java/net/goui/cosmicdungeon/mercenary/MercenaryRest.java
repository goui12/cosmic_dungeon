package net.goui.cosmicdungeon.mercenary;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Locator/deadline only. Equipment remains on the original native entity, never in a second image. */
public record MercenaryRest(long until,String dimension,long position,int flags) {
    public static final int RESPAWN_TICKS=20*60*10;
    public static final Codec<MercenaryRest> CODEC=RecordCodecBuilder.create(i->i.group(
        Codec.LONG.validate(value->value>=0?com.mojang.serialization.DataResult.success(value):com.mojang.serialization.DataResult.error(()->"Negative deadline")).fieldOf("until").forGetter(MercenaryRest::until),
        Codec.STRING.fieldOf("dimension").forGetter(MercenaryRest::dimension),
        Codec.LONG.fieldOf("position").forGetter(MercenaryRest::position),
        Codec.intRange(0,63).optionalFieldOf("flags",16).forGetter(MercenaryRest::flags)
    ).apply(i,MercenaryRest::new));
    public MercenaryRest {
        if(until<0||dimension==null||dimension.isBlank()||flags<0||flags>63)
            throw new IllegalArgumentException("Invalid mercenary rest");
    }
    public static long deadline(long now){return Math.min(Math.max(0,now),Long.MAX_VALUE-RESPAWN_TICKS)+RESPAWN_TICKS;}
    public int seconds(long now){
        long ticks=Math.max(0,until-Math.max(0,now));
        return (int)Math.min(600,ticks/20+(ticks%20==0?0:1));
    }
    public boolean due(long now){return now>=until;}
    public boolean flag(int bit){return (flags&bit)!=0;}
}
