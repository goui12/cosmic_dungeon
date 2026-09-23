package net.goui.cosmicdungeon.crafting;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import java.util.List;
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class CraftingEvents {
    private CraftingEvents() {}
    @SubscribeEvent public static void started(ServerStartedEvent event) {
        CraftingPolicy.install(CraftingConfig.snapshot());
        CraftingPolicy.validate(event.getServer());
    }
    @SubscribeEvent public static void reloaded(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) CraftingPolicy.validate(event.getPlayerList().getServer());
        event.getRelevantPlayers().forEach(CraftingPolicy::refreshPlayer);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        CraftingPolicy.install(CraftingRules.parse(true, CraftingRules.DEFAULT_PLAYERS, List.of()));
    }
}
