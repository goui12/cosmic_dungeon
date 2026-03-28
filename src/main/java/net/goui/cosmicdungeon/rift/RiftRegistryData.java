package net.goui.cosmicdungeon.rift;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;
import java.util.stream.Collectors;

public final class RiftRegistryData extends SavedData {
    private static final String SAVE_ID = "cosmicdungeon_rifts";

    public record DestinationRecord(String name, String dimensionId, long posLong) {
        public static final Codec<DestinationRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("name").forGetter(DestinationRecord::name),
                Codec.STRING.fieldOf("dimension").forGetter(DestinationRecord::dimensionId),
                Codec.LONG.fieldOf("pos").forGetter(DestinationRecord::posLong)
        ).apply(i, DestinationRecord::new));

        public BlockPos pos() { return BlockPos.of(posLong); }
    }

    public record PortalRecord(long anchorLong, String portalName, String destinationName, boolean resetTrigger) {
        public static final Codec<PortalRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("anchor").forGetter(PortalRecord::anchorLong),
                Codec.STRING.fieldOf("name").forGetter(PortalRecord::portalName),
                Codec.STRING.fieldOf("dest").forGetter(PortalRecord::destinationName),
                Codec.BOOL.optionalFieldOf("reset_trigger", false).forGetter(PortalRecord::resetTrigger)
        ).apply(i, PortalRecord::new));

        public BlockPos anchorPos() { return BlockPos.of(anchorLong); }
    }

    public record TileLink(long tileLong, long anchorLong) {
        public static final Codec<TileLink> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("tile").forGetter(TileLink::tileLong),
                Codec.LONG.fieldOf("anchor").forGetter(TileLink::anchorLong)
        ).apply(i, TileLink::new));
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
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    private final Map<String, DestinationRecord> destinations = new HashMap<>();
    private final Long2ObjectOpenHashMap<PortalRecord> portals = new Long2ObjectOpenHashMap<>();
    private final Long2LongOpenHashMap tileToAnchor = new Long2LongOpenHashMap();
    private final Map<String, LongOpenHashSet> destinationToAnchors = new HashMap<>();

    private RiftRegistryData() {
    }

    private static RiftRegistryData fromPersisted(Persisted p) {
        RiftRegistryData d = new RiftRegistryData();

        for (DestinationRecord r : p.destinations()) d.destinations.put(r.name(), r);
        for (PortalRecord r : p.portals()) d.portals.put(r.anchorLong(), r);
        for (TileLink l : p.tileLinks()) d.tileToAnchor.put(l.tileLong(), l.anchorLong());

        for (PortalRecord pr : d.portals.values()) {
            if (pr.destinationName() != null && !pr.destinationName().isBlank()) {
                d.destinationToAnchors
                        .computeIfAbsent(pr.destinationName(), k -> new LongOpenHashSet())
                        .add(pr.anchorLong());
            }
        }

        return d;
    }

    private Persisted toPersisted() {
        List<DestinationRecord> destList = new ArrayList<>(destinations.values());
        List<PortalRecord> portalList = new ArrayList<>();
        portals.values().forEach(portalList::add);

        List<TileLink> tileLinks = new ArrayList<>(tileToAnchor.size());
        tileToAnchor.long2LongEntrySet().forEach(e -> tileLinks.add(new TileLink(e.getLongKey(), e.getLongValue())));

        destList.sort(Comparator.comparing(DestinationRecord::name));
        portalList.sort(Comparator.comparingLong(PortalRecord::anchorLong));
        tileLinks.sort(Comparator.comparingLong(TileLink::tileLong));

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
        LongOpenHashSet users = destinationToAnchors.get(name);
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

    public OptionalLong getAnchorForTile(BlockPos anyTile) {
        long key = anyTile.asLong();
        if (!tileToAnchor.containsKey(key)) return OptionalLong.empty();
        return OptionalLong.of(tileToAnchor.get(key));
    }

    public Optional<PortalRecord> getPortal(long anchorLong) {
        return Optional.ofNullable(portals.get(anchorLong));
    }

    public void registerPortalWithTiles(BlockPos anchor, Collection<Long> tilePositionsPacked) {
        long anchorLong = anchor.asLong();

        portals.putIfAbsent(anchorLong, new PortalRecord(anchorLong, "", "", false));

        for (long packed : tilePositionsPacked) {
            tileToAnchor.put(packed, anchorLong);
        }

        setDirty();
    }

    public SaveResult setPortalConfig(BlockPos anchor, String portalName, String destinationName, boolean resetTrigger) {
        long a = anchor.asLong();
        PortalRecord prev = portals.get(a);
        if (prev == null) return SaveResult.notFound();

        String nameClean = (portalName == null) ? "" : portalName.trim();
        String destClean = (destinationName == null) ? "" : destinationName.trim();

        if (!destClean.isBlank() && !destinations.containsKey(destClean)) {
            return SaveResult.badDestination(destClean);
        }

        String oldDest = prev.destinationName() == null ? "" : prev.destinationName();
        if (!oldDest.isBlank()) {
            LongOpenHashSet set = destinationToAnchors.get(oldDest);
            if (set != null) {
                set.remove(a);
                if (set.isEmpty()) destinationToAnchors.remove(oldDest);
            }
        }

        if (!destClean.isBlank()) {
            destinationToAnchors.computeIfAbsent(destClean, k -> new LongOpenHashSet()).add(a);
        }

        portals.put(a, new PortalRecord(a, nameClean, destClean, resetTrigger));
        setDirty();
        return SaveResult.ok();
    }

    public void onRiftTilesBroken(Collection<Long> tilesPacked) {
        LongOpenHashSet anchors = new LongOpenHashSet();
        for (long t : tilesPacked) {
            if (tileToAnchor.containsKey(t)) anchors.add(tileToAnchor.get(t));
        }

        for (long t : tilesPacked) {
            tileToAnchor.remove(t);
        }

        for (long a : anchors) {
            boolean stillHasChild = false;
            for (var e : tileToAnchor.long2LongEntrySet()) {
                if (e.getLongValue() == a) {
                    stillHasChild = true;
                    break;
                }
            }

            if (!stillHasChild) {
                PortalRecord pr = portals.remove(a);
                if (pr != null && pr.destinationName() != null && !pr.destinationName().isBlank()) {
                    LongOpenHashSet set = destinationToAnchors.get(pr.destinationName());
                    if (set != null) {
                        set.remove(a);
                        if (set.isEmpty()) destinationToAnchors.remove(pr.destinationName());
                    }
                }
            }
        }

        setDirty();
    }

    public List<PortalRecord> listPortalsUsingDestination(String destinationName) {
        LongOpenHashSet set = destinationToAnchors.get(destinationName);
        if (set == null || set.isEmpty()) return List.of();

        List<PortalRecord> out = new ArrayList<>(set.size());
        for (long a : set) {
            PortalRecord pr = portals.get(a);
            if (pr != null) out.add(pr);
        }
        out.sort(Comparator.comparingLong(PortalRecord::anchorLong));
        return out;
    }

    public List<PortalRecord> listAllPortalsSorted() {
        List<PortalRecord> out = new ArrayList<>();
        portals.values().forEach(out::add);
        out.sort(Comparator.comparingLong(PortalRecord::anchorLong));
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
}