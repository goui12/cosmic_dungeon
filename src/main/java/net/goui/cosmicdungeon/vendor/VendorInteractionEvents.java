package net.goui.cosmicdungeon.vendor;

import net.goui.cosmicdungeon.economy.CurrencyService;
import net.goui.cosmicdungeon.faction.FactionService;
import net.goui.cosmicdungeon.faction.FactionTier;
import net.goui.cosmicdungeon.progression.ProgressionService;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class VendorInteractionEvents {
    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Villager villager)) return;
        ResourceLocation profileId = VendorAssignmentService.getProfileId(villager);
        if (profileId == null) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        event.setCanceled(true);

        VendorProfile profile = VendorProfileManager.INSTANCE.get(profileId);
        if (profile == null) {
            sp.sendSystemMessage(Component.literal("Vendor shell has unknown profile: " + profileId));
            return;
        }

        boolean unlocked = isUnlocked(sp, profile);
        String balance = CurrencyService.getFormattedBalance(sp);
        sp.sendSystemMessage(Component.literal("Vendor: " + profile.displayName()
                + " | profile=" + profile.id()
                + " | balance=" + balance
                + " | status=" + (unlocked ? "unlocked" : "locked")));
    }

    private static boolean isUnlocked(ServerPlayer sp, VendorProfile profile) {
        boolean factionOk = true;
        if (profile.requiredFactionId() != null && profile.requiredFactionTier() != null) {
            FactionTier need = factionTierFromOrdinal(profile.requiredFactionTier());
            factionOk = need != null && FactionService.hasAtLeast(sp, profile.requiredFactionId(), need);
        }

        boolean villageOk = true;
        if (profile.requiredProgressionFlag() != null && profile.requiredProgressionFlag().equalsIgnoreCase("village_access")) {
            villageOk = ProgressionService.hasVillageAccess(sp);
        } else if (profile.requiredProgressionFlag() != null) {
            // TODO: map additional requiredProgressionFlag values to concrete ProgressionService checks.
            villageOk = false;
        }

        boolean npcTierOk = true;
        if (profile.requiredNpcTier() != null) {
            int d1Tier = ProgressionService.getD1NpcUnlockTier(sp);
            int d2Tier = ProgressionService.getD2NpcUnlockTier(sp);
            npcTierOk = Math.max(d1Tier, d2Tier) >= profile.requiredNpcTier();
        }

        return factionOk && villageOk && npcTierOk;
    }

    private static FactionTier factionTierFromOrdinal(int ordinal) {
        FactionTier[] values = FactionTier.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
    }
}

