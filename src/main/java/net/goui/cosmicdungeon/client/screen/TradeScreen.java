package net.goui.cosmicdungeon.client.screen;

import net.goui.cosmicdungeon.network.ModNetwork;
import net.goui.cosmicdungeon.network.TradePayloads;
import net.goui.cosmicdungeon.trade.TradeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class TradeScreen extends AbstractContainerScreen<TradeMenu> {
    private static final ResourceLocation TRADE_WINDOW = ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "textures/gui/Trade_Window.png");

    private EditBox currency;

    public TradeScreen(TradeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 256;
        this.imageHeight = 256;
    }

    @Override
    protected void init() {
        super.init();
        currency = new EditBox(font, leftPos + 56, topPos + 66, 80, 18, Component.literal("Trace"));
        currency.setValue("0");
        currency.setResponder(s -> {
            try {
                ModNetwork.sendToServer(new TradePayloads.C2S_UpdateCurrencyOffer(Long.parseLong(s.isBlank() ? "0" : s)));
            } catch (Exception ignored) {
            }
        });
        addRenderableWidget(currency);
        addRenderableWidget(Button.builder(Component.literal("Ready"), b -> ModNetwork.sendToServer(new TradePayloads.C2S_Ready(true))).bounds(leftPos + 146, topPos + 64, 50, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Unready"), b -> ModNetwork.sendToServer(new TradePayloads.C2S_Ready(false))).bounds(leftPos + 198, topPos + 64, 50, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Confirm"), b -> ModNetwork.sendToServer(new TradePayloads.C2S_Confirm(true))).bounds(leftPos + 146, topPos + 108, 50, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> ModNetwork.sendToServer(new TradePayloads.C2S_Cancel())).bounds(leftPos + 198, topPos + 108, 50, 20).build());
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        g.blit(RenderPipelines.GUI_TEXTURED, TRADE_WINDOW, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g, mx, my, pt);
        super.render(g, mx, my, pt);
        g.drawString(font, "Partner offer", leftPos + 56, topPos + 17, 0xcccccc, false);
        g.drawString(font, "Your offer", leftPos + 56, topPos + 78, 0xffffff, false);
        renderTooltip(g, mx, my);
    }
}
