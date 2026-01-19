// file: src/main/java/net/goui/cosmicdungeon/block/custom/ClassSelectorReadyTickEvents.java
package net.goui.cosmicdungeon.block.custom;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class ClassSelectorReadyTickEvents {
    private ClassSelectorReadyTickEvents() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        if (!e.hasTime()) return; // keep timing consistent with server tick gating
        var server = e.getServer();
        if (server == null) return;
        ClassSelectorReadyManager.tick(server);
    }
}
