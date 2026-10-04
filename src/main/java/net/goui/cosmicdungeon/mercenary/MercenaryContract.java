package net.goui.cosmicdungeon.mercenary;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import java.util.*;
/** Immutable identity for one hire, distinct from its human owner and all other slot owners. */
public record MercenaryContract(UUID id, UUID hirer, String classId, int slot, long fee) {
    public static final List<String> CLASSES=List.of("bogatyr","dragoon","judicator","pyroclast","theurgist","venefex");
    public static final Codec<MercenaryContract> CODEC=RecordCodecBuilder.create(i->i.group(
        UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(MercenaryContract::id),
        UUIDUtil.STRING_CODEC.fieldOf("hirer").forGetter(MercenaryContract::hirer),
        Codec.STRING.fieldOf("class").forGetter(MercenaryContract::classId),
        Codec.INT.fieldOf("slot").forGetter(MercenaryContract::slot),
        Codec.LONG.fieldOf("fee").forGetter(MercenaryContract::fee)).apply(i,MercenaryContract::new));
    public String name() { return MercenaryIdentity.name(id); }
    public MercenaryContract {
        Objects.requireNonNull(id);Objects.requireNonNull(hirer);
        if(id.equals(hirer)||!CLASSES.contains(classId)||slot<1||slot>6||fee<0||fee>100000000)
            throw new IllegalArgumentException("Invalid mercenary contract");
    }
}
