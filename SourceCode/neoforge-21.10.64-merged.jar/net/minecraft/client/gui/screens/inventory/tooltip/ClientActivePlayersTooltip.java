package net.minecraft.client.gui.screens.inventory.tooltip;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientActivePlayersTooltip implements ClientTooltipComponent {
    private static final int SKIN_SIZE = 10;
    private static final int PADDING = 2;
    private final List<PlayerSkinRenderCache.RenderInfo> activePlayers;

    public ClientActivePlayersTooltip(ClientActivePlayersTooltip.ActivePlayersTooltip tooltip) {
        this.activePlayers = tooltip.profiles();
    }

    @Override
    public int getHeight(Font font) {
        return this.activePlayers.size() * 12 + 2;
    }

    private static String getName(PlayerSkinRenderCache.RenderInfo renderInfo) {
        return renderInfo.gameProfile().name();
    }

    @Override
    public int getWidth(Font font) {
        int i = 0;

        for (PlayerSkinRenderCache.RenderInfo playerskinrendercache$renderinfo : this.activePlayers) {
            int j = font.width(getName(playerskinrendercache$renderinfo));
            if (j > i) {
                i = j;
            }
        }

        return i + 10 + 6;
    }

    @Override
    public void renderImage(Font font, int x, int y, int width, int height, GuiGraphics guiGraphics) {
        for (int i = 0; i < this.activePlayers.size(); i++) {
            PlayerSkinRenderCache.RenderInfo playerskinrendercache$renderinfo = this.activePlayers.get(i);
            int j = y + 2 + i * 12;
            PlayerFaceRenderer.draw(guiGraphics, playerskinrendercache$renderinfo.playerSkin(), x + 2, j, 10);
            guiGraphics.drawString(font, getName(playerskinrendercache$renderinfo), x + 10 + 4, j + 2, -1);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record ActivePlayersTooltip(List<PlayerSkinRenderCache.RenderInfo> profiles) implements TooltipComponent {
    }
}
