package net.goui.cosmicdungeon.dungeon;

import net.goui.cosmicdungeon.advancement.BloomSharedAdvancements;
import net.goui.cosmicdungeon.block.custom.ClassSelectorReadyManager;
import net.goui.cosmicdungeon.block.custom.ClassSelectorTeleportUtil;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.door.DoorPassageTracker;
import net.goui.cosmicdungeon.entity.MetalmancerGolemEntity;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.api.ClassKeys;
import net.goui.cosmicdungeon.playerclass.api.ClassNet;
import net.goui.cosmicdungeon.playerclass.metalmancer.MetalmancerResonanceTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.AABB;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class DungeonLifecycleService {
    private DungeonLifecycleService() {}

    private static final String KEY_METALMANCER_ROOT = "metalmancer";
    private static final String KEY_PENDING_SELECTOR = "pending_class_selector";
    private static final String KEY_RUN_TEMP = "run_temp";
    private static final String DEFAULT_DIFFICULTY = "NORMAL";
    private record PendingReset(
            String dungeonId,
            long runId,
            DungeonResetReason reason,
            String snapshotIdOrNull,
            long nextAttemptTick,
            int attemptsRemaining
    ) {}

    private static final Map<String, PendingReset> PENDING_RESETS = new HashMap<>();
    private static final long RESET_RETRY_DELAY_TICKS = 100L; // 5 seconds
    private static final int RESET_MAX_ATTEMPTS = 12;         // 60 seconds total retry window
    public static Optional<DungeonRunRegistryData.RunRecord> findActiveRunForPlayer(ServerPlayer sp) {
        if (sp == null || !(sp.level() instanceof ServerLevel sl)) return Optional.empty();

        DungeonRunRegistryData runs = DungeonRunRegistryData.get(sl.getServer());
        Optional<DungeonRunRegistryData.RunRecord> opt = runs.findRunForPlayer(sp.getUUID());
        if (opt.isEmpty()) return Optional.empty();

        DungeonRunRegistryData.RunRecord run = opt.get();
        return run.stateEnum() == DungeonRunState.ACTIVE ? Optional.of(run) : Optional.empty();
    }

    public static String getStartRunBlocker(MinecraftServer server,
                                            net.minecraft.resources.ResourceKey<Level> dungeonDimension,
                                            Collection<UUID> party) {
        if (server == null || dungeonDimension == null) return "Server or dungeon dimension was null.";

        DungeonDefinition def = DungeonDefinitions.byDimension(dungeonDimension).orElse(null);
        if (def == null) {
            return "No logical dungeon definition is registered for " + dungeonDimension.location();
        }

        if (PENDING_RESETS.containsKey(def.id())) {
            return "Dungeon already has a queued reset in progress: " + def.id();
        }

        DungeonRunRegistryData runs = DungeonRunRegistryData.get(server);

        if (runs.findActiveOrResettingRun(def.id()).isPresent()) {
            return "Dungeon already has an active/resetting run: " + def.id();
        }

        if (party != null) {
            for (UUID id : party) {
                if (id == null) continue;
                Optional<DungeonRunRegistryData.RunRecord> other = runs.findRunForPlayer(id);
                if (other.isPresent()) {
                    return "A selected player is already tracked in run " + other.get().runId() + ".";
                }
            }
        }

        return null;
    }

    public static String startRun(MinecraftServer server,
                                  net.minecraft.resources.ResourceKey<Level> selectorDimension,
                                  long selectorPosLong,
                                  net.minecraft.resources.ResourceKey<Level> dungeonDimension,
                                  Collection<ServerPlayer> players) {
        if (server == null || selectorDimension == null || dungeonDimension == null) {
            return "Server or dimension was null.";
        }

        DungeonDefinition def = DungeonDefinitions.byDimension(dungeonDimension).orElse(null);
        if (def == null) {
            return "No logical dungeon definition is registered for " + dungeonDimension.location();
        }

        List<ServerPlayer> party = new ArrayList<>();
        List<UUID> ids = new ArrayList<>();

        if (players != null) {
            for (ServerPlayer sp : players) {
                if (sp == null) continue;
                party.add(sp);
                ids.add(sp.getUUID());
            }
        }

        if (party.isEmpty()) {
            return "Cannot start a run with an empty party.";
        }

        String blocker = getStartRunBlocker(server, dungeonDimension, ids);
        if (blocker != null) return blocker;

        List<DungeonPlayerRunSnapshot> snapshots = new ArrayList<>(party.size());
        for (ServerPlayer sp : party) {
            snapshots.add(snapshotPlayer(sp));
        }

        long runId = DungeonRunRegistryData.get(server).startRun(
                selectorDimension,
                selectorPosLong,
                def,
                ids,
                snapshots
        );

        if (runId <= 0L) {
            return "Run registration failed.";
        }

        notifyDevelopers(server, Component.literal(
                "[DungeonLifecycle] Started run " + runId + " for " + def.id() + " with " + party.size() + " player(s)."
        ).withStyle(ChatFormatting.AQUA));

        return null;
    }

    public static void onPlayerExitedThroughResetRift(ServerLevel sourceLevel, ServerPlayer sp) {
        if (sourceLevel == null || sp == null) return;

        MinecraftServer server = sourceLevel.getServer();
        if (server == null) return;

        DungeonRunRegistryData runs = DungeonRunRegistryData.get(server);
        Optional<DungeonRunRegistryData.RunRecord> opt = runs.findRunForPlayer(sp.getUUID());
        if (opt.isEmpty()) return;

        DungeonRunRegistryData.RunRecord run = opt.get();
        if (run.stateEnum() != DungeonRunState.ACTIVE) return;
        if (!run.containsDimension(sourceLevel.dimension())) return;

        clearPlayerInventory(sp);
        resetPlayerRespawnToOverworldSpawn(sp);

        runs.markCompletionExited(run.runId(), sp.getUUID());

        DungeonRunRegistryData.RunRecord updated = runs.getRun(run.runId()).orElse(run);
        if (allOnlineTrackedPlayersCompletionExited(server, updated)) {
            finishRun(server, updated.runId(), DungeonResetReason.COMPLETED, null);
        }
    }

    public static void evaluateActiveRuns(MinecraftServer server) {
        if (server == null) return;

        DungeonRunRegistryData runs = DungeonRunRegistryData.get(server);
        List<DungeonRunRegistryData.RunRecord> list = runs.listAllRuns();

        for (DungeonRunRegistryData.RunRecord run : list) {
            if (run.stateEnum() != DungeonRunState.ACTIVE) continue;

            if (shouldAbandonRun(server, run)) {
                finishRun(server, run.runId(), DungeonResetReason.ABANDONED, null);
            }
        }
    }

    public static boolean performPendingRecoveryIfNeeded(ServerPlayer sp) {
        if (sp == null || !(sp.level() instanceof ServerLevel sl)) return false;

        MinecraftServer server = sl.getServer();
        if (server == null) return false;

        PendingDungeonRecoveryData pending = PendingDungeonRecoveryData.get(server);
        Optional<PendingDungeonRecoveryData.RecoveryRecord> opt = pending.get(sp.getUUID());
        if (opt.isEmpty()) return false;

        PendingDungeonRecoveryData.RecoveryRecord rec = opt.get();
        DungeonDefinition def = DungeonDefinitions.byId(rec.dungeonId()).orElse(null);

        applyRecoveryToLivePlayer(
                server,
                sp,
                def,
                new DungeonPlayerRunSnapshot(rec.playerId(), rec.inventoryNbt()),
                true,
                rec.reason()
        );

        pending.remove(sp.getUUID());

        notifyDevelopers(server, Component.literal(
                "[DungeonLifecycle] Applied pending recovery for " + sp.getName().getString()
                        + " from " + rec.dungeonId() + " (" + rec.reason() + ")."
        ).withStyle(ChatFormatting.YELLOW));

        return true;
    }

    public static DungeonWorldSnapshotService.SnapshotResult manualReset(MinecraftServer server,
                                                                         String dungeonId,
                                                                         String snapshotIdOrNull) {
        if (server == null || dungeonId == null || dungeonId.isBlank()) {
            return new DungeonWorldSnapshotService.SnapshotResult.Error("Server or dungeon id was null.");
        }

        DungeonDefinition def = DungeonDefinitions.byId(dungeonId).orElse(null);
        if (def == null) {
            return new DungeonWorldSnapshotService.SnapshotResult.Error("Unknown dungeon: " + dungeonId);
        }

        DungeonRunRegistryData runs = DungeonRunRegistryData.get(server);
        Optional<DungeonRunRegistryData.RunRecord> existing = runs.findActiveOrResettingRun(def.id());

        if (existing.isPresent() && existing.get().stateEnum() == DungeonRunState.ACTIVE) {
            runs.setState(existing.get().runId(), DungeonRunState.RESETTING, DungeonResetReason.MANUAL);
        }

        evacuatePlayersInDungeon(server, def, "Dungeon reset queued. You were moved to safety.");
        long runId = existing.map(DungeonRunRegistryData.RunRecord::runId).orElse(-1L);
        queueReset(server, def.id(), runId, DungeonResetReason.MANUAL, snapshotIdOrNull, 0L);

        notifyDevelopers(server, Component.literal(
                "[DungeonLifecycle] Manual reset queued for " + def.id()
                        + (snapshotIdOrNull == null || snapshotIdOrNull.isBlank()
                        ? " (latest snapshot)"
                        : " (" + snapshotIdOrNull + ")")
        ).withStyle(ChatFormatting.YELLOW));

        processPendingResets(server);
        PendingReset pending = PENDING_RESETS.get(def.id());
        if (pending != null) {
            return new DungeonWorldSnapshotService.SnapshotResult.Error(
                    "Reset is still pending for " + def.id()
                            + " (attemptsRemaining=" + pending.attemptsRemaining() + ")."
            );
        }

        return new DungeonWorldSnapshotService.SnapshotResult.Ok("(completed)", null);
    }

    public static void sendRunDiagnosticsTo(CommandSourceStack source, String dungeonIdOrNull) {
        if (source == null) return;

        MinecraftServer server = source.getServer();
        DungeonRunRegistryData runs = DungeonRunRegistryData.get(server);

        List<DungeonRunRegistryData.RunRecord> list = (dungeonIdOrNull == null || dungeonIdOrNull.isBlank())
                ? runs.listAllRuns()
                : runs.listRunsForDungeon(dungeonIdOrNull);

        if (list.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No tracked dungeon runs."), false);
            return;
        }

        for (DungeonRunRegistryData.RunRecord run : list) {
            int onlineTracked = 0;
            for (UUID id : run.orderedPlayers()) {
                if (server.getPlayerList().getPlayer(id) != null) {
                    onlineTracked++;
                }
            }

            int pending = 0;
            for (var rec : PendingDungeonRecoveryData.get(server).listAll()) {
                if (rec.runId() == run.runId()) pending++;
            }

            final int onlineTrackedFinal = onlineTracked;
            final int pendingFinal = pending;

            source.sendSuccess(() -> Component.literal(
                    "Run " + run.runId()
                            + " | dungeon=" + run.dungeonId()
                            + " | state=" + run.state()
                            + " | tracked=" + run.orderedPlayers().size()
                            + " | online=" + onlineTrackedFinal
                            + " | completionExited=" + run.completionExitedPlayers().size()
                            + " | pendingRecovery=" + pendingFinal
            ), false);

            List<String> names = new ArrayList<>();
            for (UUID id : run.orderedPlayers()) {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                names.add(p != null ? p.getName().getString() : id.toString());
            }

            source.sendSuccess(() -> Component.literal("  members=" + names), false);
            source.sendSuccess(() -> Component.literal("  dimensions=" + run.dungeonDimensionIds()), false);
        }
    }

    private static void finishRun(MinecraftServer server,
                                  long runId,
                                  DungeonResetReason reason,
                                  String explicitSnapshotIdOrNull) {
        if (server == null || runId <= 0L) return;

        DungeonRunRegistryData runs = DungeonRunRegistryData.get(server);
        DungeonRunRegistryData.RunRecord run = runs.getRun(runId).orElse(null);
        if (run == null) return;
        if (run.stateEnum() == DungeonRunState.RESETTING) return;

        runs.setState(runId, DungeonRunState.RESETTING, reason);
        DungeonRunRegistryData.RunRecord resetting = runs.getRun(runId).orElse(run);

        DungeonDefinition def = DungeonDefinitions.byId(resetting.dungeonId()).orElse(null);
        if (def == null) {
            notifyDevelopers(server, Component.literal(
                    "[DungeonLifecycle] Reset failed: unknown dungeon definition for " + resetting.dungeonId()
            ).withStyle(ChatFormatting.RED));
            return;
        }

        evacuatePlayersInDungeon(server, def, "Dungeon run reset queued. You were moved to safety.");
        queueReset(server, def.id(), resetting.runId(), reason, explicitSnapshotIdOrNull, RESET_RETRY_DELAY_TICKS);

        notifyDevelopers(server, Component.literal(
                "[DungeonLifecycle] " + reason + " reset queued for " + def.id()
        ).withStyle(ChatFormatting.YELLOW));
    }

    private static void evacuatePlayersInDungeon(MinecraftServer server,
                                                 DungeonDefinition def,
                                                 String message) {
        if (server == null || def == null) return;

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!def.containsDimension(p.level().dimension())) continue;

            teleportToSafeOverworld(p);

            if (message != null && !message.isBlank()) {
                p.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.YELLOW));
            }
        }
    }

    private static void performMemberCleanup(MinecraftServer server,
                                             DungeonRunRegistryData.RunRecord run,
                                             DungeonResetReason reason) {
        if (server == null || run == null) return;

        DungeonRunProgressData progress = DungeonRunProgressData.get(server);
        PendingDungeonRecoveryData pending = PendingDungeonRecoveryData.get(server);
        DungeonDefinition def = DungeonDefinitions.byId(run.dungeonId()).orElse(null);

        if (reason == DungeonResetReason.COMPLETED) {
            for (UUID id : run.orderedPlayers()) {
                progress.markCompleted(id, run.dungeonId(), DEFAULT_DIFFICULTY);
            }
        }

        Set<UUID> seen = new HashSet<>();

        for (DungeonPlayerRunSnapshot snap : run.playerSnapshots()) {
            if (snap == null || snap.playerId() == null) continue;

            seen.add(snap.playerId());

            ServerPlayer online = server.getPlayerList().getPlayer(snap.playerId());
            if (online != null) {
                applyRecoveryToLivePlayer(server, online, def, snap, false, reason.name());
            } else {
                pending.put(new PendingDungeonRecoveryData.RecoveryRecord(
                        snap.playerId(),
                        run.runId(),
                        run.dungeonId(),
                        reason.name(),
                        snap.inventoryNbt().copy()
                ));
            }
        }

        for (UUID id : run.orderedPlayers()) {
            if (seen.contains(id)) continue;

            ServerPlayer online = server.getPlayerList().getPlayer(id);
            if (online != null) {
                DungeonPlayerRunSnapshot fallback = snapshotPlayer(online);
                applyRecoveryToLivePlayer(server, online, def, fallback, false, reason.name());
            } else {
                pending.put(new PendingDungeonRecoveryData.RecoveryRecord(
                        id,
                        run.runId(),
                        run.dungeonId(),
                        reason.name(),
                        new CompoundTag()
                ));
            }
        }

        progress.clearRun(run.runId());
    }

    private static void onSuccessfulReset(MinecraftServer server,
                                          DungeonDefinition def,
                                          long runId,
                                          DungeonResetReason reason) {
        DungeonRunRegistryData.RunRecord run = runId > 0L
                ? DungeonRunRegistryData.get(server).getRun(runId).orElse(null)
                : null;

        if (def != null) {
            DoorPassageTracker.clearRecentForDimensions(def.dimensionIds());
            net.goui.cosmicdungeon.rift.RiftRegistryData.get(server).rebuildForDimensions(server, def.dimensionIds());
        } else {
            DoorPassageTracker.clearAllRecent();
        }

        if (run != null) {
            ServerLevel selectorLevel = ClassSelectorTeleportUtil.resolveLevel(server, run.selectorDimensionId());
            if (selectorLevel != null) {
                ClassSelectorReadyManager.clearFor(selectorLevel, BlockPos.of(run.selectorPosLong()));
            }
        }

        if (runId > 0L) {
            DungeonRunRegistryData.get(server).removeRun(runId);
            DungeonRunProgressData.get(server).clearRun(runId);
        }
    }

    private static boolean allOnlineTrackedPlayersCompletionExited(MinecraftServer server,
                                                                   DungeonRunRegistryData.RunRecord run) {
        int onlineTracked = 0;
        Set<UUID> exited = new HashSet<>(run.completionExitedPlayers());

        for (UUID id : run.orderedPlayers()) {
            if (server.getPlayerList().getPlayer(id) == null) continue;
            onlineTracked++;
            if (!exited.contains(id)) {
                return false;
            }
        }

        return onlineTracked > 0;
    }

    private static boolean shouldAbandonRun(MinecraftServer server,
                                            DungeonRunRegistryData.RunRecord run) {
        int onlineTracked = 0;
        int onlineInsideRun = 0;
        int onlineCompletionExited = 0;

        for (UUID id : run.orderedPlayers()) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null) continue;

            onlineTracked++;

            if (run.isCompletionExited(id)) {
                onlineCompletionExited++;
                continue;
            }

            if (run.containsDimension(p.level().dimension())) {
                onlineInsideRun++;
            }
        }

        if (onlineInsideRun > 0) return false;
        if (onlineTracked > 0 && onlineTracked == onlineCompletionExited) return false;

        return onlineTracked == 0 || onlineInsideRun == 0;
    }

    private static DungeonPlayerRunSnapshot snapshotPlayer(ServerPlayer sp) {
        NonNullList<ItemStack> list = NonNullList.withSize(sp.getInventory().getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < list.size(); i++) {
            list.set(i, sp.getInventory().getItem(i).copy());
        }

        TagValueOutput out = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        ContainerHelper.saveAllItems(out, list);

        return new DungeonPlayerRunSnapshot(sp.getUUID(), out.buildResult());
    }

    private static void applyRecoveryToLivePlayer(MinecraftServer server,
                                                  ServerPlayer sp,
                                                  DungeonDefinition def,
                                                  DungeonPlayerRunSnapshot snapshot,
                                                  boolean forceTeleportToRecovery,
                                                  String reasonText) {
        if (sp == null) return;

        sp.closeContainer();

        boolean completedRun = DungeonResetReason.COMPLETED.name().equalsIgnoreCase(reasonText);
        if (completedRun) {
            clearPlayerInventory(sp);
            resetPlayerRespawnToOverworldSpawn(sp);
        } else {
            restoreInventoryFromSnapshot(sp, snapshot == null ? new CompoundTag() : snapshot.inventoryNbt());
        }
        clearTemporaryPlayerState(sp);
        BloomSharedAdvancements.clearTemporaryBloomProgress(server, sp);
        MetalmancerResonanceTracker.clearForPlayer(sp.getUUID());
        clearOwnedGolems(server, sp.getUUID());

        boolean playerInsideRun = def != null && def.containsDimension(sp.level().dimension());
        if (forceTeleportToRecovery || playerInsideRun) {
            teleportToSafeOverworld(sp);
        }

        sp.sendSystemMessage(
                Component.literal("Dungeon run cleanup applied (" + reasonText + ").")
                        .withStyle(ChatFormatting.YELLOW)
        );
    }

    private static void restoreInventoryFromSnapshot(ServerPlayer sp, CompoundTag inventoryNbt) {
        if (sp == null) return;

        int size = sp.getInventory().getContainerSize();
        NonNullList<ItemStack> list = NonNullList.withSize(size, ItemStack.EMPTY);

        if (inventoryNbt != null && !inventoryNbt.isEmpty()) {
            ValueInput in = TagValueInput.create(ProblemReporter.DISCARDING, sp.level().registryAccess(), inventoryNbt);
            ContainerHelper.loadAllItems(in, list);
        }

        for (int i = 0; i < size; i++) {
            sp.getInventory().setItem(i, list.get(i));
        }

        sp.getInventory().setChanged();
        sp.containerMenu.broadcastChanges();
    }

    public static void clearPlayerInventory(ServerPlayer sp) {
        if (sp == null) return;
        sp.getInventory().clearContent();
        sp.inventoryMenu.broadcastChanges();
        sp.containerMenu.broadcastChanges();
    }

    public static void setPlayerRespawnTo(ServerPlayer sp, ServerLevel level, BlockPos pos, float yaw, float pitch) {
        if (sp == null || level == null || pos == null) return;
        sp.setRespawnPosition(
                new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(level.dimension(), pos, yaw, pitch), true),
                false
        );
    }

    public static void resetPlayerRespawnToOverworldSpawn(ServerPlayer sp) {
        if (sp == null || !(sp.level() instanceof ServerLevel current)) return;

        MinecraftServer server = current.getServer();
        if (server == null) return;

        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        var rd = overworld.getLevelData().getRespawnData();
        BlockPos safe = ensureStandable(overworld, rd.pos());
        setPlayerRespawnTo(sp, overworld, safe, rd.yaw(), rd.pitch());
    }

    private static void clearTemporaryPlayerState(ServerPlayer sp) {
        if (sp == null) return;

        CompoundTag pd = sp.getPersistentData();
        CompoundTag root = pd.getCompoundOrEmpty(ClassData.ROOT_TAG).copy();

        root.putString(ClassData.KEY_CLASS_ID, ClassKeys.CLASS_ID_NONE);
        root.remove(ClassData.KEY_EXTRA);
        root.remove(KEY_METALMANCER_ROOT);
        root.remove(KEY_PENDING_SELECTOR);
        root.remove(KEY_RUN_TEMP);

        pd.put(ClassData.ROOT_TAG, root);

        ClassNet.sendFullTo(sp);
    }

    private static void clearOwnedGolems(MinecraftServer server, UUID ownerId) {
        if (server == null || ownerId == null) return;

        for (ServerLevel level : server.getAllLevels()) {
            AABB search = new AABB(
                    -30_000_000D, level.getMinY(), -30_000_000D,
                    30_000_000D, level.getMaxY(),  30_000_000D
            );

            List<MetalmancerGolemEntity> golems = level.getEntitiesOfClass(
                    MetalmancerGolemEntity.class,
                    search,
                    g -> g.isAlive() && ownerId.equals(g.getOwnerId())
            );

            for (MetalmancerGolemEntity golem : golems) {
                golem.discard();
            }
        }
    }

    private static void teleportToSafeOverworld(ServerPlayer sp) {
        if (sp == null || !(sp.level() instanceof ServerLevel current)) return;

        MinecraftServer server = current.getServer();
        if (server == null) return;

        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        var rd = overworld.getLevelData().getRespawnData();
        BlockPos safe = ensureStandable(overworld, rd.pos());

        sp.teleportTo(
                overworld,
                safe.getX() + 0.5D,
                safe.getY(),
                safe.getZ() + 0.5D,
                Set.of(),
                rd.yaw(),
                rd.pitch(),
                false
        );
    }

    private static BlockPos ensureStandable(ServerLevel level, BlockPos pos) {
        if (isStandable(level, pos)) return pos;

        BlockPos.MutableBlockPos m = pos.mutable();
        int minY = level.getMinY();
        int maxY = minY + level.getLogicalHeight() - 1;

        for (int y = Math.max(minY + 1, m.getY()); y < maxY - 1; y++) {
            m.setY(y);
            if (isStandable(level, m)) return m.immutable();
        }

        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
        m.set(pos.getX(), Math.max(surfaceY, minY + 1), pos.getZ());

        for (int y = m.getY(); y < Math.min(m.getY() + 8, maxY - 1); y++) {
            m.setY(y);
            if (isStandable(level, m)) return m.immutable();
        }

        return m.immutable();
    }
    public static void processPendingResets(MinecraftServer server) {
        if (server == null || PENDING_RESETS.isEmpty()) return;

        long nowTick = 0L;
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld != null) {
            nowTick = overworld.getGameTime();
        }

        List<String> ready = new ArrayList<>();
        for (var e : PENDING_RESETS.entrySet()) {
            PendingReset pr = e.getValue();
            if (nowTick >= pr.nextAttemptTick()) {
                ready.add(e.getKey());
            }
        }

        for (String dungeonId : ready) {
            PendingReset pr = PENDING_RESETS.get(dungeonId);
            if (pr == null) continue;

            DungeonWorldSnapshotService.SnapshotResult result =
                    (pr.snapshotIdOrNull() == null || pr.snapshotIdOrNull().isBlank())
                            ? DungeonWorldSnapshotService.resetToLatest(server, pr.dungeonId())
                            : DungeonWorldSnapshotService.resetToSnapshot(server, pr.dungeonId(), pr.snapshotIdOrNull());

            if (result instanceof DungeonWorldSnapshotService.SnapshotResult.Ok ok) {
                PENDING_RESETS.remove(dungeonId);

                if (pr.runId() > 0L) {
                    DungeonRunRegistryData runs = DungeonRunRegistryData.get(server);
                    runs.getRun(pr.runId()).ifPresent(run -> performMemberCleanup(server, run, pr.reason()));
                }

                DungeonDefinition def = DungeonDefinitions.byId(pr.dungeonId()).orElse(null);
                onSuccessfulReset(server, def, pr.runId(), pr.reason());

                notifyDevelopers(server, Component.literal(
                        "[DungeonLifecycle] " + pr.reason() + " reset complete for "
                                + pr.dungeonId() + " -> " + ok.snapshotId()
                ).withStyle(ChatFormatting.GREEN));

                continue;
            }

            DungeonWorldSnapshotService.SnapshotResult.Error err =
                    (DungeonWorldSnapshotService.SnapshotResult.Error) result;

            int remaining = pr.attemptsRemaining() - 1;
            if (remaining > 0) {
                PENDING_RESETS.put(dungeonId, new PendingReset(
                        pr.dungeonId(),
                        pr.runId(),
                        pr.reason(),
                        pr.snapshotIdOrNull(),
                        nowTick + RESET_RETRY_DELAY_TICKS,
                        remaining
                ));

                notifyDevelopers(server, Component.literal(
                        "[DungeonLifecycle] Reset retry scheduled for " + pr.dungeonId()
                                + " (" + remaining + " attempts left): " + err.message()
                ).withStyle(ChatFormatting.GOLD));
            } else {
                PENDING_RESETS.remove(dungeonId);

                if (pr.runId() > 0L) {
                    DungeonRunRegistryData runs = DungeonRunRegistryData.get(server);
                    runs.setState(pr.runId(), DungeonRunState.FAILED, pr.reason());
                    runs.getRun(pr.runId()).ifPresent(run -> performMemberCleanup(server, run, pr.reason()));
                }

                DungeonDefinition def = DungeonDefinitions.byId(pr.dungeonId()).orElse(null);
                onSuccessfulReset(server, def, pr.runId(), pr.reason());

                notifyDevelopers(server, Component.literal(
                        "[DungeonLifecycle] Reset failed permanently for " + pr.dungeonId()
                                + ": " + err.message()
                                + " | player cleanup was still applied; world contents may remain dirty."
                ).withStyle(ChatFormatting.RED));
            }
        }
    }

    private static void queueReset(MinecraftServer server,
                                   String dungeonId,
                                   long runId,
                                   DungeonResetReason reason,
                                   String snapshotIdOrNull,
                                   long initialDelayTicks) {
        long nowTick = 0L;
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld != null) {
            nowTick = overworld.getGameTime();
        }

        PENDING_RESETS.put(dungeonId, new PendingReset(
                dungeonId,
                runId,
                reason,
                snapshotIdOrNull,
                nowTick + Math.max(0L, initialDelayTicks),
                RESET_MAX_ATTEMPTS
        ));
    }
    private static boolean isStandable(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        var feet = level.getBlockState(pos);
        var head = level.getBlockState(pos.above());

        boolean sturdyBelow = level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
        boolean noFluid = level.getFluidState(pos).isEmpty() && level.getFluidState(pos.above()).isEmpty();
        boolean emptySpace = feet.getCollisionShape(level, pos).isEmpty()
                && head.getCollisionShape(level, pos.above()).isEmpty();

        return sturdyBelow && noFluid && emptySpace;
    }

    private static void notifyDevelopers(MinecraftServer server, Component msg) {
        if (server == null || msg == null) return;

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (AccessPolicy.isDeveloper(p)) {
                p.sendSystemMessage(msg);
            }
        }
    }
}
