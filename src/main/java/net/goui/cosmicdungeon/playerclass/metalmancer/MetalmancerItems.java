package net.goui.cosmicdungeon.playerclass.metalmancer;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.playerclass.metalmancer.item.MetalmancerSummoningStaffItem;
import net.goui.cosmicdungeon.playerclass.ore.SatchelOfSamplesItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MetalmancerItems {
    private MetalmancerItems() {}

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(CosmicDungeonMod.MOD_ID);

    // Satchel of Samples (ore storage)
    public static final DeferredItem<SatchelOfSamplesItem> SATCHEL_OF_SAMPLES =
            ITEMS.registerItem("satchel_of_samples", SatchelOfSamplesItem::new);

    // Bent Rod of Melted Shavings — D2 T1
    public static final DeferredItem<MetalmancerSummoningStaffItem> BENT_ROD_OF_MELTED_SHAVINGS =
            ITEMS.registerItem("bent_rod_of_melted_shavings",
                    props -> new MetalmancerSummoningStaffItem(props, 1));

    // Erzfühler (Ore-Feeler) — D2 T2
    public static final DeferredItem<MetalmancerSummoningStaffItem> ERZFUEHLER =
            ITEMS.registerItem("erzfuehler",
                    props -> new MetalmancerSummoningStaffItem(props, 2));

    public static void register(net.neoforged.bus.api.IEventBus bus) {
        ITEMS.register(bus);
    }
}
