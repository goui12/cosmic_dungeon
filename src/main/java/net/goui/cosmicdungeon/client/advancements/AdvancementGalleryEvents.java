package net.goui.cosmicdungeon.client.advancements;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID, value = Dist.CLIENT)
public final class AdvancementGalleryEvents {
    private AdvancementGalleryEvents() {}

    @SubscribeEvent
    public static void opening(ScreenEvent.Opening event) {
        // Other mods' specialized advancement screens retain their own opening behavior.
        if (event.getNewScreen() == null || event.getNewScreen().getClass() != AdvancementsScreen.class) return;
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) event.setNewScreen(
                new CosmicAdvancementScreen(connection.getAdvancements(), event.getCurrentScreen()));
    }
}
