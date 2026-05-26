package net.goui.cosmicdungeon.progression;

import net.minecraft.server.level.ServerPlayer;

public final class ProgressionService {
    private ProgressionService() {}

    public static int getD1TorchFlowersBest(ServerPlayer player) {
        if (player == null) return 0;
        return data(player).getD1TorchFlowersBest(player.getUUID());
    }

    public static void setD1TorchFlowersBest(ServerPlayer player, int value) {
        if (player == null) return;
        data(player).setD1TorchFlowersBest(player.getUUID(), value);
    }

    public static void markD1Completed(ServerPlayer player, int torchFlowerCount) {
        if (player == null) return;
        PlayerProgressionData data = data(player);
        data.setD1TorchFlowersBest(player.getUUID(), Math.max(getD1TorchFlowersBest(player), torchFlowerCount));
        if (torchFlowerCount >= 3) {
            data.setD1Completed(player.getUUID(), true);
        }
    }

    public static boolean hasVillageAccess(ServerPlayer player) {
        if (player == null) return false;
        return data(player).isVillageAccessUnlocked(player.getUUID());
    }

    public static void setVillageAccess(ServerPlayer player, boolean value) {
        if (player == null) return;
        data(player).setVillageAccessUnlocked(player.getUUID(), value);
    }

    public static int getLesserBlooms(ServerPlayer player) {
        if (player == null) return 0;
        return data(player).getLesserBlooms(player.getUUID());
    }

    public static void addLesserBlooms(ServerPlayer player, int amount) {
        if (player == null || amount <= 0) return;
        PlayerProgressionData data = data(player);
        data.setLesserBlooms(player.getUUID(), data.getLesserBlooms(player.getUUID()) + amount);
    }

    public static void setLesserBlooms(ServerPlayer player, int amount) {
        if (player == null) return;
        data(player).setLesserBlooms(player.getUUID(), amount);
    }

    public static void roundLesserBloomsOnCompletion(ServerPlayer player) {
        if (player == null) return;
        int current = getLesserBlooms(player);
        int rounded = (current / 5) * 5;
        setLesserBlooms(player, rounded);
    }

    public static int getD1NpcUnlockTier(ServerPlayer player) {
        if (player == null) return 0;
        return data(player).getNpcUnlockTierD1(player.getUUID());
    }

    public static int getCavernResidue(ServerPlayer player) {
        if (player == null) return 0;
        return data(player).getCavernResidue(player.getUUID());
    }

    public static void addCavernResidue(ServerPlayer player, int amount) {
        if (player == null || amount <= 0) return;
        PlayerProgressionData data = data(player);
        data.setCavernResidue(player.getUUID(), data.getCavernResidue(player.getUUID()) + amount);
    }

    public static void setCavernResidue(ServerPlayer player, int amount) {
        if (player == null) return;
        data(player).setCavernResidue(player.getUUID(), amount);
    }

    public static int getD2NpcUnlockTier(ServerPlayer player) {
        if (player == null) return 0;
        return data(player).getNpcUnlockTierD2(player.getUUID());
    }

    private static PlayerProgressionData data(ServerPlayer player) {
        return PlayerProgressionData.get(player.level().getServer());
    }
}
