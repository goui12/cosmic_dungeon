package net.goui.cosmicdungeon.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.minecraft.resources.ResourceLocation;
import net.goui.cosmicdungeon.vendor.VendorOffer;
import java.util.LinkedHashMap;
import java.util.Map;

/** All server vendor prices in all_vendors_prices.config. Whole Trace, not display denominations. */
public final class VendorPricesConfig {
    private VendorPricesConfig() {}
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();
    public record Price(ModConfigSpec.LongValue retail) {}
    private static final Map<String, Price> OFFERS = new LinkedHashMap<>();
    public static final ModConfigSpec.LongValue INN_BOND,FARROW_RAW_PURCHASE;
    public static final ModConfigSpec.DoubleValue DIRECT_REPAIR_MULTIPLIER, SUGGESTED_LABOR_MULTIPLIER;
    public static final ModConfigSpec SPEC;
    static {
        VendorCatalog.define(B);
        offer("NatonWhitlock", "cosmicdungeon:d1/general_supply_vendor", "flint_and_steel", 36L, 30L, "Flint and Steel . Vendor pays 30 Trace; player pays 36 Trace. Source 1imeax1orDl6PGzIMFiwnCTD6L6PK2igBflPkjQx0TfE.");
        offer("NatonWhitlock", "cosmicdungeon:d1/general_supply_vendor", "torch", 2L, 1L, "Torch . Vendor pays 1 Trace; player pays 2 Trace. Source 1imeax1orDl6PGzIMFiwnCTD6L6PK2igBflPkjQx0TfE.");
        offer("NatonWhitlock", "cosmicdungeon:d1/general_supply_vendor", "bread", 3L, 2L, "Bread . Vendor pays 2 Trace; player pays 3 Trace. Source 1imeax1orDl6PGzIMFiwnCTD6L6PK2igBflPkjQx0TfE.");
        offer("NatonWhitlock", "cosmicdungeon:d1/general_supply_vendor", "baked_potato", 3L, 1L, "Baked Potato . Vendor pays 1 Trace; player pays 3 Trace. Source 1imeax1orDl6PGzIMFiwnCTD6L6PK2igBflPkjQx0TfE.");
        offer("NatonWhitlock", "cosmicdungeon:d1/general_supply_vendor", "cooked_chicken", 4L, 2L, "Cooked Chicken . Vendor pays 2 Trace; player pays 4 Trace. Source 1imeax1orDl6PGzIMFiwnCTD6L6PK2igBflPkjQx0TfE.");
        offer("NatonWhitlock", "cosmicdungeon:d1/general_supply_vendor", "apple", 2L, 1L, "Apple . Vendor pays 1 Trace; player pays 2 Trace. Source 1imeax1orDl6PGzIMFiwnCTD6L6PK2igBflPkjQx0TfE.");
        offer("NatonWhitlock", "cosmicdungeon:d1/general_supply_vendor", "carrot", 2L, 1L, "Carrot . Vendor pays 1 Trace; player pays 2 Trace. Source 1imeax1orDl6PGzIMFiwnCTD6L6PK2igBflPkjQx0TfE.");
        offer("NatonWhitlock", "cosmicdungeon:d1/general_supply_vendor", "bucket", 60L, 50L, "Bucket . Vendor pays 50 Trace; player pays 60 Trace. Source 1imeax1orDl6PGzIMFiwnCTD6L6PK2igBflPkjQx0TfE.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "wooden_sword", 60L, 50L, "Wooden Sword . Vendor pays 50 Trace; player pays 60 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "stone_sword", 96L, 80L, "Stone Sword . Vendor pays 80 Trace; player pays 96 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "golden_sword", 192L, 160L, "Golden Sword . Vendor pays 160 Trace; player pays 192 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "copper_sword", 300L, 250L, "Copper Sword . Vendor pays 250 Trace; player pays 300 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "iron_sword", 588L, 490L, "Iron Sword . Vendor pays 490 Trace; player pays 588 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "diamond_sword", 684L, 570L, "Diamond Sword . Vendor pays 570 Trace; player pays 684 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "netherite_sword", 828L, 690L, "Netherite Sword . Vendor pays 690 Trace; player pays 828 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "wooden_pickaxe", 72L, 60L, "Wooden Pickaxe . Vendor pays 60 Trace; player pays 72 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "stone_pickaxe", 120L, 100L, "Stone Pickaxe . Vendor pays 100 Trace; player pays 120 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "golden_pickaxe", 216L, 180L, "Golden Pickaxe . Vendor pays 180 Trace; player pays 216 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "copper_pickaxe", 372L, 310L, "Copper Pickaxe . Vendor pays 310 Trace; player pays 372 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "iron_pickaxe", 744L, 620L, "Iron Pickaxe . Vendor pays 620 Trace; player pays 744 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "diamond_pickaxe", 864L, 720L, "Diamond Pickaxe . Vendor pays 720 Trace; player pays 864 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "netherite_pickaxe", 1056L, 880L, "Netherite Pickaxe . Vendor pays 880 Trace; player pays 1056 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "wooden_axe", 72L, 60L, "Wooden Axe . Vendor pays 60 Trace; player pays 72 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "stone_axe", 120L, 100L, "Stone Axe . Vendor pays 100 Trace; player pays 120 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "golden_axe", 216L, 180L, "Golden Axe . Vendor pays 180 Trace; player pays 216 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "copper_axe", 372L, 310L, "Copper Axe . Vendor pays 310 Trace; player pays 372 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "iron_axe", 744L, 620L, "Iron Axe . Vendor pays 620 Trace; player pays 744 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "diamond_axe", 864L, 720L, "Diamond Axe . Vendor pays 720 Trace; player pays 864 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "netherite_axe", 1056L, 880L, "Netherite Axe . Vendor pays 880 Trace; player pays 1056 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "shield", 288L, 240L, "Shield . Vendor pays 240 Trace; player pays 288 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "bow", 264L, 220L, "Bow . Vendor pays 220 Trace; player pays 264 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "crossbow", 432L, 360L, "Crossbow . Vendor pays 360 Trace; player pays 432 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "mace", 720L, 600L, "Mace . Vendor pays 600 Trace; player pays 720 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "trident", 696L, 580L, "Trident . Vendor pays 580 Trace; player pays 696 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "arrow", 2L, 1L, "Arrow . Vendor pays 1 Trace; player pays 2 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "oak_planks", 2L, 1L, "Oak Planks . Vendor pays 1 Trace; player pays 2 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "spruce_planks", 5L, 4L, "Spruce Planks . Vendor pays 4 Trace; player pays 5 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "cobblestone", 3L, 2L, "Cobblestone . Vendor pays 2 Trace; player pays 3 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "leather", 2L, 1L, "Leather . Vendor pays 1 Trace; player pays 2 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "gold_ingot", 4L, 3L, "Gold Ingot . Vendor pays 3 Trace; player pays 4 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "copper_ingot", 8L, 6L, "Copper Ingot . Vendor pays 6 Trace; player pays 8 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "iron_ingot", 12L, 10L, "Iron Ingot . Vendor pays 10 Trace; player pays 12 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "diamond", 18L, 15L, "Diamond . Vendor pays 15 Trace; player pays 18 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "netherite_ingot", 22L, 18L, "Netherite Ingot . Vendor pays 18 Trace; player pays 22 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "mace_repair_kit", 18L, 15L, "Mace Repair Kit . Vendor pays 15 Trace; player pays 18 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "bow_repair_kit", 8L, 6L, "Bow Repair Kit . Vendor pays 6 Trace; player pays 8 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "crossbow_repair_kit", 14L, 11L, "Crossbow Repair Kit . Vendor pays 11 Trace; player pays 14 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EliasCentvin", "cosmicdungeon:d1/weapon_supplier", "trident_repair_kit", 36L, 30L, "Trident Repair Kit . Vendor pays 30 Trace; player pays 36 Trace. Source 1Ec-oe2XPufuQF4kzH9ah2mr4IvlwgvVQb3SL6sQW8JM.");
        offer("EonPenrose", "cosmicdungeon:d1/brewing_store", "nether_wart", 24L, 20L, "Nether Wart . Vendor pays 20 Trace; player pays 24 Trace. Source 1zbdCxNEZJQff3A5c7yYHbllhs0SrTEGDxaBVXWXX6es.");
        offer("EonPenrose", "cosmicdungeon:d1/brewing_store", "sugar", 2L, 1L, "Sugar . Vendor pays 1 Trace; player pays 2 Trace. Source 1zbdCxNEZJQff3A5c7yYHbllhs0SrTEGDxaBVXWXX6es.");
        offer("EonPenrose", "cosmicdungeon:d1/brewing_store", "spider_eye", 10L, 8L, "Spider Eye . Vendor pays 8 Trace; player pays 10 Trace. Source 1zbdCxNEZJQff3A5c7yYHbllhs0SrTEGDxaBVXWXX6es.");
        offer("EonPenrose", "cosmicdungeon:d1/brewing_store", "fermented_spider_eye", 11L, 9L, "Fermented Spider Eye . Vendor pays 9 Trace; player pays 11 Trace. Source 1zbdCxNEZJQff3A5c7yYHbllhs0SrTEGDxaBVXWXX6es.");
        offer("EonPenrose", "cosmicdungeon:d1/brewing_store", "magma_cream", 9L, 7L, "Magma Cream . Vendor pays 7 Trace; player pays 9 Trace. Source 1zbdCxNEZJQff3A5c7yYHbllhs0SrTEGDxaBVXWXX6es.");
        offer("EonPenrose", "cosmicdungeon:d1/brewing_store", "glistering_melon_slice", 24L, 20L, "Glistering Melon Slice . Vendor pays 20 Trace; player pays 24 Trace. Source 1zbdCxNEZJQff3A5c7yYHbllhs0SrTEGDxaBVXWXX6es.");
        offer("EonPenrose", "cosmicdungeon:d1/brewing_store", "blaze_powder", 6L, 5L, "Blaze Powder . Vendor pays 5 Trace; player pays 6 Trace. Source 1zbdCxNEZJQff3A5c7yYHbllhs0SrTEGDxaBVXWXX6es.");
        offer("EonPenrose", "cosmicdungeon:d1/brewing_store", "ghast_tear", 24L, 20L, "Ghast Tear . Vendor pays 20 Trace; player pays 24 Trace. Source 1zbdCxNEZJQff3A5c7yYHbllhs0SrTEGDxaBVXWXX6es.");
        offer("EonPenrose", "cosmicdungeon:d1/brewing_store", "redstone", 5L, 4L, "Redstone Dust . Vendor pays 4 Trace; player pays 5 Trace. Source 1zbdCxNEZJQff3A5c7yYHbllhs0SrTEGDxaBVXWXX6es.");
        offer("EonPenrose", "cosmicdungeon:d1/brewing_store", "glowstone_dust", 8L, 6L, "Glowstone Dust . Vendor pays 6 Trace; player pays 8 Trace. Source 1zbdCxNEZJQff3A5c7yYHbllhs0SrTEGDxaBVXWXX6es.");
        offer("EonPenrose", "cosmicdungeon:d1/brewing_store", "gunpowder", 6L, 5L, "Gunpowder . Vendor pays 5 Trace; player pays 6 Trace. Source 1zbdCxNEZJQff3A5c7yYHbllhs0SrTEGDxaBVXWXX6es.");
        offer("EonPenrose", "cosmicdungeon:d1/brewing_store", "dragon_breath", 36L, 30L, "Dragon's Breath . Vendor pays 30 Trace; player pays 36 Trace. Source 1zbdCxNEZJQff3A5c7yYHbllhs0SrTEGDxaBVXWXX6es.");
        offer("EonPenrose", "cosmicdungeon:d1/brewing_store", "glass_bottle", 6L, 5L, "Glass Bottle . Vendor pays 5 Trace; player pays 6 Trace. Source 1zbdCxNEZJQff3A5c7yYHbllhs0SrTEGDxaBVXWXX6es.");
        offer("EonPenrose", "cosmicdungeon:d1/brewing_store", "brewing_stand", 96L, 80L, "Brewing Stand . Vendor pays 80 Trace; player pays 96 Trace. Source 1zbdCxNEZJQff3A5c7yYHbllhs0SrTEGDxaBVXWXX6es.");
        offer("EonPenrose", "cosmicdungeon:d1/brewing_store", "cauldron", 60L, 50L, "Cauldron . Vendor pays 50 Trace; player pays 60 Trace. Source 1zbdCxNEZJQff3A5c7yYHbllhs0SrTEGDxaBVXWXX6es.");
        offer("EonPenrose", "cosmicdungeon:d1/brewing_store", "potion_of_night_vision", 36L, 30L, "Potion of Night Vision Standard. Vendor pays 30 Trace; player pays 36 Trace. Source 1zbdCxNEZJQff3A5c7yYHbllhs0SrTEGDxaBVXWXX6es.");
        offer("EonPenrose", "cosmicdungeon:d1/brewing_store", "potion_of_fire_resistance", 48L, 14L, "Potion of Fire Resistance Standard. Vendor pays 14 Trace; player pays 48 Trace. Source 1zbdCxNEZJQff3A5c7yYHbllhs0SrTEGDxaBVXWXX6es.");
        offer("EonPenrose", "cosmicdungeon:d1/brewing_store", "potion_of_healing", 24L, 18L, "Potion of Healing Standard. Vendor pays 18 Trace; player pays 24 Trace. Source 1zbdCxNEZJQff3A5c7yYHbllhs0SrTEGDxaBVXWXX6es.");
        offer("EonPenrose", "cosmicdungeon:d1/brewing_store", "splash_potion_of_healing", 36L, 20L, "Splash Potion of Healing Standard. Vendor pays 20 Trace; player pays 36 Trace. Source 1zbdCxNEZJQff3A5c7yYHbllhs0SrTEGDxaBVXWXX6es.");
        offer("EonPenrose", "cosmicdungeon:d1/brewing_store", "potion_of_healing_ii", 48L, 20L, "Potion of Healing Potency II. Vendor pays 20 Trace; player pays 48 Trace. Source 1zbdCxNEZJQff3A5c7yYHbllhs0SrTEGDxaBVXWXX6es.");
        offer("EonPenrose", "cosmicdungeon:d1/brewing_store", "potion_of_regeneration", 48L, 18L, "Potion of Regeneration Standard. Vendor pays 18 Trace; player pays 48 Trace. Source 1zbdCxNEZJQff3A5c7yYHbllhs0SrTEGDxaBVXWXX6es.");
        offer("EonPenrose", "cosmicdungeon:d1/brewing_store", "lingering_potion_of_healing", 60L, 30L, "Lingering Potion of Healing Standard. Vendor pays 30 Trace; player pays 60 Trace. Source 1zbdCxNEZJQff3A5c7yYHbllhs0SrTEGDxaBVXWXX6es.");
        offer("EonPenrose", "cosmicdungeon:d1/brewing_store", "potion_of_strength", 48L, 13L, "Potion of Strength Standard. Vendor pays 13 Trace; player pays 48 Trace. Source 1zbdCxNEZJQff3A5c7yYHbllhs0SrTEGDxaBVXWXX6es. D2 stock deferred; price retained for later implementation.");
        offer("EonPenrose", "cosmicdungeon:d1/brewing_store", "potion_of_water_breathing", 48L, 13L, "Potion of Water Breathing Extended. Vendor pays 13 Trace; player pays 48 Trace. Source 1zbdCxNEZJQff3A5c7yYHbllhs0SrTEGDxaBVXWXX6es. D2 stock deferred; price retained for later implementation.");
        offer("EonPenrose", "cosmicdungeon:d1/brewing_store", "potion_of_invisibility", 60L, 48L, "Potion of Invisibility Standard. Vendor pays 48 Trace; player pays 60 Trace. Source 1zbdCxNEZJQff3A5c7yYHbllhs0SrTEGDxaBVXWXX6es. D2 stock deferred; price retained for later implementation.");
        offer("EonPenrose", "cosmicdungeon:d1/brewing_store", "splash_potion_of_poison", 36L, 16L, "Splash Potion of Poison Standard. Vendor pays 16 Trace; player pays 36 Trace. Source 1zbdCxNEZJQff3A5c7yYHbllhs0SrTEGDxaBVXWXX6es. D2 stock deferred; price retained for later implementation.");
        offer("EonPenrose", "cosmicdungeon:d1/brewing_store", "splash_potion_of_invisibility", 72L, 49L, "Splash Potion of Invisibility Standard. Vendor pays 49 Trace; player pays 72 Trace. Source 1zbdCxNEZJQff3A5c7yYHbllhs0SrTEGDxaBVXWXX6es. D2 stock deferred; price retained for later implementation.");
        offer("EonPenrose", "cosmicdungeon:d1/brewing_store", "lingering_potion_of_regeneration", 84L, 30L, "Lingering Potion of Regeneration Standard. Vendor pays 30 Trace; player pays 84 Trace. Source 1zbdCxNEZJQff3A5c7yYHbllhs0SrTEGDxaBVXWXX6es. D2 stock deferred; price retained for later implementation.");
        offer("BeatrixFarrow", "cosmicdungeon:d1/food_vendor", "potato", 2L, 1L, "Potato . Vendor pays 1 Trace; player pays 2 Trace. Source 1pqYKxtwoV74C-pdJJqNfl4ZPwoZe3cnQOSuTdVzJA2I.");
        offer("BeatrixFarrow", "cosmicdungeon:d1/food_vendor", "kelp", 2L, 1L, "Kelp . Vendor pays 1 Trace; player pays 2 Trace. Source 1pqYKxtwoV74C-pdJJqNfl4ZPwoZe3cnQOSuTdVzJA2I.");
        offer("BeatrixFarrow", "cosmicdungeon:d1/food_vendor", "raw_beef", 3L, 2L, "Raw Beef . Vendor pays 2 Trace; player pays 3 Trace. Source 1pqYKxtwoV74C-pdJJqNfl4ZPwoZe3cnQOSuTdVzJA2I.");
        offer("BeatrixFarrow", "cosmicdungeon:d1/food_vendor", "raw_chicken", 3L, 2L, "Raw Chicken . Vendor pays 2 Trace; player pays 3 Trace. Source 1pqYKxtwoV74C-pdJJqNfl4ZPwoZe3cnQOSuTdVzJA2I.");
        offer("BeatrixFarrow", "cosmicdungeon:d1/food_vendor", "raw_rabbit", 3L, 2L, "Raw Rabbit . Vendor pays 2 Trace; player pays 3 Trace. Source 1pqYKxtwoV74C-pdJJqNfl4ZPwoZe3cnQOSuTdVzJA2I.");
        offer("BeatrixFarrow", "cosmicdungeon:d1/food_vendor", "raw_porkchop", 3L, 2L, "Raw Porkchop . Vendor pays 2 Trace; player pays 3 Trace. Source 1pqYKxtwoV74C-pdJJqNfl4ZPwoZe3cnQOSuTdVzJA2I.");
        offer("BeatrixFarrow", "cosmicdungeon:d1/food_vendor", "raw_mutton", 3L, 2L, "Raw Mutton . Vendor pays 2 Trace; player pays 3 Trace. Source 1pqYKxtwoV74C-pdJJqNfl4ZPwoZe3cnQOSuTdVzJA2I.");
        offer("BeatrixFarrow", "cosmicdungeon:d1/food_vendor", "raw_cod", 3L, 2L, "Raw Cod . Vendor pays 2 Trace; player pays 3 Trace. Source 1pqYKxtwoV74C-pdJJqNfl4ZPwoZe3cnQOSuTdVzJA2I.");
        offer("BeatrixFarrow", "cosmicdungeon:d1/food_vendor", "raw_salmon", 3L, 2L, "Raw Salmon . Vendor pays 2 Trace; player pays 3 Trace. Source 1pqYKxtwoV74C-pdJJqNfl4ZPwoZe3cnQOSuTdVzJA2I.");
        offer("BeatrixFarrow", "cosmicdungeon:d1/food_vendor", "raw_farrows_chop", 2000L, 1600L, "Raw Farrow's Chop . Vendor pays 1600 Trace; player pays 2000 Trace. Source 1pqYKxtwoV74C-pdJJqNfl4ZPwoZe3cnQOSuTdVzJA2I.");
        offer("GritchoftheBarterPit", "cosmicdungeon:d1/d1_nether_gritch_of_the_barter_pit", "golden_carrot", 120L, 0L, "golden_carrot . Vendor pays 0 Trace; player pays 120 Trace. Price source 1B3hQLrrOkZeRPG1tomd54rd7v7OKQ_PDNozHH-DIffY, 2026-08-19; legacy NPC role remains pending.");
        offer("GritchoftheBarterPit", "cosmicdungeon:d1/d1_nether_gritch_of_the_barter_pit", "golden_melon_slice", 24L, 0L, "golden_melon_slice . Vendor pays 0 Trace; player pays 24 Trace. Price source 1B3hQLrrOkZeRPG1tomd54rd7v7OKQ_PDNozHH-DIffY, 2026-08-19; legacy NPC role remains pending.");

        B.push("BeatrixFarrow").push("raw_farrows_chop");
        FARROW_RAW_PURCHASE=B.comment("Vendor pays the owner for one unpowered Raw Chop. Cooked/bound returns are never bought.")
                .defineInRange("purchaseTrace",1600L,0L,1_000_000_000L);
        B.pop(2);
        B.push("Beluzon");
        INN_BOND = B.comment("Once per UUID. NPC Beluzon (Internal), 1FT6k2MFKgQf_tQ5UcBn0wmqJ9-yY_Wdpjna4USdVOZA.")
                .defineInRange("innBondTrace", 15L, 0L, 1_000_000_000L);
        B.pop();
        B.push("EliasCentvin").push("RepairService");
        DIRECT_REPAIR_MULTIPLIER = B.comment("Multiply complete component list subtotal once, then ceil; faction applies afterward.")
                .defineInRange("directShopMultiplier",2.25,1.0,100.0);
        SUGGESTED_LABOR_MULTIPLIER = B.comment("Guidance only; players may negotiate any whole-Trace fee, including zero.")
                .defineInRange("suggestedLaborMultiplier",0.50,0.0,100.0);
        B.pop(2);
        SPEC = B.build();
    }
    private static void offer(String vendor, String profile, String offer, long retail, long purchase, String source) {
        B.push(vendor).comment(source).push(offer);
        var sell = B.comment("Whole Trace charged to the player before faction adjustment; -1 disables retail.")
                .defineInRange("retailTrace", retail, -1L, 1_000_000_000L);
        B.pop(2);
        String offerId = offer.contains(":") ? offer : "minecraft:" + offer;
        OFFERS.put(profile + "|" + offerId, new Price(sell));
    }
    public static Price find(ResourceLocation profile, ResourceLocation offer) {
        return OFFERS.get(profile + "|" + offer);
    }
    public static long retail(ResourceLocation profile, VendorOffer offer) {
        Price configured = find(profile, offer.id());
        // Unreviewed data-pack offers fail closed instead of inventing a price outside this config.
        return configured == null ? -1L : configured.retail().get();
    }
}
