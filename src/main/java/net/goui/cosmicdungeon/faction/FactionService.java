package net.goui.cosmicdungeon.faction;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class FactionService {
    private FactionService() {}

    public static int getValue(ServerPlayer player, ResourceLocation factionId) {
        if (player == null || factionId == null) return 0;
        return PlayerFactionData.get(player.level().getServer()).getValue(player.getUUID(), factionId);
    }

    public static void setValue(ServerPlayer player, ResourceLocation factionId, int value) {
        if (player == null || factionId == null) return;
        PlayerFactionData.get(player.level().getServer()).setValue(player.getUUID(), factionId, value);
    }

    public static int adjust(ServerPlayer player, ResourceLocation factionId, int delta, String reason) {
        if (player == null || factionId == null) return 0;
        int next = getValue(player, factionId) + delta;
        setValue(player, factionId, next);
        return getValue(player, factionId);
    }

    public static FactionTier getTier(ServerPlayer player, ResourceLocation factionId) {
        FactionDefinition definition = FactionDefinitions.get(factionId);
        if (definition == null) return FactionTier.INDIFFERENT;
        return definition.tierFor(getValue(player, factionId));
    }

    public static boolean hasAtLeast(ServerPlayer player, ResourceLocation factionId, FactionTier tier) {
        if (tier == null) return false;
        return getTier(player, factionId).ordinal() >= tier.ordinal();
    }
}
