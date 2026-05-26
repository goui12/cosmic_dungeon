package net.goui.cosmicdungeon.achievement.plantflags;

import net.goui.cosmicdungeon.achievement.CosmicAchievementIds;
import net.goui.cosmicdungeon.achievement.CosmicAdvancementUtil;
import net.goui.cosmicdungeon.dungeon.DungeonLifecycleService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class PlantFlagService {
    private PlantFlagService() {}
    public static final long DISCONNECT_COOLDOWN_MILLIS = 5L * 60L * 1000L;

    public static void recordFlagPlanted(ServerPlayer sp, BlockPos pos) {
        if (sp == null || sp.level().isClientSide()) return;
        MinecraftServer server = sp.level().getServer();
        if (server == null) return;
        PlantFlagData data = PlantFlagData.get(server);
        if (!isInsideConfiguredRegion(sp, pos, data)) return;
        data.markPlanted(sp.getUUID());
        completeIfReady(server);
    }

    public static void clearForRun(MinecraftServer server, long runId) {
        if (server == null) return;
        PlantFlagData data = PlantFlagData.get(server);
        data.setRun(runId);
        data.clearPlanted();
        data.setCompleted(false);
    }

    public static List<ServerPlayer> getOnlineEligiblePlayers(MinecraftServer server) {
        if (server == null) return List.of();
        return server.getPlayerList().getPlayers().stream()
                .filter(sp -> DungeonLifecycleService.findActiveRunForPlayer(sp).isPresent())
                .toList();
    }

    public static boolean canComplete(MinecraftServer server) {
        if (server == null) return false;
        PlantFlagData data = PlantFlagData.get(server);
        if (data.completed()) return false;
        if (!isRegionConfigured(data)) return false;
        if ((System.currentTimeMillis() - data.lastDisconnectEpochMillis()) < DISCONNECT_COOLDOWN_MILLIS) return false;

        List<ServerPlayer> online = getOnlineEligiblePlayers(server);
        if (online.isEmpty()) return false;
        Set<UUID> planted = data.planted();
        return online.stream().map(ServerPlayer::getUUID).allMatch(planted::contains);
    }

    public static boolean completeIfReady(MinecraftServer server) {
        if (!canComplete(server)) return false;
        PlantFlagData data = PlantFlagData.get(server);
        List<ServerPlayer> online = getOnlineEligiblePlayers(server);
        for (ServerPlayer sp : online) {
            CosmicAdvancementUtil.grant(sp, CosmicAchievementIds.PLANT_FLAGS);
        }
        server.getPlayerList().broadcastSystemMessage(
                Component.literal("The planted banners stir. JHW answers.").withStyle(ChatFormatting.GOLD),
                false
        );
        // TODO: Replace broadcast-only placeholder with physical JHW summon flow once JHW entity/NPC is implemented.
        data.setCompleted(true);
        return true;
    }

    public static void onPlayerDisconnected(ServerPlayer sp) {
        if (sp == null || sp.level().isClientSide()) return;
        MinecraftServer server = sp.level().getServer();
        if (server == null) return;
        PlantFlagData.get(server).markDisconnectNow();
    }

    public static String statusLine(MinecraftServer server) {
        PlantFlagData data = PlantFlagData.get(server);
        List<ServerPlayer> online = getOnlineEligiblePlayers(server);
        Set<UUID> onlineIds = online.stream().map(ServerPlayer::getUUID).collect(Collectors.toSet());
        long plantedOnline = data.planted().stream().filter(onlineIds::contains).count();
        long cooldownRemaining = Math.max(0L, DISCONNECT_COOLDOWN_MILLIS - (System.currentTimeMillis() - data.lastDisconnectEpochMillis()));
        return "run=" + data.activeRunId() + ", completed=" + data.completed() + ", plantedOnline=" + plantedOnline + "/" + online.size() + ", cooldownMs=" + cooldownRemaining;
    }

    public static boolean isInsideConfiguredRegion(ServerPlayer sp, BlockPos pos, PlantFlagData data) {
        if (sp == null || pos == null || data == null || !isRegionConfigured(data)) return false;
        String dim = sp.level().dimension().location().toString();
        if (!dim.equals(data.regionDimensionId())) return false;
        BlockPos a = data.regionPos1();
        BlockPos b = data.regionPos2();
        int minX = Math.min(a.getX(), b.getX()), maxX = Math.max(a.getX(), b.getX());
        int minY = Math.min(a.getY(), b.getY()), maxY = Math.max(a.getY(), b.getY());
        int minZ = Math.min(a.getZ(), b.getZ()), maxZ = Math.max(a.getZ(), b.getZ());
        return pos.getX() >= minX && pos.getX() <= maxX
                && pos.getY() >= minY && pos.getY() <= maxY
                && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    public static boolean isRegionConfigured(PlantFlagData data) {
        return data != null && data.regionPos1() != null && data.regionPos2() != null && data.regionDimensionId() != null && !data.regionDimensionId().isBlank();
    }
}
