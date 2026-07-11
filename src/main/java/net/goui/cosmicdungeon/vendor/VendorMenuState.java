package net.goui.cosmicdungeon.vendor;

import net.goui.cosmicdungeon.economy.CurrencyAmount;
import net.goui.cosmicdungeon.faction.FactionService;
import net.goui.cosmicdungeon.faction.FactionTier;
import net.goui.cosmicdungeon.progression.ProgressionService;
import net.minecraft.server.level.ServerPlayer;

public final class VendorMenuState {
    private VendorMenuState() {}

    public record UnlockResult(boolean unlocked, String reason) {}

    public static UnlockResult unlockState(ServerPlayer sp, VendorProfile profile) {
        VendorAccessService.AccessResult result = VendorAccessService.evaluate(sp, profile);
        return new UnlockResult(result.allowed(), result.allowed() ? "" : result.message());
    }

    public static boolean isOfferUnlocked(ServerPlayer sp, VendorProfile profile, VendorOffer offer) {
        if (offer.requiredProgressionFlag() != null) {
            if (offer.requiredProgressionFlag().equalsIgnoreCase("village_access")) {
                if (!ProgressionService.hasVillageAccess(sp)) return false;
            } else {
                return false;
            }
        }
        if (offer.requiredNpcTier() != null) {
            int d1Tier = ProgressionService.getD1NpcUnlockTier(sp);
            int d2Tier = ProgressionService.getD2NpcUnlockTier(sp);
            if (Math.max(d1Tier, d2Tier) < offer.requiredNpcTier()) return false;
        }
        if (offer.requiredFactionTier() != null) {
            if (profile == null || profile.requiredFactionId() == null) return false;
            FactionTier need = VendorAccessService.factionTierFromOrdinal(offer.requiredFactionTier());
            if (need == null || !FactionService.hasAtLeast(sp, profile.requiredFactionId(), need)) return false;
        }
        return true;
    }

    public static String formatCost(VendorOffer offer) {
        return CurrencyAmount.of(offer.cost().amount(), offer.cost().denomination()).formatNormalized();
    }
}
