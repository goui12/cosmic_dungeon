package net.goui.cosmicdungeon.client.screen;

import net.goui.cosmicdungeon.economy.CurrencyAmount;
import net.goui.cosmicdungeon.economy.CurrencyDenomination;
import net.goui.cosmicdungeon.economy.pricing.VendorPricingService;
import net.goui.cosmicdungeon.menu.VendorMenu;
import net.goui.cosmicdungeon.network.ModNetwork;
import net.goui.cosmicdungeon.network.VendorPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class VendorScreen extends AbstractContainerScreen<VendorMenu> {
    private static final int OFFER_ROW_TOP_OFFSET = 46;
    private static final int OFFER_ROW_HEIGHT = 22;
    private static final int OFFERS_PER_PAGE = 4;
    private static final int OFFER_ITEM_SIZE = 16;

    private final List<Button> offerButtons = new ArrayList<>();
    private VendorClientState.VendorView renderedView;
    private int offerPage;

    public VendorScreen(VendorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 220;
        this.imageHeight = 206;
    }

    @Override
    protected void init() {
        super.init();
        rebuildVendorWidgets();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (VendorClientState.current() != renderedView) {
            rebuildVendorWidgets();
        }
    }

    private VendorClientState.VendorView view() {
        return VendorClientState.current();
    }

    private void rebuildVendorWidgets() {
        clearWidgets();
        offerButtons.clear();
        renderedView = view();
        int x0 = leftPos + 10;

        if (renderedView == null || renderedView.offers().isEmpty()) return;

        clampOfferPage();
        int pageCount = pageCount();
        int firstOffer = offerPage * OFFERS_PER_PAGE;
        int lastOffer = Math.min(firstOffer + OFFERS_PER_PAGE, renderedView.offers().size());
        int y = topPos + OFFER_ROW_TOP_OFFSET;

        if (pageCount > 1) {
            Button previous = Button.builder(Component.literal("<"), btn -> changeOfferPage(-1))
                    .bounds(x0 + 114, topPos + 16, 20, 18)
                    .build();
            previous.active = offerPage > 0;
            addRenderableWidget(previous);

            Button next = Button.builder(Component.literal(">"), btn -> changeOfferPage(1))
                    .bounds(x0 + 138, topPos + 16, 20, 18)
                    .build();
            next.active = offerPage < pageCount - 1;
            addRenderableWidget(next);
        }

        for (int i = firstOffer; i < lastOffer; i++) {
            VendorPayloads.S2C_OpenVendor.OfferView offer = renderedView.offers().get(i);
            boolean unlocked = renderedView.unlockedOffers().contains(offer.offerId());
            Button b = Button.builder(Component.literal(unlocked ? "Buy" : "Locked"), btn -> {
                VendorClientState.VendorView current = view();
                if (!unlocked || current == null) return;
                ModNetwork.sendToServer(new VendorPayloads.C2S_RequestVendorPurchase(current.vendorEntityId(), offer.offerId()));
            }).bounds(x0 + 160, y, 46, 20).build();
            b.active = unlocked;
            addRenderableWidget(b);
            offerButtons.add(b);
            y += OFFER_ROW_HEIGHT;
        }

        addRenderableWidget(Button.builder(Component.literal("Sell Held"), btn -> {
            VendorClientState.VendorView current = view();
            if (minecraft == null || minecraft.player == null || current == null) return;
            int selectedSlot = minecraft.player.getInventory().getSelectedSlot();
            ModNetwork.sendToServer(new VendorPayloads.C2S_RequestVendorSellSlot(current.vendorEntityId(), selectedSlot));
        }).bounds(x0, topPos + imageHeight - 44, 70, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Sell Set"), btn -> {
            VendorClientState.VendorView current = view();
            if (minecraft == null || minecraft.player == null || current == null) return;
            var sets = VendorPricingService.detectCompleteSets(minecraft.player, current.pricingGroup());
            if (sets.isEmpty()) return;
            ModNetwork.sendToServer(new VendorPayloads.C2S_RequestVendorSellDetectedSet(current.vendorEntityId(), sets.getFirst().setId()));
        }).bounds(x0 + 74, topPos + imageHeight - 44, 70, 20).build());
    }

    private int pageCount() {
        VendorClientState.VendorView current = view();
        if (current == null || current.offers().isEmpty()) return 1;
        return (current.offers().size() + OFFERS_PER_PAGE - 1) / OFFERS_PER_PAGE;
    }

    private void clampOfferPage() {
        offerPage = Math.max(0, Math.min(offerPage, pageCount() - 1));
    }

    private void changeOfferPage(int delta) {
        int oldPage = offerPage;
        offerPage = Math.max(0, Math.min(offerPage + delta, pageCount() - 1));
        if (offerPage != oldPage) rebuildVendorWidgets();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        VendorClientState.VendorView current = view();
        if (current == null || current.offers().isEmpty() || pageCount() <= 1 || !isMouseOver(mouseX, mouseY)) return false;
        changeOfferPage(scrollY < 0.0D ? 1 : -1);
        return true;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int bg = 0xE0202028;
        int border = 0xFF7E57C2;
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, bg);
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + 1, border);
        g.fill(leftPos, topPos + imageHeight - 1, leftPos + imageWidth, topPos + imageHeight, border);
        g.fill(leftPos, topPos, leftPos + 1, topPos + imageHeight, border);
        g.fill(leftPos + imageWidth - 1, topPos, leftPos + imageWidth, topPos + imageHeight, border);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (VendorClientState.current() != renderedView) rebuildVendorWidgets();
        renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);

        VendorClientState.VendorView current = view();
        int x0 = leftPos + 10;
        int y = topPos + 8;
        g.drawString(font, current != null ? Component.literal(current.title()) : title, x0, y, 0xFFFFFF, false);
        y += 12;
        if (current == null) {
            g.drawString(font, Component.literal("Loading vendor data..."), x0, y, 0xB0BEC5, false);
            return;
        }

        g.drawString(font, Component.literal("Balance: " + CurrencyAmount.ofTrace(current.balanceTrace()).formatNormalized()), x0, y, 0xFFE082, false);
        y += 12;

        int pageCount = pageCount();
        g.drawString(font, Component.literal(pageCount > 1 ? "Offers " + (offerPage + 1) + "/" + pageCount : "Offers"), x0, y, 0xB0BEC5, false);
        if (pageCount > 1) g.drawString(font, Component.literal("Scroll or page"), x0 + 114, topPos + 36, 0xB0BEC5, false);

        if (current.offers().isEmpty()) {
            g.drawString(font, Component.literal("This vendor has no offers."), x0, topPos + OFFER_ROW_TOP_OFFSET, 0xB0BEC5, false);
        }

        int firstOffer = offerPage * OFFERS_PER_PAGE;
        int lastOffer = Math.min(firstOffer + OFFERS_PER_PAGE, current.offers().size());
        y = topPos + OFFER_ROW_TOP_OFFSET;
        ItemStack hoveredOfferStack = ItemStack.EMPTY;

        for (int i = firstOffer; i < lastOffer; i++) {
            VendorPayloads.S2C_OpenVendor.OfferView offer = current.offers().get(i);
            boolean unlocked = current.unlockedOffers().contains(offer.offerId());
            String itemName = offer.count() > 1 ? offer.itemDisplayName() + " x" + offer.count() : offer.itemDisplayName();
            CurrencyDenomination denomination = CurrencyDenomination.fromId(offer.costDenomination());
            String cost = CurrencyAmount.of(offer.costAmount(), denomination).formatNormalized();
            int itemX = x0;
            int itemY = y - 4;
            g.renderItem(offer.stack(), itemX, itemY);
            g.renderItemDecorations(font, offer.stack(), itemX, itemY);
            g.drawString(font, Component.literal(itemName), x0 + 22, y, 0xFFFFFF, false);
            g.drawString(font, Component.literal(cost), x0 + 105, y, 0xA5D6A7, false);
            if (!unlocked) g.drawString(font, Component.literal("LOCKED"), x0 + 160, y, 0xEF9A9A, false);
            if (isHoveringItemStack(mouseX, mouseY, itemX, itemY, offer.stack())) {
                hoveredOfferStack = offer.stack();
            }
            y += OFFER_ROW_HEIGHT;
        }

        if (!hoveredOfferStack.isEmpty()) {
            g.setTooltipForNextFrame(font, hoveredOfferStack, mouseX, mouseY);
        }

        if (minecraft != null && minecraft.player != null) {
            var heldPrice = VendorPricingService.getSellValue(minecraft.player.getMainHandItem(), current.pricingGroup());
            g.drawString(font, Component.literal("Held sell value: " + heldPrice.traceValue() + " Trace"), x0, topPos + imageHeight - 66, 0x90CAF9, false);
            var sets = VendorPricingService.detectCompleteSets(minecraft.player, current.pricingGroup());
            if (sets.isEmpty()) {
                g.drawString(font, Component.literal("No complete set detected"), x0 + 146, topPos + imageHeight - 38, 0xB0BEC5, false);
            } else {
                var set = sets.getFirst();
                g.drawString(font, Component.literal("Set " + set.setId() + ": " + set.traceValue() + " Trace"), x0 + 146, topPos + imageHeight - 38, 0xC5E1A5, false);
            }
        }
    }

    private static boolean isHoveringItemStack(int mouseX, int mouseY, int itemX, int itemY, ItemStack stack) {
        return !stack.isEmpty()
                && mouseX >= itemX
                && mouseX < itemX + OFFER_ITEM_SIZE
                && mouseY >= itemY
                && mouseY < itemY + OFFER_ITEM_SIZE;
    }

    public static final class VendorClientState {
        private static VendorView current;
        public static void set(VendorView state) { current = state; }
        public static VendorView current() { return current; }

        public record VendorView(int vendorEntityId, ResourceLocation profileId, String title, long balanceTrace, String pricingGroup, List<VendorPayloads.S2C_OpenVendor.OfferView> offers, java.util.Set<String> unlockedOffers) {}
    }
}
