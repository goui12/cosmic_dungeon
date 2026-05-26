package net.goui.cosmicdungeon.client.screen;

import net.goui.cosmicdungeon.network.ModNetwork;
import net.goui.cosmicdungeon.network.TradePayloads;
import net.goui.cosmicdungeon.trade.TradeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class TradeScreen extends AbstractContainerScreen<TradeMenu> {
    private EditBox currency;
    public TradeScreen(TradeMenu menu, Inventory inv, Component title) { super(menu, inv, title); this.imageWidth=194; this.imageHeight=176; }
    @Override protected void init(){ super.init();
        currency = new EditBox(font, leftPos+10, topPos+52, 80, 18, Component.literal("Trace"));
        currency.setValue("0"); currency.setResponder(s -> { try{ ModNetwork.sendToServer(new TradePayloads.C2S_UpdateCurrencyOffer(Long.parseLong(s.isBlank()?"0":s))); } catch(Exception ignored){} });
        addRenderableWidget(currency);
        addRenderableWidget(Button.builder(Component.literal("Ready"), b->ModNetwork.sendToServer(new TradePayloads.C2S_Ready(true))).bounds(leftPos+100, topPos+50, 50, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Unready"), b->ModNetwork.sendToServer(new TradePayloads.C2S_Ready(false))).bounds(leftPos+152, topPos+50, 50, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Confirm"), b->ModNetwork.sendToServer(new TradePayloads.C2S_Confirm(true))).bounds(leftPos+100, topPos+74, 102, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b->ModNetwork.sendToServer(new TradePayloads.C2S_Cancel())).bounds(leftPos+100, topPos+98, 102, 20).build());
    }
    @Override protected void renderBg(GuiGraphics g, float pt, int mx, int my) {}
    @Override public void render(GuiGraphics g, int mx, int my, float pt){ renderBackground(g,mx,my,pt); super.render(g,mx,my,pt); g.drawString(font, "Your offer", leftPos+10, topPos+8, 0xffffff); g.drawString(font, "Partner offer", leftPos+10, topPos+32, 0xcccccc); renderTooltip(g,mx,my); }
}
