package net.goui.cosmicdungeon.item;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.item.custom.*;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.Block;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(CosmicDungeonMod.MOD_ID);

    // === Helpers ===
    public static <T extends Block> DeferredItem<Item> registerColoredBlockItem(String name, Supplier<T> block) {
        return ITEMS.registerItem(name, props -> new ColoredBlockItem(block.get(), props));
    }

    // === Core / Materials / Simple Items ===
    public static final DeferredItem<Item> BISMUTH               = ITEMS.registerSimpleItem("bismuth");
    public static final DeferredItem<Item> RAW_BISMUTH           = ITEMS.registerSimpleItem("raw_bismuth");

    public static final DeferredItem<Item> FROSTFIRE_ICE         = ITEMS.registerSimpleItem("frostfire_ice");
    public static final DeferredItem<Item> STARLIGHT_ASHES       = ITEMS.registerSimpleItem("starlight_ashes");
    public static final DeferredItem<Item> BARNACLED_PEARL       = ITEMS.registerSimpleItem("barnacled_pearl");
    public static final DeferredItem<Item> SEISMIC_CORE_FRAGMENT = ITEMS.registerSimpleItem("seismic_core_fragment");
    public static final DeferredItem<Item> SHATTERED_REALITY_OF_SHUDDE_MELL =
            ITEMS.registerSimpleItem("shattered_reality_of_shudde_mell");

    //REGION

    public static final DeferredItem<Item> REGION_WAND = ITEMS.registerItem(
            "region_wand", props -> new RegionWandItem(props.stacksTo(1))
                    );


    // === Tools / Weapons / Utility (non-Judicator) ===
    public static final DeferredItem<Item> CHISEL = ITEMS.registerItem(
            "chisel", props -> new ChiselItem(props.durability(32))
    );
    public static final DeferredItem<Item> BROODING_FORK = ITEMS.registerItem(
            "brooding_fork", props -> new BroodingForkItem(props.stacksTo(1))
    );
    public static final DeferredItem<Item> DOOR_KEY = ITEMS.registerItem(
            "door_key", props -> new DoorKeyItem(props.stacksTo(1))
    );
    public static final DeferredItem<Item> AZATHOTS_HAMMER_OF_FINAL_VERDICT = ITEMS.registerSimpleItem("azathots_hammer_of_final_verdict");
    public static final DeferredItem<Item> FORGE_CALLERS_MAUL               = ITEMS.registerSimpleItem("forge_callers_maul");
    public static final DeferredItem<Item> HIGH_VELOCITY_ARROW              = ITEMS.registerSimpleItem("high_velocity_arrow");
    public static final DeferredItem<Item> TOTEM_OF_DOG_WHISPERING          = ITEMS.registerSimpleItem("totem_of_dog_whispering");
    public static final DeferredItem<Item> AEGIS_OF_ABSOLUTION              = ITEMS.registerSimpleItem("aegis_of_absolution");
    public static final DeferredItem<Item> AEGIS_OF_CHAOS                   = ITEMS.registerSimpleItem("aegis_of_chaos");

    // === Blocks as Items ===
    public static final DeferredItem<BlockItem> PILE_OF_BOOKS =
            ITEMS.registerSimpleBlockItem("pile_of_books", ModBlocks.PILE_OF_BOOKS);

    // === Food ===
    public static final DeferredItem<Item> RADISH = ITEMS.registerItem(
            "radish",
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

    // ============================================================================================
    // JUDICATOR ITEMS — BY DUNGEON & TIER
    // ============================================================================================

    // D1 T3 (Diamond)
    public static final DeferredItem<Item> EDICT_OF_SILENCE =
            ITEMS.registerItem("edict_of_silence", p -> new MaceItem(
                    p.stacksTo(1).durability(1561)
                            .component(DataComponents.TOOL, MaceItem.createToolProperties())
                            .attributes(vanillaMaceAttrs())
            ));
    public static final DeferredItem<Item> VOWKEEPER =
            ITEMS.registerItem("vowkeeper", p -> new SimpleShieldItem(
                    p.stacksTo(1).durability(336)
                            // keep vanilla-like shield mechanics:
                            .component(DataComponents.BLOCKS_ATTACKS, new BlocksAttacks(
                                    0.05F, 1.0F,
                                    java.util.List.of(new BlocksAttacks.DamageReduction(120.0F, java.util.Optional.empty(), 0.0F, 0.60F)),
                                    new BlocksAttacks.ItemDamageFunction(2.0F, 0.0F, 0.50F),
                                    java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty()
                            ))));
    public static final DeferredItem<Item> VIELPIERCER =
            ITEMS.registerItem("vielpiercer", SpectralArrowItem::new);
    public static final DeferredItem<Item> SCINTILLA_VITALIS =
            ITEMS.registerItem("scintilla_vitalis", TippedArrowItem::new);
    public static final DeferredItem<Item> GUSTING_BOLT =
            ITEMS.registerItem("gusting_bolt", ArrowItem::new);
    public static final DeferredItem<Item> SHARD_OF_THE_MAD_STRIDER =
            ITEMS.registerItem("shard_of_the_mad_strider", TippedArrowItem::new);
    public static final DeferredItem<Item> GONFALON_OF_JUDIFICATION =
            ITEMS.registerSimpleItem("gonfalon_of_judification");

    // D1 T4 (Netherite)
    public static final DeferredItem<Item> EDICT_OF_RUIN =
            ITEMS.registerItem("edict_of_ruin", p -> new MaceItem(
                    p.stacksTo(1).durability(1561)
                            .component(DataComponents.TOOL, MaceItem.createToolProperties())
                            .attributes(vanillaMaceAttrs())
            ));

    public static final DeferredItem<Item> AEGIS_OF_TRUTH =
            ITEMS.registerItem("aegis_of_truth", p -> new SimpleShieldItem(
                    p.stacksTo(1).durability(336)
                            // keep vanilla-like shield mechanics:
                            .component(DataComponents.BLOCKS_ATTACKS, new BlocksAttacks(
                                    0.05F, 1.0F,
                                    java.util.List.of(new BlocksAttacks.DamageReduction(120.0F, java.util.Optional.empty(), 0.0F, 0.60F)),
                                    new BlocksAttacks.ItemDamageFunction(2.0F, 0.0F, 0.50F),
                                    java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty()
                            ))
            ));
    public static final DeferredItem<Item> LUX_VITALIS =
            ITEMS.registerItem("lux_vitalis", TippedArrowItem::new);
    public static final DeferredItem<Item> EBONSIGHT =
            ITEMS.registerItem("ebonsight", TippedArrowItem::new);
    public static final DeferredItem<Item> FANG_OF_THE_BRUTE =
            ITEMS.registerItem("fang_of_the_brute", TippedArrowItem::new);
    public static final DeferredItem<Item> ENSIGN_OF_THE_JUDICATOR =
            ITEMS.registerSimpleItem("ensign_of_the_judicator");

    // D2 T1 (Rustic Chainmail — custom material)
    public static final DeferredItem<Item> VISOR_OF_THE_RESOLUTE =
            ITEMS.registerItem("visor_of_the_resolute", p -> new Item(p.humanoidArmor(JudicatorArmorMaterials.JUDICATOR_CHAINMAIL, ArmorType.HELMET)));
    public static final DeferredItem<Item> CUIRASS_OF_PURPOSE =
            ITEMS.registerItem("cuirass_of_purpose", p -> new Item(p.humanoidArmor(JudicatorArmorMaterials.JUDICATOR_CHAINMAIL, ArmorType.CHESTPLATE)));
    public static final DeferredItem<Item> CHAUSSES_OF_THE_PLEDGE =
            ITEMS.registerItem("chausses_of_the_pledge", p -> new Item(p.humanoidArmor(JudicatorArmorMaterials.JUDICATOR_CHAINMAIL, ArmorType.LEGGINGS)));
    public static final DeferredItem<Item> SABATONS_OF_THE_UNHEARD_OATH =
            ITEMS.registerItem("sabatons_of_the_unheard_oath", p -> new Item(p.humanoidArmor(JudicatorArmorMaterials.JUDICATOR_CHAINMAIL, ArmorType.BOOTS)));

    public static final DeferredItem<Item> EMPOWERED_HAMMER =
            ITEMS.registerItem("empowered_hammer", p -> new MaceItem(
                    p.stacksTo(1)
                            .durability(1561)
                            // give it “is a mace” behavior
                            .component(DataComponents.TOOL, MaceItem.createToolProperties())
                            // vanilla attack damage/speed
                            .attributes(vanillaMaceAttrs())
            ));

    public static final DeferredItem<Item> WORKED_PLANK =
            ITEMS.registerItem("worked_plank", p -> new SimpleShieldItem(
                    p.stacksTo(1).durability(336)
                            // keep vanilla-like shield mechanics:
                            .component(DataComponents.BLOCKS_ATTACKS, new BlocksAttacks(
                                    0.05F, 1.0F,
                                    java.util.List.of(new BlocksAttacks.DamageReduction(120.0F, java.util.Optional.empty(), 0.0F, 0.60F)),
                                    new BlocksAttacks.ItemDamageFunction(2.0F, 0.0F, 0.50F),
                                    java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty()
                            ))));
    public static final DeferredItem<Item> ARROW_OF_WRIT =
            ITEMS.registerItem("arrow_of_writ", ArrowItem::new);
    public static final DeferredItem<Item> STANDARD_OF_THE_INITIATE_JUDGE =
            ITEMS.registerSimpleItem("standard_of_the_initiate_judge");

    // D2 T2 (Rustic Iron — custom material)
    public static final DeferredItem<Item> OATHBOUND_VISOR =
            ITEMS.registerItem("oathbound_visor", p -> new Item(p.humanoidArmor(JudicatorArmorMaterials.JUDICATOR_RUSTIC_IRON, ArmorType.HELMET)));
    public static final DeferredItem<Item> CUIRASS_OF_RESOLUTION =
            ITEMS.registerItem("cuirass_of_resolution", p -> new Item(p.humanoidArmor(JudicatorArmorMaterials.JUDICATOR_RUSTIC_IRON, ArmorType.CHESTPLATE)));
    public static final DeferredItem<Item> GREAVES_OF_BINDING =
            ITEMS.registerItem("greaves_of_binding", p -> new Item(p.humanoidArmor(JudicatorArmorMaterials.JUDICATOR_RUSTIC_IRON, ArmorType.LEGGINGS)));
    public static final DeferredItem<Item> SABATONS_OF_THE_PACT =
            ITEMS.registerItem("sabatons_of_the_pact", p -> new Item(p.humanoidArmor(JudicatorArmorMaterials.JUDICATOR_RUSTIC_IRON, ArmorType.BOOTS)));

    public static final DeferredItem<Item> REINFORCED_HAMMER =
            ITEMS.registerItem("reinforced_hammer", p -> new MaceItem(
                    p.stacksTo(1).durability(1561)
                            .component(DataComponents.TOOL, MaceItem.createToolProperties())
                            .attributes(vanillaMaceAttrs())
            ));
    public static final DeferredItem<Item> REINFORCED_IRON_SLAB =
            ITEMS.registerItem("reinforced_iron_slab", p -> new SimpleShieldItem(
                    p.stacksTo(1).durability(336)
                            // keep vanilla-like shield mechanics:
                            .component(DataComponents.BLOCKS_ATTACKS, new BlocksAttacks(
                                    0.05F, 1.0F,
                                    java.util.List.of(new BlocksAttacks.DamageReduction(120.0F, java.util.Optional.empty(), 0.0F, 0.60F)),
                                    new BlocksAttacks.ItemDamageFunction(2.0F, 0.0F, 0.50F),
                                    java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty()
                            ))));
    public static final DeferredItem<Item> STANDARD_OF_THE_NASCENT_JUDGE =
            ITEMS.registerSimpleItem("standard_of_the_nascent_judge");

    // D3 T3 (Ocean Templar Diamond — custom material)
    public static final DeferredItem<Item> VISOR_OF_THE_COVENANT =
            ITEMS.registerItem("visor_of_the_covenant", p -> new Item(p.humanoidArmor(JudicatorArmorMaterials.JUDICATOR_OCEAN_TEMPLAR_DIAMOND, ArmorType.HELMET)));
    public static final DeferredItem<Item> CUIRASS_OF_DEVOTION =
            ITEMS.registerItem("cuirass_of_devotion", p -> new Item(p.humanoidArmor(JudicatorArmorMaterials.JUDICATOR_OCEAN_TEMPLAR_DIAMOND, ArmorType.CHESTPLATE)));
    public static final DeferredItem<Item> GREAVES_OF_THE_TRIBUNAL_PATH =
            ITEMS.registerItem("greaves_of_the_tribunal_path", p -> new Item(p.humanoidArmor(JudicatorArmorMaterials.JUDICATOR_OCEAN_TEMPLAR_DIAMOND, ArmorType.LEGGINGS)));
    public static final DeferredItem<Item> SABATONS_OF_PURSUIT =
            ITEMS.registerItem("sabatons_of_pursuit", p -> new Item(p.humanoidArmor(JudicatorArmorMaterials.JUDICATOR_OCEAN_TEMPLAR_DIAMOND, ArmorType.BOOTS)));

    public static final DeferredItem<Item> TIDAL_MACE =
            ITEMS.registerItem("tidal_mace", p -> new MaceItem(
                    p.stacksTo(1).durability(1561)
                            .component(DataComponents.TOOL, MaceItem.createToolProperties())
                            .attributes(vanillaMaceAttrs())
            ));

    public static final DeferredItem<Item> SHIELD_OF_TIDAL_FORCE =
            ITEMS.registerItem("shield_of_tidal_force", p -> new SimpleShieldItem(
                    p.stacksTo(1).durability(336)
                            // keep vanilla-like shield mechanics:
                            .component(DataComponents.BLOCKS_ATTACKS, new BlocksAttacks(
                                    0.05F, 1.0F,
                                    java.util.List.of(new BlocksAttacks.DamageReduction(120.0F, java.util.Optional.empty(), 0.0F, 0.60F)),
                                    new BlocksAttacks.ItemDamageFunction(2.0F, 0.0F, 0.50F),
                                    java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty()
                            ))));
    public static final DeferredItem<Item> STANDARD_OF_THE_ABYSSAL_JUDGE =
            ITEMS.registerSimpleItem("standard_of_the_abyssal_judge");

    // D3 T4 (Ocean Templar Netherite — custom material)
    public static final DeferredItem<Item> VISOR_OF_IMMUTABLE_WILL =
            ITEMS.registerItem("visor_of_immutable_will", p -> new Item(p.humanoidArmor(JudicatorArmorMaterials.JUDICATOR_OCEAN_TEMPLAR_NETHERITE, ArmorType.HELMET)));
    public static final DeferredItem<Item> CUIRASS_OF_CONVICTION =
            ITEMS.registerItem("cuirass_of_conviction", p -> new Item(p.humanoidArmor(JudicatorArmorMaterials.JUDICATOR_OCEAN_TEMPLAR_NETHERITE, ArmorType.CHESTPLATE)));
    public static final DeferredItem<Item> GREAVES_OF_THE_ETERNAL_MARCH =
            ITEMS.registerItem("greaves_of_the_eternal_march", p -> new Item(p.humanoidArmor(JudicatorArmorMaterials.JUDICATOR_OCEAN_TEMPLAR_NETHERITE, ArmorType.LEGGINGS)));
    public static final DeferredItem<Item> SABATONS_OF_BOUNDLESS_STEPS =
            ITEMS.registerItem("sabatons_of_boundless_steps", p -> new Item(p.humanoidArmor(JudicatorArmorMaterials.JUDICATOR_OCEAN_TEMPLAR_NETHERITE, ArmorType.BOOTS)));

    public static final DeferredItem<Item> ABYSSAL_MACE =
            ITEMS.registerItem("abyssal_mace", p -> new MaceItem(
                    p.stacksTo(1).durability(1561)
                            .component(DataComponents.TOOL, MaceItem.createToolProperties())
                            .attributes(vanillaMaceAttrs())
            ));

    public static final DeferredItem<Item> SHIELD_OF_THE_DEEP =
            ITEMS.registerItem("shield_of_the_deep", p -> new SimpleShieldItem(
                    p.stacksTo(1).durability(336)
                            // keep vanilla-like shield mechanics:
                            .component(DataComponents.BLOCKS_ATTACKS, new BlocksAttacks(
                                    0.05F, 1.0F,
                                    java.util.List.of(new BlocksAttacks.DamageReduction(120.0F, java.util.Optional.empty(), 0.0F, 0.60F)),
                                    new BlocksAttacks.ItemDamageFunction(2.0F, 0.0F, 0.50F),
                                    java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty()
                            ))));



    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    private static ItemAttributeModifiers vanillaMaceAttrs() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 5.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -3.4, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }


}
