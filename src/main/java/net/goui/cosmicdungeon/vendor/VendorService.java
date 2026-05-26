package net.goui.cosmicdungeon.vendor;

import net.goui.cosmicdungeon.economy.CurrencyDenomination;
import net.goui.cosmicdungeon.economy.CurrencyService;
import net.goui.cosmicdungeon.economy.pricing.VendorPrice;
import net.goui.cosmicdungeon.economy.pricing.VendorPricingService;
import net.goui.cosmicdungeon.network.VendorPayloads;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class VendorService {
    private VendorService() {}

    public static VendorPayloads.S2C_OpenVendor buildOpenPayload(ServerPlayer sp, Villager villager, VendorProfile profile) {
        Set<String> unlocked = new HashSet<>();
        for (VendorOffer offer : profile.buyOffers()) {
            if (VendorMenuState.isOfferUnlocked(sp, offer)) unlocked.add(offer.id().toString());
        }
        return new VendorPayloads.S2C_OpenVendor(villager.getId(), profile.id().toString(), profile.displayName(), CurrencyService.getBalanceTrace(sp), java.util.List.copyOf(unlocked));
    }

    public static VendorPayloads.S2C_VendorPurchaseResult tryPurchase(ServerPlayer sp, int vendorEntityId, String offerIdRaw) {
        VendorContext context = validateVendor(sp, vendorEntityId);
        if (!context.ok()) return fail(sp, context.failMessage());
        VendorProfile profile = context.profile();

        Optional<VendorOffer> offerOpt = profile.buyOffers().stream().filter(o -> o.id().toString().equals(offerIdRaw)).findFirst();
        if (offerOpt.isEmpty()) return fail(sp, "Offer not found.");
        VendorOffer offer = offerOpt.get();

        if (!VendorMenuState.isOfferUnlocked(sp, offer)) return fail(sp, "Offer locked.");

        long traceCost = CurrencyDenomination.toTrace(offer.cost().amount(), offer.cost().denomination());
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
        sp.sendSystemMessage(Component.literal("Purchased " + toGive.getHoverName().getString() + " for " + VendorMenuState.formatCost(offer) + "."));
        return new VendorPayloads.S2C_VendorPurchaseResult(true, "Purchase complete.", newBalance);
    }

    public static VendorPayloads.S2C_VendorPurchaseResult trySellSlot(ServerPlayer sp, int vendorEntityId, int slotIndex) {
        VendorContext context = validateVendor(sp, vendorEntityId);
        if (!context.ok()) return fail(sp, context.failMessage());

        if (slotIndex < 0 || slotIndex >= sp.getInventory().getContainerSize()) {
            return fail(sp, "This vendor will not buy that.");
        }
        ItemStack stack = sp.getInventory().getItem(slotIndex);
        if (stack.isEmpty()) return fail(sp, "This vendor will not buy that.");

        VendorPrice price = VendorPricingService.getSellValue(stack, context.vendorType());
        if (price.traceValue() <= 0L) return fail(sp, "This vendor will not buy that.");
        if (!CurrencyService.canDeposit(sp, price.traceValue())) return fail(sp, "You do not have enough currency capacity.");

        ItemStack soldStack = stack.copy();
        sp.getInventory().setItem(slotIndex, ItemStack.EMPTY);

        if (!CurrencyService.tryDeposit(sp, price.traceValue())) {
            sp.getInventory().setItem(slotIndex, soldStack);
            return fail(sp, "You do not have enough currency capacity.");
        }

        String soldName = soldStack.getHoverName().getString();
        sp.sendSystemMessage(Component.literal("Sold " + soldName + " for " + price.traceValue() + " Trace."));
        return new VendorPayloads.S2C_VendorPurchaseResult(true, "Sale complete.", CurrencyService.getBalanceTrace(sp));
    }

    public static VendorPayloads.S2C_VendorPurchaseResult trySellDetectedSet(ServerPlayer sp, int vendorEntityId, String setId) {
        VendorContext context = validateVendor(sp, vendorEntityId);
        if (!context.ok()) return fail(sp, context.failMessage());

        var setDefOpt = VendorPricingService.findSetDefinition(context.vendorType(), setId);
        if (setDefOpt.isEmpty()) return fail(sp, "This vendor will not buy that.");
        var setDef = setDefOpt.get();

        List<Integer> slotsToRemove = new ArrayList<>();
        for (String itemId : setDef.pieceItemIds()) {
            int slot = findInventorySlotByItemId(sp, itemId, slotsToRemove);
            if (slot < 0) return fail(sp, "The full set is incomplete.");
            slotsToRemove.add(slot);
        }

        long payout = setDef.fullSetTraceValue();
        if (payout <= 0L) return fail(sp, "This vendor will not buy that.");
        if (!CurrencyService.canDeposit(sp, payout)) return fail(sp, "You do not have enough currency capacity.");

        List<ItemStack> removed = new ArrayList<>();
        for (int slot : slotsToRemove) {
            ItemStack original = sp.getInventory().getItem(slot);
            removed.add(original.copy());
            sp.getInventory().setItem(slot, ItemStack.EMPTY);
        }

        if (!CurrencyService.tryDeposit(sp, payout)) {
            for (int i = 0; i < slotsToRemove.size(); i++) {
                sp.getInventory().setItem(slotsToRemove.get(i), removed.get(i));
            }
            return fail(sp, "You do not have enough currency capacity.");
        }

        sp.sendSystemMessage(Component.literal("Sold " + setDef.id() + " for " + payout + " Trace."));
        return new VendorPayloads.S2C_VendorPurchaseResult(true, "Sale complete.", CurrencyService.getBalanceTrace(sp));
    }

    private static int findInventorySlotByItemId(ServerPlayer sp, String itemId, List<Integer> excludedSlots) {
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
            if (excludedSlots.contains(i)) continue;
            ItemStack stack = sp.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                ResourceLocation key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (key != null && itemId.equals(key.toString())) return i;
            }
        }
        return -1;
    }

    private static VendorContext validateVendor(ServerPlayer sp, int vendorEntityId) {
        if (!(sp.level().getEntity(vendorEntityId) instanceof Villager villager)) {
            return VendorContext.fail("Vendor no longer exists.");
        }
        if (sp.distanceToSqr(villager) > 64.0D) return VendorContext.fail("Too far from vendor.");

        ResourceLocation profileId = VendorAssignmentService.getProfileId(villager);
        if (profileId == null) return VendorContext.fail("Vendor is not assigned.");

        VendorProfile profile = VendorProfileManager.INSTANCE.get(profileId);
        if (profile == null) return VendorContext.fail("Vendor profile missing.");

        VendorMenuState.UnlockResult vendorUnlocked = VendorMenuState.unlockState(sp, profile);
        if (!vendorUnlocked.unlocked()) return VendorContext.fail("Vendor locked: " + vendorUnlocked.reason());
        if (profile.buyback() == null) return VendorContext.fail("This vendor will not buy that.");

        return VendorContext.ok(profile);
    }

    private static VendorPayloads.S2C_VendorPurchaseResult fail(ServerPlayer sp, String msg) {
        sp.sendSystemMessage(Component.literal(msg));
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
