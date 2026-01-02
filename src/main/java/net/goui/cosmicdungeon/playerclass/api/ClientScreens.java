// file: src/main/java/net/goui/cosmicdungeon/playerclass/api/ClientScreens.java
package net.goui.cosmicdungeon.playerclass.api;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID, value = Dist.CLIENT)
public final class ClientScreens {
    private ClientScreens() {}

    @SubscribeEvent
    public static void onOpen(ScreenEvent.Opening e) {
        Screen s = e.getScreen();
        if (!(s instanceof InventoryScreen)) return;

        var mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null) return;

        // Only override vanilla inventory if the player is currently Metalmancer
        if (!ClassNbtUtil.isMetalmancer(p)) return;

        // Cancel vanilla inv and ask the server to open our menu
        e.setCanceled(true);
        ClassNet.requestOpenMetalmancerInventory();
    }
}
