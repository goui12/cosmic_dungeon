package net.goui.cosmicdungeon.client.screen;

import java.util.List;
import java.util.Set;
import net.goui.cosmicdungeon.item.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

/** Detached display stacks only. This class has no player, menu, slot, inventory or network access. */
final class HelpDemoItems {
    static final Set<String> TOPICS = Set.of("healing_potions", "harming_potions", "turtle_master", "brewing_fuel", "bogatyr_wolves", "dragoon_trident", "food", "key");
    private HelpDemoItems() {}
    static List<ItemStack> create(String topic) {
        return switch (topic) {
            case "healing_potions" -> List.of(PotionContents.createItemStack(Items.SPLASH_POTION, Potions.HEALING));
            case "harming_potions" -> List.of(PotionContents.createItemStack(Items.SPLASH_POTION, Potions.HARMING));
            case "turtle_master" -> List.of(PotionContents.createItemStack(Items.POTION, Potions.TURTLE_MASTER));
            case "brewing_fuel" -> List.of(new ItemStack(Items.BLAZE_POWDER), PotionContents.createItemStack(Items.POTION, Potions.WATER));
            case "bogatyr_wolves" -> List.of(new ItemStack(Items.BONE), new ItemStack(Items.ROTTEN_FLESH), new ItemStack(Items.COOKED_BEEF));
            case "dragoon_trident" -> List.of(new ItemStack(Items.TRIDENT));
            case "food" -> List.of(new ItemStack(ModItems.RAW_FARROWS_CHOP.get()));
            case "key" -> List.of(new ItemStack(ModItems.DOOR_KEY.get()));
            default -> List.of();
        };
    }
}
