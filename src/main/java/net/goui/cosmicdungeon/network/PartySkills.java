package net.goui.cosmicdungeon.network;

import io.netty.buffer.ByteBuf;
import java.util.*;
import net.minecraft.network.codec.*;

/** Read-only, bounded signature and ability summary. Deadlines use the snapshot's server game clock. */
public record PartySkills(String abbreviation,int level,List<Ability> abilities) {
    public static final PartySkills NONE=new PartySkills("",0,List.of());
    public PartySkills {
        abilities=List.copyOf(abilities);
        if(abbreviation==null||abbreviation.length()>8||level<0||level>65537||abilities.size()>2
                ||abbreviation.isEmpty()&&(level!=0||!abilities.isEmpty()))
            throw new IllegalArgumentException("Invalid party skills");
    }
    public record Ability(String title,long readyAt,int cooldownTicks,int charge,int maxCharge,boolean armed) {
        public Ability {
            if(title==null||title.isBlank()||title.length()>128||readyAt<0||cooldownTicks<0||cooldownTicks>6000
                    ||charge<0||maxCharge<0||maxCharge>100||charge>maxCharge
                    ||cooldownTicks==0&&readyAt!=0||armed&&(maxCharge==0||charge!=maxCharge))
                throw new IllegalArgumentException("Invalid party ability");
        }
        public static final StreamCodec<ByteBuf,Ability> CODEC=StreamCodec.of((b,a)->{
            ByteBufCodecs.stringUtf8(128).encode(b,a.title());ByteBufCodecs.VAR_LONG.encode(b,a.readyAt());
            ByteBufCodecs.VAR_INT.encode(b,a.cooldownTicks());ByteBufCodecs.VAR_INT.encode(b,a.charge());
            ByteBufCodecs.VAR_INT.encode(b,a.maxCharge());b.writeBoolean(a.armed());
        },b->new Ability(ByteBufCodecs.stringUtf8(128).decode(b),ByteBufCodecs.VAR_LONG.decode(b),
                ByteBufCodecs.VAR_INT.decode(b),ByteBufCodecs.VAR_INT.decode(b),ByteBufCodecs.VAR_INT.decode(b),b.readBoolean()));
    }
    public static final StreamCodec<ByteBuf,PartySkills> CODEC=StreamCodec.composite(
            ByteBufCodecs.stringUtf8(8),PartySkills::abbreviation,ByteBufCodecs.VAR_INT,PartySkills::level,
            Ability.CODEC.apply(ByteBufCodecs.list(2)),PartySkills::abilities,PartySkills::new);
}
