package net.goui.cosmicdungeon.vendor;

import net.goui.cosmicdungeon.economy.CurrencyAmount;
import net.goui.cosmicdungeon.faction.FactionDefinitions;
import net.goui.cosmicdungeon.faction.FactionService;
import net.goui.cosmicdungeon.faction.FactionTier;
import net.goui.cosmicdungeon.progression.ProgressionService;
import net.goui.cosmicdungeon.playerclass.api.ClassItemEquipmentGuard;
import net.minecraft.server.level.ServerPlayer;

public final class VendorMenuState {
    private VendorMenuState() {}

    public record UnlockResult(boolean unlocked, String reason) {}

    public static UnlockResult unlockState(ServerPlayer sp, VendorProfile profile) {
        VendorAccessService.AccessResult result = VendorAccessService.evaluate(sp, profile);
        return new UnlockResult(result.allowed(), result.allowed() ? "" : result.message());
    }

    public static boolean isOfferUnlocked(ServerPlayer sp, VendorProfile profile, VendorOffer offer) {
        if (!net.goui.cosmicdungeon.faction.NpcFactionService.canRetail(sp)) return false;
        if (net.goui.cosmicdungeon.config.VendorPricesConfig.retail(profile.id(), offer) < 0) return false;
        if (VendorStock.exhausted(sp, profile, offer)) return false;
        if (offer.requiredClasses() != null && !offer.requiredClasses().isEmpty()) {
            String playerClass = ClassItemEquipmentGuard.getPlayerClass(sp);
            if (!offer.requiredClasses().contains(playerClass)) return false;
        }
        if (offer.requiredProgressionFlag() != null) {
            if (offer.requiredProgressionFlag().equalsIgnoreCase("village_access")) {
                if (!ProgressionService.hasVillageAccess(sp)) return false;
            } else {
                return false;
            }
        }
        if (offer.requiredNpcTier() != null) {
            if (profile == null || VendorAccessService.npcTierForSystem(sp, profile.requiredNpcSystem())
                    < offer.requiredNpcTier()) return false;
        }
        if (offer.requiredFactionTier() != null) {
            if (profile == null || profile.requiredFactionId() == null) return false;
            if (FactionDefinitions.get(profile.requiredFactionId()) == null) return false;
            FactionTier need = VendorAccessService.factionTierFromOrdinal(offer.requiredFactionTier());
            if (need == null || !FactionService.hasAtLeast(sp, profile.requiredFactionId(), need)) return false;
        }
        return true;
    }

    public static String formatCost(VendorOffer offer) {
        return CurrencyAmount.of(offer.cost().amount(), offer.cost().denomination()).formatNormalized();
    }
}
