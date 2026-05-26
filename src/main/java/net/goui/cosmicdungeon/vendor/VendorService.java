package net.goui.cosmicdungeon.vendor;

import net.goui.cosmicdungeon.economy.CurrencyDenomination;
import net.goui.cosmicdungeon.economy.CurrencyService;
import net.goui.cosmicdungeon.network.VendorPayloads;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
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
        if (!(sp.level().getEntity(vendorEntityId) instanceof Villager villager)) {
            return fail(sp, "Vendor no longer exists.");
        }
        if (sp.distanceToSqr(villager) > 64.0D) return fail(sp, "Too far from vendor.");

        ResourceLocation profileId = VendorAssignmentService.getProfileId(villager);
        if (profileId == null) return fail(sp, "Vendor is not assigned.");

        VendorProfile profile = VendorProfileManager.INSTANCE.get(profileId);
        if (profile == null) return fail(sp, "Vendor profile missing.");

        VendorMenuState.UnlockResult vendorUnlocked = VendorMenuState.unlockState(sp, profile);
        if (!vendorUnlocked.unlocked()) return fail(sp, "Vendor locked: " + vendorUnlocked.reason());

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

    private static VendorPayloads.S2C_VendorPurchaseResult fail(ServerPlayer sp, String msg) {
        sp.sendSystemMessage(Component.literal(msg));
        return new VendorPayloads.S2C_VendorPurchaseResult(false, msg, CurrencyService.getBalanceTrace(sp));
    }
}
