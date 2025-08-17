package net.goui.cosmicdungeon.item;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.item.custom.BroodingForkItem;
import net.goui.cosmicdungeon.item.custom.ChiselItem;
import net.goui.cosmicdungeon.item.custom.ColoredBlockItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(CosmicDungeonMod.MOD_ID);

    // ===== Helper for ColoredBlockItem (pre-ID'd props) =====
    public static <T extends Block> DeferredItem<Item> registerColoredBlockItem(String name, Supplier<T> block) {
        return ITEMS.registerItem(name, props -> new ColoredBlockItem(block.get(), props));
    }

    public static final DeferredItem<Item> BISMUTH = ITEMS.registerSimpleItem("bismuth");
    public static final DeferredItem<Item> RAW_BISMUTH = ITEMS.registerSimpleItem("raw_bismuth");
    public static final DeferredItem<Item> BARNACLED_PEARL = ITEMS.registerSimpleItem("barnacled_pearl");
    public static final DeferredItem<Item> SEISMIC_CORE_FRAGMENT = ITEMS.registerSimpleItem("seismic_core_fragment");
    public static final DeferredItem<Item> SHATTERED_REALITY_OF_SHUDDE_MELL = ITEMS.registerSimpleItem("shattered_reality_of_shudde_mell");
    public static final DeferredItem<Item> FROSTFIRE_ICE = ITEMS.registerSimpleItem("frostfire_ice");
    public static final DeferredItem<Item> STARLIGHT_ASHES = ITEMS.registerSimpleItem("starlight_ashes");

    public static final DeferredItem<Item> CHISEL = ITEMS.registerItem("chisel",
            props -> new ChiselItem(props.durability(32)));

    public static final DeferredItem<Item> BROODING_FORK = ITEMS.registerItem("brooding_fork",
            props -> new BroodingForkItem(props.stacksTo(1)));

    public static final DeferredItem<Item> RADISH = ITEMS.registerItem("radish",
            props -> new Item(props.food(ModFoodProperties.RADISH)) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                            Consumer<Component> tooltipConsumer, TooltipFlag tooltipFlag) {
                    tooltipConsumer.accept(Component.translatable("tooltip.cosmicdungeon.radish.tooltip"));
                }
            });

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
