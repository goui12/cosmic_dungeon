package net.goui.cosmicdungeon.achievement.plantflags;

import net.goui.cosmicdungeon.achievement.CosmicAchievementIds;
import net.goui.cosmicdungeon.achievement.CosmicAdvancementUtil;
import net.goui.cosmicdungeon.dungeon.DungeonLifecycleService;
import net.goui.cosmicdungeon.dungeon.DungeonInstanceSlots;
import net.goui.cosmicdungeon.dungeon.DungeonRunRegistryData;
import net.goui.cosmicdungeon.playerclass.api.ClassItemUtil;
import net.goui.cosmicdungeon.playerclass.api.ClassNbtUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlantFlagService {
    private PlantFlagService() {}
    private static final Pattern DUNGEON_NUMBER_PATTERN = Pattern.compile(".*?(\\d+)$");

    public static void recordFlagPlanted(ServerPlayer sp, BlockPos pos, PlantedBannerAttunement attunement) {
        if (sp == null || sp.level().isClientSide() || attunement == null) return;
        MinecraftServer server = sp.level().getServer();
        if (server == null) return;
        PlantFlagData data = PlantFlagData.get(server);
        if (!isInsideConfiguredRegion(sp, pos, data)) return;

        var runOpt = DungeonLifecycleService.findActiveRunForPlayer(sp);
        if (runOpt.isEmpty()) return;
        DungeonRunRegistryData.RunRecord run = runOpt.get();
        if (!run.containsPlayer(sp.getUUID())) return;

        Integer runDungeon = dungeonNumberFromId(run.dungeonId());
        if (runDungeon == null || runDungeon != attunement.dungeon()) {
            sp.sendSystemMessage(Component.literal("This flag is attuned to D" + attunement.dungeon() + ", but this run is " + run.dungeonId() + ".").withStyle(ChatFormatting.RED));
            return;
        }

        String playerClass = ClassNbtUtil.getClassId(sp);
        if (!attunement.classId().equals(playerClass)) {
            sp.sendSystemMessage(Component.literal("This flag is attuned to " + ClassItemUtil.displayNameForClass(attunement.classId())
                    + ", but you are " + ClassItemUtil.displayNameForClass(playerClass) + ".").withStyle(ChatFormatting.RED));
            return;
        }

        data.markPlanted(run.runId(), sp.getUUID());
        sp.sendSystemMessage(Component.literal("Your " + ClassItemUtil.displayNameForClass(attunement.classId()) + " flag has been planted.").withStyle(ChatFormatting.GREEN));
        completeIfReady(server, run.runId());
    }

    public static void initializeRun(MinecraftServer server, long runId) {
        if (server != null) PlantFlagData.get(server).initializeRun(runId);
    }

    public static void clearForRun(MinecraftServer server, long runId) {
        if (server == null) return;
        PlantFlagData data = PlantFlagData.get(server);
        if (runId > 0L) data.clearRun(runId); else data.clearAllRuns();
    }

    public static void clearPlayerForRun(MinecraftServer server, long runId, UUID playerId) {
        if (server == null || runId <= 0L || playerId == null) return;
        PlantFlagData.get(server).clearPlayerFromRun(runId, playerId);
    }

    public static List<ServerPlayer> getOnlineEligiblePlayers(MinecraftServer server, DungeonRunRegistryData.RunRecord run) {
        if (server == null || run == null) return List.of();
        List<ServerPlayer> out = new ArrayList<>();
        for (UUID id : run.orderedPlayers()) {
            ServerPlayer sp = server.getPlayerList().getPlayer(id);
            if (sp != null) out.add(sp);
        }
        return out;
    }

    public static boolean completeIfReady(MinecraftServer server, long runId) {
        if (server == null || runId <= 0L) return false;
        PlantFlagData data = PlantFlagData.get(server);
        if (data.completed(runId) || !isRegionConfigured(data)) return false;
        DungeonRunRegistryData.RunRecord run = DungeonRunRegistryData.get(server).getRun(runId).orElse(null);
        if (run == null || run.stateEnum() != net.goui.cosmicdungeon.dungeon.DungeonRunState.ACTIVE) return false;
        List<ServerPlayer> online = getOnlineEligiblePlayers(server, run);
        if (online.isEmpty()) return false;
        Set<UUID> planted = data.planted(runId);
        if (!online.stream().map(ServerPlayer::getUUID).allMatch(planted::contains)) return false;
        for (ServerPlayer sp : online) CosmicAdvancementUtil.grant(sp, CosmicAchievementIds.PLANT_FLAGS);
        server.getPlayerList().broadcastSystemMessage(Component.literal("The planted banners stir. JHW answers.").withStyle(ChatFormatting.GOLD), false);
        data.setCompleted(runId, true);
        return true;
    }

    public static boolean completeIfReady(MinecraftServer server) {
        boolean any = false;
        if (server == null) return false;
        for (DungeonRunRegistryData.RunRecord run : DungeonRunRegistryData.get(server).listAllRuns()) any |= completeIfReady(server, run.runId());
        return any;
    }

    public static String statusLine(MinecraftServer server) {
        PlantFlagData data = PlantFlagData.get(server);
        StringBuilder sb = new StringBuilder("region=").append(isRegionConfigured(data) ? data.regionDimensionId() + " " + data.regionPos1() + ".." + data.regionPos2() : "missing");
        for (DungeonRunRegistryData.RunRecord run : DungeonRunRegistryData.get(server).listAllRuns()) {
            if (run.stateEnum() != net.goui.cosmicdungeon.dungeon.DungeonRunState.ACTIVE) continue;
            List<ServerPlayer> online = getOnlineEligiblePlayers(server, run);
            Set<UUID> planted = data.planted(run.runId());
            Set<UUID> onlineIds = new HashSet<>(online.stream().map(ServerPlayer::getUUID).toList());
            List<String> missing = online.stream().filter(p -> !planted.contains(p.getUUID())).map(p -> p.getName().getString()).toList();
            long plantedOnline = planted.stream().filter(onlineIds::contains).count();
            sb.append(" | run ").append(run.runId()).append(" ").append(run.dungeonId()).append(": plantedOnline=").append(plantedOnline).append('/').append(online.size()).append(", completed=").append(data.completed(run.runId())).append(", missing=").append(missing);
        }
        return sb.toString();
    }

    public static boolean isInsideConfiguredRegion(ServerPlayer sp, BlockPos pos, PlantFlagData data) {
        if (sp == null || pos == null || data == null || !isRegionConfigured(data)) return false;
        String dimensionId = DungeonInstanceSlots.templateDimensionForPhysical(
                sp.level().getServer(), sp.level().dimension()).location().toString();
        if (!dimensionId.equals(data.regionDimensionId())) return false;
        BlockPos a = data.regionPos1(), b = data.regionPos2();
        return pos.getX() >= Math.min(a.getX(), b.getX()) && pos.getX() <= Math.max(a.getX(), b.getX())
                && pos.getY() >= Math.min(a.getY(), b.getY()) && pos.getY() <= Math.max(a.getY(), b.getY())
                && pos.getZ() >= Math.min(a.getZ(), b.getZ()) && pos.getZ() <= Math.max(a.getZ(), b.getZ());
    }

    public static boolean isRegionConfigured(PlantFlagData data) {
        return data != null && data.regionPos1() != null && data.regionPos2() != null && data.regionDimensionId() != null && !data.regionDimensionId().isBlank();
    }

    private static Integer dungeonNumberFromId(String dungeonId) {
        if (dungeonId == null) return null;
        Matcher m = DUNGEON_NUMBER_PATTERN.matcher(dungeonId);
        if (!m.matches()) return null;
        try { return Integer.parseInt(m.group(1)); } catch (NumberFormatException ex) { return null; }
    }
}
