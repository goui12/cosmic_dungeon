package net.goui.cosmicdungeon.region;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;
import java.util.stream.Collectors;

public final class RegionRegistryData extends SavedData {

    public static final String SAVE_ID = "cosmicdungeon_regions";

    // ---- Region record (persisted model) ----
    public record Region(
            String name,
            String dimensionId,
            int minX, int minY, int minZ,
            int maxX, int maxY, int maxZ,
            Map<String, String> flags
    ) {
        public static final Codec<Region> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("name").forGetter(Region::name),
                Codec.STRING.fieldOf("dimensionId").forGetter(Region::dimensionId),
                Codec.INT.fieldOf("minX").forGetter(Region::minX),
                Codec.INT.fieldOf("minY").forGetter(Region::minY),
                Codec.INT.fieldOf("minZ").forGetter(Region::minZ),
                Codec.INT.fieldOf("maxX").forGetter(Region::maxX),
                Codec.INT.fieldOf("maxY").forGetter(Region::maxY),
                Codec.INT.fieldOf("maxZ").forGetter(Region::maxZ),
                Codec.unboundedMap(Codec.STRING, Codec.STRING)
                        .optionalFieldOf("flags", Map.of())
                        .forGetter(Region::flags)
        ).apply(inst, Region::new));
    }

    // Persist as a list (forward compatible)
    private static final Codec<List<Region>> REGION_LIST_CODEC = Region.CODEC.listOf();

    public static final Codec<RegionRegistryData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            REGION_LIST_CODEC.optionalFieldOf("regions", List.of()).forGetter(RegionRegistryData::regionsForCodec)
    ).apply(inst, RegionRegistryData::fromCodec));

    // ✅ Correct import + correct constructor order (id, ctor, codec)
    public static final SavedDataType<RegionRegistryData> TYPE = new SavedDataType<>(
            SAVE_ID,
            RegionRegistryData::new,
            CODEC
    );

    // ---- Runtime storage ----
    private final Map<String, Region> regionsByName = new HashMap<>();

    public RegionRegistryData() {
        // empty
    }

    private static RegionRegistryData fromCodec(List<Region> regions) {
        RegionRegistryData data = new RegionRegistryData();
        for (Region r : regions) {
            if (r.name() == null) continue;
            data.regionsByName.put(r.name(), r);
        }
        return data;
    }

    private List<Region> regionsForCodec() {
        return regionsByName.values().stream()
                .sorted(Comparator.comparing(Region::name, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    // ---- Access ----
    public static RegionRegistryData get(ServerLevel level) {
        final MinecraftServer server = level.getServer();
        final ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    // ---- API ----
    public boolean exists(String name) {
        return regionsByName.containsKey(name);
    }

    public Optional<Region> get(String name) {
        return Optional.ofNullable(regionsByName.get(name));
    }

    public List<Region> listSorted() {
        return regionsByName.values().stream()
                .sorted(Comparator.comparing(Region::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public boolean create(String name, String dimensionId, BlockPos a, BlockPos b) {
        if (name == null || name.isBlank()) return false;
        if (dimensionId == null || dimensionId.isBlank()) return false;
        if (regionsByName.containsKey(name)) return false;

        int minX = Math.min(a.getX(), b.getX());
        int minY = Math.min(a.getY(), b.getY());
        int minZ = Math.min(a.getZ(), b.getZ());
        int maxX = Math.max(a.getX(), b.getX());
        int maxY = Math.max(a.getY(), b.getY());
        int maxZ = Math.max(a.getZ(), b.getZ());

        Region region = new Region(
                name,
                dimensionId,
                minX, minY, minZ,
                maxX, maxY, maxZ,
                new HashMap<>() // persisted, empty for now
        );

        regionsByName.put(name, region);
        this.setDirty();
        return true;
    }

    public boolean delete(String name) {
        if (!regionsByName.containsKey(name)) return false;
        regionsByName.remove(name);
        this.setDirty();
        return true;
    }
}
