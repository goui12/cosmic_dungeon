package net.goui.cosmicdungeon.npc.tamsin;

import java.util.*;
import net.goui.cosmicdungeon.dungeon.DungeonRunRegistryData;
import net.goui.cosmicdungeon.network.*;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.minecraft.server.MinecraftServer;

/** Bounded participant-only snapshots, independent of NPC/container sessions. Server thread only. */
final class D1PartyHudService {
    private static final Map<UUID, PartyPayloads.View> LAST = new HashMap<>();
    private static final Map<UUID, PartyPayloads.Member> IDENTITIES = new HashMap<>();
    private D1PartyHudService() {}
    static void track(UUID player) { LAST.putIfAbsent(player, null); }
    static void forget(UUID player) { LAST.remove(player); }
    static void clear() { LAST.clear(); IDENTITIES.clear(); }
    private static PartyPayloads.Member member(MinecraftServer server, UUID id, boolean ready, boolean leader) {
        var player = server.getPlayerList().getPlayer(id);
        var known = IDENTITIES.get(id);
        var row = new PartyPayloads.Member(player == null ? (known == null ? "Offline" : known.name()) : player.getGameProfile().name(),
                player == null ? (known == null ? "none" : known.classId()) : ClassData.getClassId(player),
                player != null && ready, leader);
        IDENTITIES.put(id, row); return row;
    }
    static void sync(MinecraftServer server, D1PartyLobby lobby) {
        var audience = new HashSet<>(LAST.keySet());
        lobby.parties().forEach(p -> audience.addAll(p.members()));
        lobby.invitations().forEach(i -> audience.add(i.target()));
        var retainedIdentities = new HashSet<UUID>();
        for (UUID id : audience) {
            var player = server.getPlayerList().getPlayer(id);
            if (player == null) { LAST.remove(id); continue; }
            var p = lobby.party(id);
            var run = p == null ? DungeonRunRegistryData.get(server).findRunForPlayer(id).orElse(null) : null;
            var invitation = lobby.invitation(id);
            var sender = invitation == null ? null : server.getPlayerList().getPlayer(invitation.inviter());
            List<PartyPayloads.Member> rows = List.of();
            String name = "", phase = "UNGROUPED";
            int capacity = 6, queue = 0, seconds = -1;
            boolean leader = false;
            if (p != null) {
                retainedIdentities.addAll(p.members());
                rows = p.members().stream().map(m -> member(server, m, p.ready().contains(m), p.leader().equals(m))).toList();
                name = p.name(); phase = p.phase().name(); capacity = p.capacity(); leader = p.leader().equals(id);
                queue = lobby.queuePosition(p);
                seconds = p.countdownEnd() < 0 ? -1 : (int)Math.max(0, (p.countdownEnd() - server.overworld().getGameTime() + 19) / 20);
            } else if (run != null && !run.isCompletionExited(id)) {
                var roster = run.orderedPlayers().stream().filter(m -> !run.isCompletionExited(m)).limit(6).toList();
                retainedIdentities.addAll(roster);
                rows = roster.stream().map(m -> member(server, m, true, run.groupLeader().filter(m::equals).isPresent())).toList();
                var previous = LAST.get(id);
                name = previous == null || previous.recruitment().groupName().isBlank() ? "Dungeon 1" : previous.recruitment().groupName();
                phase = "ACTIVE"; capacity = rows.size();
            }
            var view = new PartyPayloads.View(-1,
                    new PartyPayloads.State(lobby.revision(id), phase, leader, capacity, queue, seconds), rows,
                    new PartyPayloads.Invite(invitation == null ? "" : invitation.token(),
                            sender == null ? "" : sender.getGameProfile().name(), invitation != null && invitation.accepted(), false),
                    new PartyPayloads.Recruitment(name, false, 0, 1, List.of()));
            if (!view.equals(LAST.get(id))) ModNetwork.sendTo(player, view);
            if (rows.isEmpty() && invitation == null) LAST.remove(id);
            else LAST.put(id, view);
        }
        IDENTITIES.keySet().retainAll(retainedIdentities);
    }
}
