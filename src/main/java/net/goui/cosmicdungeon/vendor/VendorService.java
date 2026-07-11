package net.goui.cosmicdungeon.vendor;

import net.goui.cosmicdungeon.economy.CurrencyService;
import net.goui.cosmicdungeon.economy.pricing.VendorPrice;
import net.goui.cosmicdungeon.economy.pricing.VendorPricingService;
import net.goui.cosmicdungeon.network.VendorPayloads;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class VendorService {
    private VendorService() {}

    public static VendorPayloads.S2C_OpenVendor buildOpenPayload(ServerPlayer sp, Entity vendor, VendorProfile profile) {
        Set<String> unlocked = new HashSet<>();
        List<VendorPayloads.S2C_OpenVendor.OfferView> offers = new ArrayList<>();
        for (VendorOffer offer : profile.buyOffers()) {
            if (VendorMenuState.isOfferUnlocked(sp, profile, offer)) unlocked.add(offer.id().toString());
            offers.add(new VendorPayloads.S2C_OpenVendor.OfferView(
                    offer.id().toString(),
                    offer.result().copy(),
                    offer.result().getHoverName().getString(),
                    offer.result().getCount(),
                    offer.cost().amount(),
                    offer.cost().denomination().id()
            ));
        }
        String pricingGroup = profile.buyback() != null && profile.buyback().pricingGroup() != null && !profile.buyback().pricingGroup().isBlank()
                ? profile.buyback().pricingGroup()
                : "default";
        return new VendorPayloads.S2C_OpenVendor(vendor.getId(), profile.id().toString(), vendorDisplayName(vendor, profile), profile.storeDisplayName(), CurrencyService.getBalanceTrace(sp), pricingGroup, List.copyOf(offers), List.copyOf(unlocked));
    }

    private static String vendorDisplayName(Entity vendor, VendorProfile profile) {
        Component customName = vendor.getCustomName();
        if (customName != null && !customName.getString().isBlank()) {
            return customName.getString();
        }
        return profile.displayName();
    }

    public static VendorPayloads.S2C_VendorPurchaseResult tryPurchase(ServerPlayer sp, int vendorEntityId, String offerIdRaw) {
        VendorContext context = validateVendor(sp, vendorEntityId, false);
        if (!context.ok()) return fail(sp, context.failMessage());
        VendorProfile profile = context.profile();

        Optional<VendorOffer> offerOpt = profile.buyOffers().stream().filter(o -> o.id().toString().equals(offerIdRaw)).findFirst();
        if (offerOpt.isEmpty()) return fail(sp, "Offer not found.");
        VendorOffer offer = offerOpt.get();

        if (!VendorMenuState.isOfferUnlocked(sp, profile, offer)) return fail(sp, "Offer locked.");

        long traceCost = offer.cost().denomination().toTrace(offer.cost().amount());
        if (traceCost <= 0L) return fail(sp, "Invalid offer cost.");
        if (CurrencyService.getBalanceTrace(sp) < traceCost) return fail(sp, "Not enough attunement fragments.");

        ItemStack toGive = offer.result().copy();
        if (sp.getInventory().getFreeSlot() < 0 && !sp.getInventory().hasAnyMatching(s -> s.isEmpty())) {
            return fail(sp, "Inventory full.");
        }

        if (!CurrencyService.tryWithdraw(sp, traceCost)) return fail(sp, "Purchase failed while deducting currency.");

        boolean delivered = sp.getInventory().add(toGive);
        if (!delivered) {
            CurrencyService.tryDeposit(sp, traceCost);
            return fail(sp, "Inventory full. No currency deducted.");
        }

        long newBalance = CurrencyService.getBalanceTrace(sp);
        sp.sendSystemMessage(Component.literal("Purchased ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(toGive.getHoverName().getString()).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" for ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(VendorMenuState.formatCost(offer)).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(".").withStyle(ChatFormatting.WHITE)));
        return new VendorPayloads.S2C_VendorPurchaseResult(true, "Purchase complete.", newBalance);
    }

    public static VendorPayloads.S2C_VendorPurchaseResult trySellSelected(ServerPlayer sp, int vendorEntityId, List<Integer> slotIndexes) {
        VendorContext context = validateVendor(sp, vendorEntityId, true);
        if (!context.ok()) return fail(sp, context.failMessage());
        if (slotIndexes == null || slotIndexes.isEmpty()) return fail(sp, "No sellable items selected.");

        List<Integer> slotsToSell = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        long payout = 0L;
        for (Integer rawSlot : slotIndexes) {
            if (rawSlot == null || !seen.add(rawSlot)) continue;
            int slot = rawSlot;
            if (slot < 0 || slot >= sp.getInventory().getContainerSize()) return fail(sp, "Selected item is no longer valid.");
            ItemStack stack = sp.getInventory().getItem(slot);
            if (stack.isEmpty()) return fail(sp, "Selected item is no longer available.");
            VendorPrice price = VendorPricingService.getSellValue(stack, context.vendorType());
            if (price.traceValue() <= 0L) return fail(sp, "Selected item is no longer sellable.");
            if (Long.MAX_VALUE - payout < price.traceValue()) return fail(sp, "Sale payout is too large.");
            payout += price.traceValue();
            slotsToSell.add(slot);
        }

        if (slotsToSell.isEmpty() || payout <= 0L) return fail(sp, "No sellable items selected.");
        return commitSale(sp, slotsToSell, payout, "Selected sale complete.");
    }

    public static VendorPayloads.S2C_VendorPurchaseResult trySellAll(ServerPlayer sp, int vendorEntityId) {
        VendorContext context = validateVendor(sp, vendorEntityId, true);
        if (!context.ok()) return fail(sp, context.failMessage());

        List<Integer> slotsToSell = new ArrayList<>();
        long payout = 0L;
        for (int slot = 0; slot < sp.getInventory().getContainerSize(); slot++) {
            ItemStack stack = sp.getInventory().getItem(slot);
            if (stack.isEmpty()) continue;
            VendorPrice price = VendorPricingService.getSellValue(stack, context.vendorType());
            if (price.traceValue() <= 0L) continue;
            if (Long.MAX_VALUE - payout < price.traceValue()) return fail(sp, "Sale payout is too large.");
            payout += price.traceValue();
            slotsToSell.add(slot);
        }

        if (slotsToSell.isEmpty() || payout <= 0L) return fail(sp, "No sellable items found.");
        return commitSale(sp, slotsToSell, payout, "Sell all complete.");
    }

    private static VendorPayloads.S2C_VendorPurchaseResult commitSale(ServerPlayer sp, List<Integer> slotsToSell, long payout, String resultMessage) {
        if (!CurrencyService.canDeposit(sp, payout)) return fail(sp, "You do not have enough currency capacity.");

        List<ItemStack> removed = new ArrayList<>();
        for (int slot : slotsToSell) {
            ItemStack original = sp.getInventory().getItem(slot);
            removed.add(original.copy());
            sp.getInventory().setItem(slot, ItemStack.EMPTY);
        }

        if (!CurrencyService.tryDeposit(sp, payout)) {
            for (int i = 0; i < slotsToSell.size(); i++) {
                sp.getInventory().setItem(slotsToSell.get(i), removed.get(i));
            }
            return fail(sp, "You do not have enough currency capacity.");
        }

        sp.getInventory().setChanged();
        for (int slot : slotsToSell) {
            sp.connection.send(new ClientboundSetPlayerInventoryPacket(slot, sp.getInventory().getItem(slot)));
        }

        sp.sendSystemMessage(Component.literal("Sold ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(slotsToSell.size() + " stack" + (slotsToSell.size() == 1 ? "" : "s")).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" for ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(payout + " Trace").withStyle(ChatFormatting.AQUA))
                .append(Component.literal(".").withStyle(ChatFormatting.WHITE)));
        return new VendorPayloads.S2C_VendorPurchaseResult(true, resultMessage, CurrencyService.getBalanceTrace(sp));
    }

    private static VendorContext validateVendor(ServerPlayer sp, int vendorEntityId, boolean requireBuyback) {
        Entity vendor = sp.level().getEntity(vendorEntityId);
        if (vendor == null) {
            return VendorContext.fail("Vendor no longer exists.");
        }
        if (sp.distanceToSqr(vendor) > 64.0D) return VendorContext.fail("Too far from vendor.");

        ResourceLocation profileId = VendorAssignmentService.getProfileId(vendor);
        if (profileId == null) return VendorContext.fail("Vendor is not assigned.");

        VendorProfile profile = VendorProfileManager.INSTANCE.get(profileId);
        if (profile == null) return VendorContext.fail("Vendor profile missing.");

        VendorMenuState.UnlockResult vendorUnlocked = VendorMenuState.unlockState(sp, profile);
        if (!vendorUnlocked.unlocked()) return VendorContext.fail("Vendor locked: " + vendorUnlocked.reason());
        if (requireBuyback && profile.buyback() == null) return VendorContext.fail("This vendor will not buy that.");

        return VendorContext.ok(profile);
    }

    private static VendorPayloads.S2C_VendorPurchaseResult fail(ServerPlayer sp, String msg) {
        sp.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.RED));
        return new VendorPayloads.S2C_VendorPurchaseResult(false, msg, CurrencyService.getBalanceTrace(sp));
    }

    private record VendorContext(boolean ok, String failMessage, VendorProfile profile) {
        static VendorContext ok(VendorProfile profile) { return new VendorContext(true, "", profile); }
        static VendorContext fail(String message) { return new VendorContext(false, message, null); }
        String vendorType() {
            if (profile == null || profile.buyback() == null || profile.buyback().pricingGroup() == null || profile.buyback().pricingGroup().isBlank()) return "default";
            return profile.buyback().pricingGroup();
        }
    }
}
