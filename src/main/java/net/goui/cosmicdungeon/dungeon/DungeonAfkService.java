package net.goui.cosmicdungeon.dungeon;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class DungeonAfkService {
    private DungeonAfkService() {}

    public static final long AFK_TIMEOUT_TICKS = 15L * 60L * 20L;
    private static final long CHECK_INTERVAL_TICKS = 20L;

    private record Activity(long lastActiveTick, boolean afk, double x, double y, double z, float yRot, float xRot) {}

    private static final Map<UUID, Activity> ACTIVITY = new HashMap<>();
    private static long nextCheckTick = 0L;

    public static boolean isAfk(UUID playerId) {
        Activity activity = ACTIVITY.get(playerId);
        return activity != null && activity.afk();
    }

    public static void markActivity(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) return;
        long now = player.level().getGameTime();
        Activity old = ACTIVITY.get(player.getUUID());
        boolean wasAfk = old != null && old.afk();
        ACTIVITY.put(player.getUUID(), snapshot(player, now, false));
        if (wasAfk) {
            broadcastReturned(player);
        }
    }

    public static void onPlayerLoggedOut(ServerPlayer player) {
        if (player != null) ACTIVITY.remove(player.getUUID());
    }

    public static void tick(MinecraftServer server) {
        if (server == null) return;
        var overworld = server.overworld();
        long now = overworld.getGameTime();
        if (now < nextCheckTick) return;
        nextCheckTick = now + CHECK_INTERVAL_TICKS;

        Set<UUID> online = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            online.add(player.getUUID());
            tickPlayer(player, now);
        }
        ACTIVITY.keySet().removeIf(id -> !online.contains(id));
    }

    private static void tickPlayer(ServerPlayer player, long now) {
        Activity old = ACTIVITY.get(player.getUUID());
        if (old == null) {
            ACTIVITY.put(player.getUUID(), snapshot(player, now, false));
            return;
        }

        if (hasMovedOrLooked(player, old)) {
            markActivity(player);
            return;
        }

        Optional<DungeonRunRegistryData.RunRecord> runOpt = DungeonLifecycleService.findActiveRunForPlayer(player);
        if (runOpt.isEmpty()) {
            ACTIVITY.put(player.getUUID(), snapshot(player, now, false));
            return;
        }

        if (!old.afk() && now - old.lastActiveTick() > AFK_TIMEOUT_TICKS) {
            ACTIVITY.put(player.getUUID(), snapshot(player, old.lastActiveTick(), true));
            broadcastBecameAfk(player, runOpt.get());
        }
    }

    private static boolean hasMovedOrLooked(ServerPlayer player, Activity old) {
        return Double.compare(player.getX(), old.x()) != 0
                || Double.compare(player.getY(), old.y()) != 0
                || Double.compare(player.getZ(), old.z()) != 0
                || Float.compare(player.getYRot(), old.yRot()) != 0
                || Float.compare(player.getXRot(), old.xRot()) != 0;
    }

    private static Activity snapshot(ServerPlayer player, long lastActiveTick, boolean afk) {
        return new Activity(lastActiveTick, afk, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
    }

    private static void broadcastBecameAfk(ServerPlayer afkPlayer, DungeonRunRegistryData.RunRecord run) {
        MinecraftServer server = afkPlayer.level().getServer();
        if (server == null) return;
        String name = afkPlayer.getName().getString();
        Component groupMessage = Component.literal("Player: " + name + " has been AFK for 15 minutes. They will no longer receive Group Split until they return")
                .withStyle(ChatFormatting.YELLOW);
        for (UUID id : run.orderedPlayers()) {
            if (id.equals(afkPlayer.getUUID())) continue;
            ServerPlayer member = server.getPlayerList().getPlayer(id);
            if (member != null) member.sendSystemMessage(groupMessage);
        }

        run.groupLeader().map(server.getPlayerList()::getPlayer).ifPresent(leader -> {
            if (leader.getUUID().equals(afkPlayer.getUUID())) return;
            leader.sendSystemMessage(kickPrompt(name, afkPlayer.getUUID()));
        });
    }

    private static MutableComponent kickPrompt(String playerName, UUID playerId) {
        return Component.literal("Player: " + playerName + " has been AFK for 15 minutes. Would you like to kick them from the dungeon? ")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("[YES]").withStyle(style -> style.withColor(ChatFormatting.GREEN)
                        .withBold(true)
                        .withClickEvent(new ClickEvent.RunCommand("/dungeoneer afk-kick yes " + playerId))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Kick " + playerName + " from the dungeon")))))
                .append(Component.literal(" "))
                .append(Component.literal("[NO]").withStyle(style -> style.withColor(ChatFormatting.RED)
                        .withBold(true)
                        .withClickEvent(new ClickEvent.RunCommand("/dungeoneer afk-kick no " + playerId))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Leave " + playerName + " in the dungeon")))));
    }

    private static void broadcastReturned(ServerPlayer player) {
        Optional<DungeonRunRegistryData.RunRecord> runOpt = DungeonLifecycleService.findActiveRunForPlayer(player);
        if (runOpt.isEmpty()) return;
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        String name = player.getName().getString();
        Component message = Component.literal("Player: " + name + " is no longer afk. Group split is reactivated for this player")
                .withStyle(ChatFormatting.GREEN);
        for (UUID id : runOpt.get().orderedPlayers()) {
            ServerPlayer member = server.getPlayerList().getPlayer(id);
            if (member != null) member.sendSystemMessage(message);
        }
    }
}
