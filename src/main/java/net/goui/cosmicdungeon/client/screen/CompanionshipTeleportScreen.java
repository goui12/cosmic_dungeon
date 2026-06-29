package net.goui.cosmicdungeon.client.screen;

import net.goui.cosmicdungeon.client.ModNetworkClient;
import net.goui.cosmicdungeon.network.CompanionshipTeleportPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class CompanionshipTeleportScreen extends Screen {
    private final List<CompanionshipTeleportPayloads.PlayerEntry> players;
    public CompanionshipTeleportScreen(List<CompanionshipTeleportPayloads.PlayerEntry> players) { super(Component.literal("Teleport to a Dungeoneer")); this.players = List.copyOf(players); }
    @Override protected void init() {
        int panelW = 230, x = (width - panelW) / 2, y = height / 2 - 78;
        addRenderableWidget(Button.builder(Component.literal("Select a Player… ▾"), b -> {}).bounds(x + 25, y + 34, 180, 20).build()).active = false;
        int rowY = y + 58;
        for (var entry : players) {
            addRenderableWidget(Button.builder(Component.literal(entry.name()), b -> { ModNetworkClient.sendToServer(new CompanionshipTeleportPayloads.C2S_Select(entry.playerId())); onClose(); }).bounds(x + 25, rowY, 180, 20).build());
            rowY += 22;
        }
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose()).bounds(x + 65, rowY + 8, 100, 20).build());
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        int panelW = 230, panelH = 78 + players.size() * 22 + 38, x = (width - panelW) / 2, y = height / 2 - 78;
        g.fill(x, y, x + panelW, y + panelH, 0xE0101018); g.renderOutline(x, y, panelW, panelH, 0xFFB0BEC5);
        g.drawCenteredString(font, "Teleport to a Dungeoneer:", width / 2, y + 14, 0xFFF8BBD0);
        super.render(g, mouseX, mouseY, partialTick);
    }
    @Override public boolean isPauseScreen() { return false; }
}
