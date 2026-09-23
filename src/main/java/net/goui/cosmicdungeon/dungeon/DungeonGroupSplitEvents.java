package net.goui.cosmicdungeon.dungeon;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class DungeonGroupSplitEvents {
    private DungeonGroupSplitEvents() {}

    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        DungeonGroupSplitService.onMobKilled(event.getEntity());
    }
}
