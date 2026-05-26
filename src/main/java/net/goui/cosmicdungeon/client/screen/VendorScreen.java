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
    private final VendorClientState.VendorView view;
    private final List<Button> offerButtons = new ArrayList<>();

    public VendorScreen(VendorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.view = VendorClientState.current();
        this.imageWidth = 220;
        this.imageHeight = 206;
    }

    @Override
    protected void init() {
        super.init();
        offerButtons.clear();
        int x0 = leftPos + 10;
        int y = topPos + 36;

        if (view == null || view.profile() == null) return;
        for (VendorOffer offer : view.profile().buyOffers()) {
            boolean unlocked = view.unlockedOffers().contains(offer.id().toString());
            Button b = Button.builder(Component.literal(unlocked ? "Buy" : "Locked"), btn -> {
                if (!unlocked) return;
                ModNetwork.sendToServer(new VendorPayloads.C2S_RequestVendorPurchase(view.vendorEntityId(), offer.id().toString()));
            }).bounds(x0 + 160, y, 46, 20).build();
            b.active = unlocked;
            addRenderableWidget(b);
            offerButtons.add(b);
            y += 22;
        }

        addRenderableWidget(Button.builder(Component.literal("Sell Held"), btn -> {
            if (minecraft == null || minecraft.player == null || view == null) return;
            int selectedSlot = minecraft.player.getInventory().selected;
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
        y += 16;

        for (VendorOffer offer : view.profile().buyOffers()) {
            boolean unlocked = view.unlockedOffers().contains(offer.id().toString());
            String itemName = offer.result().getHoverName().getString();
            String cost = CurrencyAmount.of(offer.cost().amount(), offer.cost().denomination()).formatNormalized();
            g.drawString(font, Component.literal(itemName), x0, y, 0xFFFFFF, false);
            g.drawString(font, Component.literal(cost), x0 + 105, y, 0xA5D6A7, false);
            if (!unlocked) {
                g.drawString(font, Component.literal("LOCKED"), x0 + 160, y, 0xEF9A9A, false);
            }
            y += 22;
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
