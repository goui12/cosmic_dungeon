package net.goui.cosmicdungeon.dungeon;

import net.goui.cosmicdungeon.economy.CurrencyService;
import net.goui.cosmicdungeon.entity.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class DungeonGroupSplitService {
    private DungeonGroupSplitService() {}

    public static final int MAX_ELIGIBLE_DISTANCE_BLOCKS = 100;
    private static final double MAX_ELIGIBLE_DISTANCE_SQR = MAX_ELIGIBLE_DISTANCE_BLOCKS * MAX_ELIGIBLE_DISTANCE_BLOCKS;
    private static final String COSMIC_SPAWNER_TAG_PREFIX = "cosmic_spawner_";

    public static void onMobKilled(LivingEntity killed) {
        if (!isRewardableDungeonMob(killed)) return;
        if (!(killed.level() instanceof ServerLevel level)) return;

        MinecraftServer server = level.getServer();
        Optional<DungeonRunRegistryData.RunRecord> runOpt = findActiveRunForKilledMob(level);
        if (runOpt.isEmpty()) return;

        long tracePool = tracePoolFor(killed);
        if (tracePool <= 0L) return;

        List<ServerPlayer> eligiblePlayers = eligiblePlayers(server, runOpt.get(), killed);
        if (eligiblePlayers.isEmpty()) return;

        long traceEach = tracePool / eligiblePlayers.size();
        if (traceEach <= 0L) return;

        for (ServerPlayer player : eligiblePlayers) {
            if (CurrencyService.tryDeposit(player, traceEach)) {
                player.sendSystemMessage(Component.literal("Group Split: +" + traceEach + " Trace").withStyle(ChatFormatting.GREEN));
            } else {
                player.sendSystemMessage(Component.literal("Group Split skipped: not enough Trace capacity for +" + traceEach + " Trace.").withStyle(ChatFormatting.RED));
            }
        }
    }

    private static Optional<DungeonRunRegistryData.RunRecord> findActiveRunForKilledMob(ServerLevel level) {
        DungeonRunRegistryData runs = DungeonRunRegistryData.get(level.getServer());
        return runs.listAllRuns().stream()
                .filter(run -> run.stateEnum() == DungeonRunState.ACTIVE)
                .filter(run -> run.containsDimension(level.dimension()))
                .findFirst();
    }

    private static long tracePoolFor(LivingEntity killed) {
        return Math.max(0L, (long) Math.floor(killed.getMaxHealth() / 2.0F));
    }

    private static boolean isRewardableDungeonMob(LivingEntity killed) {
        return killed != null
                && !killed.level().isClientSide()
                && !(killed instanceof Player)
                && killed.getType() != ModEntities.METALMANCER_GOLEM.get()
                && killed.getType().getCategory() == MobCategory.MONSTER
                && killed instanceof Enemy
                && killed.getTags().stream().anyMatch(tag -> tag.startsWith(COSMIC_SPAWNER_TAG_PREFIX));
    }

    private static List<ServerPlayer> eligiblePlayers(MinecraftServer server, DungeonRunRegistryData.RunRecord run, LivingEntity killed) {
        List<ServerPlayer> players = new ArrayList<>();
        for (UUID playerId : run.orderedPlayers()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (isEligible(player, killed)) {
                players.add(player);
            }
        }
        return players;
    }

    private static boolean isEligible(ServerPlayer player, LivingEntity killed) {
        return player != null
                && !player.isRemoved()
                && !player.isSpectator()
                && player.level().dimension().equals(killed.level().dimension())
                && player.distanceToSqr(killed) <= MAX_ELIGIBLE_DISTANCE_SQR
                && !DungeonAfkService.isAfk(player.getUUID());
    }
}
