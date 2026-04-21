package net.goui.cosmicdungeon.dungeon;

import it.unimi.dsi.fastutil.longs.LongIterator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;

public final class DungeonWorldSnapshotService {
    private DungeonWorldSnapshotService() {}

    private static final String SNAPSHOT_ROOT_DIR = "cosmicdungeon_snapshots";
    private static final int MAX_SNAPSHOTS_PER_DUNGEON = 10;

    public sealed interface SnapshotResult {
        record Ok(String snapshotId, Path path) implements SnapshotResult {}
        record Error(String message) implements SnapshotResult {}
    }

    public static SnapshotResult saveSnapshot(MinecraftServer server, String dungeonId) {
        if (server == null || dungeonId == null || dungeonId.isBlank()) {
            return new SnapshotResult.Error("Server or dungeon id was null.");
        }

        DungeonDefinition def = DungeonDefinitions.byId(dungeonId).orElse(null);
        if (def == null) {
            return new SnapshotResult.Error("Unknown dungeon: " + dungeonId);
        }

        try {
            System.out.println("[DUNGEON DEBUG] ==================================================");
            System.out.println("[DUNGEON DEBUG] saveSnapshot START dungeonId=" + dungeonId);

            List<ServerLevel> levels = resolveLoadedLevels(server, def);
            System.out.println("[DUNGEON DEBUG] saveSnapshot resolved levels count=" + levels.size());

            if (levels.isEmpty()) {
                System.out.println("[DUNGEON DEBUG] saveSnapshot ABORT: no loaded levels for " + def.id());
                return new SnapshotResult.Error("No dungeon levels were loaded for " + def.id());
            }

            for (ServerLevel level : levels) {
                Path livePath = getDimensionFolder(server, level.dimension());
                System.out.println("[DUNGEON DEBUG] saveSnapshot checking live dimension path "
                        + level.dimension().location() + " -> " + livePath);
                if (!Files.exists(livePath)) {
                    System.out.println("[DUNGEON DEBUG] saveSnapshot ABORT: live path missing for "
                            + level.dimension().location());
                    return new SnapshotResult.Error("Dimension folder does not exist: " + livePath);
                }
            }

            String snapshotId = buildSnapshotId(def.id());
            Path dungeonSnapshotRoot = getSnapshotRoot(server).resolve(def.id());
            Files.createDirectories(dungeonSnapshotRoot);

            Path snapshotPath = dungeonSnapshotRoot.resolve(snapshotId);
            System.out.println("[DUNGEON DEBUG] saveSnapshot snapshotId=" + snapshotId);
            System.out.println("[DUNGEON DEBUG] saveSnapshot dungeonSnapshotRoot=" + dungeonSnapshotRoot);
            System.out.println("[DUNGEON DEBUG] saveSnapshot snapshotPath=" + snapshotPath);

            if (Files.exists(snapshotPath)) {
                System.out.println("[DUNGEON DEBUG] saveSnapshot ABORT: snapshot folder already exists");
                return new SnapshotResult.Error("Snapshot folder already exists: " + snapshotId);
            }

            System.out.println("[DUNGEON DEBUG] saveSnapshot calling server.saveEverything");
            server.saveEverything(true, false, true);

            for (ServerLevel level : levels) {
                System.out.println("[DUNGEON DEBUG] saveSnapshot saving level " + level.dimension().location());
                level.save(null, true, false);
                level.getChunkSource().save(true);
            }

            for (ServerLevel level : levels) {
                Path livePath = getDimensionFolder(server, level.dimension());
                Path dimOut = snapshotPath.resolve(sanitizeDimensionId(level.dimension()));
                System.out.println("[DUNGEON DEBUG] saveSnapshot copying " + livePath + " -> " + dimOut);
                copyDirectory(livePath, dimOut);
            }

            System.out.println("[DUNGEON DEBUG] saveSnapshot pruning old snapshots in " + dungeonSnapshotRoot);
            pruneSnapshots(dungeonSnapshotRoot);

            System.out.println("[DUNGEON DEBUG] saveSnapshot SUCCESS snapshotId=" + snapshotId);
            System.out.println("[DUNGEON DEBUG] ==================================================");
            return new SnapshotResult.Ok(snapshotId, snapshotPath);
        } catch (Exception e) {
            System.out.println("[DUNGEON DEBUG] saveSnapshot EXCEPTION dungeonId=" + dungeonId + ": " + e);
            e.printStackTrace();
            System.out.println("[DUNGEON DEBUG] ==================================================");
            return new SnapshotResult.Error("Snapshot save failed: " + e.getMessage());
        }
    }

    public static SnapshotResult saveSnapshot(MinecraftServer server, ResourceKey<Level> dimensionKey) {
        DungeonDefinition def = DungeonDefinitions.byDimension(dimensionKey).orElse(null);
        if (def == null) {
            return new SnapshotResult.Error("No logical dungeon definition is registered for " + dimensionKey.location());
        }
        return saveSnapshot(server, def.id());
    }

    public static SnapshotResult resetToLatest(MinecraftServer server, String dungeonId) {
        Optional<String> latest = getLatestSnapshotId(server, dungeonId);
        if (latest.isEmpty()) {
            System.out.println("[DUNGEON DEBUG] resetToLatest no snapshots found for dungeonId=" + dungeonId);
            return new SnapshotResult.Error("No snapshots found for " + dungeonId);
        }
        System.out.println("[DUNGEON DEBUG] resetToLatest selected snapshotId=" + latest.get()
                + " for dungeonId=" + dungeonId);
        return resetToSnapshot(server, dungeonId, latest.get());
    }

    public static SnapshotResult resetToLatest(MinecraftServer server, ResourceKey<Level> dimensionKey) {
        DungeonDefinition def = DungeonDefinitions.byDimension(dimensionKey).orElse(null);
        if (def == null) {
            return new SnapshotResult.Error("No logical dungeon definition is registered for " + dimensionKey.location());
        }
        return resetToLatest(server, def.id());
    }

    public static SnapshotResult resetToSnapshot(MinecraftServer server, String dungeonId, String snapshotId) {
        if (server == null || dungeonId == null || dungeonId.isBlank()) {
            return new SnapshotResult.Error("Server or dungeon id was null.");
        }
        if (snapshotId == null || snapshotId.isBlank()) {
            return new SnapshotResult.Error("Snapshot id was blank.");
        }

        DungeonDefinition def = DungeonDefinitions.byId(dungeonId).orElse(null);
        if (def == null) {
            return new SnapshotResult.Error("Unknown dungeon: " + dungeonId);
        }

        try {
            System.out.println("[DUNGEON DEBUG] ==================================================");
            System.out.println("[DUNGEON DEBUG] resetToSnapshot START dungeonId=" + dungeonId + " snapshotId=" + snapshotId);

            List<String> occupants = getPlayersInDungeon(server, def);
            System.out.println("[DUNGEON DEBUG] resetToSnapshot occupants=" + occupants);
            if (!occupants.isEmpty()) {
                System.out.println("[DUNGEON DEBUG] resetToSnapshot ABORT: players still inside dungeon");
                return new SnapshotResult.Error(
                        "Cannot reset while players are still inside " + def.id() + ": " + occupants
                );
            }

            List<ServerLevel> levels = resolveLoadedLevels(server, def);
            System.out.println("[DUNGEON DEBUG] resetToSnapshot resolved levels count=" + levels.size());
            if (levels.isEmpty()) {
                System.out.println("[DUNGEON DEBUG] resetToSnapshot ABORT: no dungeon levels loaded");
                return new SnapshotResult.Error("No dungeon levels were loaded for " + def.id());
            }

            Path snapshotRoot = getSnapshotRoot(server).resolve(def.id()).resolve(snapshotId);
            System.out.println("[DUNGEON DEBUG] resetToSnapshot snapshotRoot=" + snapshotRoot);
            if (!Files.exists(snapshotRoot)) {
                System.out.println("[DUNGEON DEBUG] resetToSnapshot ABORT: snapshot root missing");
                return new SnapshotResult.Error("Snapshot not found: " + snapshotId);
            }

            for (ServerLevel level : levels) {
                Path livePath = getDimensionFolder(server, level.dimension());
                System.out.println("[DUNGEON DEBUG] resetToSnapshot checking live path for "
                        + level.dimension().location() + " -> " + livePath);
                if (!Files.exists(livePath)) {
                    System.out.println("[DUNGEON DEBUG] resetToSnapshot ABORT: live path missing for "
                            + level.dimension().location());
                    return new SnapshotResult.Error("Live dimension folder does not exist: " + livePath);
                }

                Path dimSnapshot = snapshotRoot.resolve(sanitizeDimensionId(level.dimension()));
                System.out.println("[DUNGEON DEBUG] resetToSnapshot checking snapshot path for "
                        + level.dimension().location() + " -> " + dimSnapshot);
                if (!Files.exists(dimSnapshot)) {
                    System.out.println("[DUNGEON DEBUG] resetToSnapshot ABORT: snapshot path missing for "
                            + level.dimension().location());
                    return new SnapshotResult.Error(
                            "Snapshot is missing data for " + level.dimension().location() + ": " + dimSnapshot
                    );
                }
            }

            System.out.println("[DUNGEON DEBUG] resetToSnapshot calling server.saveEverything");
            server.saveEverything(true, false, true);

            for (ServerLevel level : levels) {
                System.out.println("[DUNGEON DEBUG] resetToSnapshot saving level before restore prep: "
                        + level.dimension().location());
                level.save(null, true, false);
                level.getChunkSource().save(true);
            }

            for (ServerLevel level : levels) {
                System.out.println("[DUNGEON DEBUG] resetToSnapshot preparing level for restore: "
                        + level.dimension().location());
                Optional<String> blocker = prepareLevelForFilesystemRestore(level);
                if (blocker.isPresent()) {
                    System.out.println("[DUNGEON DEBUG] resetToSnapshot ABORT from prepareLevelForFilesystemRestore: "
                            + blocker.get());
                    System.out.println("[DUNGEON DEBUG] ==================================================");
                    System.out.println("[DUNGEON DEBUG] NEW BUILD MARKER 04-11-A");
                    return new SnapshotResult.Error(blocker.get());
                }
            }

            for (ServerLevel level : levels) {
                Path livePath = getDimensionFolder(server, level.dimension());
                Path dimSnapshot = snapshotRoot.resolve(sanitizeDimensionId(level.dimension()));

                System.out.println("[DUNGEON DEBUG] resetToSnapshot forcing chunk IO flush before filesystem restore for "
                        + level.dimension().location());
                flushChunkIoWorker(level);

                System.out.println("[DUNGEON DEBUG] resetToSnapshot deleting live contents for "
                        + level.dimension().location() + " path=" + livePath);
                logDirectoryDiagnostics("live-before-delete", livePath);
                deleteDirectoryContents(livePath);
                logDirectoryDiagnostics("live-after-delete", livePath);

                System.out.println("[DUNGEON DEBUG] resetToSnapshot copying snapshot contents for "
                        + level.dimension().location() + " from " + dimSnapshot + " -> " + livePath);
                logDirectoryDiagnostics("snapshot-source-before-copy", dimSnapshot);
                copyDirectory(dimSnapshot, livePath);
                logDirectoryDiagnostics("live-after-copy", livePath);

                System.out.println("[DUNGEON DEBUG] resetToSnapshot clearing DimensionDataStorage cache for "
                        + level.dimension().location());
                clearDimensionDataCache(level);

                System.out.println("[DUNGEON DEBUG] resetToSnapshot invalidating chunk IO caches for "
                        + level.dimension().location());
                invalidateChunkIoCaches(level);

                System.out.println("[DUNGEON DEBUG] resetToSnapshot clearing runtime chunk access caches for "
                        + level.dimension().location());
                clearChunkSourceHotCaches(level);

                System.out.println("[DUNGEON DEBUG] resetToSnapshot forcing post-restore unload verification for "
                        + level.dimension().location());
                Optional<String> postRestoreBlocker = enforcePostRestoreChunkDrain(level);
                if (postRestoreBlocker.isPresent()) {
                    System.out.println("[DUNGEON DEBUG] resetToSnapshot ABORT from enforcePostRestoreChunkDrain: "
                            + postRestoreBlocker.get());
                    System.out.println("[DUNGEON DEBUG] ==================================================");
                    return new SnapshotResult.Error(postRestoreBlocker.get());
                }
            }

            System.out.println("[DUNGEON DEBUG] resetToSnapshot SUCCESS dungeonId=" + dungeonId
                    + " snapshotId=" + snapshotId);
            System.out.println("[DUNGEON DEBUG] ==================================================");
            return new SnapshotResult.Ok(snapshotId, snapshotRoot);
        } catch (Exception e) {
            System.out.println("[DUNGEON DEBUG] resetToSnapshot EXCEPTION dungeonId=" + dungeonId
                    + " snapshotId=" + snapshotId + ": " + e);
            e.printStackTrace();
            System.out.println("[DUNGEON DEBUG] ==================================================");
            return new SnapshotResult.Error("Snapshot reset failed: " + e.getMessage());
        }
    }

    public static SnapshotResult resetToSnapshot(MinecraftServer server,
                                                 ResourceKey<Level> dimensionKey,
                                                 String snapshotId) {
        DungeonDefinition def = DungeonDefinitions.byDimension(dimensionKey).orElse(null);
        if (def == null) {
            return new SnapshotResult.Error("No logical dungeon definition is registered for " + dimensionKey.location());
        }
        return resetToSnapshot(server, def.id(), snapshotId);
    }

    public static List<String> listSnapshotIds(MinecraftServer server, String dungeonId) {
        if (server == null || dungeonId == null || dungeonId.isBlank()) return List.of();

        Path dungeonSnapshotRoot = getSnapshotRoot(server).resolve(dungeonId);
        if (!Files.exists(dungeonSnapshotRoot) || !Files.isDirectory(dungeonSnapshotRoot)) {
            return List.of();
        }

        try {
            List<Path> dirs = new ArrayList<>();
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(dungeonSnapshotRoot)) {
                for (Path p : ds) {
                    if (Files.isDirectory(p)) dirs.add(p);
                }
            }

            dirs.sort((a, b) -> {
                try {
                    FileTime ta = Files.getLastModifiedTime(a);
                    FileTime tb = Files.getLastModifiedTime(b);
                    return tb.compareTo(ta);
                } catch (IOException e) {
                    return b.getFileName().toString().compareTo(a.getFileName().toString());
                }
            });

            List<String> out = new ArrayList<>(dirs.size());
            for (Path p : dirs) out.add(p.getFileName().toString());
            return List.copyOf(out);
        } catch (IOException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public static List<String> listSnapshotIds(MinecraftServer server, ResourceKey<Level> dimensionKey) {
        DungeonDefinition def = DungeonDefinitions.byDimension(dimensionKey).orElse(null);
        if (def == null) return List.of();
        return listSnapshotIds(server, def.id());
    }

    public static Optional<String> getLatestSnapshotId(MinecraftServer server, String dungeonId) {
        List<String> ids = listSnapshotIds(server, dungeonId);
        return ids.isEmpty() ? Optional.empty() : Optional.of(ids.get(0));
    }

    public static Optional<String> getLatestSnapshotId(MinecraftServer server, ResourceKey<Level> dimensionKey) {
        DungeonDefinition def = DungeonDefinitions.byDimension(dimensionKey).orElse(null);
        if (def == null) return Optional.empty();
        return getLatestSnapshotId(server, def.id());
    }

    public static void sendSnapshotListTo(CommandSourceStack source, String dungeonId) {
        List<String> ids = listSnapshotIds(source.getServer(), dungeonId);
        if (ids.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No snapshots found for " + dungeonId), false);
            return;
        }

        source.sendSuccess(() -> Component.literal("Snapshots for " + dungeonId + ":"), false);
        for (String id : ids) {
            source.sendSuccess(() -> Component.literal(" - " + id), false);
        }
    }

    public static void sendSnapshotListTo(CommandSourceStack source, ResourceKey<Level> dimensionKey) {
        DungeonDefinition def = DungeonDefinitions.byDimension(dimensionKey).orElse(null);
        if (def == null) {
            source.sendSuccess(() -> Component.literal("No logical dungeon definition for " + dimensionKey.location()), false);
            return;
        }
        sendSnapshotListTo(source, def.id());
    }

    private static Optional<String> prepareLevelForFilesystemRestore(ServerLevel level) {
        if (level == null) {
            System.out.println("[DUNGEON DEBUG] prepareLevelForFilesystemRestore: level was null");
            return Optional.of("Reset aborted because a linked level reference was null.");
        }

        String dimId = level.dimension().location().toString();
        ServerChunkCache chunkSource = level.getChunkSource();

        System.out.println("[DUNGEON DEBUG] --------------------------------------------------");
        System.out.println("[DUNGEON DEBUG] PREPARE LEVEL FOR FILESYSTEM RESTORE START");
        System.out.println("[DUNGEON DEBUG] dimension=" + dimId);
        System.out.println("[DUNGEON DEBUG] players=" + level.players().size());
        System.out.println("[DUNGEON DEBUG] playerList=" + describePlayers(level));
        System.out.println("[DUNGEON DEBUG] forcedChunks(before)=" + level.getForceLoadedChunks().size());
        System.out.println("[DUNGEON DEBUG] loadedChunks(before)=" + chunkSource.getLoadedChunksCount());
        System.out.println("[DUNGEON DEBUG] activeTickets(before)=" + chunkSource.hasActiveTickets());

        if (!level.players().isEmpty()) {
            System.out.println("[DUNGEON DEBUG] ABORT: players still present in " + dimId);
            return Optional.of(
                    "Reset aborted because players are still present in "
                            + dimId
                            + ": " + describePlayers(level)
            );
        }

        System.out.println("[DUNGEON DEBUG] clearing forced chunks for " + dimId);
        clearForcedChunks(level);

        System.out.println("[DUNGEON DEBUG] clearing loaded LevelChunk runtime state for " + dimId);
        clearLoadedLevelChunks(level);

        System.out.println("[DUNGEON DEBUG] forcedChunks(after clear)=" + level.getForceLoadedChunks().size());
        System.out.println("[DUNGEON DEBUG] loadedChunks(before unload passes)=" + chunkSource.getLoadedChunksCount());
        System.out.println("[DUNGEON DEBUG] activeTickets(before unload passes)=" + chunkSource.hasActiveTickets());

        System.out.println("[DUNGEON DEBUG] driving unload passes for " + dimId);
        driveUnloadPasses(level);

        int loadedChunks = chunkSource.getLoadedChunksCount();
        int forcedChunks = level.getForceLoadedChunks().size();
        boolean activeTickets = chunkSource.hasActiveTickets();

        int entityCount = 0;
        try {
            for (Entity e : level.getEntities().getAll()) {
                entityCount++;
            }
        } catch (Throwable t) {
            System.out.println("[DUNGEON DEBUG] failed counting entities for " + dimId + ": " + t);
        }

        System.out.println("[DUNGEON DEBUG] POST-UNLOAD SUMMARY for " + dimId);
        System.out.println("[DUNGEON DEBUG] loadedChunks(after)=" + loadedChunks);
        System.out.println("[DUNGEON DEBUG] forcedChunks(after)=" + forcedChunks);
        System.out.println("[DUNGEON DEBUG] activeTickets(after)=" + activeTickets);
        System.out.println("[DUNGEON DEBUG] players(after)=" + level.players().size());
        System.out.println("[DUNGEON DEBUG] entities(after)=" + entityCount);

        if (loadedChunks > 0 || forcedChunks > 0 || activeTickets) {
            StringBuilder sb = new StringBuilder();
            sb.append("Reset aborted because ")
                    .append(dimId)
                    .append(" is still live: loadedChunks=").append(loadedChunks)
                    .append(", forcedChunks=").append(forcedChunks)
                    .append(", activeTickets=").append(activeTickets);

            sb.append(" The reset will need another queued retry after chunk/ticket cleanup settles.");

            System.out.println("[DUNGEON DEBUG] ABORT: " + sb);
            System.out.println("[DUNGEON DEBUG] PREPARE LEVEL FOR FILESYSTEM RESTORE END (FAILED) for " + dimId);
            System.out.println("[DUNGEON DEBUG] --------------------------------------------------");
            return Optional.of(sb.toString());
        }

        System.out.println("[DUNGEON DEBUG] SUCCESS: level is safe for filesystem restore: " + dimId);
        System.out.println("[DUNGEON DEBUG] PREPARE LEVEL FOR FILESYSTEM RESTORE END (SUCCESS) for " + dimId);
        System.out.println("[DUNGEON DEBUG] --------------------------------------------------");
        return Optional.empty();
    }

    private static void driveUnloadPasses(ServerLevel level) {
        ServerChunkCache chunkSource = level.getChunkSource();
        BooleanSupplier alwaysTime = () -> true;

        int lastCount = Integer.MAX_VALUE;
        int stableTicks = 0;
        int maxPasses = 200;

        for (int i = 0; i < maxPasses; i++) {
            int current = chunkSource.getLoadedChunksCount();

            System.out.println("[DUNGEON DEBUG] [" + level.dimension().location() + "] unload pass " + i
                    + " loadedChunks=" + current
                    + " forcedChunks=" + level.getForceLoadedChunks().size()
                    + " activeTickets=" + chunkSource.hasActiveTickets());

            if (current <= 0
                    && level.getForceLoadedChunks().isEmpty()
                    && !chunkSource.hasActiveTickets()) {
                System.out.println("[DUNGEON DEBUG] [" + level.dimension().location() + "] fully drained.");
                return;
            }

            if (current == lastCount) {
                stableTicks++;
            } else {
                stableTicks = 0;
            }

            if (stableTicks > 20) {
                System.out.println("[DUNGEON DEBUG] [" + level.dimension().location() + "] unload stalled at " + current);
                return;
            }

            lastCount = current;

            chunkSource.tick(alwaysTime, true);
            chunkSource.save(true);
        }

        System.out.println("[DUNGEON DEBUG] [" + level.dimension().location() + "] max unload passes reached.");
    }

    private static void clearForcedChunks(ServerLevel level) {
        List<Long> forced = new ArrayList<>();
        LongIterator it = level.getForceLoadedChunks().iterator();
        while (it.hasNext()) {
            forced.add(it.nextLong());
        }

        System.out.println("[DUNGEON DEBUG] clearForcedChunks dimension=" + level.dimension().location()
                + " forcedCount=" + forced.size());

        for (long packed : forced) {
            ChunkPos pos = new ChunkPos(packed);
            System.out.println("[DUNGEON DEBUG] clearForcedChunks removing forced chunk " + pos + " in "
                    + level.dimension().location());
            level.setChunkForced(pos.x, pos.z, false);
        }

        System.out.println("[DUNGEON DEBUG] clearForcedChunks complete dimension=" + level.dimension().location()
                + " forcedChunksNow=" + level.getForceLoadedChunks().size());
    }

    private static void clearLoadedLevelChunks(ServerLevel level) {
        System.out.println("[DUNGEON DEBUG] clearLoadedLevelChunks START for " + level.dimension().location());

        int holdersSeen = 0;
        int levelChunksSeen = 0;
        int unloadCalls = 0;

        try {
            ServerChunkCache chunkSource = level.getChunkSource();

            Field chunkMapField = ServerChunkCache.class.getDeclaredField("chunkMap");
            chunkMapField.setAccessible(true);
            Object chunkMap = chunkMapField.get(chunkSource);

            Field visibleChunkMapField = findField(chunkMap.getClass(), "visibleChunkMap");
            if (visibleChunkMapField == null) {
                System.out.println("[DUNGEON DEBUG] clearLoadedLevelChunks: visibleChunkMap field not found");
                return;
            }

            Object visibleChunkMap = visibleChunkMapField.get(chunkMap);
            if (visibleChunkMap == null) {
                System.out.println("[DUNGEON DEBUG] clearLoadedLevelChunks: visibleChunkMap was null");
                return;
            }

            Method valuesMethod = visibleChunkMap.getClass().getMethod("values");
            Object rawValues = valuesMethod.invoke(visibleChunkMap);
            if (!(rawValues instanceof Iterable<?> iterable)) {
                System.out.println("[DUNGEON DEBUG] clearLoadedLevelChunks: visibleChunkMap.values() was not Iterable");
                return;
            }

            Method getLatestChunkMethod = findNoArgMethod(iterable, "getLatestChunk");
            Method getTickingChunkMethod = findNoArgMethod(iterable, "getTickingChunk");
            Method getChunkToSendMethod = findNoArgMethod(iterable, "getChunkToSend");

            for (Object holder : iterable) {
                if (holder == null) {
                    continue;
                }

                holdersSeen++;

                Object chunkObj = invokeFirstNonNull(holder, getLatestChunkMethod, getTickingChunkMethod, getChunkToSendMethod);
                if (!(chunkObj instanceof LevelChunk levelChunk)) {
                    continue;
                }

                levelChunksSeen++;

                try {
                    System.out.println("[DUNGEON DEBUG] clearLoadedLevelChunks calling level.unload on "
                            + level.dimension().location()
                            + " chunk=" + levelChunk.getPos());
                    level.unload(levelChunk);
                    unloadCalls++;
                } catch (Throwable t) {
                    System.out.println("[DUNGEON DEBUG] clearLoadedLevelChunks failed unloading chunk "
                            + levelChunk.getPos() + ": " + t);
                    t.printStackTrace();
                }
            }
        } catch (Throwable t) {
            System.out.println("[DUNGEON DEBUG] clearLoadedLevelChunks ERROR for "
                    + level.dimension().location() + ": " + t);
            t.printStackTrace();
        }

        System.out.println("[DUNGEON DEBUG] clearLoadedLevelChunks RESULT dimension=" + level.dimension().location()
                + " holdersSeen=" + holdersSeen
                + " levelChunksSeen=" + levelChunksSeen
                + " unloadCalls=" + unloadCalls);
        System.out.println("[DUNGEON DEBUG] clearLoadedLevelChunks END for " + level.dimension().location());
    }

    private static Field findField(Class<?> startClass, String name) {
        Class<?> c = startClass;
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                System.out.println("[DUNGEON DEBUG] findField found " + name + " on " + c.getName());
                return f;
            } catch (NoSuchFieldException ignored) {
            } catch (Throwable t) {
                System.out.println("[DUNGEON DEBUG] findField error while locating " + name + " on "
                        + c.getName() + ": " + t);
                t.printStackTrace();
                return null;
            }
            c = c.getSuperclass();
        }

        System.out.println("[DUNGEON DEBUG] findField could not find field " + name + " starting from " + startClass.getName());
        return null;
    }

    private static void flushChunkIoWorker(ServerLevel level) {
        try {
            ServerChunkCache chunkSource = level.getChunkSource();
            Field chunkMapField = findField(ServerChunkCache.class, "chunkMap");
            if (chunkMapField == null) {
                System.out.println("[DUNGEON DEBUG] flushChunkIoWorker: chunkMap field not found");
                return;
            }

            Object chunkMap = chunkMapField.get(chunkSource);
            if (chunkMap == null) {
                System.out.println("[DUNGEON DEBUG] flushChunkIoWorker: chunkMap was null");
                return;
            }

            Method flushWorker = chunkMap.getClass().getMethod("flushWorker");
            flushWorker.invoke(chunkMap);
            System.out.println("[DUNGEON DEBUG] flushChunkIoWorker: flushWorker invoked for "
                    + level.dimension().location());
        } catch (Throwable t) {
            System.out.println("[DUNGEON DEBUG] flushChunkIoWorker ERROR for "
                    + level.dimension().location() + ": " + t);
            t.printStackTrace();
            throw new RuntimeException("Failed to flush chunk IO worker for " + level.dimension().location(), t);
        }
    }

    private static void invalidateChunkIoCaches(ServerLevel level) {
        try {
            ServerChunkCache chunkSource = level.getChunkSource();

            Field chunkMapField = findField(ServerChunkCache.class, "chunkMap");
            if (chunkMapField == null) {
                System.out.println("[DUNGEON DEBUG] invalidateChunkIoCaches: chunkMap field not found");
                return;
            }

            Object chunkMap = chunkMapField.get(chunkSource);
            if (chunkMap == null) {
                System.out.println("[DUNGEON DEBUG] invalidateChunkIoCaches: chunkMap was null");
                return;
            }

            Field workerField = findField(chunkMap.getClass(), "worker");
            if (workerField == null) {
                System.out.println("[DUNGEON DEBUG] invalidateChunkIoCaches: worker field not found");
                return;
            }

            Object worker = workerField.get(chunkMap);
            if (worker == null) {
                System.out.println("[DUNGEON DEBUG] invalidateChunkIoCaches: worker was null");
                return;
            }

            Field pendingWritesField = findField(worker.getClass(), "pendingWrites");
            if (pendingWritesField != null) {
                Object rawPending = pendingWritesField.get(worker);
                if (rawPending instanceof Map<?, ?> pendingWrites) {
                    int pendingCount = pendingWrites.size();
                    ((Map<?, ?>) pendingWrites).clear();
                    System.out.println("[DUNGEON DEBUG] invalidateChunkIoCaches: cleared pendingWrites="
                            + pendingCount + " for " + level.dimension().location());
                }
            }

            Field storageField = findField(worker.getClass(), "storage");
            if (storageField == null) {
                System.out.println("[DUNGEON DEBUG] invalidateChunkIoCaches: storage field not found");
                return;
            }

            Object storage = storageField.get(worker);
            if (storage == null) {
                System.out.println("[DUNGEON DEBUG] invalidateChunkIoCaches: storage was null");
                return;
            }

            Field regionCacheField = findField(storage.getClass(), "regionCache");
            if (regionCacheField == null) {
                System.out.println("[DUNGEON DEBUG] invalidateChunkIoCaches: regionCache field not found");
                return;
            }

            Object rawRegionCache = regionCacheField.get(storage);
            if (!(rawRegionCache instanceof Iterable<?> iterable)) {
                System.out.println("[DUNGEON DEBUG] invalidateChunkIoCaches: regionCache was not Iterable: "
                        + rawRegionCache);
                return;
            }

            int closed = 0;
            for (Object regionFile : iterable) {
                if (regionFile == null) continue;
                try {
                    Method closeMethod = regionFile.getClass().getMethod("close");
                    closeMethod.invoke(regionFile);
                    closed++;
                } catch (Throwable t) {
                    System.out.println("[DUNGEON DEBUG] invalidateChunkIoCaches: failed closing region file "
                            + regionFile.getClass().getName() + ": " + t);
                    t.printStackTrace();
                }
            }

            if (rawRegionCache instanceof Map<?, ?> regionMap) {
                int before = regionMap.size();
                ((Map<?, ?>) regionMap).clear();
                System.out.println("[DUNGEON DEBUG] invalidateChunkIoCaches: closedRegionFiles=" + closed
                        + " clearedRegionCacheEntries=" + before
                        + " for " + level.dimension().location());
            } else {
                System.out.println("[DUNGEON DEBUG] invalidateChunkIoCaches: regionCache was Iterable but not Map");
            }
        } catch (Throwable t) {
            System.out.println("[DUNGEON DEBUG] invalidateChunkIoCaches ERROR for "
                    + level.dimension().location() + ": " + t);
            t.printStackTrace();
            throw new RuntimeException("Failed to invalidate chunk IO caches for " + level.dimension().location(), t);
        }
    }

    private static void clearChunkSourceHotCaches(ServerLevel level) {
        try {
            ServerChunkCache chunkSource = level.getChunkSource();
            Field chunkMapField = findField(ServerChunkCache.class, "chunkMap");
            if (chunkMapField == null) {
                System.out.println("[DUNGEON DEBUG] clearChunkSourceHotCaches: chunkMap field not found");
                return;
            }

            Object chunkMap = chunkMapField.get(chunkSource);
            if (chunkMap == null) {
                System.out.println("[DUNGEON DEBUG] clearChunkSourceHotCaches: chunkMap was null");
                return;
            }

            int clearedCollections = 0;
            for (String fieldName : List.of(
                    "chunkTypeCache",
                    "nextChunkSaveTime",
                    "chunksToEagerlySave",
                    "lastChunkPos",
                    "lastChunkStatus",
                    "lastChunk"
            )) {
                Field f = findField(chunkMap.getClass(), fieldName);
                if (f == null) {
                    continue;
                }

                Object raw = f.get(chunkMap);
                if (raw == null) {
                    continue;
                }

                boolean cleared = false;
                if (raw instanceof Map<?, ?> map) {
                    int before = map.size();
                    ((Map<?, ?>) raw).clear();
                    System.out.println("[DUNGEON DEBUG] clearChunkSourceHotCaches cleared Map field="
                            + fieldName + " sizeBefore=" + before + " for " + level.dimension().location());
                    cleared = true;
                } else if (raw instanceof java.util.Collection<?> collection) {
                    int before = collection.size();
                    ((java.util.Collection<?>) raw).clear();
                    System.out.println("[DUNGEON DEBUG] clearChunkSourceHotCaches cleared Collection field="
                            + fieldName + " sizeBefore=" + before + " for " + level.dimension().location());
                    cleared = true;
                }

                if (cleared) {
                    clearedCollections++;
                } else {
                    System.out.println("[DUNGEON DEBUG] clearChunkSourceHotCaches field " + fieldName
                            + " is not clearable collection type: " + raw.getClass().getName());
                }
            }

            Method promoteChunkMap = findNoArgMethod(List.of(chunkMap), "promoteChunkMap");
            if (promoteChunkMap != null) {
                try {
                    promoteChunkMap.invoke(chunkMap);
                    System.out.println("[DUNGEON DEBUG] clearChunkSourceHotCaches invoked promoteChunkMap for "
                            + level.dimension().location());
                } catch (Throwable t) {
                    System.out.println("[DUNGEON DEBUG] clearChunkSourceHotCaches failed invoking promoteChunkMap: " + t);
                    t.printStackTrace();
                }
            }

            Method clearCache = findNoArgMethod(List.of(chunkSource), "clearCache");
            if (clearCache != null) {
                try {
                    clearCache.invoke(chunkSource);
                    System.out.println("[DUNGEON DEBUG] clearChunkSourceHotCaches invoked clearCache on chunk source for "
                            + level.dimension().location());
                } catch (Throwable t) {
                    System.out.println("[DUNGEON DEBUG] clearChunkSourceHotCaches failed invoking clearCache: " + t);
                    t.printStackTrace();
                }
            }

            System.out.println("[DUNGEON DEBUG] clearChunkSourceHotCaches done clearedCollections="
                    + clearedCollections + " for " + level.dimension().location());
        } catch (Throwable t) {
            System.out.println("[DUNGEON DEBUG] clearChunkSourceHotCaches ERROR for "
                    + level.dimension().location() + ": " + t);
            t.printStackTrace();
            throw new RuntimeException("Failed clearing chunk source caches for " + level.dimension().location(), t);
        }
    }


    private static Optional<String> enforcePostRestoreChunkDrain(ServerLevel level) {
        if (level == null) {
            return Optional.of("Post-restore chunk drain failed: level was null.");
        }

        ServerChunkCache chunkSource = level.getChunkSource();
        driveUnloadPasses(level);
        clearChunkSourceHotCaches(level);

        int loaded = chunkSource.getLoadedChunksCount();
        boolean activeTickets = chunkSource.hasActiveTickets();
        int forced = level.getForceLoadedChunks().size();

        System.out.println("[DUNGEON DEBUG] enforcePostRestoreChunkDrain summary dimension="
                + level.dimension().location()
                + " loadedChunks=" + loaded
                + " forcedChunks=" + forced
                + " activeTickets=" + activeTickets);

        if (loaded > 0 || forced > 0 || activeTickets) {
            return Optional.of("Reset copied files but runtime chunks are still live in "
                    + level.dimension().location()
                    + " (loadedChunks=" + loaded
                    + ", forcedChunks=" + forced
                    + ", activeTickets=" + activeTickets
                    + "). Reset is retried instead of reporting false success.");
        }

        return Optional.empty();
    }

    private static void logDirectoryDiagnostics(String stage, Path dir) {
        try {
            if (dir == null) {
                System.out.println("[DUNGEON DEBUG] logDirectoryDiagnostics stage=" + stage + " dir=<null>");
                return;
            }

            if (!Files.exists(dir)) {
                System.out.println("[DUNGEON DEBUG] logDirectoryDiagnostics stage=" + stage
                        + " dir=" + dir + " exists=false");
                return;
            }

            long fileCount = 0L;
            long dirCount = 0L;
            long totalBytes = 0L;

            try (var walk = Files.walk(dir)) {
                var it = walk.iterator();
                while (it.hasNext()) {
                    Path p = it.next();
                    if (Files.isDirectory(p)) {
                        dirCount++;
                    } else {
                        fileCount++;
                        try {
                            totalBytes += Files.size(p);
                        } catch (IOException ignored) {
                        }
                    }
                }
            }

            System.out.println("[DUNGEON DEBUG] logDirectoryDiagnostics stage=" + stage
                    + " dir=" + dir
                    + " files=" + fileCount
                    + " dirs=" + dirCount
                    + " totalBytes=" + totalBytes);
        } catch (Throwable t) {
            System.out.println("[DUNGEON DEBUG] logDirectoryDiagnostics FAILED stage=" + stage
                    + " dir=" + dir + " error=" + t);
            t.printStackTrace();
        }
    }

    private static Method findNoArgMethod(Iterable<?> iterable, String methodName) {
        for (Object obj : iterable) {
            if (obj == null) {
                continue;
            }

            Class<?> c = obj.getClass();
            while (c != null) {
                try {
                    Method m = c.getDeclaredMethod(methodName);
                    m.setAccessible(true);
                    System.out.println("[DUNGEON DEBUG] findNoArgMethod found " + methodName + " on " + c.getName());
                    return m;
                } catch (NoSuchMethodException ignored) {
                } catch (Throwable t) {
                    System.out.println("[DUNGEON DEBUG] findNoArgMethod error while locating " + methodName
                            + " on " + c.getName() + ": " + t);
                    t.printStackTrace();
                    return null;
                }
                c = c.getSuperclass();
            }
        }

        System.out.println("[DUNGEON DEBUG] findNoArgMethod could not find method " + methodName);
        return null;
    }

    private static Object invokeFirstNonNull(Object target, Method... methods) {
        for (Method method : methods) {
            if (method == null) {
                continue;
            }

            try {
                Object out = method.invoke(target);
                if (out != null) {
                    return out;
                }
            } catch (Throwable t) {
                System.out.println("[DUNGEON DEBUG] invokeFirstNonNull failed invoking "
                        + method.getName() + " on " + target.getClass().getName() + ": " + t);
                t.printStackTrace();
            }
        }

        return null;
    }

    private static String describePlayers(ServerLevel level) {
        List<String> names = new ArrayList<>();
        for (var p : level.players()) {
            names.add(p.getName().getString());
        }
        return names.toString();
    }

    private static Path getSnapshotRoot(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(SNAPSHOT_ROOT_DIR);
    }

    private static List<ServerLevel> resolveLoadedLevels(MinecraftServer server, DungeonDefinition def) {
        List<ServerLevel> out = new ArrayList<>();
        for (ResourceKey<Level> key : def.dimensions()) {
            ServerLevel level = server.getLevel(key);
            if (level == null) {
                throw new IllegalStateException("Dimension is not loaded: " + key.location());
            }
            out.add(level);
        }
        return out;
    }

    private static Path getDimensionFolder(MinecraftServer server, ResourceKey<Level> dimensionKey) {
        Path root = server.getWorldPath(LevelResource.ROOT);

        if (dimensionKey == Level.OVERWORLD) {
            return root;
        }
        if (dimensionKey == Level.NETHER) {
            return root.resolve("DIM-1");
        }
        if (dimensionKey == Level.END) {
            return root.resolve("DIM1");
        }

        String namespace = dimensionKey.location().getNamespace();
        String path = dimensionKey.location().getPath();

        return root.resolve("dimensions").resolve(namespace).resolve(path);
    }

    private static String buildSnapshotId(String dungeonId) {
        String stamp = new SimpleDateFormat("MM-dd-yyyy_HH-mm-ss").format(new Date());
        return stamp + "_" + dungeonId;
    }

    private static String sanitizeDimensionId(ResourceKey<Level> dim) {
        String raw = dim.location().toString();
        return raw.replace(':', '_').replace('/', '_').replace('\\', '_').replace(' ', '_');
    }

    private static List<String> getPlayersInDungeon(MinecraftServer server, DungeonDefinition def) {
        List<String> names = new ArrayList<>();
        for (var p : server.getPlayerList().getPlayers()) {
            if (def.containsDimension(p.level().dimension())) {
                names.add(p.getName().getString() + "@" + p.level().dimension().location());
            }
        }
        return names;
    }

    private static void pruneSnapshots(Path dungeonSnapshotRoot) throws IOException {
        if (!Files.exists(dungeonSnapshotRoot) || !Files.isDirectory(dungeonSnapshotRoot)) return;

        List<Path> dirs = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dungeonSnapshotRoot)) {
            for (Path p : ds) {
                if (Files.isDirectory(p)) dirs.add(p);
            }
        }

        dirs.sort((a, b) -> {
            try {
                FileTime ta = Files.getLastModifiedTime(a);
                FileTime tb = Files.getLastModifiedTime(b);
                return tb.compareTo(ta);
            } catch (IOException e) {
                return b.getFileName().toString().compareTo(a.getFileName().toString());
            }
        });

        for (int i = MAX_SNAPSHOTS_PER_DUNGEON; i < dirs.size(); i++) {
            System.out.println("[DUNGEON DEBUG] pruneSnapshots deleting old snapshot " + dirs.get(i));
            deleteDirectory(dirs.get(i));
        }
    }

    @SuppressWarnings("unchecked")
    private static void clearDimensionDataCache(ServerLevel level) {
        try {
            System.out.println("[DUNGEON DEBUG] clearDimensionDataCache START for " + level.dimension().location());
            DimensionDataStorage storage = level.getDataStorage();

            Field cacheField = DimensionDataStorage.class.getDeclaredField("cache");
            cacheField.setAccessible(true);

            Object raw = cacheField.get(storage);
            if (raw instanceof Map<?, ?> map) {
                int before = map.size();
                ((Map<Object, Object>) map).clear();
                System.out.println("[DUNGEON DEBUG] clearDimensionDataCache cleared cache entries "
                        + before + " for " + level.dimension().location());
            } else {
                System.out.println("[DUNGEON DEBUG] clearDimensionDataCache raw cache was not a Map for "
                        + level.dimension().location() + ": " + raw);
            }
            System.out.println("[DUNGEON DEBUG] clearDimensionDataCache END for " + level.dimension().location());
        } catch (Exception e) {
            System.out.println("[DUNGEON DEBUG] clearDimensionDataCache EXCEPTION for "
                    + level.dimension().location() + ": " + e);
            throw new RuntimeException("Failed to clear DimensionDataStorage cache for " + level.dimension().location(), e);
        }
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        if (!Files.exists(source)) {
            throw new IOException("Source path does not exist: " + source);
        }

        System.out.println("[DUNGEON DEBUG] copyDirectory START source=" + source + " target=" + target);

        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path rel = source.relativize(dir);
                Path out = target.resolve(rel);
                System.out.println("[DUNGEON DEBUG] copyDirectory creating directory " + out);
                Files.createDirectories(out);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path rel = source.relativize(file);
                Path out = target.resolve(rel);
                System.out.println("[DUNGEON DEBUG] copyDirectory copying file " + file + " -> " + out);
                Files.createDirectories(out.getParent());
                Files.copy(file, out, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                return FileVisitResult.CONTINUE;
            }
        });

        System.out.println("[DUNGEON DEBUG] copyDirectory END source=" + source + " target=" + target);
    }

    private static void deleteDirectoryContents(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            System.out.println("[DUNGEON DEBUG] deleteDirectoryContents skipped missing dir " + dir);
            return;
        }

        System.out.println("[DUNGEON DEBUG] deleteDirectoryContents START dir=" + dir);
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            for (Path child : ds) {
                System.out.println("[DUNGEON DEBUG] deleteDirectoryContents deleting child " + child);
                deleteDirectory(child);
            }
        }
        System.out.println("[DUNGEON DEBUG] deleteDirectoryContents END dir=" + dir);
    }

    private static void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            System.out.println("[DUNGEON DEBUG] deleteDirectory skipped missing dir " + dir);
            return;
        }

        System.out.println("[DUNGEON DEBUG] deleteDirectory START dir=" + dir);

        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                System.out.println("[DUNGEON DEBUG] deleteDirectory deleting file " + file);
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                System.out.println("[DUNGEON DEBUG] deleteDirectory deleting directory " + d);
                Files.deleteIfExists(d);
                return FileVisitResult.CONTINUE;
            }
        });

        System.out.println("[DUNGEON DEBUG] deleteDirectory END dir=" + dir);
    }
}
