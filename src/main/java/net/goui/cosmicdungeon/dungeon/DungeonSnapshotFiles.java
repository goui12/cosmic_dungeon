package net.goui.cosmicdungeon.dungeon;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.UUID;

/** Filesystem operations for immutable snapshots and never-reused instance directories. */
final class DungeonSnapshotFiles {
    void copyFresh(Path source, Path target) throws IOException {
        if (!Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Missing snapshot dimension: " + source);
        Files.createDirectories(target.getParent());
        Files.createDirectory(target); // Never overwrite, merge with, or reopen an old instance.
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!dir.equals(source)) Files.createDirectory(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!attrs.isRegularFile()) throw new IOException("Unsupported snapshot entry: " + file);
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.COPY_ATTRIBUTES);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    void publish(Path destination, Map<String, Path> dimensions) throws IOException {
        if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS))
            throw new FileAlreadyExistsException(destination.toString());
        Path staging = destination.resolveSibling(".saving-" + UUID.randomUUID());
        Files.createDirectory(staging);
        try {
            for (var dimension : dimensions.entrySet()) copyFresh(dimension.getValue(), staging.resolve(dimension.getKey()));
            try {
                Files.move(staging, destination, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(staging, destination);
            }
        } catch (IOException failure) {
            try { remove(staging); } catch (IOException cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    static boolean published(Path path) {
        return !path.getFileName().toString().startsWith(".")
                && Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS);
    }

    /** Caller must verify ownership and close every world/storage handle before removal. */
    void remove(Path directory) throws IOException {
        if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) return;
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Refusing non-directory instance path: " + directory);
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult postVisitDirectory(Path dir, IOException error) throws IOException {
                if (error != null) throw error;
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
