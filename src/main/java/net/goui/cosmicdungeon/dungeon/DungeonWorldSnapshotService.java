package net.goui.cosmicdungeon.dungeon;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.BooleanSupplier;

public final class DungeonWorldSnapshotService {
    private DungeonWorldSnapshotService() {}

    private static final String SNAPSHOT_ROOT_DIR = "cosmicdungeon_snapshots";
    private static final int MAX_SNAPSHOTS_PER_WORLD = 10;

    public sealed interface SnapshotResult {
        record Ok(String snapshotId, Path path) implements SnapshotResult {}
        record Error(String message) implements SnapshotResult {}
    }

    public static SnapshotResult saveSnapshot(MinecraftServer server, ResourceKey<Level> dimensionKey) {
        if (server == null || dimensionKey == null) {
            return new SnapshotResult.Error("Server or dimension was null.");
        }

        if (dimensionKey == Level.OVERWORLD) {
            return new SnapshotResult.Error("Saving the entire Overworld root is intentionally blocked. Use dungeon dimensions only.");
        }

        ServerLevel level = server.getLevel(dimensionKey);
        if (level == null) {
            return new SnapshotResult.Error("Dimension is not loaded: " + dimensionKey.location());
        }

        try {
            Path livePath = getDimensionFolder(server, dimensionKey);
            if (!Files.exists(livePath)) {
                return new SnapshotResult.Error("Dimension folder does not exist: " + livePath);
            }

            String worldName = sanitizeWorldName(dimensionKey.location().getPath());
            String snapshotId = buildSnapshotId(worldName);

            Path worldSnapshotRoot = getSnapshotRoot(server).resolve(worldName);
            Files.createDirectories(worldSnapshotRoot);

            Path snapshotPath = worldSnapshotRoot.resolve(snapshotId);
            if (Files.exists(snapshotPath)) {
                return new SnapshotResult.Error("Snapshot folder already exists: " + snapshotId);
            }

            // Flush live world state first.
            server.saveEverything(true, false, true);
            level.save(null, true, false);
            level.getChunkSource().save(true);

            copyDirectory(livePath, snapshotPath);
            pruneSnapshots(worldSnapshotRoot);

            return new SnapshotResult.Ok(snapshotId, snapshotPath);
        } catch (Exception e) {
            e.printStackTrace();
            return new SnapshotResult.Error("Snapshot save failed: " + e.getMessage());
        }
    }

    public static SnapshotResult resetToLatest(MinecraftServer server, ResourceKey<Level> dimensionKey) {
        Optional<String> latest = getLatestSnapshotId(server, dimensionKey);
        if (latest.isEmpty()) {
            return new SnapshotResult.Error("No snapshots found for " + dimensionKey.location());
        }
        return resetToSnapshot(server, dimensionKey, latest.get());
    }

    public static SnapshotResult resetToSnapshot(MinecraftServer server, ResourceKey<Level> dimensionKey, String snapshotId) {
        if (server == null || dimensionKey == null) {
            return new SnapshotResult.Error("Server or dimension was null.");
        }

        if (dimensionKey == Level.OVERWORLD) {
            return new SnapshotResult.Error("Resetting the entire Overworld root is intentionally blocked. Use dungeon dimensions only.");
        }

        ServerLevel level = server.getLevel(dimensionKey);
        if (level == null) {
            return new SnapshotResult.Error("Dimension is not loaded: " + dimensionKey.location());
        }

        try {
            List<String> occupants = getPlayersInDimension(server, dimensionKey);
            if (!occupants.isEmpty()) {
                return new SnapshotResult.Error("Cannot reset while players are still inside " + dimensionKey.location() + ": " + occupants);
            }

            String worldName = sanitizeWorldName(dimensionKey.location().getPath());
            Path snapshotPath = getSnapshotRoot(server).resolve(worldName).resolve(snapshotId);
            if (!Files.exists(snapshotPath)) {
                return new SnapshotResult.Error("Snapshot not found: " + snapshotId);
            }

            Path livePath = getDimensionFolder(server, dimensionKey);
            if (!Files.exists(livePath)) {
                return new SnapshotResult.Error("Live dimension folder does not exist: " + livePath);
            }

            // Save/flush everything.
            server.saveEverything(true, false, true);
            level.save(null, true, false);
            level.getChunkSource().save(true);

            // Force as many chunks out of memory as possible before touching files.
            boolean unloaded = purgeLoadedChunks(level);
            if (!unloaded) {
                return new SnapshotResult.Error(
                        "Reset aborted because loaded chunks would not fully unload for " + dimensionKey.location() +
                                ". This protects you from a half-live, half-restored state."
                );
            }

            deleteDirectoryContents(livePath);
            copyDirectory(snapshotPath, livePath);

            return new SnapshotResult.Ok(snapshotId, snapshotPath);
        } catch (Exception e) {
            e.printStackTrace();
            return new SnapshotResult.Error("Snapshot reset failed: " + e.getMessage());
        }
    }

    public static List<String> listSnapshotIds(MinecraftServer server, ResourceKey<Level> dimensionKey) {
        if (server == null || dimensionKey == null) return List.of();

        String worldName = sanitizeWorldName(dimensionKey.location().getPath());
        Path worldSnapshotRoot = getSnapshotRoot(server).resolve(worldName);

        if (!Files.exists(worldSnapshotRoot) || !Files.isDirectory(worldSnapshotRoot)) {
            return List.of();
        }

        try {
            List<Path> dirs = new ArrayList<>();
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(worldSnapshotRoot)) {
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
            return out;
        } catch (IOException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public static Optional<String> getLatestSnapshotId(MinecraftServer server, ResourceKey<Level> dimensionKey) {
        List<String> ids = listSnapshotIds(server, dimensionKey);
        return ids.isEmpty() ? Optional.empty() : Optional.of(ids.get(0));
    }

    public static void sendSnapshotListTo(CommandSourceStack source, ResourceKey<Level> dimensionKey) {
        List<String> ids = listSnapshotIds(source.getServer(), dimensionKey);
        if (ids.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No snapshots found for " + dimensionKey.location()), false);
            return;
        }

        source.sendSuccess(() -> Component.literal("Snapshots for " + dimensionKey.location() + ":"), false);
        for (String id : ids) {
            source.sendSuccess(() -> Component.literal(" - " + id), false);
        }
    }

    private static Path getSnapshotRoot(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(SNAPSHOT_ROOT_DIR);
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

    private static String sanitizeWorldName(String raw) {
        if (raw == null || raw.isBlank()) return "unknown_dimension";
        return raw.replace(':', '_').replace('/', '_').replace('\\', '_').replace(' ', '_');
    }

    private static String buildSnapshotId(String worldName) {
        String stamp = new SimpleDateFormat("MM-dd-yyyy_HH-mm-ss").format(new Date());
        return stamp + "_" + worldName;
    }

    private static List<String> getPlayersInDimension(MinecraftServer server, ResourceKey<Level> dimensionKey) {
        List<String> names = new ArrayList<>();
        for (var p : server.getPlayerList().getPlayers()) {
            if (p.level().dimension().equals(dimensionKey)) {
                names.add(p.getGameProfile().getName());
            }
        }
        return names;
    }

    private static void pruneSnapshots(Path worldSnapshotRoot) throws IOException {
        if (!Files.exists(worldSnapshotRoot) || !Files.isDirectory(worldSnapshotRoot)) return;

        List<Path> dirs = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(worldSnapshotRoot)) {
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

        for (int i = MAX_SNAPSHOTS_PER_WORLD; i < dirs.size(); i++) {
            deleteDirectory(dirs.get(i));
        }
    }

    private static boolean purgeLoadedChunks(ServerLevel level) {
        try {
            ServerChunkCache chunkSource = level.getChunkSource();

            Field chunkMapField = ServerChunkCache.class.getDeclaredField("chunkMap");
            chunkMapField.setAccessible(true);
            Object chunkMap = chunkMapField.get(chunkSource);

            Field toDropField = chunkMap.getClass().getDeclaredField("toDrop");
            toDropField.setAccessible(true);
            Object toDropObj = toDropField.get(chunkMap);

            Field visibleField = chunkMap.getClass().getDeclaredField("visibleChunkMap");
            visibleField.setAccessible(true);
            Object visibleMap = visibleField.get(chunkMap);

            Field updatingField = chunkMap.getClass().getDeclaredField("updatingChunkMap");
            updatingField.setAccessible(true);
            Object updatingMap = updatingField.get(chunkMap);

            if (toDropObj instanceof it.unimi.dsi.fastutil.longs.LongSet toDrop) {
                if (visibleMap instanceof it.unimi.dsi.fastutil.longs.Long2ObjectMap<?> visible) {
                    for (long key : visible.keySet()) toDrop.add(key);
                }
                if (updatingMap instanceof it.unimi.dsi.fastutil.longs.Long2ObjectMap<?> updating) {
                    for (long key : updating.keySet()) toDrop.add(key);
                }
            }

            BooleanSupplier alwaysTime = () -> true;

            for (int i = 0; i < 200; i++) {
                if (chunkSource.getLoadedChunksCount() <= 0) {
                    return true;
                }

                chunkSource.tick(alwaysTime, false);
                chunkSource.save(true);

                try {
                    Thread.sleep(5L);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }

            return chunkSource.getLoadedChunksCount() <= 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        if (!Files.exists(source)) {
            throw new IOException("Source path does not exist: " + source);
        }

        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path rel = source.relativize(dir);
                Path out = target.resolve(rel);
                Files.createDirectories(out);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path rel = source.relativize(file);
                Path out = target.resolve(rel);
                Files.createDirectories(out.getParent());
                Files.copy(file, out, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void deleteDirectoryContents(Path dir) throws IOException {
        if (!Files.exists(dir)) return;

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            for (Path child : ds) {
                deleteDirectory(child);
            }
        }
    }

    private static void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) return;

        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                Files.deleteIfExists(d);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}