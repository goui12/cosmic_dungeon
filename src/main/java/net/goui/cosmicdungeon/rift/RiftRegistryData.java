package net.goui.cosmicdungeon.rift;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.goui.cosmicdungeon.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Collectors;

public final class RiftRegistryData extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SAVE_ID = "cosmicdungeon_rifts_v2";
    private static final String LEGACY_SAVE_ID = "cosmicdungeon_rifts";
    private static final String LEGACY_NETHER_DIM = "minecraft:the_nether";
    private static final String DUNGEON_1_LINKED_NETHER_DIM = "cosmicdungeon:dungeon_1_nether";

    private record PosKey(String dimensionId, long posLong) {
        PosKey {
            dimensionId = normalizeDimensionId(dimensionId);
        }

        BlockPos pos() {
            return BlockPos.of(posLong);
        }
    }

    public record DestinationRecord(String name, String dimensionId, long posLong) {
        public static final Codec<DestinationRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("name").forGetter(DestinationRecord::name),
                Codec.STRING.fieldOf("dimension").forGetter(DestinationRecord::dimensionId),
                Codec.LONG.fieldOf("pos").forGetter(DestinationRecord::posLong)
        ).apply(i, DestinationRecord::new));

        public DestinationRecord {
            dimensionId = normalizeDimensionId(dimensionId);
        }

        public BlockPos pos() {
            return BlockPos.of(posLong);
        }
    }

    public record PortalRecord(String dimensionId, long anchorLong, String portalName, String destinationName, boolean resetTrigger) {
        public static final Codec<PortalRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("dimension", "").forGetter(PortalRecord::dimensionId),
                Codec.LONG.fieldOf("anchor").forGetter(PortalRecord::anchorLong),
                Codec.STRING.fieldOf("name").forGetter(PortalRecord::portalName),
                Codec.STRING.fieldOf("dest").forGetter(PortalRecord::destinationName),
                Codec.BOOL.optionalFieldOf("reset_trigger", false).forGetter(PortalRecord::resetTrigger)
        ).apply(i, PortalRecord::new));

        public PortalRecord {
            dimensionId = normalizeDimensionId(dimensionId);
        }

        public BlockPos anchorPos() {
            return BlockPos.of(anchorLong);
        }

        public boolean hasDimension() {
            return !dimensionId.isBlank();
        }
    }

    public record TileLink(String dimensionId, long tileLong, long anchorLong) {
        public static final Codec<TileLink> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("dimension", "").forGetter(TileLink::dimensionId),
                Codec.LONG.fieldOf("tile").forGetter(TileLink::tileLong),
                Codec.LONG.fieldOf("anchor").forGetter(TileLink::anchorLong)
        ).apply(i, TileLink::new));

        public TileLink {
            dimensionId = normalizeDimensionId(dimensionId);
        }
    }

    private record Persisted(
            List<DestinationRecord> destinations,
            List<PortalRecord> portals,
            List<TileLink> tileLinks
    ) {
        private static final Codec<Persisted> CODEC = RecordCodecBuilder.create(i -> i.group(
                DestinationRecord.CODEC.listOf().fieldOf("destinations").forGetter(Persisted::destinations),
                PortalRecord.CODEC.listOf().fieldOf("portals").forGetter(Persisted::portals),
                TileLink.CODEC.listOf().fieldOf("tiles").forGetter(Persisted::tileLinks)
        ).apply(i, Persisted::new));
    }

    private static final Codec<RiftRegistryData> CODEC = Persisted.CODEC.xmap(
            RiftRegistryData::fromPersisted,
            RiftRegistryData::toPersisted
    );

    public static final SavedDataType<RiftRegistryData> TYPE =
            new SavedDataType<>(SAVE_ID, RiftRegistryData::new, CODEC);

    public static RiftRegistryData get(ServerLevel anyLevel) {
        return get(anyLevel.getServer());
    }

    public static RiftRegistryData get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld is not available; cannot load RiftRegistryData.");
        }

        migrateLegacySaveFileIfNeeded(server);

        RiftRegistryData data = overworld.getDataStorage().computeIfAbsent(TYPE);
        data.applyPostLoadMigrations(server);
        return data;
    }

    private static void migrateLegacySaveFileIfNeeded(MinecraftServer server) {
        Path dataDir = server.getWorldPath(LevelResource.ROOT).resolve("data");
        Path currentFile = dataDir.resolve(SAVE_ID + ".dat");
        Path legacyFile = dataDir.resolve(LEGACY_SAVE_ID + ".dat");

        if (Files.exists(currentFile) || !Files.exists(legacyFile)) {
            return;
        }

        try {
            Files.createDirectories(dataDir);
            Files.copy(legacyFile, currentFile);
            LOGGER.info("[CosmicDungeon] Migrated legacy rift save {} -> {} (legacy file retained).",
                    legacyFile.getFileName(),
                    currentFile.getFileName());
        } catch (IOException e) {
            LOGGER.error("[CosmicDungeon] Failed to migrate legacy rift save file {} -> {}.",
                    legacyFile,
                    currentFile,
                    e);
        }
    }

    private final Map<String, DestinationRecord> destinations = new HashMap<>();
    private final Map<PosKey, PortalRecord> portals = new HashMap<>();
    private final Map<PosKey, PosKey> tileToAnchor = new HashMap<>();
    private final Map<String, Set<PosKey>> destinationToAnchors = new HashMap<>();
    private boolean needsPostLoadMigration = false;

    private RiftRegistryData() {
    }

    private static RiftRegistryData fromPersisted(Persisted p) {
        RiftRegistryData d = new RiftRegistryData();

        for (DestinationRecord r : p.destinations()) {
            d.destinations.put(r.name(), r);
            if (LEGACY_NETHER_DIM.equals(r.dimensionId())) {
                d.needsPostLoadMigration = true;
            }
        }

        for (PortalRecord r : p.portals()) {
            if (!r.hasDimension()) {
                d.needsPostLoadMigration = true;
                continue;
            }

            PosKey key = new PosKey(r.dimensionId(), r.anchorLong());
            d.portals.put(key, r);
            d.addDestinationAnchorIndex(r.destinationName(), key);
        }

        for (TileLink l : p.tileLinks()) {
            if (normalizeDimensionId(l.dimensionId()).isBlank()) {
                d.needsPostLoadMigration = true;
                continue;
            }

            PosKey tileKey = new PosKey(l.dimensionId(), l.tileLong());
            PosKey anchorKey = new PosKey(l.dimensionId(), l.anchorLong());
            d.tileToAnchor.put(tileKey, anchorKey);
        }

        return d;
    }

    private Persisted toPersisted() {
        List<DestinationRecord> destList = new ArrayList<>(destinations.values());
        List<PortalRecord> portalList = new ArrayList<>(portals.values());
        List<TileLink> tileLinks = new ArrayList<>(tileToAnchor.size());

        for (Map.Entry<PosKey, PosKey> e : tileToAnchor.entrySet()) {
            tileLinks.add(new TileLink(e.getKey().dimensionId(), e.getKey().posLong(), e.getValue().posLong()));
        }

        destList.sort(Comparator.comparing(DestinationRecord::name, String.CASE_INSENSITIVE_ORDER));
        portalList.sort(Comparator
                .comparing(PortalRecord::dimensionId, String.CASE_INSENSITIVE_ORDER)
                .thenComparingLong(PortalRecord::anchorLong));
        tileLinks.sort(Comparator
                .comparing(TileLink::dimensionId, String.CASE_INSENSITIVE_ORDER)
                .thenComparingLong(TileLink::tileLong));

        return new Persisted(destList, portalList, tileLinks);
    }

    public boolean destinationExists(String name) {
        return destinations.containsKey(name);
    }

    public Optional<DestinationRecord> getDestination(String name) {
        return Optional.ofNullable(destinations.get(name));
    }

    public List<String> listDestinationNamesSorted() {
        return destinations.keySet().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    public boolean createDestination(String name, ResourceLocation dimensionId, BlockPos pos) {
        if (destinations.containsKey(name)) return false;
        destinations.put(name, new DestinationRecord(name, dimensionId.toString(), pos.asLong()));
        setDirty();
        return true;
    }

    public DeleteResult deleteDestination(String name) {
        Set<PosKey> users = destinationToAnchors.get(name);
        if (users != null && !users.isEmpty()) {
            return DeleteResult.inUse(users.size());
        }
        DestinationRecord removed = destinations.remove(name);
        if (removed == null) return DeleteResult.notFound();
        setDirty();
        return DeleteResult.deleted();
    }

    public sealed interface DeleteResult {
        record Deleted() implements DeleteResult {}
        record NotFound() implements DeleteResult {}
        record InUse(int count) implements DeleteResult {}

        static DeleteResult deleted() { return new Deleted(); }
        static DeleteResult notFound() { return new NotFound(); }
        static DeleteResult inUse(int c) { return new InUse(c); }
    }

    public OptionalLong getAnchorForTile(Level level, BlockPos anyTile) {
        if (level == null || anyTile == null) return OptionalLong.empty();
        PosKey key = tileKey(level, anyTile);
        PosKey anchor = tileToAnchor.get(key);
        return anchor == null ? OptionalLong.empty() : OptionalLong.of(anchor.posLong());
    }

    public Optional<PortalRecord> getPortal(Level level, BlockPos anchorPos) {
        if (level == null || anchorPos == null) return Optional.empty();
        return Optional.ofNullable(portals.get(anchorKey(level, anchorPos)));
    }

    public void registerPortalWithTiles(Level level, BlockPos anchor, Collection<Long> tilePositionsPacked) {
        if (level == null || anchor == null) return;

        PosKey anchorKey = anchorKey(level, anchor);
        PortalRecord existing = portals.get(anchorKey);
        if (existing == null) {
            existing = new PortalRecord(anchorKey.dimensionId(), anchor.asLong(), "", "", false);
            portals.put(anchorKey, existing);
        }

        if (tilePositionsPacked != null) {
            for (long packed : tilePositionsPacked) {
                tileToAnchor.put(new PosKey(anchorKey.dimensionId(), packed), anchorKey);
            }
        }

        setDirty();
    }

    public SaveResult setPortalConfig(Level level, BlockPos anchor, String portalName, String destinationName, boolean resetTrigger) {
        if (level == null || anchor == null) return SaveResult.notFound();

        PosKey key = anchorKey(level, anchor);
        PortalRecord prev = portals.get(key);
        if (prev == null) return SaveResult.notFound();

        String nameClean = portalName == null ? "" : portalName.trim();
        String destClean = destinationName == null ? "" : destinationName.trim();

        if (!destClean.isBlank() && !destinations.containsKey(destClean)) {
            return SaveResult.badDestination(destClean);
        }

        removeDestinationAnchorIndex(prev.destinationName(), key);

        PortalRecord updated = new PortalRecord(key.dimensionId(), anchor.asLong(), nameClean, destClean, resetTrigger);
        portals.put(key, updated);
        addDestinationAnchorIndex(destClean, key);
        setDirty();
        return SaveResult.ok();
    }

    public void onRiftTilesBroken(Level level, Collection<Long> tilesPacked) {
        if (level == null || tilesPacked == null || tilesPacked.isEmpty()) return;

        String dimId = normalizeDimensionId(level.dimension().location().toString());
        Set<PosKey> affectedAnchors = new HashSet<>();

        for (long t : tilesPacked) {
            PosKey tileKey = new PosKey(dimId, t);
            PosKey anchor = tileToAnchor.remove(tileKey);
            if (anchor != null) {
                affectedAnchors.add(anchor);
            }
        }

        for (PosKey anchorKey : affectedAnchors) {
            boolean stillHasChild = false;
            for (PosKey linkedAnchor : tileToAnchor.values()) {
                if (linkedAnchor.equals(anchorKey)) {
                    stillHasChild = true;
                    break;
                }
            }

            if (!stillHasChild) {
                PortalRecord pr = portals.remove(anchorKey);
                if (pr != null) {
                    removeDestinationAnchorIndex(pr.destinationName(), anchorKey);
                }
            }
        }

        setDirty();
    }

    public List<PortalRecord> listPortalsUsingDestination(String destinationName) {
        Set<PosKey> set = destinationToAnchors.get(destinationName);
        if (set == null || set.isEmpty()) return List.of();

        List<PortalRecord> out = new ArrayList<>(set.size());
        for (PosKey key : set) {
            PortalRecord pr = portals.get(key);
            if (pr != null) out.add(pr);
        }
        out.sort(Comparator
                .comparing(PortalRecord::dimensionId, String.CASE_INSENSITIVE_ORDER)
                .thenComparingLong(PortalRecord::anchorLong));
        return out;
    }

    public List<PortalRecord> listAllPortalsSorted() {
        List<PortalRecord> out = new ArrayList<>(portals.values());
        out.sort(Comparator
                .comparing(PortalRecord::dimensionId, String.CASE_INSENSITIVE_ORDER)
                .thenComparingLong(PortalRecord::anchorLong));
        return out;
    }

    public sealed interface SaveResult {
        record Ok() implements SaveResult {}
        record NotFound() implements SaveResult {}
        record BadDestination(String name) implements SaveResult {}

        static SaveResult ok() { return new Ok(); }
        static SaveResult notFound() { return new NotFound(); }
        static SaveResult badDestination(String n) { return new BadDestination(n); }
    }

    public void rebuildForDimensions(MinecraftServer server, Collection<String> dimensionIds) {
        if (server == null || dimensionIds == null || dimensionIds.isEmpty()) return;

        Set<String> targetDims = dimensionIds.stream()
                .filter(Objects::nonNull)
                .map(RiftRegistryData::normalizeDimensionId)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toCollection(HashSet::new));
        if (targetDims.isEmpty()) return;

        Map<PosKey, PortalRecord> oldPortals = new HashMap<>();
        for (Map.Entry<PosKey, PortalRecord> e : portals.entrySet()) {
            if (targetDims.contains(e.getKey().dimensionId())) {
                oldPortals.put(e.getKey(), e.getValue());
            }
        }

        boolean changed = clearPortalStateForDimensions(targetDims);
        changed |= rebuildFromLiveWorld(server, targetDims, oldPortals);

        if (changed) {
            setDirty();
        }
    }

    private void applyPostLoadMigrations(MinecraftServer server) {
        boolean changed = false;

        for (Map.Entry<String, DestinationRecord> e : new ArrayList<>(destinations.entrySet())) {
            DestinationRecord rec = e.getValue();
            if (LEGACY_NETHER_DIM.equals(rec.dimensionId())) {
                destinations.put(e.getKey(), new DestinationRecord(rec.name(), DUNGEON_1_LINKED_NETHER_DIM, rec.posLong()));
                changed = true;
            }
        }

        if (needsPostLoadMigration) {
            // Legacy v1 portal/tile state had no dimension key, so it cannot be trusted after switching
            // Dungeon 1 away from the shared vanilla Nether. Drop only the ambiguous linkage state and keep
            // named destinations, then rebuild live rift linkage for currently loaded levels.
            if (!portals.isEmpty() || !tileToAnchor.isEmpty()) {
                portals.clear();
                tileToAnchor.clear();
                destinationToAnchors.clear();
                changed = true;
            }

            for (ServerLevel level : server.getAllLevels()) {
                rebuildFromLiveLevel(level, Map.of());
            }

            needsPostLoadMigration = false;
            changed = true;
        }

        if (changed) {
            setDirty();
        }
    }

    private boolean rebuildFromLiveWorld(MinecraftServer server, Set<String> targetDims, Map<PosKey, PortalRecord> oldPortals) {
        boolean changed = false;
        for (String dimId : targetDims) {
            ResourceLocation rl = ResourceLocation.tryParse(dimId);
            if (rl == null) continue;
            ResourceKey<Level> key = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, rl);
            ServerLevel level = server.getLevel(key);
            if (level == null) continue;
            changed |= rebuildFromLiveLevel(level, oldPortals);
        }
        return changed;
    }

    private boolean rebuildFromLiveLevel(ServerLevel level, Map<PosKey, PortalRecord> oldPortals) {
        Block riftBlock = ModBlocks.COSMIC_RIFT_TILE.get();
        String dimId = normalizeDimensionId(level.dimension().location().toString());
        Set<Long> visited = new HashSet<>();
        boolean changed = false;

        for (PortalRecord old : oldPortals.values()) {
            if (!dimId.equals(old.dimensionId())) continue;

            long startLong = old.anchorLong();
            if (!visited.add(startLong)) continue;

            BlockPos start = BlockPos.of(startLong);
            if (level.getBlockState(start).getBlock() != riftBlock) continue;

            List<Long> component = collectConnectedTiles(level, start, visited, riftBlock);
            if (component.isEmpty()) continue;

            PosKey anchorKey = new PosKey(dimId, old.anchorLong());
            portals.put(anchorKey, old);
            if (old.destinationName() != null && !old.destinationName().isBlank()) {
                addDestinationAnchorIndex(old.destinationName(), anchorKey);
            }
            for (long packed : component) {
                tileToAnchor.put(new PosKey(dimId, packed), anchorKey);
            }
            changed = true;
        }

        return changed;
    }

    private List<Long> collectConnectedTiles(ServerLevel level, BlockPos start, Set<Long> visited, Block riftBlock) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        List<Long> out = new ArrayList<>();
        int y = start.getY();

        queue.add(start);
        while (!queue.isEmpty()) {
            BlockPos cur = queue.removeFirst();
            if (level.getBlockState(cur).getBlock() != riftBlock) continue;

            out.add(cur.asLong());
            for (BlockPos next : new BlockPos[]{cur.north(), cur.south(), cur.east(), cur.west()}) {
                if (next.getY() != y) continue;
                long packed = next.asLong();
                if (!visited.add(packed)) continue;
                if (level.getBlockState(next).getBlock() != riftBlock) continue;
                queue.add(next);
            }
        }

        return out;
    }

    private boolean clearPortalStateForDimensions(Set<String> targetDims) {
        boolean changed = false;

        if (portals.entrySet().removeIf(e -> targetDims.contains(e.getKey().dimensionId()))) {
            changed = true;
        }
        if (tileToAnchor.entrySet().removeIf(e -> targetDims.contains(e.getKey().dimensionId()))) {
            changed = true;
        }
        if (destinationToAnchors.entrySet().removeIf(e -> {
            e.getValue().removeIf(k -> targetDims.contains(k.dimensionId()));
            return e.getValue().isEmpty();
        })) {
            changed = true;
        }

        return changed;
    }

    private void addDestinationAnchorIndex(String destinationName, PosKey key) {
        if (destinationName == null || destinationName.isBlank() || key == null) return;
        destinationToAnchors.computeIfAbsent(destinationName, k -> new HashSet<>()).add(key);
    }

    private void removeDestinationAnchorIndex(String destinationName, PosKey key) {
        if (destinationName == null || destinationName.isBlank() || key == null) return;
        Set<PosKey> set = destinationToAnchors.get(destinationName);
        if (set == null) return;
        set.remove(key);
        if (set.isEmpty()) {
            destinationToAnchors.remove(destinationName);
        }
    }

    private static PosKey tileKey(Level level, BlockPos pos) {
        return new PosKey(level.dimension().location().toString(), pos.asLong());
    }

    private static PosKey anchorKey(Level level, BlockPos pos) {
        return new PosKey(level.dimension().location().toString(), pos.asLong());
    }

    private static String normalizeDimensionId(String raw) {
        if (raw == null) return "";
        String clean = raw.trim();
        if (clean.isEmpty()) return "";
        return clean.toLowerCase(Locale.ROOT);
    }
}
