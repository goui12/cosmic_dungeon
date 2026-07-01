package net.goui.cosmicdungeon.achievement;

import net.goui.cosmicdungeon.network.ModNetwork;
import net.goui.cosmicdungeon.network.TradePayloads;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class TradeAchievementService {
    private TradeAchievementService() {}

    public static void syncPromptState(ServerPlayer player) {
        if (player == null) return;
        ModNetwork.sendTo(player, new TradePayloads.S2C_TradePromptState(hasCompletedFirstTrade(player)));
    }

    public static void onSuccessfulTrade(ServerPlayer first, ServerPlayer second) {
        grantAndSync(first);
        grantAndSync(second);
    }

    private static void grantAndSync(ServerPlayer player) {
        if (player == null) return;
        CosmicAdvancementUtil.grant(player, CosmicAchievementIds.FIRST_PLAYER_TRADE);
        syncPromptState(player);
    }

    private static boolean hasCompletedFirstTrade(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return false;
        AdvancementHolder holder = server.getAdvancements().get(CosmicAchievementIds.FIRST_PLAYER_TRADE);
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }
}
