package net.goui.cosmicdungeon.config;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.*;
/** Contributes to the SAME all_vendors_prices.config spec; never creates another config file. */
public final class VendorCatalog {
    private VendorCatalog() {}
    public record Entry(ModConfigSpec.LongValue purchase, ModConfigSpec.LongValue retail) {}
    private static final Map<String,Entry> ITEMS = new LinkedHashMap<>(), ENCHANTMENTS = new LinkedHashMap<>();
    private static final Map<String, ModConfigSpec.LongValue> NAMED_D1 = new LinkedHashMap<>();
    public static void define(ModConfigSpec.Builder b) {
        b.comment("Dungeon Dropped Gear and Items, 2026-08-28; authoritative final per-item purchase prices.",
                "Includes the authored enchantments: do not add enchantment value again. No retail stock.",
                "Only trusted identities qualify; display names never grant these prices. -1 disables purchase.")
                .push("NamedDungeon1");
        for (var entry : net.goui.cosmicdungeon.item.identity.D1LootCatalog.entries()) {
            b.comment(entry.name() + " (" + entry.baseItem() + ")").push(entry.id());
            NAMED_D1.put(entry.id(), b.defineInRange("purchaseTrace", entry.defaultPurchaseTrace(), -1L, 1_000_000_000L));
            b.pop();
        }
        b.pop();
        b.comment("Pricing Master 2.0, 1B3hQLrrOkZeRPG1tomd54rd7v7OKQ_PDNozHH-DIffY, 2026-08-19.",
                "Documented list prices. Reviewed conversion ceilings may reduce the final sale quote.",
                "Universal purchase prices apply at every eligible vendor. -1 means unpriced/disabled.")
                .push("Universal");
        item(b, "apple", 1L, 2L, "Apple");
        item(b, "baked_potato", 1L, 3L, "Baked Potato");
        item(b, "beetroot", 1L, 2L, "Beetroot");
        item(b, "beetroot_soup", 3L, 4L, "Beetroot Soup");
        item(b, "bread", 2L, 3L, "Bread");
        item(b, "cake", 5L, 6L, "Cake");
        item(b, "carrot", 1L, 2L, "Carrot");
        item(b, "chorus_fruit", 3L, 4L, "Chorus Fruit");
        item(b, "cooked_chicken", 2L, 4L, "Cooked Chicken");
        item(b, "cooked_cod", 2L, 4L, "Cooked Cod");
        item(b, "cooked_mutton", 2L, 4L, "Cooked Mutton");
        item(b, "cooked_porkchop", 2L, 4L, "Cooked Porkchop");
        item(b, "cooked_rabbit", 2L, 4L, "Cooked Rabbit");
        item(b, "cooked_salmon", 2L, 4L, "Cooked Salmon");
        item(b, "cookie", 1L, 2L, "Cookie");
        item(b, "dried_kelp", 1L, 2L, "Dried Kelp");
        item(b, "enchanted_golden_apple", 100L, 120L, "Enchanted Golden Apple");
        item(b, "glow_berries", 1L, 2L, "Glow Berries");
        item(b, "golden_apple", 100L, 120L, "Golden Apple");
        item(b, "golden_carrot", 100L, 120L, "Golden Carrot");
        item(b, "honey_bottle", 3L, 4L, "Honey Bottle");
        item(b, "kelp", 1L, 2L, "Kelp");
        item(b, "melon_slice", 1L, 2L, "Melon Slice");
        item(b, "milk_bucket", 3L, 4L, "Milk Bucket");
        item(b, "mushroom_stew", 3L, 4L, "Mushroom Stew");
        item(b, "poisonous_potato", 1L, 2L, "Poisonous Potato");
        item(b, "potato", 1L, 2L, "Potato");
        item(b, "pufferfish", 2L, 3L, "Pufferfish");
        item(b, "pumpkin_pie", 3L, 4L, "Pumpkin Pie");
        item(b, "rabbit_stew", 5L, 6L, "Rabbit Stew");
        item(b, "beef", 2L, 3L, "Raw Beef");
        item(b, "chicken", 2L, 3L, "Raw Chicken");
        item(b, "cod", 2L, 3L, "Raw Cod");
        item(b, "mutton", 2L, 3L, "Raw Mutton");
        item(b, "porkchop", 2L, 3L, "Raw Porkchop");
        item(b, "rabbit", 2L, 3L, "Raw Rabbit");
        item(b, "salmon", 2L, 3L, "Raw Salmon");
        item(b, "rotten_flesh", 1L, 2L, "Rotten Flesh");
        item(b, "cooked_beef", 2L, 4L, "Steak");
        item(b, "suspicious_stew", 100L, 120L, "Suspicious Stew");
        item(b, "sweet_berries", 1L, 2L, "Sweet Berries");
        item(b, "tropical_fish", 2L, 3L, "Tropical Fish");
        item(b, "leather_boots", 100L, 120L, "Leather Boots");
        item(b, "leather_helmet", 102L, 123L, "Leather Cap");
        item(b, "leather_leggings", 105L, 126L, "Leather Pants");
        item(b, "leather_chestplate", 108L, 130L, "Leather Tunic");
        item(b, "golden_boots", 200L, 240L, "Golden Boots");
        item(b, "golden_helmet", 204L, 245L, "Golden Helmet");
        item(b, "golden_leggings", 210L, 252L, "Golden Leggings");
        item(b, "golden_chestplate", 215L, 258L, "Golden Chestplate");
        item(b, "copper_boots", 400L, 480L, "Copper Boots");
        item(b, "copper_helmet", 408L, 490L, "Copper Helmet");
        item(b, "copper_leggings", 417L, 501L, "Copper Leggings");
        item(b, "copper_chestplate", 426L, 512L, "Copper Chestplate");
        item(b, "chainmail_boots", 600L, 720L, "Chainmail Boots");
        item(b, "chainmail_helmet", 612L, 735L, "Chainmail Helmet");
        item(b, "chainmail_leggings", 626L, 752L, "Chainmail Leggings");
        item(b, "chainmail_chestplate", 640L, 768L, "Chainmail Chestplate");
        item(b, "iron_boots", 800L, 960L, "Iron Boots");
        item(b, "iron_helmet", 815L, 978L, "Iron Helmet");
        item(b, "iron_leggings", 834L, 1001L, "Iron Leggings");
        item(b, "iron_chestplate", 858L, 1030L, "Iron Chestplate");
        item(b, "diamond_boots", 900L, 1080L, "Diamond Boots");
        item(b, "diamond_helmet", 910L, 1092L, "Diamond Helmet");
        item(b, "diamond_leggings", 935L, 1122L, "Diamond Leggings");
        item(b, "diamond_chestplate", 960L, 1152L, "Diamond Chestplate");
        item(b, "netherite_boots", 1100L, 1320L, "Netherite Boots");
        item(b, "netherite_helmet", 1120L, 1344L, "Netherite Helmet");
        item(b, "netherite_leggings", 1150L, 1380L, "Netherite Leggings");
        item(b, "netherite_chestplate", 1180L, 1416L, "Netherite Chestplate");
        item(b, "wooden_sword", 50L, 60L, "Wooden Sword");
        item(b, "stone_sword", 80L, 96L, "Stone Sword");
        item(b, "golden_sword", 160L, 192L, "Golden Sword");
        item(b, "copper_sword", 250L, 300L, "Copper Sword");
        item(b, "iron_sword", 490L, 588L, "Iron Sword");
        item(b, "diamond_sword", 570L, 684L, "Diamond Sword");
        item(b, "netherite_sword", 690L, 828L, "Netherite Sword");
        item(b, "wooden_pickaxe", 60L, 72L, "Wooden Pickaxe");
        item(b, "stone_pickaxe", 100L, 120L, "Stone Pickaxe");
        item(b, "golden_pickaxe", 180L, 216L, "Golden Pickaxe");
        item(b, "copper_pickaxe", 310L, 372L, "Copper Pickaxe");
        item(b, "iron_pickaxe", 620L, 744L, "Iron Pickaxe");
        item(b, "diamond_pickaxe", 720L, 864L, "Diamond Pickaxe");
        item(b, "netherite_pickaxe", 880L, 1056L, "Netherite Pickaxe");
        item(b, "wooden_axe", 60L, 72L, "Wooden Axe");
        item(b, "stone_axe", 100L, 120L, "Stone Axe");
        item(b, "golden_axe", 180L, 216L, "Golden Axe");
        item(b, "copper_axe", 310L, 372L, "Copper Axe");
        item(b, "iron_axe", 620L, 744L, "Iron Axe");
        item(b, "diamond_axe", 720L, 864L, "Diamond Axe");
        item(b, "netherite_axe", 880L, 1056L, "Netherite Axe");
        item(b, "shield", 240L, 288L, "Shield");
        item(b, "bow", 220L, 264L, "Bow");
        item(b, "crossbow", 360L, 432L, "Crossbow");
        item(b, "trident", 580L, 696L, "Trident");
        item(b, "mace", 600L, 720L, "Mace");
        item(b, "arrow", 1L, 2L, "Arrow");
        item(b, "flint_and_steel", 30L, 36L, "Flint and Steel");
        item(b, "torch", 1L, 2L, "Torch");
        item(b, "bucket", 50L, 60L, "Bucket");
        item(b, "nether_wart", 20L, 24L, "Nether Wart Standard");
        item(b, "sugar", 1L, 2L, "Sugar Standard");
        item(b, "spider_eye", 8L, 10L, "Spider Eye Standard");
        item(b, "fermented_spider_eye", 9L, 11L, "Fermented Spider Eye Standard");
        item(b, "magma_cream", 7L, 9L, "Magma Cream Standard");
        item(b, "glistering_melon_slice", 20L, 24L, "Glistering Melon Slice Standard");
        item(b, "blaze_powder", 5L, 6L, "Blaze Powder Standard");
        item(b, "ghast_tear", 20L, 24L, "Ghast Tear Standard");
        item(b, "redstone", 4L, 5L, "Redstone Dust Standard");
        item(b, "glowstone_dust", 6L, 8L, "Glowstone Dust Standard");
        item(b, "gunpowder", 5L, 6L, "Gunpowder Standard");
        item(b, "dragon_breath", 30L, 36L, "Dragon's Breath Standard");
        item(b, "brewing_stand", 80L, 96L, "Brewing Stand Standard");
        item(b, "cauldron", 50L, 60L, "Cauldron Standard");
        item(b, "glass_bottle", 5L, 6L, "Glass Bottle Standard");
        item(b, "potion__healing", 18L, 24L, "Potion of Healing Standard");
        item(b, "potion__strong_healing", 20L, 48L, "Potion of Healing Potency II");
        item(b, "potion__regeneration", 18L, 48L, "Potion of Regeneration Standard");
        item(b, "potion__strength", 13L, 48L, "Potion of Strength Standard");
        item(b, "potion__night_vision", 30L, 36L, "Potion of Night Vision Standard");
        item(b, "potion__fire_resistance", 14L, 48L, "Potion of Fire Resistance Standard");
        item(b, "potion__long_water_breathing", 13L, 48L, "Potion of Water Breathing Extended");
        item(b, "potion__invisibility", 48L, 60L, "Potion of Invisibility Standard");
        item(b, "splash_potion__healing", 20L, 36L, "Splash Potion of Healing Standard");
        item(b, "splash_potion__poison", 16L, 36L, "Splash Potion of Poison Standard");
        item(b, "splash_potion__invisibility", 49L, 72L, "Splash Potion of Invisibility Standard");
        item(b, "lingering_potion__healing", 30L, 60L, "Lingering Potion of Healing Standard");
        item(b, "lingering_potion__regeneration", 30L, 84L, "Lingering Potion of Regeneration Standard");
        item(b, "repair__oak_planks", 1L, 2L, "Oak Planks (repair component)");
        item(b, "repair__spruce_planks", 4L, 5L, "Spruce Planks (repair component)");
        item(b, "repair__cobblestone", 2L, 3L, "Cobblestone (repair component)");
        item(b, "repair__leather", 1L, 2L, "Leather (repair component)");
        item(b, "repair__gold_ingot", 3L, 4L, "Gold Ingot (repair component)");
        item(b, "repair__copper_ingot", 6L, 8L, "Copper Ingot (repair component)");
        item(b, "repair__iron_ingot", 10L, 12L, "Iron Ingot (repair component)");
        item(b, "repair__diamond", 15L, 18L, "Diamond (repair component)");
        item(b, "repair__netherite_ingot", 18L, 22L, "Netherite Ingot (repair component)");
        item(b, "repair__bow_repair_kit", 6L, 8L, "Bow Repair Kit (repair component)");
        item(b, "repair__crossbow_repair_kit", 11L, 14L, "Crossbow Repair Kit (repair component)");
        item(b, "repair__trident_repair_kit", 30L, 36L, "Trident Repair Kit (repair component)");
        item(b, "repair__mace_repair_kit", 15L, 18L, "Mace Repair Kit (repair component)");
        b.pop();
        b.push("Enchantments");
        enchant(b, "aqua_affinity", 4L, 40L);
        enchant(b, "blast_protection", 16L, 160L);
        enchant(b, "binding_curse", -50L, -500L);
        enchant(b, "depth_strider", 16L, 160L);
        enchant(b, "feather_falling", 10L, 100L);
        enchant(b, "fire_protection", 16L, 160L);
        enchant(b, "frost_walker", 9L, 90L);
        enchant(b, "projectile_protection", 16L, 160L);
        enchant(b, "protection", 16L, 160L);
        enchant(b, "respiration", 16L, 160L);
        enchant(b, "soul_speed", 10L, 100L);
        enchant(b, "swift_sneak", 4L, 40L);
        enchant(b, "thorns", 20L, 200L);
        enchant(b, "bane_of_arthropods", 16L, 160L);
        enchant(b, "breach", 12L, 120L);
        enchant(b, "density", 18L, 180L);
        enchant(b, "fire_aspect", 17L, 170L);
        enchant(b, "knockback", 12L, 120L);
        enchant(b, "looting", 14L, 140L);
        enchant(b, "sharpness", 25L, 250L);
        enchant(b, "smite", 25L, 250L);
        enchant(b, "sweeping", 20L, 200L);
        enchant(b, "wind_burst", 25L, 250L);
        enchant(b, "flame", 11L, 110L);
        enchant(b, "infinity", 20L, 200L);
        enchant(b, "multishot", 8L, 80L);
        enchant(b, "piercing", 18L, 180L);
        enchant(b, "power", 23L, 230L);
        enchant(b, "punch", 8L, 75L);
        enchant(b, "quick_charge", 19L, 190L);
        enchant(b, "channeling", 25L, 250L);
        enchant(b, "impaling", 12L, 120L);
        enchant(b, "loyalty", 14L, 140L);
        enchant(b, "riptide", 15L, 150L);
        enchant(b, "efficiency", 25L, 250L);
        enchant(b, "silk_touch", 8L, 80L);
        enchant(b, "luck_of_the_sea", 8L, 80L);
        enchant(b, "lure", 8L, 80L);
        enchant(b, "vanishing_curse", -50L, -500L);
        enchant(b, "mending", 28L, 275L);
        enchant(b, "unbreaking", 28L, 275L);
        b.pop();
    }
    private static void item(ModConfigSpec.Builder b,String key,long purchase,long retail,String name) {
        b.comment(name).push(key);
        if(key.equals("bucket")) b.comment("Documented list price; the live milk/container conversion ceiling may reduce the quote (Q&A D83).");
        ITEMS.put(key,new Entry(b.defineInRange("purchaseTrace",purchase,-1L,1_000_000_000L),
                b.defineInRange("retailTrace",retail,-1L,1_000_000_000L)));
        b.pop();
    }
    private static void enchant(ModConfigSpec.Builder b,String key,long purchase,long retail) {
        b.push(key);
        ENCHANTMENTS.put(key,new Entry(b.comment("Per actual enchantment level. Curses subtract value.")
                .defineInRange("purchasePerLevelTrace",purchase,-1_000_000_000L,1_000_000_000L),
                b.defineInRange("retailPerLevelTrace",retail,-1_000_000_000L,1_000_000_000L)));
        b.pop();
    }
    public static ModConfigSpec.LongValue namedD1(String key) { return NAMED_D1.get(key); }
    public static Entry item(String key) { return ITEMS.get(key); }
    public static Entry enchantment(String key) {
        // Native 1.21.10 ID; retain the existing config section and developer overrides.
        return ENCHANTMENTS.get(key.equals("sweeping_edge") ? "sweeping" : key);
    }
    // TODO(M106, authored-world catalogue review): the retained 2026-07-07 Non-Exhaustive
    // Item List (1xT6KWQ_iLsygcQA-FwI0mH_79p0p4hv96l-aSlyf4rw) is a 26.2 candidate list,
    // not approved stock. Keep admin items, unpriced rods/shovels/spears/books/rockets and
    // deleted-tab references out of inferred offers. Resolve links against retained D1 item
    // identities and authored containers without renaming equipment or restoring deleted tabs.
    // Fortune has no approved adjustment; named final prices never receive a second premium.
    // TODO(M87, later-version data): Pricing Master lists Lunge from Minecraft 1.21.11.
    // Keep this 1.21.10 build unchanged; add its 20/200 Trace per-level schedule only
    // after an explicitly approved game-version upgrade and registry validation.
}
