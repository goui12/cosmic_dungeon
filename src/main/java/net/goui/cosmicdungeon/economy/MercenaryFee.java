package net.goui.cosmicdungeon.economy;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import java.util.*;
/** Group fee decision shares the authoritative account image and its reservation indexes. */
public record MercenaryFee(Map<UUID,Long> charges,String status) {
    public static final Codec<MercenaryFee> CODEC=RecordCodecBuilder.create(i->i.group(
        Codec.unboundedMap(UUIDUtil.STRING_CODEC,Codec.LONG).fieldOf("charges").forGetter(MercenaryFee::charges),
        Codec.STRING.fieldOf("status").forGetter(MercenaryFee::status)).apply(i,MercenaryFee::new));
    public MercenaryFee {
        charges=Map.copyOf(charges);
        if(charges.isEmpty()||charges.size()>3||charges.values().stream().anyMatch(v->v<0||v>100000000)
                ||!Set.of("reserved","committed","cancelled").contains(status))
            throw new IllegalArgumentException("Invalid mercenary fees");
    }
    public boolean reserved(){return status.equals("reserved");}
}
