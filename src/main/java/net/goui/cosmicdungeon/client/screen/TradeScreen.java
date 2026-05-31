package net.goui.cosmicdungeon.client.screen;

import net.goui.cosmicdungeon.economy.CurrencyAmount;
import net.goui.cosmicdungeon.economy.CurrencyDenomination;
import net.goui.cosmicdungeon.item.ModItems;
import net.goui.cosmicdungeon.network.ModNetwork;
import net.goui.cosmicdungeon.network.TradePayloads;
import net.goui.cosmicdungeon.trade.TradeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class TradeScreen extends AbstractContainerScreen<TradeMenu> {
    private static final ResourceLocation TRADE_WINDOW = ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "textures/gui/trade_window.png");
    private static final int ICON_SIZE = 16;
    private static final int ICON_STEP = 18;
    private static final CurrencyIcon[] CURRENCY_ICONS = new CurrencyIcon[] {
            new CurrencyIcon(CurrencyDenomination.ANCHOR, ModItems.ATTUNEMENT_ANCHOR),
            new CurrencyIcon(CurrencyDenomination.CROWN, ModItems.ATTUNEMENT_CROWN),
            new CurrencyIcon(CurrencyDenomination.SEAL, ModItems.ATTUNEMENT_SEAL),
            new CurrencyIcon(CurrencyDenomination.MARK, ModItems.ATTUNEMENT_MARK),
            new CurrencyIcon(CurrencyDenomination.TRACE, ModItems.ATTUNEMENT_TRACE)
    };

    public TradeScreen(TradeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 256;
        this.imageHeight = 256;
    }

    @Override
    protected void init() {
        super.init();
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
        renderCurrencyStacks(g, otherBalance, leftPos + 8, topPos + 17, false, mx, my);
        g.drawString(font, otherName + " offer", leftPos + 56, topPos + 17, 0xcccccc, false);
        renderOfferedCurrency(g, otherOffered, leftPos + 56, topPos + 47, mx, my);

        g.drawString(font, selfName, leftPos + 8, topPos + 65, 0xffffff, false);
        renderCurrencyStacks(g, selfBalance, leftPos + 8, topPos + 76, true, mx, my);
        g.drawString(font, selfName + " offer", leftPos + 56, topPos + 78, 0xffffff, false);
        renderOfferedCurrency(g, selfOffered, leftPos + 56, topPos + 108, mx, my);

        if (view != null) {
            g.drawString(font, "You: " + tradeStatus(view.selfReady(), view.selfConfirmed()), leftPos + 146, topPos + 88, 0xffffff, false);
            g.drawString(font, otherName + ": " + tradeStatus(view.otherReady(), view.otherConfirmed()), leftPos + 146, topPos + 98, 0xcccccc, false);
            if (!view.statusMessage().isBlank()) {
                g.drawString(font, view.statusMessage(), leftPos + 8, topPos + 238, 0xffdd66, false);
            }
        }

        renderTooltip(g, mx, my);
        renderCurrencyTooltips(g, mx, my, selfBalance, otherOffered, selfOffered);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int button = event.button();
        if ((button == 0 || button == 1) && handleOwnCurrencyClick((int) event.x(), (int) event.y(), button)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean handleOwnCurrencyClick(int mouseX, int mouseY, int button) {
        CurrencyIcon hovered = hoveredOwnCurrency(mouseX, mouseY);
        if (hovered == null) return false;
        int amount = Minecraft.getInstance().hasShiftDown() ? 10 : 1;
        int delta = button == 0 ? amount : -amount;
        ModNetwork.sendToServer(new TradePayloads.C2S_AdjustCurrencyOffer(hovered.denomination().id(), delta));
        return true;
    }

    private void renderCurrencyStacks(GuiGraphics g, long traceAmount, int x, int y, boolean interactive, int mouseX, int mouseY) {
        long[] counts = normalizedCounts(traceAmount);
        for (int i = 0; i < CURRENCY_ICONS.length; i++) {
            int iconX = x + i * ICON_STEP;
            ItemStack stack = stackFor(CURRENCY_ICONS[i]);
            g.renderItem(stack, iconX, y);
            g.renderItemDecorations(font, stack, iconX, y, Long.toString(counts[i]));
            if (interactive && isInside(mouseX, mouseY, iconX, y, ICON_SIZE, ICON_SIZE)) {
                drawHoverBorder(g, iconX, y);
            }
        }
    }

    private void renderOfferedCurrency(GuiGraphics g, long offeredTrace, int x, int y, int mouseX, int mouseY) {
        g.drawString(font, "Offering", x, y - 10, 0xaaaaaa, false);
        renderCurrencyStacks(g, offeredTrace, x, y, false, mouseX, mouseY);
    }

    private void renderCurrencyTooltips(GuiGraphics g, int mouseX, int mouseY, long selfBalance, long otherOffered, long selfOffered) {
        CurrencyIcon hoveredOwn = hoveredOwnCurrency(mouseX, mouseY);
        if (hoveredOwn != null) {
            g.setComponentTooltipForNextFrame(font, List.of(
                    Component.literal("Add/remove Attunement " + displayName(hoveredOwn.denomination())),
                    Component.literal("Left-click: add 1"),
                    Component.literal("Right-click: remove 1"),
                    Component.literal("Shift: x10"),
                    Component.literal("Balance: " + CurrencyAmount.ofTrace(selfBalance).formatNormalized())
            ), mouseX, mouseY);
            return;
        }

        if (isInside(mouseX, mouseY, leftPos + 56, topPos + 47, ICON_STEP * CURRENCY_ICONS.length, ICON_SIZE)) {
            g.setTooltipForNextFrame(font, Component.literal("Offered currency: " + CurrencyAmount.ofTrace(otherOffered).formatNormalized()), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, leftPos + 56, topPos + 108, ICON_STEP * CURRENCY_ICONS.length, ICON_SIZE)) {
            g.setTooltipForNextFrame(font, Component.literal("Offered currency: " + CurrencyAmount.ofTrace(selfOffered).formatNormalized()), mouseX, mouseY);
        }
    }

    private CurrencyIcon hoveredOwnCurrency(int mouseX, int mouseY) {
        int x = leftPos + 8;
        int y = topPos + 76;
        for (int i = 0; i < CURRENCY_ICONS.length; i++) {
            int iconX = x + i * ICON_STEP;
            if (isInside(mouseX, mouseY, iconX, y, ICON_SIZE, ICON_SIZE)) {
                return CURRENCY_ICONS[i];
            }
        }
        return null;
    }

    private static long[] normalizedCounts(long traceAmount) {
        long remaining = Math.max(0L, traceAmount);
        long[] counts = new long[CURRENCY_ICONS.length];
        for (int i = 0; i < CURRENCY_ICONS.length; i++) {
            long value = CURRENCY_ICONS[i].denomination().traceValue();
            counts[i] = remaining / value;
            remaining %= value;
        }
        return counts;
    }

    private static ItemStack stackFor(CurrencyIcon icon) {
        return new ItemStack(icon.item().get());
    }

    private static void drawHoverBorder(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + ICON_SIZE + 1, y, 0xffffffff);
        g.fill(x - 1, y + ICON_SIZE, x + ICON_SIZE + 1, y + ICON_SIZE + 1, 0xffffffff);
        g.fill(x - 1, y, x, y + ICON_SIZE, 0xffffffff);
        g.fill(x + ICON_SIZE, y, x + ICON_SIZE + 1, y + ICON_SIZE, 0xffffffff);
    }

    private static boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static String displayName(CurrencyDenomination denomination) {
        return switch (denomination) {
            case ANCHOR -> "Anchor";
            case CROWN -> "Crown";
            case SEAL -> "Seal";
            case MARK -> "Mark";
            case TRACE -> "Trace";
        };
    }

    private static String tradeStatus(boolean ready, boolean confirmed) {
        if (confirmed) return "Confirmed";
        if (ready) return "Ready";
        return "Not ready";
    }

    private record CurrencyIcon(CurrencyDenomination denomination, Supplier<? extends Item> item) {}

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
