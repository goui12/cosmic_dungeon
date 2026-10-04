package net.goui.cosmicdungeon.client;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.dungeon.DungeonInstanceSlots;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** The same server-validated vote remains usable without leaving the death screen. */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID, value = Dist.CLIENT)
public final class DungeonDeathScreen {
    private DungeonDeathScreen() {}

    @SubscribeEvent public static void init(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof DeathScreen death)) return;
        var client = Minecraft.getInstance();
        if (client.player == null || client.getConnection() == null
                || DungeonInstanceSlots.slotOf(client.player.level().dimension()).isEmpty()) return;
        event.addListener(Button.builder(Component.literal("Forfeit"), button -> {
            client.setScreen(new ConfirmScreen(yes -> {
                client.setScreen(death);
                if (yes && client.player != null && client.player.isDeadOrDying()
                        && client.getConnection() != null) {
                    // Ordered commands reuse membership, expiry, threshold and duplicate-vote guards.
                    client.getConnection().sendCommand("ff");
                    client.getConnection().sendCommand("ff yes");
                }
            }, Component.literal("Vote to forfeit?"),
                    Component.literal("Start or vote Yes on the current group forfeit. If two-thirds agree, "
                            + "everyone returns alive with their saved outside belongings. Dungeon loot is lost."),
                    Component.literal("Yes, forfeit"), Component.literal("Back")) {
                @Override public boolean isPauseScreen() { return false; }
                @Override public boolean shouldCloseOnEsc() { return false; }
                @Override public void tick() {
                    super.tick();
                    if (client.player != null && !client.player.isDeadOrDying()) client.setScreen(null);
                }
            });
        }).bounds(death.width / 2 - 100, death.height / 4 + 120, 200, 20).build());
    }
}
