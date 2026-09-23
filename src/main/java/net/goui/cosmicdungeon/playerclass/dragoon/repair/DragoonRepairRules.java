package net.goui.cosmicdungeon.playerclass.dragoon.repair;
import net.goui.cosmicdungeon.Config;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.Map;
import java.util.Optional;

/** Dragoon Repair 2.0, 1Gbcq7Piqg2uHO1smx5oHOyeGxoT9g93cH-_G8WvhOoo, 2026-08-18. */
public final class DragoonRepairRules {
    private DragoonRepairRules() {}
    public static final int MIN_UNITS=1, MAX_UNITS=4;
    private static final Map<String,String> MATERIAL=Map.of(
            "wooden","oak_planks","stone","cobblestone","leather","leather","golden","gold_ingot",
            "copper","copper_ingot","chainmail","iron_ingot","iron","iron_ingot",
            "diamond","diamond","netherite","netherite_ingot");
    public static String componentKey(ItemStack stack) {
        if(!isValidRepairItemShape(stack)) return null;
        var id=BuiltInRegistries.ITEM.getKey(stack.getItem());
        if(!id.getNamespace().equals("minecraft")) return null;
        String path=id.getPath();
        if(path.equals("shield")) return "spruce_planks";
        if(java.util.Set.of("bow","crossbow","trident","mace").contains(path)) return path+"_repair_kit";
        int split=path.indexOf('_');
        if(split<0) return null;
        String shape=path.substring(split+1);
        if(!java.util.Set.of("helmet","chestplate","leggings","boots","sword","axe","pickaxe","shovel").contains(shape)) return null;
        return MATERIAL.get(path.substring(0,split));
    }
    public static Optional<Item> materialFor(ItemStack stack) { return Optional.ofNullable(RepairComponents.item(componentKey(stack))); }
    public static boolean isSupportedDamagedItem(ItemStack stack) { return componentKey(stack)!=null; }
    public static boolean isValidRepairItemShape(ItemStack stack) {
        return !stack.isEmpty() && stack.getCount()==1 && stack.isDamageableItem() && stack.getDamageValue()>0
                && !stack.has(DataComponents.UNBREAKABLE) && !RepairComponents.marked(stack);
    }
    public static boolean fullKit(ItemStack stack) {
        String key=componentKey(stack);
        return key!=null && java.util.Set.of("bow_repair_kit","crossbow_repair_kit","trident_repair_kit").contains(key);
    }
    public static int unitRepairAmount(ItemStack stack) { return Math.max(1,(int)Math.ceil(stack.getMaxDamage()*0.25)); }
    public static int requiredUnitsToFull(ItemStack stack) { return fullKit(stack)?4:Math.max(1,Math.min(4,(int)Math.ceil(stack.getDamageValue()/(stack.getMaxDamage()*0.25)))); }
    public static int clampUnits(ItemStack stack,int units) { return fullKit(stack)?4:Math.max(1,Math.min(4,units)); }
    public static int componentCount(ItemStack stack,int units) { return fullKit(stack)?1:units; }
    public static int projectedRepair(ItemStack stack,int units) { return Math.min(stack.getDamageValue(),(int)Math.ceil(stack.getMaxDamage()*(Math.max(0,units)/4.0))); }
    public static int durationTicks(int units) { return Config.REPAIR_STEP_TICKS.get()*units; }
    // TODO(D1 compatibility, later game version): Repair 2.0 also lists Spears from 1.21.11.
    // This mod remains on 1.21.10, which has no vanilla Spear registry entries. Add that row
    // only with an approved version upgrade; preserve every existing item and world registry ID.
}
