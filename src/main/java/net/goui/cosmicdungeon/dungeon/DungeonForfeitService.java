package net.goui.cosmicdungeon.dungeon;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-thread coordinator; inventories and reset durability remain owned by the lifecycle service. */
public final class DungeonForfeitService {
    private static final long VOTE_TICKS = 60L * 20L;
    private static final Map<MinecraftServer, DungeonForfeitService> SERVERS = new HashMap<>();
    private final MinecraftServer server;
    private final Map<Long, DungeonForfeitBallot> ballots = new HashMap<>();

    private DungeonForfeitService(MinecraftServer server) { this.server = server; }

    public static DungeonForfeitService get(MinecraftServer server) {
        return SERVERS.computeIfAbsent(server, DungeonForfeitService::new);
    }

    public static void stop(MinecraftServer server) { SERVERS.remove(server); }

    public static void tick(MinecraftServer server) {
        var service = SERVERS.get(server);
        if (service != null) service.expire();
    }

    public static void notifyEntry(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("type /ff to forfeit the dungeon instance.")
                .withStyle(style -> style.withColor(ChatFormatting.YELLOW)
                        .withClickEvent(new ClickEvent.RunCommand("/ff"))));
    }

    public int open(ServerPlayer player) {
        var run = eligibleRun(player);
        if (run == null) return 0;
        var ballot = current(run);
        if (ballot != null) {
            prompt(player, ballot);
            return 1;
        }
        ballot = new DungeonForfeitBallot(run.orderedPlayers(), now() + VOTE_TICKS);
        ballots.put(run.runId(), ballot);
        broadcast(ballot, Component.literal(player.getName().getString() + " opened a dungeon forfeit vote.")
                .withStyle(ChatFormatting.YELLOW));
        for (UUID owner : ballot.members()) {
            var member = server.getPlayerList().getPlayer(owner);
            if (member != null) prompt(member, ballot);
        }
        return 1;
    }

    public int vote(ServerPlayer player, boolean yes, String expectedId) {
        var run = eligibleRun(player);
        if (run == null) return 0;
        var ballot = current(run);
        if (ballot == null || !ballot.matches(expectedId)) {
            player.sendSystemMessage(Component.literal("That forfeit vote is no longer open. Use /ff to view or start a vote."));
            return 0;
        }
        if (!ballot.vote(player.getUUID(), yes, run.orderedPlayers(), now())) {
            player.sendSystemMessage(Component.literal("You already voted in this ballot."));
            return 0;
        }
        broadcast(ballot, Component.literal("Forfeit vote: " + ballot.yesCount() + "/" + ballot.required()
                + " required Yes votes (" + ballot.members().size() + " dungeon members)."));
        var state = ballot.refresh(run.orderedPlayers(), now());
        if (state == DungeonForfeitBallot.State.OPEN) return 1;
        // Remove before invoking cleanup: dimension changes and recovery events may run synchronously.
        ballots.remove(run.runId());
        if (state == DungeonForfeitBallot.State.PASSED) {
            if (DungeonLifecycleService.forfeitRun(server, run.runId())) {
                com.mojang.logging.LogUtils.getLogger().info("Dungeon forfeit accepted for run {}: {}/{} Yes votes",
                        run.runId(), ballot.yesCount(), ballot.members().size());
                broadcast(ballot, Component.literal("Vote passed. Dungeon failed: dungeon loot is forfeited; "
                        + "your saved outside inventory will be restored. Respawn if you are on the death screen.")
                        .withStyle(ChatFormatting.RED));
            } else {
                broadcast(ballot, Component.literal("Forfeit could not safely start because this run has ended "
                        + "or has pending recovery. Your saved inventories are retained; resolve recovery before voting again.")
                        .withStyle(ChatFormatting.YELLOW));
            }
        } else {
            broadcast(ballot, Component.literal("Forfeit vote rejected. The dungeon continues."));
        }
        return 1;
    }

    private DungeonRunRegistryData.RunRecord eligibleRun(ServerPlayer player) {
        var runs = DungeonRunRegistryData.get(server);
        var run = runs.findRunForPlayer(player.getUUID()).orElse(null);
        if (run == null || run.stateEnum() != DungeonRunState.ACTIVE || runs.starting(run.runId())) {
            player.sendSystemMessage(Component.literal("You must belong to an active dungeon instance with entry complete."));
            return null;
        }
        return run;
    }

    private DungeonForfeitBallot current(DungeonRunRegistryData.RunRecord run) {
        var ballot = ballots.get(run.runId());
        if (ballot == null) return null;
        var state = ballot.refresh(run.orderedPlayers(), now());
        if (state == DungeonForfeitBallot.State.OPEN) return ballot;
        ballots.remove(run.runId());
        broadcast(ballot, Component.literal(state == DungeonForfeitBallot.State.EXPIRED
                ? "Forfeit vote expired. The dungeon continues."
                : "Dungeon membership changed; the forfeit vote was cancelled. Use /ff for a new vote."));
        return null;
    }

    private void expire() {
        var runs = DungeonRunRegistryData.get(server);
        for (long runId : new ArrayList<>(ballots.keySet())) {
            var run = runs.getRun(runId).orElse(null);
            if (run == null || run.stateEnum() != DungeonRunState.ACTIVE || runs.starting(runId)) {
                var ballot = ballots.remove(runId);
                broadcast(ballot, Component.literal("Forfeit vote closed because the dungeon is no longer active."));
            } else current(run);
        }
    }

    private long now() { return server.overworld().getGameTime(); }

    private void prompt(ServerPlayer player, DungeonForfeitBallot ballot) {
        player.sendSystemMessage(Component.literal("Forfeit this instance? Passing fails the run, forfeits dungeon loot, "
                + "and restores saved outside belongings. " + ballot.required() + " of " + ballot.members().size()
                + " members must vote Yes. The ballot expires 60 seconds after opening; offline members still count."));
        player.sendSystemMessage(choice("[Yes, forfeit]", "/ff yes " + ballot.id(), ChatFormatting.RED)
                .append(Component.literal("  "))
                .append(choice("[No, continue]", "/ff no " + ballot.id(), ChatFormatting.GREEN)));
    }

    private net.minecraft.network.chat.MutableComponent choice(String text, String command, ChatFormatting color) {
        return Component.literal(text).withStyle(style -> style.withColor(color)
                .withClickEvent(new ClickEvent.RunCommand(command)));
    }

    private void broadcast(DungeonForfeitBallot ballot, Component message) {
        for (UUID owner : ballot.members()) {
            var player = server.getPlayerList().getPlayer(owner);
            if (player != null) player.sendSystemMessage(message);
        }
    }
}
