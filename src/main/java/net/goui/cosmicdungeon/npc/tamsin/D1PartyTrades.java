package net.goui.cosmicdungeon.npc.tamsin;

import java.util.*;
import net.goui.cosmicdungeon.trade.TradeSessionData;
import net.minecraft.server.MinecraftServer;

/** Server-thread handoff: menu changes never alter readiness; explicit valid Start ends trades first. */
final class D1PartyTrades {
    // Readiness lobbies are transient. Weak keys retain retries without keeping ended groups/servers alive.
    private static final Map<D1PartyLobby.Party, Map<UUID,UUID>> RECOVERY = new WeakHashMap<>();
    private D1PartyTrades() {}
    static boolean allowed(D1PartyLobby.Party party) {
        return party == null || party.phase() != D1PartyLobby.Phase.QUEUED
                && party.phase() != D1PartyLobby.Phase.PREPARING;
    }
    static String start(MinecraftServer server, D1PartyLobby lobby, UUID leader, long revision) {
        String problem = lobby.queueProblem(leader, revision);
        if (problem != null) return problem;
        var party = lobby.party(leader);
        // Check every participant before changing either side of any trade.
        for (UUID id : party.members())
            if (server.getPlayerList().getPlayer(id) == null) return "A group member disconnected.";
        var peers = RECOVERY.computeIfAbsent(party, ignored -> new HashMap<>());
        for (UUID id : party.members())
            if (!TradeSessionData.endForAdventure(server.getPlayerList().getPlayer(id), peers))
                return "A trade recovery is pending. Resolve it before starting the adventure.";
        RECOVERY.remove(party);
        return lobby.queue(leader, revision);
    }
}
