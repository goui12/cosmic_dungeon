package net.goui.cosmicdungeon.spawner.editor;

import java.util.*;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerPreset.Slot;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;

/** Overlay only explicitly changed slots; retain native equipment, weighted entries, drop masks and unknown data. */
public final class SpawnerEquipmentConfig {
    private SpawnerEquipmentConfig(){}
    public static List<ItemStack> snapshot(CompoundTag source,HolderLookup.Provider registries){
        var preset=source.getCompoundOrEmpty("SpawnerPreset");
        var nativeEntity=source.getCompoundOrEmpty("SpawnData").getCompoundOrEmpty("entity");
        var input=TagValueInput.create(ProblemReporter.DISCARDING,registries,nativeEntity);
        var equipment=input.read("equipment",EntityEquipment.CODEC).orElseGet(EntityEquipment::new);
        int mask=source.contains("SpawnerPreset")?preset.getIntOr("equipmentMask",63):0;
        var presetInput=TagValueInput.create(ProblemReporter.DISCARDING,registries,preset);
        var result=new ArrayList<ItemStack>();
        for(var slot:Slot.values())result.add(((mask&(1<<slot.ordinal()))!=0
                ?presetInput.read("eq_"+slot.id,ItemStack.CODEC).orElse(ItemStack.EMPTY):equipment.get(slot.equipmentSlot)).copy());
        return List.copyOf(result);
    }
    public static CompoundTag patch(CompoundTag source,String entityId,SpawnerEquipmentDraft draft,List<ItemStack> resolved,HolderLookup.Provider registries){
        if(!draft.dirty())return source;
        var result=source.copy();var preset=SpawnerEditorConfig.preset(result,entityId);
        int mask=preset.getIntOr("equipmentMask",63);
        for(var slot:Slot.values()){
            if(!draft.get(slot).changed())continue;
            mask|=1<<slot.ordinal();String key="eq_"+slot.id;var stack=resolved.get(slot.ordinal());
            if(stack.isEmpty())preset.remove(key);
            else preset.put(key,ItemStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE),stack)
                    .getOrThrow(message->new IllegalArgumentException("Item components could not be saved: "+message)));
        }
        preset.putInt("equipmentMask",mask);result.put("SpawnerPreset",preset);return result;
    }
}
