package net.goui.cosmicdungeon.client.screen;

import net.goui.cosmicdungeon.economy.CurrencyAmount;
import net.goui.cosmicdungeon.economy.pricing.VendorPricingService;
import net.goui.cosmicdungeon.menu.VendorMenu;
import net.goui.cosmicdungeon.network.ModNetwork;
import net.goui.cosmicdungeon.network.VendorPayloads;
import net.goui.cosmicdungeon.vendor.VendorOffer;
import net.goui.cosmicdungeon.vendor.VendorProfile;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public final class VendorScreen extends AbstractContainerScreen<VendorMenu> {
    private static final int OFFER_ROW_TOP_OFFSET = 46;
    private static final int OFFER_ROW_HEIGHT = 22;
    private static final int OFFERS_PER_PAGE = 4;

    private final VendorClientState.VendorView view;
    private final List<Button> offerButtons = new ArrayList<>();
    private int offerPage;

    public VendorScreen(VendorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.view = VendorClientState.current();
        this.imageWidth = 220;
        this.imageHeight = 206;
    }

    @Override
    protected void init() {
        super.init();
        rebuildVendorWidgets();
    }

    private void rebuildVendorWidgets() {
        clearWidgets();
        offerButtons.clear();
        int x0 = leftPos + 10;

        if (view == null || view.profile() == null) return;

        clampOfferPage();
        int pageCount = pageCount();
        int firstOffer = offerPage * OFFERS_PER_PAGE;
        int lastOffer = Math.min(firstOffer + OFFERS_PER_PAGE, view.profile().buyOffers().size());
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
            VendorOffer offer = view.profile().buyOffers().get(i);
            boolean unlocked = view.unlockedOffers().contains(offer.id().toString());
            Button b = Button.builder(Component.literal(unlocked ? "Buy" : "Locked"), btn -> {
                if (!unlocked) return;
                ModNetwork.sendToServer(new VendorPayloads.C2S_RequestVendorPurchase(view.vendorEntityId(), offer.id().toString()));
            }).bounds(x0 + 160, y, 46, 20).build();
            b.active = unlocked;
            addRenderableWidget(b);
            offerButtons.add(b);
            y += OFFER_ROW_HEIGHT;
        }

        addRenderableWidget(Button.builder(Component.literal("Sell Held"), btn -> {
            if (minecraft == null || minecraft.player == null || view == null) return;
            int selectedSlot = minecraft.player.getInventory().getSelectedSlot();
            ModNetwork.sendToServer(new VendorPayloads.C2S_RequestVendorSellSlot(view.vendorEntityId(), selectedSlot));
        }).bounds(x0, topPos + imageHeight - 44, 70, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Sell Set"), btn -> {
            if (minecraft == null || minecraft.player == null || view == null || view.profile() == null) return;
            String pricingGroup = view.profile().buyback() != null ? view.profile().buyback().pricingGroup() : "default";
            var sets = VendorPricingService.detectCompleteSets(minecraft.player, pricingGroup);
            if (sets.isEmpty()) return;
            ModNetwork.sendToServer(new VendorPayloads.C2S_RequestVendorSellDetectedSet(view.vendorEntityId(), sets.getFirst().setId()));
        }).bounds(x0 + 74, topPos + imageHeight - 44, 70, 20).build());
    }

    private int pageCount() {
        if (view == null || view.profile() == null || view.profile().buyOffers().isEmpty()) {
            return 1;
        }
        return (view.profile().buyOffers().size() + OFFERS_PER_PAGE - 1) / OFFERS_PER_PAGE;
    }

    private void clampOfferPage() {
        offerPage = Math.max(0, Math.min(offerPage, pageCount() - 1));
    }

    private void changeOfferPage(int delta) {
        int oldPage = offerPage;
        offerPage = Math.max(0, Math.min(offerPage + delta, pageCount() - 1));
        if (offerPage != oldPage) {
            rebuildVendorWidgets();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        if (view == null || view.profile() == null || pageCount() <= 1 || !isMouseOver(mouseX, mouseY)) {
            return false;
        }
        changeOfferPage(scrollY < 0.0D ? 1 : -1);
        return true;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);

        int x0 = leftPos + 10;
        int y = topPos + 8;
        g.drawString(font, title, x0, y, 0xFFFFFF, false);
        y += 12;
        if (view == null || view.profile() == null) {
            g.drawString(font, Component.literal("Vendor data unavailable."), x0, y, 0xFF5555, false);
            return;
        }

        g.drawString(font, Component.literal("Balance: " + CurrencyAmount.ofTrace(view.balanceTrace()).formatNormalized()), x0, y, 0xFFE082, false);
        y += 12;

        int pageCount = pageCount();
        if (pageCount > 1) {
            g.drawString(font, Component.literal("Offers " + (offerPage + 1) + "/" + pageCount), x0, y, 0xB0BEC5, false);
            g.drawString(font, Component.literal("Scroll or page"), x0 + 114, topPos + 36, 0xB0BEC5, false);
        } else {
            g.drawString(font, Component.literal("Offers"), x0, y, 0xB0BEC5, false);
        }

        int firstOffer = offerPage * OFFERS_PER_PAGE;
        int lastOffer = Math.min(firstOffer + OFFERS_PER_PAGE, view.profile().buyOffers().size());
        y = topPos + OFFER_ROW_TOP_OFFSET;

        for (int i = firstOffer; i < lastOffer; i++) {
            VendorOffer offer = view.profile().buyOffers().get(i);
            boolean unlocked = view.unlockedOffers().contains(offer.id().toString());
            String itemName = offer.result().getHoverName().getString();
            String cost = CurrencyAmount.of(offer.cost().amount(), offer.cost().denomination()).formatNormalized();
            g.drawString(font, Component.literal(itemName), x0, y, 0xFFFFFF, false);
            g.drawString(font, Component.literal(cost), x0 + 105, y, 0xA5D6A7, false);
            if (!unlocked) {
                g.drawString(font, Component.literal("LOCKED"), x0 + 160, y, 0xEF9A9A, false);
            }
            y += OFFER_ROW_HEIGHT;
        }

        if (minecraft != null && minecraft.player != null) {
            String pricingGroup = view.profile().buyback() != null ? view.profile().buyback().pricingGroup() : "default";
            var heldPrice = VendorPricingService.getSellValue(minecraft.player.getMainHandItem(), pricingGroup);
            g.drawString(font, Component.literal("Held sell value: " + heldPrice.traceValue() + " Trace"), x0, topPos + imageHeight - 66, 0x90CAF9, false);
            var sets = VendorPricingService.detectCompleteSets(minecraft.player, pricingGroup);
            if (sets.isEmpty()) {
                g.drawString(font, Component.literal("No complete set detected"), x0 + 146, topPos + imageHeight - 38, 0xB0BEC5, false);
            } else {
                var set = sets.getFirst();
                g.drawString(font, Component.literal("Set " + set.setId() + ": " + set.traceValue() + " Trace"), x0 + 146, topPos + imageHeight - 38, 0xC5E1A5, false);
            }
        }
    }

    public static final class VendorClientState {
        private static VendorView current;
        public static void set(VendorView state) { current = state; }
        public static VendorView current() { return current; }

        public record VendorView(int vendorEntityId, ResourceLocation profileId, String title, VendorProfile profile, long balanceTrace, java.util.Set<String> unlockedOffers) {}
    }
}
