// file: src/main/java/net/goui/cosmicdungeon/region/client/RegionLookClientTickEvents.java
package net.goui.cosmicdungeon.region.client;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID, value = Dist.CLIENT)
public final class RegionLookClientTickEvents {
    private RegionLookClientTickEvents() {}

    private static int cooldown = 0;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!RegionLookClient.isAllEnabled()) return;

        if (cooldown-- > 0) return;
        cooldown = 40; // every 2 seconds

        RegionLookClient.requestAllRefreshIfEnabled();
    }
}
