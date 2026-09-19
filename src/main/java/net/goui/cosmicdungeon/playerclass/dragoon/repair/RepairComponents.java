package net.goui.cosmicdungeon.playerclass.dragoon.repair;
import net.goui.cosmicdungeon.component.ModDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.Map;

/** Repair 2.0 (2026-08-18): vanilla IDs + internal marker, including all four Repair Kits. */
public final class RepairComponents {
    private RepairComponents() {}
    private static final Map<String,String> ITEMS = Map.ofEntries(
            Map.entry("oak_planks","oak_planks"),Map.entry("spruce_planks","spruce_planks"),
            Map.entry("cobblestone","cobblestone"),Map.entry("leather","leather"),
            Map.entry("gold_ingot","gold_ingot"),Map.entry("copper_ingot","copper_ingot"),
            Map.entry("iron_ingot","iron_ingot"),Map.entry("diamond","diamond"),
            Map.entry("netherite_ingot","netherite_ingot"),Map.entry("bow_repair_kit","bow"),
            Map.entry("crossbow_repair_kit","crossbow"),Map.entry("trident_repair_kit","trident"),
            Map.entry("mace_repair_kit","mace"));
    public static boolean marked(ItemStack stack) { return stack.has(ModDataComponents.REPAIR_COMPONENT.get()); }
    public static String key(ItemStack stack) {
        String key = stack.get(ModDataComponents.REPAIR_COMPONENT.get());
        if (key == null || !ITEMS.containsKey(key)) return null;
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!id.toString().equals("minecraft:"+ITEMS.get(key)) || stack.getDamageValue() != 0
                || !stack.getOrDefault(DataComponents.ENCHANTMENTS, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY).isEmpty()) return null;
        return key;
    }
    public static void mark(ItemStack stack, String key) {
        if (!ITEMS.containsKey(key)) throw new IllegalArgumentException("Unknown repair component "+key);
        if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals("minecraft:"+ITEMS.get(key)))
            throw new IllegalArgumentException("Wrong underlying repair item");
        stack.set(ModDataComponents.REPAIR_COMPONENT.get(),key);
        if (key.endsWith("_repair_kit")) {
            String weapon = key.substring(0,key.indexOf('_'));
            stack.set(DataComponents.CUSTOM_NAME,Component.literal(Character.toUpperCase(weapon.charAt(0))+weapon.substring(1)+" Repair Kit"));
        }
    }
    public static boolean validFor(ItemStack stack, Item item) { return stack.is(item) && key(stack) != null; }
    public static Item item(String key) {
        String id=key==null?null:ITEMS.get(key);
        return id==null?null:BuiltInRegistries.ITEM.getValue(ResourceLocation.withDefaultNamespace(id));
    }
    public static String keyForItem(Item item) {
        String id=BuiltInRegistries.ITEM.getKey(item).getPath();
        return ITEMS.entrySet().stream().filter(e->e.getValue().equals(id)).map(Map.Entry::getKey).findFirst().orElse(null);
    }
}
