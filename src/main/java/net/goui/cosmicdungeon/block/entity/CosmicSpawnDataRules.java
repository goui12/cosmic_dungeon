package net.goui.cosmicdungeon.block.entity;

import net.minecraft.nbt.*;
import java.util.Objects;

/** Adds provenance to a copy; authored persistent data, equipment and all other NBT survive. */
public final class CosmicSpawnDataRules {
    private CosmicSpawnDataRules(){}
    public static boolean canTag(CompoundTag original) {
        Tag raw=original.get("Tags");
        if(raw==null)return true;
        if(!(raw instanceof ListTag list))return false;
        for(Tag value:list)if(!(value instanceof StringTag))return false;
        return true;
    }
    public static CompoundTag tagged(CompoundTag original,String marker,String fallbackEntity) {
        var copy=original.copy();
        if(!copy.contains("id")&&fallbackEntity!=null)copy.putString("id",fallbackEntity);
        Tag raw=copy.get("Tags");
        if(raw!=null&&!(raw instanceof ListTag))
            return copy; // Preserve malformed evidence; do not invent qualifying provenance.
        var tags=raw instanceof ListTag list?list.copy():new ListTag();
        boolean found=false;
        for(Tag value:tags) {
            if(!(value instanceof StringTag))return copy;
            if(value.asString().orElse("").equals(marker))found=true;
        }
        if(!found)tags.add(StringTag.valueOf(Objects.requireNonNull(marker)));
        copy.put("Tags",tags);
        return copy;
    }
}
