package net.goui.cosmicdungeon.vendor;

import net.goui.cosmicdungeon.faction.FactionDefinitions;
import net.goui.cosmicdungeon.faction.FactionService;
import net.goui.cosmicdungeon.faction.FactionTier;
import net.goui.cosmicdungeon.progression.ProgressionService;
import net.minecraft.server.level.ServerPlayer;

public final class VendorAccessService {
    private VendorAccessService() {}

    public record AccessResult(boolean allowed, String message) {}

    public static AccessResult evaluate(ServerPlayer sp, VendorProfile profile) {
        if (sp == null || profile == null) return new AccessResult(false, "Vendor unavailable.");

        if (profile.requiredVillageAccess()) {
            if (!ProgressionService.hasVillageAccess(sp)) {
                return new AccessResult(false, "Village access is required.");
            }
            if (ProgressionService.getD1LesserBloomsBest(sp) < 3) {
                return new AccessResult(false, "Need D1 completion with at least 3 Lesser Blooms.");
            }
        }

        if (profile.requiredNpcTier() != null) {
            int unlocked = npcTierForSystem(sp, profile.requiredNpcSystem());
            if (unlocked < profile.requiredNpcTier()) {
                return new AccessResult(false, "Requires " + (profile.requiredNpcSystem() == null ? "NPC" : profile.requiredNpcSystem().toUpperCase()) + " tier " + profile.requiredNpcTier() + ".");
            }
        }

        if (profile.requiredFactionId() != null && profile.requiredFactionTier() != null) {
            if (FactionDefinitions.get(profile.requiredFactionId()) == null) {
                return new AccessResult(false, "Faction requirement not met.");
            }
            FactionTier need = factionTierFromOrdinal(profile.requiredFactionTier());
            if (need == null || !FactionService.hasAtLeast(sp, profile.requiredFactionId(), need)) {
                return new AccessResult(false, "Faction requirement not met.");
            }
        }

        return new AccessResult(true, "Access granted.");
    }

    private static int npcTierForSystem(ServerPlayer sp, String system) {
        if (system == null || system.isBlank() || system.equalsIgnoreCase("D1")) return ProgressionService.getD1NpcUnlockTier(sp);
        if (system.equalsIgnoreCase("D2")) return ProgressionService.getD2NpcUnlockTier(sp);
        return 0;
    }

    static FactionTier factionTierFromOrdinal(int ordinal) {
        FactionTier[] values = FactionTier.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
    }
}
