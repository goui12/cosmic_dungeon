package net.goui.cosmicdungeon.achievement.d1;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.achievement.*;
import net.goui.cosmicdungeon.auth.*;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.util.*;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class D1InstanceAchievements {
    private static final Set<ResourceLocation> SHARED = Set.of(CosmicAchievementIds.SYNCHRONOUS_PEAL,
            CosmicAchievementIds.SIXFOLD_VIGIL, CosmicAchievementIds.CYCLE_OF_RECORDED_SOUND);
    private D1InstanceAchievements() {}
    public static List<UUID> recipients(ServerLevel level, DungeonRunRegistryData.RunRecord run) {
        var result = new ArrayList<UUID>();
        var escrow = DungeonInventoryEscrowData.get(level.getServer());
        var ranks = RankStore.get(level);
        for (UUID id : run.orderedPlayers()) {
            var member = level.getServer().getPlayerList().getPlayer(id);
            boolean outside = escrow.get(run.runId(), id).map(DungeonInventoryEscrowData.Entry::outsideActive).orElse(false);
            if (SharedAchievementRules.eligible(true, run.completionExitedPlayers().contains(id), outside,
                    ranks.getRank(id).isDeveloper(), member != null && member.isSpectator(), member != null,
                    member != null && run.containsDimension(member.level().dimension()))) result.add(id);
        }
        return List.copyOf(result);
    }
    public static boolean hasOnlineParticipant(ServerLevel level, DungeonRunRegistryData.RunRecord run) {
        return recipients(level, run).stream().anyMatch(id -> level.getServer().getPlayerList().getPlayer(id) != null);
    }
    public static void grant(ServerLevel level, DungeonRunRegistryData.RunRecord run, ResourceLocation id) {
        if (!SHARED.contains(id) || run.stateEnum() != DungeonRunState.ACTIVE || !run.dungeonId().equals("dungeon_1")) return;
        var data = D1RunData.get(level.getServer());
        var recipients = recipients(level, run);
        if (!data.creditShared(run.runId(), id.toString(), recipients)) return;
        for (UUID owner : recipients) {
            var member = level.getServer().getPlayerList().getPlayer(owner);
            if (member != null) replay(member);
        }
    }
    private static void replay(ServerPlayer player) {
        if (AccessPolicy.isDeveloper(player)) return;
        for (String value : D1RunData.get(player.level().getServer()).achievementCredits(player.getUUID())) {
            var id = ResourceLocation.tryParse(value);
            if (id != null && (SHARED.contains(id) || id.equals(CosmicAchievementIds.LIBRARIAN_1)))
                CosmicAdvancementUtil.grant(player, id);
        }
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) replay(player);
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) replay(player);
    }
    // D71/D74 interpretation: briefly disconnected members retain instance membership until
    // lifecycle removes them. Village/outside escrow and completed exits do not receive new credit.
    // Earned entitlements survive run cleanup; objective counters do not. No retroactive inference
    // of recipients for legacy already-awarded runs. Licensed restart/offline acceptance remains.
}
