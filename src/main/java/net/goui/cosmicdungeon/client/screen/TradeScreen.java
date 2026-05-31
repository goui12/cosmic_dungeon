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

import java.util.UUID;

public class TradeScreen extends AbstractContainerScreen<TradeMenu> {
    private static final ResourceLocation TRADE_WINDOW = ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "textures/gui/trade_window.png");

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

        TradeClientState.TradeView view = TradeClientState.currentFor(menu.containerId);
        String otherName = view == null || view.otherName().isBlank() ? "Partner" : view.otherName();
        String selfName = view == null || view.selfName().isBlank() ? "You" : view.selfName();
        long otherBalance = view == null ? 0L : view.otherBalanceTrace();
        long selfBalance = view == null ? 0L : view.selfBalanceTrace();
        long otherOffered = view == null ? 0L : view.otherOfferedTrace();
        long selfOffered = view == null ? 0L : view.selfOfferedTrace();

        g.drawString(font, "Trading with: " + otherName, leftPos + 8, topPos + 6, 0xffffff, false);
        g.drawString(font, otherName + " offer", leftPos + 56, topPos + 17, 0xcccccc, false);
        g.drawString(font, "Balance: " + otherBalance + " Trace", leftPos + 56, topPos + 47, 0xaaaaaa, false);
        g.drawString(font, "Offering: " + otherOffered + " Trace", leftPos + 56, topPos + 57, 0xaaaaaa, false);
        g.drawString(font, selfName + " offer", leftPos + 56, topPos + 78, 0xffffff, false);
        g.drawString(font, "Balance: " + selfBalance + " Trace", leftPos + 56, topPos + 108, 0xffffff, false);
        g.drawString(font, "Offering: " + selfOffered + " Trace", leftPos + 56, topPos + 118, 0xffffff, false);

        if (view != null) {
            g.drawString(font, "You: " + tradeStatus(view.selfReady(), view.selfConfirmed()), leftPos + 146, topPos + 88, 0xffffff, false);
            g.drawString(font, otherName + ": " + tradeStatus(view.otherReady(), view.otherConfirmed()), leftPos + 146, topPos + 98, 0xcccccc, false);
            if (!view.statusMessage().isBlank()) {
                g.drawString(font, view.statusMessage(), leftPos + 8, topPos + 238, 0xffdd66, false);
            }
        }

        renderTooltip(g, mx, my);
    }

    private static String tradeStatus(boolean ready, boolean confirmed) {
        if (confirmed) return "Confirmed";
        if (ready) return "Ready";
        return "Not ready";
    }

    public static final class TradeClientState {
        private static TradeView current;

        private TradeClientState() {}

        public static void set(TradeView view) {
            current = view;
        }

        public static TradeView current() {
            return current;
        }

        public static TradeView currentFor(int containerId) {
            return current != null && current.containerId() == containerId ? current : null;
        }

        public record TradeView(
                int containerId,
                UUID sessionId,
                String selfName,
                String otherName,
                long selfBalanceTrace,
                long otherBalanceTrace,
                long selfOfferedTrace,
                long otherOfferedTrace,
                boolean selfReady,
                boolean otherReady,
                boolean selfConfirmed,
                boolean otherConfirmed,
                String statusMessage
        ) {}
    }
}
