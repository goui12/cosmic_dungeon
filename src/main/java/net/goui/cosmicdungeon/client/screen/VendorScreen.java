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
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Locale;

public final class VendorScreen extends AbstractContainerScreen<VendorMenu> {
    private static final int OFFER_ROW_TOP_OFFSET = 62;
    private static final int OFFER_ROW_HEIGHT = 22;
    private static final int OFFERS_PER_PAGE = 5;
    private static final int OFFER_ITEM_SIZE = 16;
    private static final int SELLABLE_TABLE_X_OFFSET = 238;
    private static final int SELLABLE_TABLE_TOP_OFFSET = 62;
    private static final int SELLABLE_ROW_HEIGHT = 18;
    private static final int SELLABLE_ROWS = 6;
    private static final int SELLABLE_ITEM_NAME_X_OFFSET = 20;
    private static final int SELLABLE_VALUE_X_OFFSET = 88;
    private static final int SELLABLE_COLUMN_GAP = 4;

    private final List<Button> offerButtons = new ArrayList<>();
    private final Set<Integer> selectedSellSlots = new HashSet<>();
    private VendorClientState.VendorView renderedView;
    private int offerPage;
    private Button sellSelectedButton;
    private Button sellAllButton;

    public VendorScreen(VendorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 360;
        this.imageHeight = 258;
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
                    .bounds(x0 + 126, topPos + 39, 20, 18)
                    .build();
            previous.active = offerPage > 0;
            addRenderableWidget(previous);

            Button next = Button.builder(Component.literal(">"), btn -> changeOfferPage(1))
                    .bounds(x0 + 150, topPos + 39, 20, 18)
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
            }).bounds(x0 + 160, y - 6, 46, 20).build();
            b.active = unlocked;
            addRenderableWidget(b);
            offerButtons.add(b);
            y += OFFER_ROW_HEIGHT;
        }

        int sellX = leftPos + SELLABLE_TABLE_X_OFFSET;
        int sellY = topPos + imageHeight - 66;
        sellSelectedButton = Button.builder(Component.literal("Sell Selected"), btn -> {
            VendorClientState.VendorView current = view();
            if (current == null || selectedSellSlots.isEmpty()) return;
            ModNetwork.sendToServer(new VendorPayloads.C2S_RequestVendorSellSelected(current.vendorEntityId(), List.copyOf(selectedSellSlots)));
        }).bounds(sellX, sellY, 104, 20).build();
        sellAllButton = Button.builder(Component.literal("Sell All"), btn -> {
            VendorClientState.VendorView current = view();
            if (current == null) return;
            ModNetwork.sendToServer(new VendorPayloads.C2S_RequestVendorSellAll(current.vendorEntityId()));
        }).bounds(sellX, sellY + 34, 104, 20).build();
        addRenderableWidget(sellSelectedButton);
        addRenderableWidget(sellAllButton);
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
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Draw all labels manually so the inherited empty player inventory label is not shown.
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
        g.drawString(font, current != null ? Component.literal(displayTitle(current)) : title, x0, y, 0xFFFFFFFF, false);
        y += 12;
        if (current == null) {
            g.drawString(font, Component.literal("Loading vendor data..."), x0, y, 0xFFB0BEC5, false);
            return;
        }

        g.drawString(font, Component.literal(displayStoreName(current)), x0, y, 0xFFB39DDB, false);
        y += 20;

        int pageCount = pageCount();
        g.drawString(font, Component.literal("Vendor Selling:"), x0, y, 0xFFB0BEC5, false);
        if (pageCount > 1) {
            String pageText = (offerPage + 1) + "/" + pageCount;
            g.drawString(font, Component.literal(pageText), x0 + 128 + (42 - font.width(pageText)) / 2, topPos + 30, 0xFFB0BEC5, false);
        }

        if (current.offers().isEmpty()) {
            g.drawString(font, Component.literal("This vendor has no offers."), x0, topPos + OFFER_ROW_TOP_OFFSET, 0xFFB0BEC5, false);
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
            String cost = formatRawCost(offer.costAmount(), denomination);
            int itemX = x0;
            int itemY = y - 4;
            drawBorder(g, x0 - 3, y - 6, x0 + 209, y + 16, unlocked ? 0xFF5F6370 : 0xFF7A4550);
            g.renderItem(offer.stack(), itemX, itemY);
            g.renderItemDecorations(font, offer.stack(), itemX, itemY);
            g.drawString(font, Component.literal(font.plainSubstrByWidth(itemName, 76)), x0 + 22, y, 0xFFFFFFFF, false);
            g.drawString(font, Component.literal(cost), x0 + 105, y, 0xFFA5D6A7, false);
            if (!unlocked) g.drawString(font, Component.literal("LOCKED"), x0 + 160, y, 0xFFEF9A9A, false);
            if (isHoveringItemStack(mouseX, mouseY, itemX, itemY, offer.stack())) {
                hoveredOfferStack = offer.stack();
            }
            y += OFFER_ROW_HEIGHT;
        }

        if (!hoveredOfferStack.isEmpty()) {
            g.setTooltipForNextFrame(font, hoveredOfferStack, mouseX, mouseY);
        }

        renderSellableInventoryPreview(g, mouseX, mouseY, current);
        long selectedPreview = previewSelectedPayout(current);
        long allPreview = previewAllPayout(current);
        int footerX = leftPos + SELLABLE_TABLE_X_OFFSET;
        g.drawString(font, Component.literal("Selected Value: " + CurrencyAmount.ofTrace(selectedPreview).formatNormalized()), footerX, topPos + imageHeight - 77, 0xFF90CAF9, false);
        g.drawString(font, Component.literal("Inventory Value: " + CurrencyAmount.ofTrace(allPreview).formatNormalized()), footerX, topPos + imageHeight - 43, 0xFF90CAF9, false);

        String balance = "Balance: " + CurrencyAmount.ofTrace(current.balanceTrace()).formatNormalized();
        g.drawString(font, Component.literal(balance), footerX, topPos + imageHeight - 10, 0xFFFFE082, false);
    }

    private void renderSellableInventoryPreview(GuiGraphics g, int mouseX, int mouseY, VendorClientState.VendorView current) {
        int tableX = leftPos + SELLABLE_TABLE_X_OFFSET;
        int tableY = topPos + SELLABLE_TABLE_TOP_OFFSET;
        g.drawString(font, Component.literal("Player Inventory"), tableX, topPos + 40, 0xFFB0BEC5, false);

        if (minecraft == null || minecraft.player == null) {
            g.drawString(font, Component.literal("Inventory unavailable"), tableX, tableY, 0xFFB0BEC5, false);
            return;
        }

        List<SellableStackView> sellable = new ArrayList<>();
        var inventory = minecraft.player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) continue;
            var price = VendorPricingService.getSellValue(stack, current.pricingGroup());
            if (price.traceValue() > 0L) sellable.add(new SellableStackView(slot, stack, price.traceValue()));
        }

        if (sellable.isEmpty()) {
            g.drawString(font, Component.literal("No sellable items found"), tableX, tableY, 0xFFB0BEC5, false);
            updateSellButtons(0L, 0L);
            return;
        }

        selectedSellSlots.removeIf(slot -> sellable.stream().noneMatch(v -> v.slotIndex() == slot));
        long selectedPayout = sellable.stream().filter(v -> selectedSellSlots.contains(v.slotIndex())).mapToLong(SellableStackView::traceValue).sum();
        long allPayout = sellable.stream().mapToLong(SellableStackView::traceValue).sum();
        updateSellButtons(selectedPayout, allPayout);

        ItemStack hoveredSellableStack = ItemStack.EMPTY;
        int rows = Math.min(SELLABLE_ROWS, sellable.size());
        for (int i = 0; i < rows; i++) {
            SellableStackView sellableStack = sellable.get(i);
            int rowY = tableY + i * SELLABLE_ROW_HEIGHT;
            g.fill(tableX - 2, rowY - 2, leftPos + imageWidth - 8, rowY + SELLABLE_ROW_HEIGHT - 2, 0x502B2B36);
            if (selectedSellSlots.contains(sellableStack.slotIndex())) drawBorder(g, tableX - 1, rowY - 4, tableX + 17, rowY + 14, 0xFFFFFFFF);
            g.renderItem(sellableStack.stack(), tableX, rowY - 3);
            g.renderItemDecorations(font, sellableStack.stack(), tableX, rowY - 3);
            int nameX = tableX + SELLABLE_ITEM_NAME_X_OFFSET;
            int valueX = tableX + SELLABLE_VALUE_X_OFFSET;
            int nameColumnWidth = valueX - nameX - SELLABLE_COLUMN_GAP;
            String itemName = truncateWithEllipsis(sellableStack.stack().getHoverName().getString(), nameColumnWidth);
            g.drawString(font, Component.literal(itemName), nameX, rowY + 1, 0xFFFFFFFF, false);
            g.drawString(font, Component.literal(CurrencyAmount.ofTrace(sellableStack.traceValue()).formatNormalized()), valueX, rowY + 1, 0xFF90CAF9, false);
            if (isHoveringItemStack(mouseX, mouseY, tableX, rowY - 3, sellableStack.stack())) {
                hoveredSellableStack = sellableStack.stack();
            }
        }
        if (sellable.size() > SELLABLE_ROWS) {
            g.drawString(font, Component.literal("+" + (sellable.size() - SELLABLE_ROWS) + " more sellable stacks"), tableX, tableY + SELLABLE_ROWS * SELLABLE_ROW_HEIGHT + 2, 0xFFB0BEC5, false);
        }
        if (!hoveredSellableStack.isEmpty()) {
            g.setTooltipForNextFrame(font, hoveredSellableStack, mouseX, mouseY);
        }
    }

    private String truncateWithEllipsis(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;

        String ellipsis = "...";
        int ellipsisWidth = font.width(ellipsis);
        if (maxWidth <= ellipsisWidth) return font.plainSubstrByWidth(ellipsis, maxWidth);

        String trimmed = text;
        while (!trimmed.isEmpty()) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
            String candidate = trimmed + ellipsis;
            if (font.width(candidate) <= maxWidth) return candidate;
        }
        return ellipsis;
    }

    private long previewSelectedPayout(VendorClientState.VendorView current) {
        return collectSellable(current).stream().filter(v -> selectedSellSlots.contains(v.slotIndex())).mapToLong(SellableStackView::traceValue).sum();
    }

    private long previewAllPayout(VendorClientState.VendorView current) {
        return collectSellable(current).stream().mapToLong(SellableStackView::traceValue).sum();
    }

    private List<SellableStackView> collectSellable(VendorClientState.VendorView current) {
        List<SellableStackView> sellable = new ArrayList<>();
        if (minecraft == null || minecraft.player == null) return sellable;
        var inventory = minecraft.player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) continue;
            var price = VendorPricingService.getSellValue(stack, current.pricingGroup());
            if (price.traceValue() > 0L) sellable.add(new SellableStackView(slot, stack, price.traceValue()));
        }
        return sellable;
    }

    private void updateSellButtons(long selectedPayout, long allPayout) {
        if (sellSelectedButton != null) sellSelectedButton.active = selectedPayout > 0L;
        if (sellAllButton != null) sellAllButton.active = allPayout > 0L;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int button = event.button();
        double mouseX = event.x();
        double mouseY = event.y();
        VendorClientState.VendorView current = view();
        if (button == 0 && current != null) {
            SellableStackView clicked = sellableAt(mouseX, mouseY, current);
            if (clicked != null) {
                if (!selectedSellSlots.add(clicked.slotIndex())) selectedSellSlots.remove(clicked.slotIndex());
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private SellableStackView sellableAt(double mouseX, double mouseY, VendorClientState.VendorView current) {
        if (minecraft == null || minecraft.player == null) return null;
        int tableX = leftPos + SELLABLE_TABLE_X_OFFSET;
        int tableY = topPos + SELLABLE_TABLE_TOP_OFFSET;
        var inventory = minecraft.player.getInventory();
        int row = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) continue;
            var price = VendorPricingService.getSellValue(stack, current.pricingGroup());
            if (price.traceValue() <= 0L) continue;
            if (row < SELLABLE_ROWS) {
                int rowY = tableY + row * SELLABLE_ROW_HEIGHT;
                if (mouseX >= tableX && mouseX < tableX + OFFER_ITEM_SIZE && mouseY >= rowY - 3 && mouseY < rowY - 3 + OFFER_ITEM_SIZE) {
                    return new SellableStackView(slot, stack, price.traceValue());
                }
            }
            row++;
        }
        return null;
    }

    private static void drawBorder(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1, y1, x2, y1 + 1, color);
        g.fill(x1, y2 - 1, x2, y2, color);
        g.fill(x1, y1, x1 + 1, y2, color);
        g.fill(x2 - 1, y1, x2, y2, color);
    }

    private static String formatRawCost(long amount, CurrencyDenomination denomination) {
        if (denomination == null) return amount + " Trace";
        String name = denomination.id().isEmpty() ? "trace" : denomination.id();
        return amount + " " + Character.toUpperCase(name.charAt(0)) + name.substring(1).toLowerCase(Locale.ROOT);
    }

    private static String displayTitle(VendorClientState.VendorView current) {
        if (current.title() != null && !current.title().isBlank()) return current.title();
        return VendorClientState.descriptorFromProfileId(current.profileId());
    }

    private static String displayStoreName(VendorClientState.VendorView current) {
        if (current.storeDisplayName() != null && !current.storeDisplayName().isBlank()) return current.storeDisplayName();
        String descriptor = VendorClientState.descriptorFromProfileId(current.profileId());
        return descriptor.equals("Vendor") ? "Vendor Store" : descriptor + " Store";
    }

    public static void clearSelectionsIfOpen() {
        if (net.minecraft.client.Minecraft.getInstance().screen instanceof VendorScreen screen) {
            screen.selectedSellSlots.clear();
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

        public static String descriptorFromProfileId(ResourceLocation profileId) {
            if (profileId == null) return "Vendor";
            String path = profileId.getPath();
            int slash = path.lastIndexOf('/');
            String raw = slash >= 0 ? path.substring(slash + 1) : path;
            String[] words = raw.split("[_\\s-]+");
            StringBuilder descriptor = new StringBuilder();
            for (String word : words) {
                if (word.isBlank()) continue;
                if (!descriptor.isEmpty()) descriptor.append(' ');
                descriptor.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) descriptor.append(word.substring(1).toLowerCase());
            }
            return descriptor.isEmpty() ? "Vendor" : descriptor.toString();
        }

        public record VendorView(int vendorEntityId, ResourceLocation profileId, String title, String storeDisplayName, long balanceTrace, String pricingGroup, List<VendorPayloads.S2C_OpenVendor.OfferView> offers, java.util.Set<String> unlockedOffers) {}
    }

    private record SellableStackView(int slotIndex, ItemStack stack, long traceValue) {}
}
