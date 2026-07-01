package net.goui.cosmicdungeon.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public final class TradeLookPromptOverlay {

    private TradeLookPromptOverlay() {}

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null || minecraft.level == null) {
            return;
        }
        if (TradePromptClientState.shouldHidePrompt()) {
            return;
        }
        if (minecraft.screen != null || minecraft.getDebugOverlay().showDebugScreen()) {
            return;
        }

        Player target = TradeRequestKeybindClient.getLookTradeTarget(minecraft);
        if (target == null) {
            return;
        }

        Component name = Component.translatable("hud.cosmicdungeon.trade.look_name", target.getName());
        Component prompt = Component.translatable("hud.cosmicdungeon.trade.press_key");

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int nameWidth = minecraft.font.width(name);
        int promptWidth = minecraft.font.width(prompt);
        int boxWidth = Math.max(nameWidth, promptWidth) + 12;
        int boxHeight = 24;
        int boxX = (screenWidth - boxWidth) / 2;
        int boxY = screenHeight - 70;

        event.getGuiGraphics().fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0x66000000);
        event.getGuiGraphics().drawString(minecraft.font, name, (screenWidth - nameWidth) / 2, boxY + 4, 0xFFFFFFFF, true);
        event.getGuiGraphics().drawString(minecraft.font, prompt, (screenWidth - promptWidth) / 2, boxY + 14, 0xFFFFE082, true);
    }
}
