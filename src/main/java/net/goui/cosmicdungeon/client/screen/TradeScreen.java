package net.goui.cosmicdungeon.client.screen;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.client.economy.CurrencyBalanceOverlay;
import net.goui.cosmicdungeon.economy.CurrencyAmount;
import net.goui.cosmicdungeon.economy.CurrencyDenomination;
import net.goui.cosmicdungeon.item.ModItems;
import net.goui.cosmicdungeon.network.ModNetwork;
import net.goui.cosmicdungeon.network.TradePayloads;
import net.goui.cosmicdungeon.trade.TradeMenu;
import net.goui.cosmicdungeon.trade.TradeScreenLayout;
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
import static net.goui.cosmicdungeon.trade.TradeScreenLayout.*;

public class TradeScreen extends AbstractContainerScreen<TradeMenu> {
    private static final ResourceLocation TRADE_WINDOW = ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "textures/gui/container/trade_window.png");
    private static final ResourceLocation ACCEPT_BUTTON = ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "textures/gui/gui_accept.png");
    private static final ResourceLocation DENY_BUTTON = ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "textures/gui/gui_deny.png");
    private static final int HOVER_BORDER_COLOR = 0xff66ffff;
    private static final int READY_BORDER_COLOR = 0xff55ff55;
    private static final int DISABLED_TEXTURE_COLOR = 0x66ffffff;
    private static final CurrencyIcon[] CURRENCY_ICONS = {
            new CurrencyIcon(CurrencyDenomination.ANCHOR, ModItems.ATTUNEMENT_ANCHOR),
            new CurrencyIcon(CurrencyDenomination.CROWN, ModItems.ATTUNEMENT_CROWN),
            new CurrencyIcon(CurrencyDenomination.SEAL, ModItems.ATTUNEMENT_SEAL),
            new CurrencyIcon(CurrencyDenomination.MARK, ModItems.ATTUNEMENT_MARK),
            new CurrencyIcon(CurrencyDenomination.TRACE, ModItems.ATTUNEMENT_TRACE)
    };
    private final ItemStack[] currencyStacks = new ItemStack[5];
    private List<TradeScreenLayout.CurrencyCell> ownCells = List.of(), otherCells = List.of();
    private long lastOwnOffer = Long.MIN_VALUE, lastOtherOffer = Long.MIN_VALUE;
    private float xMouse, yMouse;

    public TradeScreen(TradeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        lastOwnOffer = lastOtherOffer = Long.MIN_VALUE;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + WIDTH, y + HEIGHT, 0xffc6c6c6);
        g.fill(x + 1, y + 1, x + WIDTH - 1, y + 2, 0xffffffff);
        g.fill(x + 1, y + HEIGHT - 2, x + WIDTH - 1, y + HEIGHT - 1, 0xff555555);
        // Separate, full-width offer panels leave currency clear of slots and inventory.
        g.fill(x + 5, y + 35, x + WIDTH - 5, y + 88, 0xff30303a);
        g.fill(x + 5, y + 90, x + WIDTH - 5, y + 142, 0xff25252f);
        g.fill(x + 5, y + 145, x + WIDTH - 5, y + 146, 0xff777777);
        for (var slot : menu.slots) {
            // Reuse one original 18px slot frame without stretching its authored pixels.
            g.blit(RenderPipelines.GUI_TEXTURED, TRADE_WINDOW, x + slot.x - 1, y + slot.y - 1,
                    54.0F, 26.0F, 18, 18, 256, 256);
        }
        if (minecraft != null && minecraft.player != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, x + 181, y + 163,
                    x + 214, y + 232, 22, 0.0625F, xMouse, yMouse, minecraft.player);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Text is rendered after native slots at the same shared layout anchors.
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        var view = TradeClientState.currentFor(menu.containerId);
        var data = TradeViewData.from(view);
        updateCurrencyRows(data);
        xMouse = mx;
        yMouse = my;
        super.render(g, mx, my, pt);
        renderOfferedCurrency(g, otherCells, OTHER_CURRENCY_Y);
        renderOfferedCurrency(g, ownCells, OWN_CURRENCY_Y);
        renderTradeButtons(g, view);
        renderTradeText(g, view, data);
        renderHoverBorders(g, mx, my);
        renderTooltip(g, mx, my);
        renderCustomTooltips(g, mx, my, view, data);
        // The exact inventory account renderer, including live total/available hover details.
        CurrencyBalanceOverlay.drawAccount(g, leftPos + 8, topPos + 4, WIDTH - 16, mx, my);
    }

    private void updateCurrencyRows(TradeViewData data) {
        if (lastOwnOffer != data.selfOffered()) {
            ownCells = currencyCells(data.selfOffered(), font::width);
            lastOwnOffer = data.selfOffered();
        }
        if (lastOtherOffer != data.otherOffered()) {
            otherCells = currencyCells(data.otherOffered(), font::width);
            lastOtherOffer = data.otherOffered();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int button = event.button(), mx = (int) event.x(), my = (int) event.y();
        if (button == 0) {
            if (inside(mx, my, ACCEPT_X, OWN_ACCEPT_Y, ICON_SIZE, ICON_SIZE)) {
                clickAccept();
                return true;
            }
            if (inside(mx, my, DENY_X, DENY_Y, ICON_SIZE, ICON_SIZE)) {
                ModNetwork.sendToServer(new TradePayloads.C2S_Cancel());
                return true;
            }
        }
        if ((button == 0 || button == 1) && handleOwnCurrencyClick(mx, my, button)) return true;
        return super.mouseClicked(event, doubleClick);
    }

    private void clickAccept() {
        var view = TradeClientState.currentFor(menu.containerId);
        if (view != null && view.selfReady() && view.otherReady() && !view.selfConfirmed())
            ModNetwork.sendToServer(new TradePayloads.C2S_Confirm(true));
        else if (view == null || !view.selfReady())
            ModNetwork.sendToServer(new TradePayloads.C2S_Ready(true));
        else if (view.otherReady() && !view.selfConfirmed())
            ModNetwork.sendToServer(new TradePayloads.C2S_Confirm(true));
    }

    private boolean handleOwnCurrencyClick(int mx, int my, int button) {
        var hovered = hoveredCurrency(mx, my, ownCells, OWN_CURRENCY_Y);
        if (hovered == null) return false;
        var view = TradeClientState.currentFor(menu.containerId);
        if (view != null && view.selfReady()) return true;
        int amount = Minecraft.getInstance().hasShiftDown() ? 10 : 1;
        ModNetwork.sendToServer(new TradePayloads.C2S_AdjustCurrencyOffer(
                CURRENCY_ICONS[hovered.index()].denomination().id(), button == 0 ? amount : -amount));
        return true;
    }

    private void renderTradeButtons(GuiGraphics g, TradeClientState.TradeView view) {
        boolean accepted = view != null && (view.otherReady() || view.otherConfirmed());
        g.blit(RenderPipelines.GUI_TEXTURED, ACCEPT_BUTTON, leftPos + ACCEPT_X, topPos + OTHER_ACCEPT_Y,
                0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, accepted ? -1 : DISABLED_TEXTURE_COLOR);
        if (accepted) drawBorder(g, leftPos + ACCEPT_X, topPos + OTHER_ACCEPT_Y, ICON_SIZE, ICON_SIZE, READY_BORDER_COLOR, 1);
        g.blit(RenderPipelines.GUI_TEXTURED, ACCEPT_BUTTON, leftPos + ACCEPT_X, topPos + OWN_ACCEPT_Y,
                0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        g.blit(RenderPipelines.GUI_TEXTURED, DENY_BUTTON, leftPos + DENY_X, topPos + DENY_Y,
                0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
    }

    private void renderTradeText(GuiGraphics g, TradeClientState.TradeView view, TradeViewData data) {
        g.drawString(font, font.plainSubstrByWidth(data.otherName() + "'s offer", 183),
                leftPos + 9, topPos + 38, 0xffeeeeee, false);
        g.drawString(font, "Your offer", leftPos + 9, topPos + 92, 0xffffffff, false);
        String theirs = tradeStatus(view != null && view.otherReady(), view != null && view.otherConfirmed());
        String ours = tradeStatus(view != null && view.selfReady(), view != null && view.selfConfirmed());
        g.drawString(font, theirs, leftPos + WIDTH - 10 - font.width(theirs), topPos + 38, 0xffcccccc, false);
        g.drawString(font, ours, leftPos + WIDTH - 10 - font.width(ours), topPos + 92, 0xffcccccc, false);
        g.drawString(font, playerInventoryTitle, leftPos + 9, topPos + 148, 0xff404040, false);
        g.drawString(font, "Trade status", leftPos + 219, topPos + 148, 0xff404040, false);
        var lines = font.split(Component.literal(statusText(view)), 72);
        for (int i = 0; i < Math.min(8, lines.size()); i++)
            g.drawString(font, lines.get(i), leftPos + 219, topPos + 160 + i * 9, 0xff404040, false);
    }

    private void renderOfferedCurrency(GuiGraphics g, List<TradeScreenLayout.CurrencyCell> cells, int y) {
        for (var cell : cells) {
            if (currencyStacks[cell.index()] == null)
                currencyStacks[cell.index()] = new ItemStack(CURRENCY_ICONS[cell.index()].item().get());
            g.renderItem(currencyStacks[cell.index()], leftPos + cell.x(), topPos + y);
            // Plain integer beside each denomination: no text field border or stack-count overlay.
            g.drawString(font, Long.toString(cell.count()), leftPos + cell.x() + 18, topPos + y + 4, 0xffffffff, false);
        }
    }

    private void renderHoverBorders(GuiGraphics g, int mx, int my) {
        var cell = hoveredCurrency(mx, my, ownCells, OWN_CURRENCY_Y);
        if (cell != null) drawBorder(g, leftPos + cell.x(), topPos + OWN_CURRENCY_Y, ICON_SIZE, ICON_SIZE, HOVER_BORDER_COLOR, 1);
        if (inside(mx, my, ACCEPT_X, OWN_ACCEPT_Y, ICON_SIZE, ICON_SIZE))
            drawBorder(g, leftPos + ACCEPT_X, topPos + OWN_ACCEPT_Y, ICON_SIZE, ICON_SIZE, HOVER_BORDER_COLOR, 2);
        if (inside(mx, my, DENY_X, DENY_Y, ICON_SIZE, ICON_SIZE))
            drawBorder(g, leftPos + DENY_X, topPos + DENY_Y, ICON_SIZE, ICON_SIZE, HOVER_BORDER_COLOR, 2);
    }

    private void renderCustomTooltips(GuiGraphics g, int mx, int my, TradeClientState.TradeView view, TradeViewData data) {
        var own = hoveredCurrency(mx, my, ownCells, OWN_CURRENCY_Y);
        var other = hoveredCurrency(mx, my, otherCells, OTHER_CURRENCY_Y);
        if (own != null) {
            g.setComponentTooltipForNextFrame(font, List.of(
                    Component.literal("Offer Attunement " + displayName(CURRENCY_ICONS[own.index()].denomination())),
                    Component.literal("Your offer: " + own.count()),
                    Component.literal("Left-click: add 1"), Component.literal("Right-click: remove 1"),
                    Component.literal(view != null && view.selfReady() ? "Locked after accepting" : "Shift: adjust by 10"),
                    Component.literal("Your offered currency: " + CurrencyAmount.ofTrace(data.selfOffered()).formatNormalized()),
                    Component.literal("Balance: " + CurrencyAmount.ofTrace(data.selfBalance()).formatNormalized())), mx, my);
        } else if (other != null) {
            g.setComponentTooltipForNextFrame(font, List.of(
                    Component.literal(data.otherName() + " offers " + other.count() + " " + displayName(CURRENCY_ICONS[other.index()].denomination())),
                    Component.literal("Their offered currency: " + CurrencyAmount.ofTrace(data.otherOffered()).formatNormalized())), mx, my);
        } else if (inside(mx, my, ACCEPT_X, OWN_ACCEPT_Y, ICON_SIZE, ICON_SIZE)) {
            g.setComponentTooltipForNextFrame(font, List.of(Component.literal(acceptTooltipTitle(view)),
                    Component.literal("Locks your current item and currency offer."),
                    Component.literal("Offer changes reset accepted/finalized state.")), mx, my);
        } else if (inside(mx, my, DENY_X, DENY_Y, ICON_SIZE, ICON_SIZE)) {
            g.setComponentTooltipForNextFrame(font, List.of(Component.literal("Cancel trade"),
                    Component.literal("Returns offered items and closes both screens.")), mx, my);
        } else if (inside(mx, my, ACCEPT_X, OTHER_ACCEPT_Y, ICON_SIZE, ICON_SIZE)) {
            g.setComponentTooltipForNextFrame(font, List.of(Component.literal("Other player status"),
                    Component.literal(view != null && (view.otherReady() || view.otherConfirmed()) ? "Accepted" : "Not accepted yet")), mx, my);
        } else if (inside(mx, my, 9, 38, 183, 9)) {
            g.setComponentTooltipForNextFrame(font, List.of(Component.literal("Trading with: " + data.otherName()),
                    Component.literal("Balance: " + CurrencyAmount.ofTrace(data.otherBalance()).formatNormalized())), mx, my);
        } else if (inside(mx, my, 219, 148, 73, 84)) {
            g.setTooltipForNextFrame(Component.literal(statusText(view)), mx, my);
        }
    }

    private TradeScreenLayout.CurrencyCell hoveredCurrency(int mx, int my, List<TradeScreenLayout.CurrencyCell> cells, int y) {
        if (my < topPos + y || my >= topPos + y + ICON_SIZE) return null;
        for (var cell : cells) if (cell.contains(mx - leftPos)) return cell;
        return null;
    }

    private boolean inside(int mx, int my, int x, int y, int width, int height) {
        return mx >= leftPos + x && mx < leftPos + x + width && my >= topPos + y && my < topPos + y + height;
    }

    private static void drawBorder(GuiGraphics g, int x, int y, int width, int height, int color, int thickness) {
        for (int i = 0; i < thickness; i++) {
            g.fill(x - i - 1, y - i - 1, x + width + i + 1, y - i, color);
            g.fill(x - i - 1, y + height + i, x + width + i + 1, y + height + i + 1, color);
            g.fill(x - i - 1, y - i, x - i, y + height + i, color);
            g.fill(x + width + i, y - i, x + width + i + 1, y + height + i, color);
        }
    }

    private static String displayName(CurrencyDenomination denomination) {
        return switch (denomination) {
            case ANCHOR -> "Anchor"; case CROWN -> "Crown"; case SEAL -> "Seal"; case MARK -> "Mark"; case TRACE -> "Trace";
        };
    }

    private static String tradeStatus(boolean ready, boolean confirmed) {
        return confirmed ? "Finalized" : ready ? "Accepted" : "Waiting";
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
        if (view != null && view.selfReady() && view.otherReady() && !view.selfConfirmed()) return "Finalize trade";
        if (view != null && view.selfReady()) return "Waiting for other player";
        return "Accept current offer";
    }

    private record CurrencyIcon(CurrencyDenomination denomination, Supplier<? extends Item> item) {}
    private record TradeViewData(String selfName, String otherName, long selfBalance, long otherBalance, long selfOffered, long otherOffered) {
        static TradeViewData from(TradeClientState.TradeView view) {
            return new TradeViewData(
                    view == null || view.selfName().isBlank() ? "You" : view.selfName(),
                    view == null || view.otherName().isBlank() ? "Partner" : view.otherName(),
                    view == null ? 0L : view.selfBalanceTrace(), view == null ? 0L : view.otherBalanceTrace(),
                    view == null ? 0L : view.selfOfferedTrace(), view == null ? 0L : view.otherOfferedTrace());
        }
    }

    public static final class TradeClientState {
        private static TradeView current;

        private TradeClientState() {}

        public static void set(TradeView view) {
            current = view;
        }

        public static TradeView current() {
            var player = net.minecraft.client.Minecraft.getInstance().player;
            return current != null && player != null && player.containerMenu instanceof TradeMenu
                    && net.goui.cosmicdungeon.menu.SessionMenu.matches(player.containerMenu, current.containerId(), current.sessionId())
                    ? current : null;
        }

        public static TradeView currentFor(int containerId) {
            var view = current();
            return view != null && view.containerId() == containerId ? view : null;
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