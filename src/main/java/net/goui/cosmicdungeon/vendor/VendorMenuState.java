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
        if (profile.requiredFactionId() != null && profile.requiredFactionTier() != null) {
            FactionTier need = factionTierFromOrdinal(profile.requiredFactionTier());
            if (need == null || !FactionService.hasAtLeast(sp, profile.requiredFactionId(), need)) {
                return new UnlockResult(false, "Faction requirement not met.");
            }
        }

        if (profile.requiredProgressionFlag() != null) {
            if (profile.requiredProgressionFlag().equalsIgnoreCase("village_access")) {
                if (!ProgressionService.hasVillageAccess(sp)) {
                    return new UnlockResult(false, "Village access not unlocked.");
                }
            } else {
                return new UnlockResult(false, "Progression requirement not met: " + profile.requiredProgressionFlag());
            }
        }

        if (profile.requiredNpcTier() != null) {
            int d1Tier = ProgressionService.getD1NpcUnlockTier(sp);
            int d2Tier = ProgressionService.getD2NpcUnlockTier(sp);
            if (Math.max(d1Tier, d2Tier) < profile.requiredNpcTier()) {
                return new UnlockResult(false, "NPC tier requirement not met.");
            }
        }

        return new UnlockResult(true, "");
    }

    public static boolean isOfferUnlocked(ServerPlayer sp, VendorOffer offer) {
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
        return true;
    }

    public static String formatCost(VendorOffer offer) {
        return CurrencyAmount.of(offer.cost().amount(), offer.cost().denomination()).formatNormalized();
    }

    private static FactionTier factionTierFromOrdinal(int ordinal) {
        FactionTier[] values = FactionTier.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
    }
}
