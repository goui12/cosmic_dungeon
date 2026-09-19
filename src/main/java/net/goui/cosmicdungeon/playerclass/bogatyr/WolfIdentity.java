package net.goui.cosmicdungeon.playerclass.bogatyr;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import java.util.Optional;
import java.util.UUID;

/** Bond identity is independent of entity UUIDs duplicated by authored instance snapshots. */
public final class WolfIdentity {
    public static final String KEY="cosmicdungeon.bogatyr_identity";
    private WolfIdentity(){}
    public static Optional<UUID> explicit(CompoundTag persistent){return persistent.read(KEY,UUIDUtil.CODEC);}
    public static UUID id(CompoundTag persistent,UUID nativeUuid){return explicit(persistent).orElse(nativeUuid);}
    public static UUID imageId(CompoundTag entity){
        var persistent=entity.getCompoundOrEmpty("NeoForgeData");
        if(persistent.contains(KEY)&&explicit(persistent).isEmpty())return null;
        return id(persistent,entity.read("UUID",UUIDUtil.CODEC).orElse(null));
    }
    public static void set(CompoundTag persistent,UUID id){persistent.store(KEY,UUIDUtil.CODEC,id);}
    public static boolean matches(BogatyrCompanionData.Companion old,UUID owner,UUID physical,String dimension,
                                  boolean dimensionTransfer,boolean archivedDelivery){
        if(old==null)return true;
        if(!old.owner().equals(owner))return false;
        return archivedDelivery||(old.entityUuid().equals(physical)
                &&(old.dimension().equals(dimension)||dimensionTransfer));
    }
}
