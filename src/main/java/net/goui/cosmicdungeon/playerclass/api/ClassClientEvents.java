// file: src/main/java/net/goui/cosmicdungeon/playerclass/api/ClassClientEvents.java
package net.goui.cosmicdungeon.playerclass.api;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * Client-only hooks related to the player class system.
 * On login, asks the server for our current class + extra NBT,
 * which comes back as S2C_ClassSync and re-initializes Metalmancer HUD,
 * extra inventory, etc.
 */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID, value = Dist.CLIENT)
public final class ClassClientEvents {
    private ClassClientEvents() {}

    @SubscribeEvent
    public static void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        // Ask the server "what class am I + what's my extra inventory?"
        ClassNet.requestClassSync();
    }
}
