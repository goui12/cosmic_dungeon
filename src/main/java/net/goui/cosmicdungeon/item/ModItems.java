package net.goui.cosmicdungeon.item;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.item.custom.BroodingForkItem;
import net.goui.cosmicdungeon.item.custom.ChiselItem;
import net.goui.cosmicdungeon.item.custom.ColoredBlockItem;
import net.goui.cosmicdungeon.item.custom.DoorKeyItem;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.Block;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;
import java.util.function.Supplier;


/**
 * ModItems (NeoForge 1.21.8)
 *
 * Uses DeferredRegister.Items (createItems) and DeferredItem<T>.
 * - Keep all item registration centralized here.
 * - Group items by category with clear headers.
 * - Use anonymous subclasses when we just need custom tooltips.
 *
 * Notes:
 * - Judicator armor variants below use vanilla CHAINMAIL stats & models,
 *   are NOT craftable by default (no recipes provided),
 *   and each has a purple/magenta tooltip line via translatable keys.
 * - Texture filenames should be lowercase underscored (e.g., visor_of_first_oath.png).
 */
public class ModItems {

    /** Primary item register for this mod id. */
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(CosmicDungeonMod.MOD_ID);

    // --------------------------------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------------------------------

    /** Helper for ColoredBlockItem (keeps BlockItem props consistent & avoids lambdas throughout). */
    public static <T extends Block> DeferredItem<Item> registerColoredBlockItem(String name, Supplier<T> block) {
        return ITEMS.registerItem(name, props -> new ColoredBlockItem(block.get(), props));
    }

    // --------------------------------------------------------------------------------------------
    // Core / Materials / Simple Items
    // --------------------------------------------------------------------------------------------

    public static final DeferredItem<Item> BISMUTH                  = ITEMS.registerSimpleItem("bismuth");
    public static final DeferredItem<Item> RAW_BISMUTH              = ITEMS.registerSimpleItem("raw_bismuth");

    public static final DeferredItem<Item> FROSTFIRE_ICE            = ITEMS.registerSimpleItem("frostfire_ice");
    public static final DeferredItem<Item> STARLIGHT_ASHES          = ITEMS.registerSimpleItem("starlight_ashes");
    public static final DeferredItem<Item> BARNACLED_PEARL          = ITEMS.registerSimpleItem("barnacled_pearl");
    public static final DeferredItem<Item> SEISMIC_CORE_FRAGMENT    = ITEMS.registerSimpleItem("seismic_core_fragment");
    public static final DeferredItem<Item> SHATTERED_REALITY_OF_SHUDDE_MELL = ITEMS.registerSimpleItem("shattered_reality_of_shudde_mell");

    // --------------------------------------------------------------------------------------------
    // Tools / Weapons / Utility
    // --------------------------------------------------------------------------------------------

    public static final DeferredItem<Item> CHISEL = ITEMS.registerItem(
            "chisel",
            props -> new ChiselItem(props.durability(32))
    );

    public static final DeferredItem<Item> BROODING_FORK = ITEMS.registerItem(
            "brooding_fork",
            props -> new BroodingForkItem(props.stacksTo(1))
    );

    public static final DeferredItem<Item> DOOR_KEY = ITEMS.registerItem(
            "door_key",
            props -> new DoorKeyItem(props.stacksTo(1))
    );

    public static final DeferredItem<Item> AZATHOTS_HAMMER_OF_FINAL_VERDICT = ITEMS.registerSimpleItem("azathots_hammer_of_final_verdict");
    public static final DeferredItem<Item> FORGE_CALLERS_MAUL               = ITEMS.registerSimpleItem("forge_callers_maul");
    public static final DeferredItem<Item> HIGH_VELOCITY_ARROW              = ITEMS.registerSimpleItem("high_velocity_arrow");
    public static final DeferredItem<Item> TOTEM_OF_DOG_WHISPERING          = ITEMS.registerSimpleItem("totem_of_dog_whispering");
    public static final DeferredItem<Item> AEGIS_OF_ABSOLUTION              = ITEMS.registerSimpleItem("aegis_of_absolution");
    public static final DeferredItem<Item> AEGIS_OF_CHAOS                   = ITEMS.registerSimpleItem("aegis_of_chaos");

    // --------------------------------------------------------------------------------------------
    // Blocks as Items
    // --------------------------------------------------------------------------------------------

    public static final DeferredItem<BlockItem> PILE_OF_BOOKS =
            ITEMS.registerSimpleBlockItem("pile_of_books", ModBlocks.PILE_OF_BOOKS);

    // --------------------------------------------------------------------------------------------
    // Food
    // --------------------------------------------------------------------------------------------

    public static final DeferredItem<Item> RADISH = ITEMS.registerItem(
            "radish",
            // Example with modern tooltip override signature (1.21.8):
            props -> new Item(props.food(ModFoodProperties.RADISH)) {
                @Override
                public void appendHoverText(ItemStack stack,
                                            Item.TooltipContext context,
                                            TooltipDisplay display,
                                            Consumer<Component> tooltipConsumer,
                                            TooltipFlag tooltipFlag) {
                    tooltipConsumer.accept(Component.translatable("tooltip.cosmicdungeon.radish.tooltip"));
                }
            }
    );

    // ========================================================================
    //  J U D I C A T O R   I T E M S  —  CHAINMAIL VARIANT (1.21.8-style armor)
    // ========================================================================
    // Uses vanilla CHAINMAIL material with custom item names + magenta tooltip.
    // Non-craftable: no recipes are provided.
    //
    // Textures (inventory icons):
    //   assets/cosmicdungeon/textures/item/<lowercase_name>.png
    //     visor_of_first_oath.png
    //     hauberk_of_the_sworn_word.png
    //     chausses_of_the_first_vow.png
    //     sabatons_of_the_silent_promise.png
    //
    // Wear rendering:
    //   Uses the default CHAINMAIL equipment assets from ArmorMaterials.
    //   (If you want fully custom worn visuals, we can add an EquipmentClientInfo
    //    JSON and a custom ArmorMaterial asset key later.)

    // Helm — VISOR_OF_FIRST_OATH
    public static final DeferredItem<Item> VISOR_OF_FIRST_OATH =
            ITEMS.registerItem("visor_of_first_oath", props ->
                    new Item(props.humanoidArmor(JudicatorArmorMaterials.JUDICATOR_CHAINMAIL, ArmorType.HELMET)
                    ) {
                        @Override
                        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                                    Consumer<Component> tooltip, TooltipFlag flags) {
                            tooltip.accept(Component.literal("§dThe helm grants voice to the oath, spoken aloud for all to hear."));
                        }
                    });

    // Chestplate — HAUBERK_OF_THE_SWORN_WORD
    public static final DeferredItem<Item> HAUBERK_OF_THE_SWORN_WORD =
            ITEMS.registerItem("hauberk_of_the_sworn_word", props ->
                    new Item(props.humanoidArmor(JudicatorArmorMaterials.JUDICATOR_CHAINMAIL, ArmorType.CHESTPLATE)
                    ) {
                        @Override
                        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                                    Consumer<Component> tooltip, TooltipFlag flags) {
                            tooltip.accept(Component.literal("§dThe chest binds the sworn word to the heart, sealed within."));
                        }
                    });

    // Leggings — CHAUSSES_OF_THE_FIRST_VOW
    public static final DeferredItem<Item> CHAUSSES_OF_THE_FIRST_VOW =
            ITEMS.registerItem("chausses_of_the_first_vow", props ->
                    new Item(props.humanoidArmor(JudicatorArmorMaterials.JUDICATOR_CHAINMAIL, ArmorType.LEGGINGS)
                    ) {
                        @Override
                        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                                    Consumer<Component> tooltip, TooltipFlag flags) {
                            tooltip.accept(Component.literal("§dThe leggings carry the first vow, each step marking the path of service."));
                        }
                    });

    // Boots — SABATONS_OF_THE_SILENT_PROMISE
    public static final DeferredItem<Item> SABATONS_OF_THE_SILENT_PROMISE =
            ITEMS.registerItem("sabatons_of_the_silent_promise", props ->
                    new Item(props.humanoidArmor(JudicatorArmorMaterials.JUDICATOR_CHAINMAIL, ArmorType.BOOTS)
                    ) {
                        @Override
                        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                                    Consumer<Component> tooltip, TooltipFlag flags) {
                            tooltip.accept(Component.literal("§dThe boots bear the silent promise, unspoken yet unbroken."));
                        }
                    });



    // --------------------------------------------------------------------------------------------
    // Registration hook
    // --------------------------------------------------------------------------------------------

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
