// file: src/main/java/net/goui/cosmicdungeon/region/RegionRegistryData.java
package net.goui.cosmicdungeon.region;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.goui.cosmicdungeon.dungeon.DungeonInstanceSlots;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class RegionRegistryData extends SavedData {

    public static final String DATA_NAME = "cosmicdungeon_regions";

    // Inheritance toggles (stored in flags map)
    public static final String FLAG_INHERIT_FLAGS = "inherit.flags";
    public static final String FLAG_INHERIT_EXCEPTIONS = "inherit.exceptions";

    public enum ValueSource {
        DEFAULT,
        INHERITED,
        OVERRIDDEN
    }

    public record ResolvedBool(boolean value, ValueSource source, @Nullable String fromRegion) {}

    public record Region(
            String name,
            String dimensionId,
            BlockPos min,
            BlockPos max,
            Map<String, String> flags,
            @Nullable String parent,
            long createdOrder
    ) {
        public static final Codec<Region> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("name").forGetter(Region::name),
                Codec.STRING.fieldOf("dimensionId").forGetter(Region::dimensionId),
                BlockPos.CODEC.fieldOf("min").forGetter(Region::min),
                BlockPos.CODEC.fieldOf("max").forGetter(Region::max),
                Codec.unboundedMap(Codec.STRING, Codec.STRING)
                        .optionalFieldOf("flags", Map.of())
                        .forGetter(r -> r.flags == null ? Map.of() : r.flags),
                Codec.STRING.optionalFieldOf("parent", "")
                        .forGetter(r -> r.parent == null ? "" : r.parent),
                Codec.LONG.optionalFieldOf("createdOrder", 0L)
                        .forGetter(Region::createdOrder)
        ).apply(inst, (name, dim, min, max, flags, parentStr, createdOrder) -> {
            String parent = parentStr == null || parentStr.isBlank() ? null : parentStr;
            return new Region(name, dim, min, max, new HashMap<>(flags), parent, createdOrder);
        }));
    }

    public static final Codec<RegionRegistryData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.unboundedMap(Codec.STRING, Region.CODEC)
                    .optionalFieldOf("regions", Map.of())
                    .forGetter(d -> d.regionsByName),
            Codec.LONG.optionalFieldOf("nextOrder", 1L)
                    .forGetter(d -> d.nextOrder)
    ).apply(inst, (regions, nextOrder) -> {
        RegionRegistryData data = new RegionRegistryData();
        data.regionsByName.putAll(regions);
        data.nextOrder = Math.max(1L, nextOrder);
        data.fixupOrdersIfNeeded();
        return data;
    }));

    public static final SavedDataType<RegionRegistryData> TYPE =
            new SavedDataType<>(
                    DATA_NAME,
                    ctx -> new RegionRegistryData(),
                    ctx -> CODEC
            );

    private final Map<String, Region> regionsByName = new HashMap<>();
    private long nextOrder = 1L;

    public RegionRegistryData() {}

    public static RegionRegistryData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /* -------------------- Accessors -------------------- */

    public Collection<Region> all() {
        return Collections.unmodifiableCollection(regionsByName.values());
    }

    public List<Region> listSorted() {
        ArrayList<Region> out = new ArrayList<>(regionsByName.values());
        out.sort(Comparator.comparing(Region::name, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    public Optional<Region> get(String name) {
        return Optional.ofNullable(regionsByName.get(name));
    }

    public boolean exists(String name) {
        return regionsByName.containsKey(name);
    }

    /* -------------------- Create/Delete -------------------- */

    public boolean create(String name, String dimensionId, BlockPos a, BlockPos b) {
        if (name == null || name.isBlank()) return false;
        if (dimensionId == null || dimensionId.isBlank()) return false;
        if (a == null || b == null) return false;
        if (regionsByName.containsKey(name)) return false;

        BlockPos min = new BlockPos(
                Math.min(a.getX(), b.getX()),
                Math.min(a.getY(), b.getY()),
                Math.min(a.getZ(), b.getZ())
        );
        BlockPos max = new BlockPos(
                Math.max(a.getX(), b.getX()),
                Math.max(a.getY(), b.getY()),
                Math.max(a.getZ(), b.getZ())
        );

        long order = nextOrder++;
        String parent = pickDefaultParentForNewRegion(dimensionId, min, max);

        Map<String, String> initialFlags = new HashMap<>();
        initialFlags.put("interact", "true"); // default: /region flag interact allow on new regions

        regionsByName.put(name, new Region(name, dimensionId, min, max, initialFlags, parent, order));
        this.setDirty();
        return true;
    }

    public boolean delete(String name) {
        if (!regionsByName.containsKey(name)) return false;

        // Orphan children -> parent=null
        for (Region r : new ArrayList<>(regionsByName.values())) {
            if (name.equals(r.parent())) {
                regionsByName.put(r.name(), new Region(r.name(), r.dimensionId(), r.min(), r.max(), r.flags(), null, r.createdOrder()));
            }
        }

        regionsByName.remove(name);
        this.setDirty();
        return true;
    }

    public boolean copyFlags(String targetRegionName, String sourceRegionName) {
        if (targetRegionName == null || targetRegionName.isBlank()) return false;
        if (sourceRegionName == null || sourceRegionName.isBlank()) return false;
        Region target = regionsByName.get(targetRegionName);
        Region source = regionsByName.get(sourceRegionName);
        if (target == null || source == null) return false;

        Map<String, String> copied = new HashMap<>();
        if (source.flags() != null) copied.putAll(source.flags());

        regionsByName.put(
                targetRegionName,
                new Region(
                        target.name(),
                        target.dimensionId(),
                        target.min(),
                        target.max(),
                        copied,
                        target.parent(),
                        target.createdOrder()
                )
        );
        this.setDirty();
        return true;
    }

    /* -------------------- Parenting -------------------- */

    public boolean setParent(String regionName, @Nullable String newParentName) {
        if (regionName == null || regionName.isBlank()) return false;

        Region r = regionsByName.get(regionName);
        if (r == null) return false;

        String parent = (newParentName == null || newParentName.isBlank()) ? null : newParentName;

        if (parent != null) {
            if (!regionsByName.containsKey(parent)) return false;
            if (parent.equals(regionName)) return false;
            if (isDescendantOf(parent, regionName)) return false; // prevent cycles
        }

        regionsByName.put(regionName, new Region(r.name(), r.dimensionId(), r.min(), r.max(), r.flags(), parent, r.createdOrder()));
        this.setDirty();
        return true;
    }

    public @Nullable String getParentName(String regionName) {
        Region r = regionsByName.get(regionName);
        return r == null ? null : r.parent();
    }

    private boolean isDescendantOf(String candidateChild, String candidateAncestor) {
        String cur = candidateChild;
        int guard = 0;
        while (cur != null && guard++ < 10_000) {
            Region r = regionsByName.get(cur);
            if (r == null) return false;
            String p = r.parent();
            if (p == null) return false;
            if (p.equals(candidateAncestor)) return true;
            cur = p;
        }
        return false;
    }

    private String pickDefaultParentForNewRegion(String dimId, BlockPos newMin, BlockPos newMax) {
        Region best = null;

        for (Region existing : regionsByName.values()) {
            if (!dimId.equals(existing.dimensionId())) continue;
            if (!overlaps(existing.min(), existing.max(), newMin, newMax)) continue;

            if (best == null || existing.createdOrder() < best.createdOrder()) {
                best = existing;
            }
        }

        return best == null ? null : best.name();
    }

    private static boolean overlaps(BlockPos aMinP, BlockPos aMaxP, BlockPos bMinP, BlockPos bMaxP) {
        int aMinX = Math.min(aMinP.getX(), aMaxP.getX());
        int aMaxX = Math.max(aMinP.getX(), aMaxP.getX());
        int aMinY = Math.min(aMinP.getY(), aMaxP.getY());
        int aMaxY = Math.max(aMinP.getY(), aMaxP.getY());
        int aMinZ = Math.min(aMinP.getZ(), aMaxP.getZ());
        int aMaxZ = Math.max(aMinP.getZ(), aMaxP.getZ());

        int bMinX = Math.min(bMinP.getX(), bMaxP.getX());
        int bMaxX = Math.max(bMinP.getX(), bMaxP.getX());
        int bMinY = Math.min(bMinP.getY(), bMaxP.getY());
        int bMaxY = Math.max(bMinP.getY(), bMaxP.getY());
        int bMinZ = Math.min(bMinP.getZ(), bMaxP.getZ());
        int bMaxZ = Math.max(bMinP.getZ(), bMaxP.getZ());

        return aMaxX >= bMinX && bMaxX >= aMinX
                && aMaxY >= bMinY && bMaxY >= aMinY
                && aMaxZ >= bMinZ && bMaxZ >= aMinZ;
    }

    /* -------------------- Query helpers -------------------- */

    public List<Region> regionsAt(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return List.of();

        String dim = DungeonInstanceSlots.templateDimensionForPhysical(level.getServer(), level.dimension()).location().toString();
        if (regionsByName.isEmpty()) return List.of();

        ArrayList<Region> out = new ArrayList<>();
        for (Region r : regionsByName.values()) {
            if (!dim.equals(r.dimensionId())) continue;
            if (contains(r, pos)) out.add(r);
        }
        return out;
    }

    public boolean contains(Region r, BlockPos pos) {
        BlockPos min = r.min();
        BlockPos max = r.max();
        return pos.getX() >= min.getX() && pos.getX() <= max.getX()
                && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    /**
     * Resolve effective region at a position using parenting.
     * Deepest child wins; ties -> smallest volume then newest createdOrder.
     *
     * NOTE: This now delegates to the new single-scan helper.
     */
    public Region effectiveRegionAt(ServerLevel level, BlockPos pos) {
        List<Region> regions = regionsAt(level, pos);
        if (regions.isEmpty()) throw new IllegalArgumentException("No region at pos");
        return effectiveRegionFromList(regions);
    }

    /**
     * NEW API (additive, no behavior change):
     * Compute the effective region from an already-computed regionsAt(...) list.
     *
     * This is the key to eliminating double scans in hot-path event handlers.
     */
    public Region effectiveRegionFromList(List<Region> regionsAtPos) {
        if (regionsAtPos == null || regionsAtPos.isEmpty()) {
            throw new IllegalArgumentException("No region candidates");
        }

        // Preserve original semantics:
        // "Deepest child wins" = remove any candidate that is an ancestor of another candidate.
        ArrayList<Region> regions = (regionsAtPos instanceof ArrayList<Region> al)
                ? new ArrayList<>(al)
                : new ArrayList<>(regionsAtPos);

        ArrayList<Region> candidates = new ArrayList<>(regions);
        candidates.removeIf(r -> {
            for (Region other : regions) {
                if (r == other) continue;
                if (isAncestorOf(r.name(), other.name())) return true;
            }
            return false;
        });

        if (candidates.isEmpty()) candidates = new ArrayList<>(regions);

        Region best = candidates.get(0);
        long bestVol = volume(best);

        for (int i = 1; i < candidates.size(); i++) {
            Region r = candidates.get(i);
            long vol = volume(r);

            if (vol < bestVol) {
                best = r;
                bestVol = vol;
                continue;
            }
            if (vol == bestVol && r.createdOrder() > best.createdOrder()) {
                best = r;
            }
        }

        return best;
    }

    /**
     * NEW API (additive convenience):
     * If a caller already has the regions list, this avoids any chance of rescanning.
     */
    public Region effectiveRegionAt(ServerLevel level, BlockPos pos, List<Region> regionsAtPos) {
        if (regionsAtPos == null || regionsAtPos.isEmpty()) {
            throw new IllegalArgumentException("No region at pos");
        }
        // We intentionally ignore (level,pos) here — they’re only present for call-site clarity.
        return effectiveRegionFromList(regionsAtPos);
    }

    private boolean isAncestorOf(String maybeAncestor, String maybeDescendant) {
        if (maybeAncestor == null || maybeDescendant == null) return false;
        if (maybeAncestor.equals(maybeDescendant)) return false;

        String cur = maybeDescendant;
        int guard = 0;
        while (cur != null && guard++ < 10_000) {
            Region r = regionsByName.get(cur);
            if (r == null) return false;
            String p = r.parent();
            if (p == null) return false;
            if (p.equals(maybeAncestor)) return true;
            cur = p;
        }
        return false;
    }

    private static long volume(Region r) {
        BlockPos min = r.min();
        BlockPos max = r.max();
        long dx = (long) max.getX() - (long) min.getX() + 1L;
        long dy = (long) max.getY() - (long) min.getY() + 1L;
        long dz = (long) max.getZ() - (long) min.getZ() + 1L;
        return Math.max(1L, dx) * Math.max(1L, dy) * Math.max(1L, dz);
    }

    /* -------------------- Flags -------------------- */

    public boolean setFlag(String regionName, String key, @Nullable String value) {
        if (regionName == null || regionName.isBlank()) return false;
        if (key == null || key.isBlank()) return false;

        Region r = regionsByName.get(regionName);
        if (r == null) return false;

        Map<String, String> flags = (r.flags() == null) ? new HashMap<>() : new HashMap<>(r.flags());
        if (value == null || value.isBlank()) {
            flags.remove(key);
        } else {
            flags.put(key, value);
        }

        regionsByName.put(regionName, new Region(r.name(), r.dimensionId(), r.min(), r.max(), flags, r.parent(), r.createdOrder()));
        this.setDirty();
        return true;
    }

    public boolean hasLocalFlag(Region r, String key) {
        if (r == null || key == null) return false;
        Map<String, String> flags = r.flags();
        return flags != null && flags.containsKey(key);
    }

    public boolean isFlagTrueOrDefault(Region r, String key, boolean defaultIfMissing) {
        if (r == null) return defaultIfMissing;
        Map<String, String> flags = r.flags();
        if (flags == null) return defaultIfMissing;

        String v = flags.get(key);
        if (v == null) return defaultIfMissing;

        v = v.trim().toLowerCase(Locale.ROOT);
        return v.equals("true") || v.equals("1") || v.equals("yes") || v.equals("allow") || v.equals("enabled");
    }

    public boolean inheritFlagsEnabled(Region r) {
        // default TRUE
        return isFlagTrueOrDefault(r, FLAG_INHERIT_FLAGS, true);
    }

    public boolean inheritExceptionsEnabled(Region r) {
        // default TRUE
        return isFlagTrueOrDefault(r, FLAG_INHERIT_EXCEPTIONS, true);
    }

    public ResolvedBool resolveFlagBool(Region start, String key, boolean defaultIfMissing) {
        return resolveBool(start, key, defaultIfMissing, true);
    }

    public ResolvedBool resolveExceptionBool(Region start, String key, boolean defaultIfMissing) {
        return resolveBool(start, key, defaultIfMissing, false);
    }

    private ResolvedBool resolveBool(Region start, String key, boolean defaultIfMissing, boolean isFlagNotException) {
        if (start == null) return new ResolvedBool(defaultIfMissing, ValueSource.DEFAULT, null);

        Region cur = start;
        int guard = 0;

        while (cur != null && guard++ < 10_000) {
            if (hasLocalFlag(cur, key)) {
                boolean v = isFlagTrueOrDefault(cur, key, defaultIfMissing);
                ValueSource src = (cur == start) ? ValueSource.OVERRIDDEN : ValueSource.INHERITED;
                return new ResolvedBool(v, src, cur.name());
            }

            boolean inherit = isFlagNotException ? inheritFlagsEnabled(cur) : inheritExceptionsEnabled(cur);
            if (!inherit) break;

            String p = cur.parent();
            if (p == null || p.isBlank()) break;

            cur = regionsByName.get(p);
        }

        return new ResolvedBool(defaultIfMissing, ValueSource.DEFAULT, null);
    }

    /* -------------------- Fixups -------------------- */

    private void fixupOrdersIfNeeded() {
        long max = 0L;
        boolean needs = false;

        for (Region r : regionsByName.values()) {
            if (r.createdOrder() <= 0L) needs = true;
            max = Math.max(max, r.createdOrder());
        }

        if (!needs) {
            nextOrder = Math.max(nextOrder, max + 1L);
            return;
        }

        ArrayList<Region> list = new ArrayList<>(regionsByName.values());
        list.sort(Comparator.comparing(Region::name, String.CASE_INSENSITIVE_ORDER));

        long order = 1L;
        for (Region r : list) {
            long o = r.createdOrder();
            if (o > 0L) {
                order = Math.max(order, o + 1L);
                continue;
            }
            regionsByName.put(r.name(), new Region(r.name(), r.dimensionId(), r.min(), r.max(), r.flags(), r.parent(), order++));
        }

        long newMax = 0L;
        for (Region r : regionsByName.values()) newMax = Math.max(newMax, r.createdOrder());
        nextOrder = Math.max(nextOrder, newMax + 1L);
        this.setDirty();
    }
}
