package net.goui.cosmicdungeon.spawner.editor;

import java.util.*;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerPreset.Slot;
import net.goui.cosmicdungeon.spawner.editor.SpawnerEquipmentDraft.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;

/** Shared preview checks; the server independently resolves the same recipe from its own snapshots. */
public final class SpawnerEquipmentRules {
    public static final int MAX_LEVEL=255;
    private SpawnerEquipmentRules(){}
    public static boolean fits(Slot slot,ItemStack stack){
        if(stack.isEmpty()||slot==Slot.MAINHAND||slot==Slot.OFFHAND)return true;
        var equippable=stack.get(DataComponents.EQUIPPABLE);
        return equippable!=null&&equippable.slot()==slot.equipmentSlot;
    }
    public static ItemStack resolve(ItemStack original,ItemStack held,Edit edit,HolderLookup.Provider registries){
        if(edit.origin()!=Origin.ITEM&&!edit.itemId().isEmpty())throw new IllegalArgumentException("Unexpected item identifier.");
        ItemStack result=switch(edit.origin()){
            case KEEP -> original.copy();
            case HELD -> held.copy();
            case CLEAR -> ItemStack.EMPTY;
            case ITEM -> {
                var id=ResourceLocation.tryParse(edit.itemId());
                var item=id==null?null:BuiltInRegistries.ITEM.getOptional(id).orElse(null);
                if(item==null||item==Items.AIR)throw new IllegalArgumentException("Choose a registered item.");
                yield new ItemStack(item);
            }
        };
        if(!edit.changed())return result; // Untouched authored exceptions survive unrelated edits.
        if(result.isEmpty()){
            if(!edit.enchantments().isEmpty())throw new IllegalArgumentException("Select an item before enchanting.");
            if(edit.origin()==Origin.HELD)throw new IllegalArgumentException("The held-item snapshot is empty.");
            return result;
        }
        var key=result.is(Items.ENCHANTED_BOOK)?DataComponents.STORED_ENCHANTMENTS:DataComponents.ENCHANTMENTS;
        var originalEnchantments=result.getOrDefault(key,ItemEnchantments.EMPTY);
        var changed=new ItemEnchantments.Mutable(originalEnchantments);
        var seen=new HashSet<String>();var registry=registries.lookupOrThrow(Registries.ENCHANTMENT);
        for(var change:edit.enchantments()){
            var id=ResourceLocation.tryParse(change.id());
            if(id==null||!seen.add(id.toString())||change.level()<0||change.level()>MAX_LEVEL)
                throw new IllegalArgumentException("Duplicate, invalid, or out-of-range enchantment (0–255).");
            var enchantment=registry.get(ResourceKey.create(Registries.ENCHANTMENT,id)).orElseThrow(()->new IllegalArgumentException("Unknown enchantment."));
            changed.set(enchantment,change.level());
        }
        var finalEnchantments=changed.toImmutable();
        if(!edit.advanced()){
            var list=new ArrayList<>(finalEnchantments.keySet());
            for(int i=0;i<list.size();i++){
                var a=list.get(i);
                if(finalEnchantments.getLevel(a)>a.value().getMaxLevel())throw new IllegalArgumentException("Level exceeds the Normal maximum.");
                if(!result.is(Items.ENCHANTED_BOOK)&&!a.value().isSupportedItem(result))throw new IllegalArgumentException("Enchantment is incompatible with this item.");
                for(int j=0;j<i;j++)if(!Enchantment.areCompatible(a,list.get(j)))throw new IllegalArgumentException("Conflicting enchantments require Advanced mode.");
            }
        }
        if(!edit.enchantments().isEmpty())result.set(key,finalEnchantments);
        return result;
    }
}
