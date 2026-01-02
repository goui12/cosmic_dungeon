// file: src/main/java/net/goui/cosmicdungeon/region/client/RegionLookClient.java
package net.goui.cosmicdungeon.region.client;

import net.goui.cosmicdungeon.network.ModNetwork;
import net.goui.cosmicdungeon.network.payload.RegionLookAllPayload;
import net.goui.cosmicdungeon.network.payload.RegionLookAllRequestPayload;
import net.goui.cosmicdungeon.network.payload.RegionLookPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RegionLookClient {
    private RegionLookClient() {}

    // --- Mode ---
    private static boolean singleEnabled = false;
    private static boolean allEnabled = false;

    // --- Single ---
    public record RenderRegion(String name, BlockPos min, BlockPos max) {}
    private static RenderRegion singleRegion = null;
    private static ResourceKey<Level> singleDimension = Level.OVERWORLD;

    // --- All ---
    private static final List<RenderRegion> allRegions = new ArrayList<>();
    private static ResourceKey<Level> allDimension = Level.OVERWORLD;

    public static boolean isEnabled() {
        return singleEnabled || allEnabled;
    }

    public static boolean isSingleEnabled() {
        return singleEnabled;
    }

    public static boolean isAllEnabled() {
        return allEnabled;
    }

    public static ResourceKey<Level> getSingleDimension() {
        return singleDimension;
    }

    public static ResourceKey<Level> getAllDimension() {
        return allDimension;
    }

    public static RenderRegion getSingleRegion() {
        return singleRegion;
    }

    public static List<RenderRegion> getAllRegions() {
        return allRegions;
    }

    /**
     * Unified list used by the wireframe renderer:
     * - if single is enabled -> returns [singleRegion]
     * - if all is enabled -> returns allRegions
     */
    public static List<RenderRegion> getRegionsToRender() {
        if (singleEnabled && singleRegion != null) {
            return Collections.singletonList(singleRegion);
        }
        if (allEnabled) {
            return allRegions;
        }
        return Collections.emptyList();
    }

    // ===== Incoming: single look =====
    public static void handle(RegionLookPayload payload) {
        // switching to single disables all
        allEnabled = false;
        allRegions.clear();

        singleEnabled = payload.enabled();
        singleDimension = payload.dimension();

        if (!singleEnabled) {
            singleRegion = null;
            return;
        }

        BlockPos min = payload.min();
        BlockPos max = payload.max();

        // Normalize bounds (so renderer never worries about swapped corners)
        int minX = Math.min(min.getX(), max.getX());
        int minY = Math.min(min.getY(), max.getY());
        int minZ = Math.min(min.getZ(), max.getZ());
        int maxX = Math.max(min.getX(), max.getX());
        int maxY = Math.max(min.getY(), max.getY());
        int maxZ = Math.max(min.getZ(), max.getZ());

        singleRegion = new RenderRegion(
                payload.regionName(),
                new BlockPos(minX, minY, minZ),
                new BlockPos(maxX, maxY, maxZ)
        );
    }

    // ===== Incoming: all look =====
    public static void handleAll(RegionLookAllPayload payload) {
        // switching to all disables single
        singleEnabled = false;
        singleRegion = null;

        allEnabled = payload.enabled();
        allDimension = payload.dimension();
        allRegions.clear();

        if (!allEnabled) return;

        if (payload.regions() != null) {
            for (var e : payload.regions()) {
                // Normalize each region bounds
                BlockPos min = e.min();
                BlockPos max = e.max();

                int minX = Math.min(min.getX(), max.getX());
                int minY = Math.min(min.getY(), max.getY());
                int minZ = Math.min(min.getZ(), max.getZ());
                int maxX = Math.max(min.getX(), max.getX());
                int maxY = Math.max(min.getY(), max.getY());
                int maxZ = Math.max(min.getZ(), max.getZ());

                allRegions.add(new RenderRegion(
                        e.name(),
                        new BlockPos(minX, minY, minZ),
                        new BlockPos(maxX, maxY, maxZ)
                ));
            }
        }
    }

    /**
     * Client-side helper to request an updated list based on CURRENT render distance and chunk position.
     * Call this when /region look all is enabled and you want it to track movement.
     */
    public static void requestAllRefreshIfEnabled() {
        if (!allEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        ResourceKey<Level> dim = mc.level.dimension();
        var chunkPos = mc.player.chunkPosition();

        int radius = getClientRenderDistanceChunks(mc);
        RegionLookAllRequestPayload req = new RegionLookAllRequestPayload(dim, chunkPos.x, chunkPos.z, radius);

        ModNetwork.sendToServer(req);
    }

    private static int getClientRenderDistanceChunks(Minecraft mc) {
        try {
            int v = mc.options.renderDistance().get();
            return Mth.clamp(v, 2, 32);
        } catch (Throwable ignored) {
            return 12;
        }
    }

    // ===== Dimension check =====
    public static boolean isInSameDimensionAsClient() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;

        if (singleEnabled) {
            return mc.level.dimension().equals(singleDimension);
        }
        if (allEnabled) {
            return mc.level.dimension().equals(allDimension);
        }
        return false;
    }
}
