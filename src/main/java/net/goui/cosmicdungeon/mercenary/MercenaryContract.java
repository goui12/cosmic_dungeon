package net.goui.cosmicdungeon.mercenary;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.nbt.CompoundTag;
import java.util.*;

/** Optional v1 name presentation freezes old identities; unrecognized contract fields round-trip. */
public record MercenaryContract(UUID id, UUID hirer, String classId, int slot, long fee,
        String name, CompoundTag unknown) {
    public static final List<String> CLASSES=List.of("bogatyr","dragoon","judicator","pyroclast","theurgist","venefex");
    private static final Set<String> KNOWN=Set.of("id","hirer","class","slot","fee","display_name");
    public static final Codec<MercenaryContract> CODEC=CompoundTag.CODEC.comapFlatMap(MercenaryContract::decode,MercenaryContract::image);
    /** Only new lobby hires use this constructor. Legacy decoding explicitly retains the old mapping. */
    public MercenaryContract(UUID id,UUID hirer,String classId,int slot,long fee){
        this(id,hirer,classId,slot,fee,MercenaryIdentity.newName(id),new CompoundTag());
    }
    public MercenaryContract {
        Objects.requireNonNull(id);Objects.requireNonNull(hirer);Objects.requireNonNull(unknown);
        if(id.equals(hirer)||!CLASSES.contains(classId)||slot<1||slot>6||fee<0||fee>100000000
                ||name==null||name.isBlank()||name.length()>256)
            throw new IllegalArgumentException("Invalid mercenary contract");
        unknown=unknown.copy();for(String key:KNOWN)unknown.remove(key);
    }
    @Override public CompoundTag unknown(){return unknown.copy();}
    private static DataResult<MercenaryContract> decode(CompoundTag tag){
        try{
            UUID id=UUID.fromString(tag.getStringOr("id",""));
            var extra=tag.copy();for(String key:KNOWN)extra.remove(key);
            return DataResult.success(new MercenaryContract(id,UUID.fromString(tag.getStringOr("hirer","")),
                    tag.getStringOr("class",""),tag.getIntOr("slot",-1),tag.getLongOr("fee",-1),
                    tag.getString("display_name").orElseGet(()->MercenaryIdentity.name(id)),extra));
        }catch(RuntimeException invalid){return DataResult.error(()->"Invalid mercenary contract: "+invalid.getMessage());}
    }
    private CompoundTag image(){
        var out=unknown.copy();out.putString("id",id.toString());out.putString("hirer",hirer.toString());
        out.putString("class",classId);out.putInt("slot",slot);out.putLong("fee",fee);out.putString("display_name",name);
        return out;
    }
}
