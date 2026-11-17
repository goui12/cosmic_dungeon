package net.goui.cosmicdungeon.network;

import io.netty.buffer.ByteBuf;
import java.util.*;
import net.minecraft.network.codec.*;
import net.minecraft.resources.ResourceLocation;

/** Immutable bounded state with stable deadlines; sample clock is transport metadata, not a delta change. */
public record PartyVitals(float health,float maxHealth,String state,List<Effect> effects,int omittedEffects,
                          long sampledAt,PartySkills skills) {
    public static final int MAX_EFFECTS=64;
    public static final PartyVitals OFFLINE=unavailable("OFFLINE"),UNLOADED=unavailable("UNLOADED"),DEAD=unavailable("DEAD");
    public PartyVitals(float health,float maxHealth,String state,List<Effect> effects,int omittedEffects){
        this(health,maxHealth,state,effects,omittedEffects,0,PartySkills.NONE);
    }
    public PartyVitals {
        effects=List.copyOf(effects);Objects.requireNonNull(skills);
        if(!List.of("ACTIVE","OFFLINE","UNLOADED","DEAD").contains(state)||sampledAt<0
                ||!Float.isFinite(health)||!Float.isFinite(maxHealth)||health<0||maxHealth<health
                ||effects.size()>MAX_EFFECTS||omittedEffects<0
                ||effects.stream().map(Effect::id).distinct().count()!=effects.size()
                ||!state.equals("ACTIVE")&&(health!=0||maxHealth!=0||!effects.isEmpty()||omittedEffects!=0||!skills.abilities().isEmpty()))
            throw new IllegalArgumentException("Invalid party vitals");
    }
    public PartyVitals withSkills(PartySkills value,long now){return new PartyVitals(health,maxHealth,state,effects,omittedEffects,now,value);}
    public static PartyVitals unavailable(String state){return new PartyVitals(0,0,state,List.of(),0);}
    @Override public boolean equals(Object other){
        return other instanceof PartyVitals v&&Float.compare(health,v.health)==0&&Float.compare(maxHealth,v.maxHealth)==0
                &&state.equals(v.state)&&effects.equals(v.effects)&&omittedEffects==v.omittedEffects&&skills.equals(v.skills);
    }
    @Override public int hashCode(){return Objects.hash(health,maxHealth,state,effects,omittedEffects,skills);}
    public record Effect(String id,int amplifier,String displayName,String amount,String caster,long expiresAt) {
        public Effect(String id,int amplifier){this(id,amplifier,"","","Unknown",0);}
        public Effect {
            if(id==null||id.length()>256||ResourceLocation.tryParse(id)==null||amplifier<0||amplifier>255
                    ||displayName==null||displayName.length()>128||amount==null||amount.length()>128
                    ||caster==null||caster.isBlank()||caster.length()>64||expiresAt< -1)
                throw new IllegalArgumentException("Invalid party effect");
        }
        public static final StreamCodec<ByteBuf,Effect> CODEC=StreamCodec.of((b,e)->{
            ByteBufCodecs.stringUtf8(256).encode(b,e.id());ByteBufCodecs.VAR_INT.encode(b,e.amplifier());
            ByteBufCodecs.stringUtf8(128).encode(b,e.displayName());ByteBufCodecs.stringUtf8(128).encode(b,e.amount());
            ByteBufCodecs.stringUtf8(64).encode(b,e.caster());ByteBufCodecs.VAR_LONG.encode(b,e.expiresAt());
        },b->new Effect(ByteBufCodecs.stringUtf8(256).decode(b),ByteBufCodecs.VAR_INT.decode(b),
                ByteBufCodecs.stringUtf8(128).decode(b),ByteBufCodecs.stringUtf8(128).decode(b),
                ByteBufCodecs.stringUtf8(64).decode(b),ByteBufCodecs.VAR_LONG.decode(b)));
    }
    public static final StreamCodec<ByteBuf,PartyVitals> CODEC=StreamCodec.of((b,v)->{
        b.writeFloat(v.health());b.writeFloat(v.maxHealth());ByteBufCodecs.stringUtf8(16).encode(b,v.state());
        Effect.CODEC.apply(ByteBufCodecs.list(MAX_EFFECTS)).encode(b,v.effects());ByteBufCodecs.VAR_INT.encode(b,v.omittedEffects());
        ByteBufCodecs.VAR_LONG.encode(b,v.sampledAt());PartySkills.CODEC.encode(b,v.skills());
    },b->new PartyVitals(b.readFloat(),b.readFloat(),ByteBufCodecs.stringUtf8(16).decode(b),
            Effect.CODEC.apply(ByteBufCodecs.list(MAX_EFFECTS)).decode(b),ByteBufCodecs.VAR_INT.decode(b),
            ByteBufCodecs.VAR_LONG.decode(b),PartySkills.CODEC.decode(b)));
}
