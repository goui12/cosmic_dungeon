package net.goui.cosmicdungeon.client.screen;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.economy.CurrencyAmount;
import net.goui.cosmicdungeon.economy.CurrencyDenomination;
import net.goui.cosmicdungeon.item.ModItems;
import net.goui.cosmicdungeon.network.ModNetwork;
import net.goui.cosmicdungeon.network.TradePayloads;
import net.goui.cosmicdungeon.trade.TradeMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
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
    private static final ResourceLocation TRADE_WINDOW = ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "textures/gui/container/trade_window.png");
    private static final ResourceLocation ACCEPT_BUTTON = ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "textures/gui/gui_accept.png");
    private static final ResourceLocation DENY_BUTTON = ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "textures/gui/gui_deny.png");

    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;
    private static final int ICON_SIZE = 16;
    private static final int ICON_STEP = 18;
    private static final int BALANCE_ICON_X = 8;
    private static final int OTHER_BALANCE_ICON_Y = 17;
    private static final int SELF_NAME_Y = 65;
    private static final int SELF_BALANCE_ICON_Y = 76;
    private static final int OTHER_OFFER_SUMMARY_X = 56;
    private static final int OTHER_OFFER_SUMMARY_Y = 47;
    private static final int SELF_OFFER_SUMMARY_X = 56;
    private static final int SELF_OFFER_SUMMARY_Y = 108;
    private static final int OTHER_ACCEPT_X = 228;
    private static final int OTHER_ACCEPT_Y = 28;
    private static final int OWN_ACCEPT_X = 228;
    private static final int OWN_ACCEPT_Y = 89;
    private static final int OWN_DENY_X = 228;
    private static final int OWN_DENY_Y = 107;
    private static final int PLAYER_PREVIEW_X1 = 28;
    private static final int PLAYER_PREVIEW_Y1 = 72;
    private static final int PLAYER_PREVIEW_X2 = 52;
    private static final int PLAYER_PREVIEW_Y2 = 124;
    private static final int PLAYER_PREVIEW_SCALE = 22;
    private static final float PLAYER_PREVIEW_Y_OFFSET = 0.0625F;
    private static final int HOVER_BORDER_COLOR = 0xff66ffff;
    private static final int READY_BORDER_COLOR = 0xff55ff55;
    private static final int DISABLED_TEXTURE_COLOR = 0x66ffffff;

    private static final CurrencyIcon[] CURRENCY_ICONS = new CurrencyIcon[] {
            new CurrencyIcon(CurrencyDenomination.ANCHOR, ModItems.ATTUNEMENT_ANCHOR),
            new CurrencyIcon(CurrencyDenomination.CROWN, ModItems.ATTUNEMENT_CROWN),
            new CurrencyIcon(CurrencyDenomination.SEAL, ModItems.ATTUNEMENT_SEAL),
            new CurrencyIcon(CurrencyDenomination.MARK, ModItems.ATTUNEMENT_MARK),
            new CurrencyIcon(CurrencyDenomination.TRACE, ModItems.ATTUNEMENT_TRACE)
    };

    private float xMouse;
    private float yMouse;

    public TradeScreen(TradeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = TEXTURE_WIDTH;
        this.imageHeight = TEXTURE_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        g.blit(RenderPipelines.GUI_TEXTURED, TRADE_WINDOW, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        if (this.minecraft != null && this.minecraft.player != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    g,
                    this.leftPos + PLAYER_PREVIEW_X1,
                    this.topPos + PLAYER_PREVIEW_Y1,
                    this.leftPos + PLAYER_PREVIEW_X2,
                    this.topPos + PLAYER_PREVIEW_Y2,
                    PLAYER_PREVIEW_SCALE,
                    PLAYER_PREVIEW_Y_OFFSET,
                    this.xMouse,
                    this.yMouse,
                    this.minecraft.player
            );
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // The PNG supplies the panel/chrome; draw trade text explicitly after slots instead of vanilla labels.
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        TradeClientState.TradeView view = TradeClientState.currentFor(menu.containerId);
        TradeViewData data = TradeViewData.from(view);

        this.xMouse = mx;
        this.yMouse = my;

        super.render(g, mx, my, pt);

        renderCurrencyStacks(g, data.otherBalance(), leftPos + BALANCE_ICON_X, topPos + OTHER_BALANCE_ICON_Y, false, mx, my);
        renderOfferedCurrency(g, data.otherOffered(), leftPos + OTHER_OFFER_SUMMARY_X, topPos + OTHER_OFFER_SUMMARY_Y, mx, my);
        renderCurrencyStacks(g, data.selfBalance(), leftPos + BALANCE_ICON_X, topPos + SELF_BALANCE_ICON_Y, true, mx, my);
        renderOfferedCurrency(g, data.selfOffered(), leftPos + SELF_OFFER_SUMMARY_X, topPos + SELF_OFFER_SUMMARY_Y, mx, my);
        renderTradeButtons(g, view, mx, my);
        renderTradeText(g, view, data);
        renderHoverBorders(g, mx, my);
        renderTooltip(g, mx, my);
        renderCustomTooltips(g, mx, my, view, data);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int button = event.button();
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        if (button == 0) {
            if (isInside(mouseX, mouseY, leftPos + OWN_ACCEPT_X, topPos + OWN_ACCEPT_Y, ICON_SIZE, ICON_SIZE)) {
                clickAccept();
                return true;
            }
            if (isInside(mouseX, mouseY, leftPos + OWN_DENY_X, topPos + OWN_DENY_Y, ICON_SIZE, ICON_SIZE)) {
                ModNetwork.sendToServer(new TradePayloads.C2S_Cancel());
                return true;
            }
        }
        if ((button == 0 || button == 1) && handleOwnCurrencyClick(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void clickAccept() {
        TradeClientState.TradeView view = TradeClientState.currentFor(menu.containerId);
        if (view != null && view.selfReady() && view.otherReady() && !view.selfConfirmed()) {
            ModNetwork.sendToServer(new TradePayloads.C2S_Confirm(true));
        } else if (view == null || !view.selfReady()) {
            ModNetwork.sendToServer(new TradePayloads.C2S_Ready(true));
        } else if (view.otherReady() && !view.selfConfirmed()) {
            ModNetwork.sendToServer(new TradePayloads.C2S_Confirm(true));
        }
    }

    private boolean handleOwnCurrencyClick(int mouseX, int mouseY, int button) {
        CurrencyIcon hovered = hoveredOwnCurrency(mouseX, mouseY);
        if (hovered == null) return false;
        TradeClientState.TradeView view = TradeClientState.currentFor(menu.containerId);
        if (view != null && view.selfReady()) {
            return true;
        }
        int amount = Minecraft.getInstance().hasShiftDown() ? 10 : 1;
        int delta = button == 0 ? amount : -amount;
        ModNetwork.sendToServer(new TradePayloads.C2S_AdjustCurrencyOffer(hovered.denomination().id(), delta));
        return true;
    }

    private void renderTradeButtons(GuiGraphics g, TradeClientState.TradeView view, int mouseX, int mouseY) {
        boolean otherAccepted = view != null && (view.otherReady() || view.otherConfirmed());
        int otherColor = otherAccepted ? -1 : DISABLED_TEXTURE_COLOR;
        g.blit(RenderPipelines.GUI_TEXTURED, ACCEPT_BUTTON, leftPos + OTHER_ACCEPT_X, topPos + OTHER_ACCEPT_Y, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, otherColor);
        if (otherAccepted) {
            drawBorder(g, leftPos + OTHER_ACCEPT_X, topPos + OTHER_ACCEPT_Y, ICON_SIZE, ICON_SIZE, READY_BORDER_COLOR, 1);
        }

        g.blit(RenderPipelines.GUI_TEXTURED, ACCEPT_BUTTON, leftPos + OWN_ACCEPT_X, topPos + OWN_ACCEPT_Y, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        g.blit(RenderPipelines.GUI_TEXTURED, DENY_BUTTON, leftPos + OWN_DENY_X, topPos + OWN_DENY_Y, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
    }

    private void renderTradeText(GuiGraphics g, TradeClientState.TradeView view, TradeViewData data) {
        g.drawString(font, "Trading with: " + data.otherName(), leftPos + BALANCE_ICON_X, topPos + 6, 0xffffff, false);
        g.drawString(font, data.otherName() + " offer", leftPos + 56, topPos + 17, 0xcccccc, false);
        g.drawString(font, data.selfName(), leftPos + BALANCE_ICON_X, topPos + SELF_NAME_Y, 0xffffff, false);
        g.drawString(font, data.selfName() + " offer", leftPos + 56, topPos + 78, 0xffffff, false);

        g.drawString(font, "You: " + tradeStatus(view == null ? false : view.selfReady(), view == null ? false : view.selfConfirmed()), leftPos + 146, topPos + 64, 0xffffff, false);
        g.drawString(font, data.otherName() + ": " + tradeStatus(view == null ? false : view.otherReady(), view == null ? false : view.otherConfirmed()), leftPos + 146, topPos + 74, 0xcccccc, false);

        String status = statusText(view);
        if (!status.isBlank()) {
            g.drawString(font, font.plainSubstrByWidth(status, 238), leftPos + 8, topPos + 238, 0xffdd66, false);
        }
    }

    private void renderHoverBorders(GuiGraphics g, int mouseX, int mouseY) {
        CurrencyIcon hoveredOwn = hoveredOwnCurrency(mouseX, mouseY);
        if (hoveredOwn != null) {
            int iconY = topPos + SELF_BALANCE_ICON_Y + indexOf(hoveredOwn) * ICON_STEP;
            drawBorder(g, leftPos + BALANCE_ICON_X, iconY, ICON_SIZE, ICON_SIZE, HOVER_BORDER_COLOR, 1);
        }
        if (isInside(mouseX, mouseY, leftPos + OWN_ACCEPT_X, topPos + OWN_ACCEPT_Y, ICON_SIZE, ICON_SIZE)) {
            drawBorder(g, leftPos + OWN_ACCEPT_X, topPos + OWN_ACCEPT_Y, ICON_SIZE, ICON_SIZE, HOVER_BORDER_COLOR, 2);
        }
        if (isInside(mouseX, mouseY, leftPos + OWN_DENY_X, topPos + OWN_DENY_Y, ICON_SIZE, ICON_SIZE)) {
            drawBorder(g, leftPos + OWN_DENY_X, topPos + OWN_DENY_Y, ICON_SIZE, ICON_SIZE, HOVER_BORDER_COLOR, 2);
        }
    }

    private void renderCurrencyStacks(GuiGraphics g, long traceAmount, int x, int y, boolean interactive, int mouseX, int mouseY) {
        long[] counts = normalizedCounts(traceAmount);
        for (int i = 0; i < CURRENCY_ICONS.length; i++) {
            int iconY = y + i * ICON_STEP;
            ItemStack stack = stackFor(CURRENCY_ICONS[i]);
            g.renderItem(stack, x, iconY);
            g.renderItemDecorations(font, stack, x, iconY, Long.toString(counts[i]));
        }
    }

    private void renderOfferedCurrency(GuiGraphics g, long offeredTrace, int x, int y, int mouseX, int mouseY) {
        long[] counts = normalizedCounts(offeredTrace);
        for (int i = 0; i < CURRENCY_ICONS.length; i++) {
            int iconX = x + i * ICON_STEP;
            ItemStack stack = stackFor(CURRENCY_ICONS[i]);
            g.renderItem(stack, iconX, y);
            g.renderItemDecorations(font, stack, iconX, y, Long.toString(counts[i]));
        }
    }

    private void renderCustomTooltips(GuiGraphics g, int mouseX, int mouseY, TradeClientState.TradeView view, TradeViewData data) {
        CurrencyIcon hoveredOwn = hoveredOwnCurrency(mouseX, mouseY);
        if (hoveredOwn != null) {
            g.setComponentTooltipForNextFrame(font, List.of(
                    Component.literal("Offer Attunement " + displayName(hoveredOwn.denomination())),
                    Component.literal("Left-click: add 1"),
                    Component.literal("Right-click: remove 1"),
                    Component.literal(view != null && view.selfReady() ? "Locked after accepting" : "Shift: adjust by 10"),
                    Component.literal("Balance: " + CurrencyAmount.ofTrace(data.selfBalance()).formatNormalized())
            ), mouseX, mouseY);
            return;
        }
        if (isInside(mouseX, mouseY, leftPos + OWN_ACCEPT_X, topPos + OWN_ACCEPT_Y, ICON_SIZE, ICON_SIZE)) {
            g.setComponentTooltipForNextFrame(font, List.of(
                    Component.literal(acceptTooltipTitle(view)),
                    Component.literal("Locks your current item and currency offer."),
                    Component.literal("Offer changes reset accepted/finalized state.")
            ), mouseX, mouseY);
            return;
        }
        if (isInside(mouseX, mouseY, leftPos + OWN_DENY_X, topPos + OWN_DENY_Y, ICON_SIZE, ICON_SIZE)) {
            g.setComponentTooltipForNextFrame(font, List.of(
                    Component.literal("Cancel trade"),
                    Component.literal("Returns offered items and closes both screens.")
            ), mouseX, mouseY);
            return;
        }
        if (isInside(mouseX, mouseY, leftPos + OTHER_ACCEPT_X, topPos + OTHER_ACCEPT_Y, ICON_SIZE, ICON_SIZE)) {
            g.setComponentTooltipForNextFrame(font, List.of(
                    Component.literal("Other player status"),
                    Component.literal(view != null && (view.otherReady() || view.otherConfirmed()) ? "Accepted" : "Not accepted yet")
            ), mouseX, mouseY);
            return;
        }
        if (isInside(mouseX, mouseY, leftPos + OTHER_OFFER_SUMMARY_X, topPos + OTHER_OFFER_SUMMARY_Y, ICON_STEP * CURRENCY_ICONS.length, ICON_SIZE)) {
            g.setTooltipForNextFrame(Component.literal("Their offered currency: " + CurrencyAmount.ofTrace(data.otherOffered()).formatNormalized()), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, leftPos + SELF_OFFER_SUMMARY_X, topPos + SELF_OFFER_SUMMARY_Y, ICON_STEP * CURRENCY_ICONS.length, ICON_SIZE)) {
            g.setTooltipForNextFrame(Component.literal("Your offered currency: " + CurrencyAmount.ofTrace(data.selfOffered()).formatNormalized()), mouseX, mouseY);
        }
    }

    private CurrencyIcon hoveredOwnCurrency(int mouseX, int mouseY) {
        int x = leftPos + BALANCE_ICON_X;
        int y = topPos + SELF_BALANCE_ICON_Y;
        for (int i = 0; i < CURRENCY_ICONS.length; i++) {
            int iconY = y + i * ICON_STEP;
            if (isInside(mouseX, mouseY, x, iconY, ICON_SIZE, ICON_SIZE)) {
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

    private static void drawBorder(GuiGraphics g, int x, int y, int width, int height, int color, int thickness) {
        for (int i = 0; i < thickness; i++) {
            g.fill(x - i - 1, y - i - 1, x + width + i + 1, y - i, color);
            g.fill(x - i - 1, y + height + i, x + width + i + 1, y + height + i + 1, color);
            g.fill(x - i - 1, y - i, x - i, y + height + i, color);
            g.fill(x + width + i, y - i, x + width + i + 1, y + height + i, color);
        }
    }

    private static boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static int indexOf(CurrencyIcon icon) {
        for (int i = 0; i < CURRENCY_ICONS.length; i++) {
            if (CURRENCY_ICONS[i] == icon) return i;
        }
        return 0;
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
        if (confirmed) return "Finalized";
        if (ready) return "Accepted";
        return "Waiting";
    }

    private static String statusText(TradeClientState.TradeView view) {
        if (view == null) return "Waiting for trade state...";
        if (!view.statusMessage().isBlank()) return view.statusMessage();
        if (view.otherConfirmed() && !view.selfConfirmed()) return "Other player finalized. Press Accept to finalize.";
        if (view.selfConfirmed() && !view.otherConfirmed()) return "Finalized. Waiting for other player to finalize.";
        if (view.otherReady() && !view.selfConfirmed()) return "Other player accepted. Press Accept to finalize.";
        if (view.selfReady() && !view.otherReady()) return "Waiting for other player to accept.";
        return "Review offers, then press Accept.";
    }

    private static String acceptTooltipTitle(TradeClientState.TradeView view) {
        if (view != null && view.selfReady() && view.otherReady() && !view.selfConfirmed()) {
            return "Finalize trade";
        }
        if (view != null && view.selfReady()) {
            return "Waiting for other player";
        }
        return "Accept current offer";
    }

    private record CurrencyIcon(CurrencyDenomination denomination, Supplier<? extends Item> item) {}

    private record TradeViewData(String selfName, String otherName, long selfBalance, long otherBalance, long selfOffered, long otherOffered) {
        static TradeViewData from(TradeClientState.TradeView view) {
            return new TradeViewData(
                    view == null || view.selfName().isBlank() ? "You" : view.selfName(),
                    view == null || view.otherName().isBlank() ? "Partner" : view.otherName(),
                    view == null ? 0L : view.selfBalanceTrace(),
                    view == null ? 0L : view.otherBalanceTrace(),
                    view == null ? 0L : view.selfOfferedTrace(),
                    view == null ? 0L : view.otherOfferedTrace()
            );
        }
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
