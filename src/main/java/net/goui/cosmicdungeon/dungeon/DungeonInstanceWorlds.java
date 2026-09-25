package net.goui.cosmicdungeon.dungeon;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.goui.cosmicdungeon.mixin.DungeonServerAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Owns native level objects and their directories, independently of reusable party slots. */
public final class DungeonInstanceWorlds {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<MinecraftServer, DungeonInstanceWorlds> SERVERS = new WeakHashMap<>();
    private final MinecraftServer server;
    private final DungeonSnapshotFiles files = new DungeonSnapshotFiles();
    // Failed closes stay detached and quarantined; never put a partially closed level back in service.
    private final Map<ResourceKey<Level>, ServerLevel> closing = new HashMap<>();

    private DungeonInstanceWorlds(MinecraftServer server) { this.server = server; }
    public static DungeonInstanceWorlds get(MinecraftServer server) {
        return SERVERS.computeIfAbsent(server, DungeonInstanceWorlds::new);
    }

    public DungeonLifecycleService.InstancePreparation prepare(DungeonDefinition definition, int slot) {
        var registry = DungeonRunRegistryData.get(server);
        DungeonRunRegistryData.RunRecord reservation = null;
        try {
            String snapshot = DungeonWorldSnapshotService.getLatestSnapshotId(server, definition.id())
                    .orElseThrow(() -> new IOException("No saved snapshot. A developer must run /world save " + definition.id()));
            var snapshotRoot = DungeonWorldSnapshotService.getSnapshotRoot(server).resolve(definition.id()).resolve(snapshot);
            for (var template : definition.dimensions()) {
                if (!Files.isDirectory(snapshotRoot.resolve(DungeonWorldSnapshotService.sanitizeDimensionId(template)), LinkOption.NOFOLLOW_LINKS))
                    throw new IOException("Saved snapshot is incomplete: " + snapshot);
            }
            // Refuse pre-existing keys before reserving ownership, including restored/stale folders.
            // Never turn a name collision into permission to delete someone else's directory.
            for (var physical : DungeonInstanceSlots.mapping(definition, slot, registry.nextInstanceRunId()).values()) {
                if (server.getLevel(physical) != null || closing.containsKey(physical)
                        || Files.exists(DungeonWorldSnapshotService.getDimensionFolder(server, physical), LinkOption.NOFOLLOW_LINKS))
                    throw new IOException("Reserved generation already exists; recovery is required for " + physical.location());
            }
            reservation = registry.reserveInstanceVerified(definition, slot);
            var mapping = DungeonInstanceSlots.mapping(definition, reservation);
            for (var pair : mapping.entrySet()) {
                if (server.getLevel(pair.getValue()) != null || closing.containsKey(pair.getValue()))
                    throw new IOException("Instance ID is already loaded: " + pair.getValue().location());
                files.copyFresh(snapshotRoot.resolve(DungeonWorldSnapshotService.sanitizeDimensionId(pair.getKey())),
                        DungeonWorldSnapshotService.getDimensionFolder(server, pair.getValue()));
            }
            restore(reservation);
            net.goui.cosmicdungeon.rift.RiftRegistryData.get(server).copyTemplatePortals(mapping);
            LOGGER.info("[DungeonInstance] Prepared run {} from saved snapshot {} in {}", reservation.runId(), snapshot,
                    reservation.dungeonDimensionIds());
            return new DungeonLifecycleService.PreparedInstance(definition, slot, reservation.runId(), mapping);
        } catch (Exception failure) {
            LOGGER.error("[DungeonInstance] Preparation failed for {}", definition.id(), failure);
            if (reservation != null) discardPreparation(reservation.runId());
            return new DungeonLifecycleService.PreparationError("Could not prepare a fresh dungeon: " + failure.getMessage());
        }
    }

    /** Restart recovery resumes an existing run from its files; it never recaptures the template. */
    public void restore(DungeonRunRegistryData.RunRecord run) throws IOException {
        if (DungeonRunRegistryData.get(server).retiring(run.runId())) return;
        var definition = DungeonDefinitions.byId(run.dungeonId()).orElseThrow();
        var mapping = DungeonInstanceSlots.mapping(definition, run);
        for (var physical : mapping.values()) {
            if (!Files.isDirectory(DungeonWorldSnapshotService.getDimensionFolder(server, physical), LinkOption.NOFOLLOW_LINKS))
                throw new IOException("Existing run directory is missing; recovery retained for " + physical.location());
        }
        for (var pair : mapping.entrySet()) {
            if (server.getLevel(pair.getValue()) == null) open(pair.getKey(), pair.getValue());
        }
    }

    @SuppressWarnings("deprecation")
    private void open(ResourceKey<Level> template, ResourceKey<Level> physical) throws IOException {
        if (closing.containsKey(physical)) throw new IOException("Instance close is still pending: " + physical.location());
        var access = (DungeonServerAccess) server;
        var stem = server.registryAccess().lookupOrThrow(Registries.LEVEL_STEM)
                .getOrThrow(ResourceKey.create(Registries.LEVEL_STEM, template.location())).value();
        // Each world owns a fresh generator as well as fresh chunk/entity/POI storage and caches.
        var ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var isolatedStem = LevelStem.CODEC.parse(ops, LevelStem.CODEC.encodeStart(ops, stem).getOrThrow()).getOrThrow();
        var worldData = server.getWorldData();
        var level = new ServerLevel(server, access.cosmicdungeon$executor(), access.cosmicdungeon$storage(),
                new DerivedLevelData(worldData, worldData.overworldData()), physical, isolatedStem,
                worldData.isDebugWorld(), BiomeManager.obfuscateSeed(worldData.worldGenOptions().seed()),
                List.of(), false, server.overworld().getRandomSequences());
        server.forgeGetWorldMap().put(physical, level);
        server.markWorldsDirty();
        NeoForge.EVENT_BUS.post(new LevelEvent.Load(level));
    }

    public void discardPreparation(long runId) {
        var registry = DungeonRunRegistryData.get(server);
        var run = registry.getRun(runId).orElse(null);
        if (run == null || run.stateEnum() != DungeonRunState.PREPARING) return;
        var result = retire(run);
        if (result instanceof DungeonWorldSnapshotService.SnapshotResult.Ok && registry.retireVerified(runId)) return;
        LOGGER.error("[DungeonInstance] Unfinished preparation {} remains held for startup recovery: {}", runId, result);
    }

    /** Called only after player handoff and companion archival guards have completed. */
    public DungeonWorldSnapshotService.SnapshotResult retire(DungeonRunRegistryData.RunRecord run) {
        if (run == null) return new DungeonWorldSnapshotService.SnapshotResult.Error("Missing run retirement record");
        try {
            var definition = DungeonDefinitions.byId(run.dungeonId()).orElseThrow();
            var mapping = DungeonInstanceSlots.mapping(definition, run); // Reject template/foreign paths.
            var registry = DungeonRunRegistryData.get(server);
            boolean unused = run.unusedPreparation() && !registry.starting(run.runId());
            if (run.stateEnum() == DungeonRunState.PREPARING && !unused)
                throw new IOException("Preparation unexpectedly owns player recovery state");
            for (var physical : mapping.values()) {
                var level = server.getLevel(physical);
                String dimension = physical.location().toString();
                if (level != null && !level.players().isEmpty()) throw new IOException("Players remain in " + dimension);
                if (!unused && !DungeonInventoryHandoffs.dimensionReady(server, dimension))
                    throw new IOException("Inventory recovery still holds " + dimension);
                if (DungeonInventoryEscrowData.get(server).pendingDimension(dimension))
                    throw new IOException("Chop journey recovery still holds " + dimension);
                if (!unused && !registry.retiring(run.runId())) {
                    if (level == null) throw new IOException("Source world unavailable; recovery retained for " + dimension);
                    var blocker = net.goui.cosmicdungeon.playerclass.bogatyr.BogatyrCompanions.resetBlocker(level);
                    if (blocker.isPresent()) throw new IOException(blocker.get());
                }
            }
            // Settle ALL currency sources before the durable marker allows startup to skip loading them.
            for (var physical : mapping.values()) {
                var level = server.getLevel(physical);
                if (level != null && !net.goui.cosmicdungeon.economy.DeathCurrencyService.resetDimension(level))
                    throw new IOException("Death currency requires reconciliation before retiring " + physical.location());
            }
            if (!DungeonRunRegistryData.get(server).prepareRetirementVerified(run.runId()))
                throw new IOException("Instance retirement could not be saved");
            // Close ALL storage handles before deleting ANY dimension, including on retries.
            for (var physical : mapping.values()) close(physical);
            for (var physical : mapping.values()) files.remove(DungeonWorldSnapshotService.getDimensionFolder(server, physical));
            net.goui.cosmicdungeon.rift.RiftRegistryData.get(server).clearInstancePortals(run.dungeonDimensionIds());
            LOGGER.info("[DungeonInstance] Retired run {} and removed {}", run.runId(), run.dungeonDimensionIds());
            return new DungeonWorldSnapshotService.SnapshotResult.Ok("retired-run-" + run.runId(),
                    DungeonWorldSnapshotService.getDimensionFolder(server, mapping.get(definition.primaryDimension())));
        } catch (Exception failure) {
            LOGGER.error("[DungeonInstance] Retirement held for run {}", run.runId(), failure);
            return new DungeonWorldSnapshotService.SnapshotResult.Error(failure.getMessage());
        }
    }

    @SuppressWarnings("deprecation")
    private void close(ResourceKey<Level> physical) throws IOException {
        var level = closing.get(physical);
        if (level == null) {
            level = server.getLevel(physical);
            if (level == null) return;
            if (!level.players().isEmpty()) throw new IOException("Players remain in " + physical.location());
            NeoForge.EVENT_BUS.post(new LevelEvent.Unload(level));
            closing.put(physical, level);
            server.forgeGetWorldMap().remove(physical);
            server.markWorldsDirty();
        }
        level.close();
        closing.remove(physical);
        ((DungeonServerAccess) server).cosmicdungeon$tickTimes().remove(physical);
    }

    public static void stop(MinecraftServer server) {
        var worlds = SERVERS.remove(server);
        if (worlds == null) return;
        for (var level : worlds.closing.values()) {
            try { level.close(); }
            catch (Exception error) { LOGGER.error("[DungeonInstance] Quarantined close failed at shutdown", error); }
        }
        worlds.closing.clear();
    }
}
