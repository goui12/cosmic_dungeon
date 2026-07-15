package net.goui.cosmicdungeon.playerclass.dragoon.repair;

import net.goui.cosmicdungeon.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.Optional;

public final class DragoonRepairRules {
    private DragoonRepairRules() {}
    public static final int MIN_UNITS = 1;
    public static final int MAX_UNITS = 4;

    private static final Map<Item, Item> MATERIAL_BY_ITEM = Map.ofEntries(
            e(Items.LEATHER_HELMET, ModItems.LEATHER_PATCH.get()), e(Items.LEATHER_CHESTPLATE, ModItems.LEATHER_PATCH.get()), e(Items.LEATHER_LEGGINGS, ModItems.LEATHER_PATCH.get()), e(Items.LEATHER_BOOTS, ModItems.LEATHER_PATCH.get()),
            e(Items.GOLDEN_HELMET, Items.GOLD_INGOT), e(Items.GOLDEN_CHESTPLATE, Items.GOLD_INGOT), e(Items.GOLDEN_LEGGINGS, Items.GOLD_INGOT), e(Items.GOLDEN_BOOTS, Items.GOLD_INGOT), e(Items.GOLDEN_SWORD, Items.GOLD_INGOT), e(Items.GOLDEN_PICKAXE, Items.GOLD_INGOT), e(Items.GOLDEN_AXE, Items.GOLD_INGOT), e(Items.GOLDEN_SHOVEL, Items.GOLD_INGOT), e(Items.GOLDEN_HOE, Items.GOLD_INGOT),
            e(Items.CHAINMAIL_HELMET, ModItems.CHAIN_LINK.get()), e(Items.CHAINMAIL_CHESTPLATE, ModItems.CHAIN_LINK.get()), e(Items.CHAINMAIL_LEGGINGS, ModItems.CHAIN_LINK.get()), e(Items.CHAINMAIL_BOOTS, ModItems.CHAIN_LINK.get()),
            e(Items.IRON_HELMET, Items.IRON_INGOT), e(Items.IRON_CHESTPLATE, Items.IRON_INGOT), e(Items.IRON_LEGGINGS, Items.IRON_INGOT), e(Items.IRON_BOOTS, Items.IRON_INGOT), e(Items.IRON_SWORD, Items.IRON_INGOT), e(Items.IRON_PICKAXE, Items.IRON_INGOT), e(Items.IRON_AXE, Items.IRON_INGOT), e(Items.IRON_SHOVEL, Items.IRON_INGOT), e(Items.IRON_HOE, Items.IRON_INGOT),
            e(Items.DIAMOND_HELMET, Items.DIAMOND), e(Items.DIAMOND_CHESTPLATE, Items.DIAMOND), e(Items.DIAMOND_LEGGINGS, Items.DIAMOND), e(Items.DIAMOND_BOOTS, Items.DIAMOND), e(Items.DIAMOND_SWORD, Items.DIAMOND), e(Items.DIAMOND_PICKAXE, Items.DIAMOND), e(Items.DIAMOND_AXE, Items.DIAMOND), e(Items.DIAMOND_SHOVEL, Items.DIAMOND), e(Items.DIAMOND_HOE, Items.DIAMOND),
            e(Items.NETHERITE_HELMET, ModItems.NETHERITE_REPAIR_FRAGMENT.get()), e(Items.NETHERITE_CHESTPLATE, ModItems.NETHERITE_REPAIR_FRAGMENT.get()), e(Items.NETHERITE_LEGGINGS, ModItems.NETHERITE_REPAIR_FRAGMENT.get()), e(Items.NETHERITE_BOOTS, ModItems.NETHERITE_REPAIR_FRAGMENT.get()), e(Items.NETHERITE_SWORD, ModItems.NETHERITE_REPAIR_FRAGMENT.get()), e(Items.NETHERITE_PICKAXE, ModItems.NETHERITE_REPAIR_FRAGMENT.get()), e(Items.NETHERITE_AXE, ModItems.NETHERITE_REPAIR_FRAGMENT.get()), e(Items.NETHERITE_SHOVEL, ModItems.NETHERITE_REPAIR_FRAGMENT.get()), e(Items.NETHERITE_HOE, ModItems.NETHERITE_REPAIR_FRAGMENT.get())
    );
    private static Map.Entry<Item, Item> e(Item item, Item material) { return Map.entry(item, material); }

    public static Optional<Item> materialFor(ItemStack stack) {
        if (!isValidRepairItemShape(stack)) return Optional.empty();
        return Optional.ofNullable(MATERIAL_BY_ITEM.get(stack.getItem()));
    }
    public static boolean isSupportedDamagedItem(ItemStack stack) { return materialFor(stack).isPresent(); }
    public static boolean isValidRepairItemShape(ItemStack stack) { return !stack.isEmpty() && stack.getCount() == 1 && stack.isDamageableItem() && stack.getDamageValue() > 0; }
    public static int unitRepairAmount(ItemStack stack) { return Math.max(1, (int)Math.ceil(stack.getMaxDamage() * 0.25D)); }
    public static int requiredUnitsToFull(ItemStack stack) { return Math.max(MIN_UNITS, Math.min(MAX_UNITS, (int)Math.ceil(stack.getDamageValue() / (double)unitRepairAmount(stack)))); }
    public static int clampUnits(ItemStack stack, int units) { return Math.max(MIN_UNITS, Math.min(requiredUnitsToFull(stack), units)); }
    public static int projectedRepair(ItemStack stack, int units) { return Math.min(stack.getDamageValue(), unitRepairAmount(stack) * Math.max(0, units)); }
}
