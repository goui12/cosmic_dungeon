package net.goui.cosmicdungeon.playerclass.metalmancer;

import net.goui.cosmicdungeon.item.ModItems;
import net.goui.cosmicdungeon.playerclass.metalmancer.item.MetalmancerSummoningStaffItem;
import net.goui.cosmicdungeon.playerclass.ore.SatchelOfSamplesItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Compatibility wrapper: Metalmancer items now live in {@link ModItems} to keep ONE Items registry.
 */
public final class MetalmancerItems {
    private MetalmancerItems() {}

    public static final DeferredItem<SatchelOfSamplesItem> SATCHEL_OF_SAMPLES =
            ModItems.SATCHEL_OF_SAMPLES;

    public static final DeferredItem<MetalmancerSummoningStaffItem> BENT_ROD_OF_MELTED_SHAVINGS =
            ModItems.BENT_ROD_OF_MELTED_SHAVINGS;

    public static final DeferredItem<MetalmancerSummoningStaffItem> ERZFUEHLER =
            ModItems.ERZFUEHLER;

    /** No-op: registrations are centralized in ModItems now. */
    public static void register(IEventBus bus) {
        // intentionally empty
    }
}
