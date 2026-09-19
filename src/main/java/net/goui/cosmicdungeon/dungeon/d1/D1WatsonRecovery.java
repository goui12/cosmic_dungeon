package net.goui.cosmicdungeon.dungeon.d1;

import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.faction.PlayerFactionData;
import net.goui.cosmicdungeon.npc.tamsin.TamsinTaxProgress;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.progression.PlayerProgressionData;
import net.goui.cosmicdungeon.transaction.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/** Recover inputs first, then receipt-protected rewards, then delegate to the existing cleanup journal. */
public final class D1WatsonRecovery {
    public static final String RECEIPT = "d1_watson_receipt_v1";
    private static final Set<ServerPlayer> HOLDS = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<MinecraftServer, Integer> CURSORS = new WeakHashMap<>();
    private static final Map<MinecraftServer, Set<Long>> REPORTED = new WeakHashMap<>();
    private D1WatsonRecovery() {}
    private static CompoundTag root(ServerPlayer p) { return p.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG); }
    private static boolean historyMatches(ServerPlayer p, D1RunData data) {
        var actual = root(p).getCompoundOrEmpty(RECEIPT); var outcome = data.outcomeFor(p.getUUID());
        if (outcome != null && outcome.receipt(p.getUUID()).matches(actual)) return true;
        if (outcome != null && outcome.acknowledged().contains(p.getUUID())) return false;
        var last = data.lastWatson(p.getUUID()); return last == null ? actual.isEmpty() : last.matches(actual);
    }
    public static boolean blocked(ServerPlayer p) {
        var data = D1RunData.get(p.level().getServer());
        if (HOLDS.contains(p) || data.outcomeFor(p.getUUID()) != null || !historyMatches(p, data)) return true;
        return DungeonRunRegistryData.get(p.level().getServer()).findRunForPlayer(p.getUUID())
                .filter(run -> data.sealed(run.runId())).isPresent();
    }
    public static boolean readyForCleanup(MinecraftServer server, List<UUID> owners) {
        var data = D1RunData.get(server);
        for (UUID owner : owners) {
            var outcome = data.outcomeFor(owner);
            if (outcome != null && !outcome.ready()) return false;
            var p = server.getPlayerList().getPlayer(owner);
            if (p != null && (HOLDS.contains(p) || !historyMatches(p, data))) return false;
            var run = DungeonRunRegistryData.get(server).findRunForPlayer(owner).orElse(null);
            if (run != null && data.sealed(run.runId())) {
                var runOutcome = data.outcome(run.runId());
                if (runOutcome == null || !runOutcome.ready()) return false;
            }
        }
        return true;
    }
    /** A committed Watson result cannot be replaced by kick, timeout, exit, or abandonment. */
    public static boolean permitsCleanup(MinecraftServer server, long runId, String reason) {
        var data = D1RunData.get(server); var outcome = data.outcome(runId);
        if (outcome == null) return !data.sealed(runId) && !reason.equals("COMPLETED");
        return outcome.permitsCleanup(reason);
    }
    private static void hold(ServerPlayer p, String message) {
        if (HOLDS.add(p)) p.connection.disconnect(Component.literal(message));
    }
    private static void holdOwners(MinecraftServer server, WatsonOutcome outcome) {
        for (UUID owner : outcome.owners()) {
            var p = server.getPlayerList().getPlayer(owner);
            if (p != null) hold(p, "Watson's saved outcome needs recovery. Reconnect to settle your hand-in; remaining participants may also need to reconnect.");
        }
    }
    public static boolean begin(MinecraftServer server, DungeonRunRegistryData.RunRecord run, List<ServerPlayer> members) {
        var data = D1RunData.get(server);
        if (data.sealed(run.runId()) || DungeonRunRegistryData.get(server).starting(run.runId())) return false;
        var inventories = new LinkedHashMap<UUID, CompoundTag>(); var taxes = new LinkedHashMap<UUID, CompoundTag>();
        var counters = new LinkedHashMap<UUID, int[]>();
        try {
            if (!InventoryTransactionGuard.readyForCleanup(server, run.orderedPlayers())) return false;
            for (var p : members) {
                if (!p.isAlive() || !run.containsPlayer(p.getUUID()) || !run.containsDimension(p.level().dimension())
                        || !InventoryTransactionGuard.beforeInventoryChange(p) || blocked(p)) return false;
                inventories.put(p.getUUID(), ChopTravelRecovery.saveInventory(p));
                taxes.put(p.getUUID(), root(p).getCompoundOrEmpty(TamsinTaxProgress.KEY).copy());
                counters.put(p.getUUID(), new int[]{data.count(run.runId(), "kills:" + p.getUUID()),
                        data.count(run.runId(), "lesser:" + p.getUUID()), data.count(run.runId(), "lesser_success:" + p.getUUID())});
            }
            var outcome = WatsonOutcome.create(run.runId(), List.copyOf(inventories.keySet()), inventories, taxes, counters, Config.NPC_BLOOM_GAIN.get());
            for (var p : members) {
                ChopTravelRecovery.decode(p, outcome.member(p.getUUID()).getCompoundOrEmpty("after"));
                if (!PlayerSaveProof.saveWithLocation(p)) throw new IllegalStateException("Watson source owner save not verified");
            }
            if (!DungeonRunRegistryData.get(server).flushVerified()) throw new IllegalStateException("Watson run identity not verified");
            data.beginOutcome(outcome);
            if (!data.flushVerified()) throw new IllegalStateException("Watson decision not verified");
            return recover(server, run.runId());
        } catch (RuntimeException failure) {
            report(server, run.runId(), failure);
            for (var p : members) hold(p, "The Watson hand-in needs save recovery. Reconnect; if it repeats, ask a developer to review the saved outcome.");
            return false;
        }
    }
    private static void settleInput(ServerPlayer p, WatsonOutcome outcome) {
        if (HOLDS.contains(p) || !p.isAlive()) throw new IllegalStateException("Watson input owner must reconnect alive");
        if (ChopTravelRecovery.blocked(p) || DungeonInventoryHandoffs.blocked(p)
                || net.goui.cosmicdungeon.economy.DeathCurrencyService.blocked(p)
                || net.goui.cosmicdungeon.vendor.CommerceTransactions.blocked(p)
                || net.goui.cosmicdungeon.playerclass.dragoon.repair.RepairTransactions.blocked(p)
                || net.goui.cosmicdungeon.trade.TradeTransactions.blocked(p))
            throw new IllegalStateException("Watson input overlaps another custody record");
        p.closeContainer(); var member = outcome.member(p.getUUID()); var state = root(p);
        var receipt = state.getCompoundOrEmpty(RECEIPT);
        if (!outcome.canApply(p.getUUID(), receipt, state.getLongOr(DungeonInventoryHandoffs.COMPLETED, 0),
                ChopTravelRecovery.saveInventory(p), state.getCompoundOrEmpty(TamsinTaxProgress.KEY)))
            throw new IllegalStateException("Watson input differs from its frozen inventory, Tax proof, or receipt");
        if (!outcome.receipt(p.getUUID()).matches(receipt)) {
            var after = ChopTravelRecovery.decode(p, member.getCompoundOrEmpty("after"));
            for (int slot = 0; slot < after.size(); slot++) p.getInventory().setItem(slot, after.get(slot).copy());
            p.getInventory().setChanged();
            var next = root(p).copy(); next.put(RECEIPT, outcome.receipt(p.getUUID()).image());
            next.put(TamsinTaxProgress.KEY, member.getCompoundOrEmpty("tax_after").copy());
            p.getPersistentData().put(ClassData.ROOT_TAG, next);
        }
        if (!PlayerSaveProof.saveWithLocation(p)) throw new IllegalStateException("Watson input owner receipt not verified");
        var data = D1RunData.get(p.level().getServer()); data.acknowledgeOutcome(outcome, p.getUUID());
        if (!data.flushVerified()) throw new IllegalStateException("Watson input acknowledgement not verified");
        p.inventoryMenu.broadcastChanges();
    }
    private static void project(MinecraftServer server, WatsonOutcome outcome) {
        var lifetime = D1LifetimeData.get(server);
        var progression = PlayerProgressionData.get(server);
        var factions = PlayerFactionData.get(server);
        for (UUID owner : outcome.owners()) {
            var member = outcome.member(owner); var receipt = outcome.receipt(owner);
            lifetime.applyWatson(receipt, member.getIntOr("kills", 0), member.getIntOr("lifetime_lesser", 0));
            progression.applyWatson(receipt, member.getIntOr("rounded", 0));
            factions.applyWatson(receipt, member.getLongOr("faction_bonus", 0));
        }
        if (!lifetime.flushVerified() || !progression.flushVerified() || !factions.flushVerified())
            throw new IllegalStateException("Watson reward projections not verified");
    }
    public static boolean recover(MinecraftServer server, long runId) {
        var data = D1RunData.get(server); var outcome = data.outcome(runId);
        if (outcome == null) return !data.sealed(runId);
        try {
            var run = DungeonRunRegistryData.get(server).getRun(runId).orElse(null);
            if (run == null) {
                if (!outcome.ready()) throw new IllegalStateException("Unsettled Watson outcome has no run");
                return data.retireOutcomeVerified(runId);
            }
            if (!run.dungeonId().equals("dungeon_1") || !run.orderedPlayers().containsAll(outcome.owners())
                    || DungeonRunRegistryData.get(server).starting(runId))
                throw new IllegalStateException("Watson run identity differs");
            if (!outcome.ready()) {
                if (run.stateEnum() != DungeonRunState.ACTIVE) throw new IllegalStateException("Cleanup started before Watson settled");
                if (!data.flushVerified()) throw new IllegalStateException("Watson decision readback failed");
                for (UUID owner : outcome.owners()) {
                    var p = server.getPlayerList().getPlayer(owner);
                    if (p != null && !historyMatches(p, data)) throw new IllegalStateException("Watson owner receipt disagrees");
                    if (!outcome.acknowledged().contains(owner) && p != null && !HOLDS.contains(p)) {
                        settleInput(p, outcome); outcome = data.outcome(runId);
                    }
                }
                if (outcome.acknowledged().size() != outcome.owners().size()) {
                    holdOwners(server, outcome); return false;
                }
                project(server, outcome);
                if (!data.readyOutcomeVerified(outcome)) throw new IllegalStateException("Watson ready receipt not verified");
                outcome = data.outcome(runId);
                for (UUID owner : outcome.owners()) {
                    var p = server.getPlayerList().getPlayer(owner);
                    if (p != null && !HOLDS.contains(p)) {
                        D1Scoreboards.lifetime(p);
                        p.sendSystemMessage(Component.literal(outcome.success()
                                ? "All six Blooms are returned. Watson is restored. Dungeon 1 completed!"
                                : "The six Blooms are incomplete. This Dungeon 1 run has failed."));
                    }
                }
            }
            if (run.stateEnum() == DungeonRunState.ACTIVE) DungeonLifecycleService.resolveD1Run(server, runId, outcome.success());
            else if (!permitsCleanup(server, runId, run.resetReason()))
                throw new IllegalStateException("Cleanup disagrees with committed Watson outcome");
            var reported = REPORTED.get(server); if (reported != null) reported.remove(runId);
            return true;
        } catch (RuntimeException failure) { report(server, runId, failure); holdOwners(server, outcome); return false; }
    }
    public static boolean login(ServerPlayer p) {
        if (HOLDS.contains(p)) return false;
        var server = p.level().getServer(); var data = D1RunData.get(server);
        var outcome = data.outcomeFor(p.getUUID());
        if (outcome != null && !recover(server, outcome.run())) return false;
        if (!historyMatches(p, data)) {
            hold(p, "Your Watson receipt differs from the saved outcome. Ask a developer to review a complete save backup."); return false;
        }
        var run = DungeonRunRegistryData.get(server).findRunForPlayer(p.getUUID()).orElse(null);
        if (run != null && data.outcome(run.runId()) == null && data.sealed(run.runId())) {
            hold(p, "This older Watson outcome has no recovery receipt. Ask a developer to review the saved run."); return false;
        }
        return !HOLDS.contains(p);
    }
    /** At most one pending run per configured poll; offline missing inputs cause no repeated disk writes. */
    public static void tick(MinecraftServer server) {
        if (server.overworld().getGameTime() % Config.WATSON_RECOVERY_POLL_TICKS.get() != 0) return;
        var data = D1RunData.get(server); var runs = data.outcomeRuns(); if (runs.isEmpty()) return;
        int index = Math.floorMod(CURSORS.getOrDefault(server, 0), runs.size()); CURSORS.put(server, (index + 1) % runs.size());
        var outcome = data.outcome(runs.get(index));
        if (outcome.ready() || outcome.acknowledged().size() == outcome.owners().size()
                || outcome.owners().stream().anyMatch(owner -> {
                    var p = server.getPlayerList().getPlayer(owner);
                    return !outcome.acknowledged().contains(owner) && p != null && !HOLDS.contains(p);
                })) recover(server, outcome.run());
    }
    private static void report(MinecraftServer server, long run, RuntimeException failure) {
        if (REPORTED.computeIfAbsent(server, s -> new HashSet<>()).add(run))
            com.mojang.logging.LogUtils.getLogger().error("Watson outcome {} held; saved evidence retained", run, failure);
    }
    // TODO(M03/M93, licensed TEST): Q&A D20/D23 and Tax Doc 1dIuaeMFMZaaWo7AS1zQ51kXSbdPkb9tmUm864MBs2Q0
    // require exact Bloom consumption and success-only permanent statistics. Interrupt native
    // dedicated/integrated saves at decision, each owner receipt/ack, every reward projection,
    // readiness, cleanup and retirement. Older watson_outcome flags have no input/reward proof;
    // hold them for complete-backup review rather than replaying grants or guessing failure.
}
