// file: src/main/java/net/goui/cosmicdungeon/playerclass/api/ClassClientEvents.java
package net.goui.cosmicdungeon.playerclass.api;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** Rehydrate the current client player from the existing authoritative class sync. */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID, value = Dist.CLIENT)
public final class ClassClientEvents {
    private ClassClientEvents() {}

    @SubscribeEvent
    public static void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        ClassNet.requestClassSync();
    }

    @SubscribeEvent
    public static void onClientClone(ClientPlayerNetworkEvent.Clone event) {
        // Dimension travel and respawn replace LocalPlayer without copying its persistent class NBT.
        // This hook runs after Minecraft.player is replaced, so the reply reaches the new instance.
        // Do not copy stale client state or request again on every inventory frame.
        ClassNet.requestClassSync();
    }
}
