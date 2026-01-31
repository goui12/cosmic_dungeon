// file: src/main/java/net/goui/cosmicdungeon/region/RegionLookServer.java
package net.goui.cosmicdungeon.region;

import net.goui.cosmicdungeon.network.ModNetwork;
import net.goui.cosmicdungeon.network.payload.RegionLookAllPayload;
import net.goui.cosmicdungeon.network.payload.RegionLookAllRequestPayload;
import net.goui.cosmicdungeon.network.payload.RegionLookPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RegionLookServer {
    private RegionLookServer() {}

    // Per-player toggles for single-region look
    private static final ConcurrentHashMap<UUID, Set<String>> ACTIVE_SINGLE = new ConcurrentHashMap<>();

    // Per-player toggle for "all"
    private static final Set<UUID> ACTIVE_ALL = ConcurrentHashMap.newKeySet();

    public static void toggle(ServerPlayer player, String regionNameRaw) {
        if (player == null) return;
        if (player.level().isClientSide()) return;

        // Turning on single look should turn off "all"
        ACTIVE_ALL.remove(player.getUUID());

        final String regionName = regionNameRaw == null ? "" : regionNameRaw.trim();
        if (regionName.isBlank()) {
            player.displayClientMessage(Component.literal("Usage: /region look <name>"), false);
            return;
        }

        final ServerLevel level = player.level();
        final RegionRegistryData data = RegionRegistryData.get(level);

        final var opt = data.get(regionName);
        if (opt.isEmpty()) {
            player.displayClientMessage(Component.literal("Unknown region: " + regionName), false);
            return;
        }

        final RegionRegistryData.Region r = opt.get();

        final ResourceKey<Level> dimKey = parseDimKey(r.dimensionId());
        if (dimKey == null) {
            player.displayClientMessage(Component.literal("Region has invalid dimensionId: " + r.dimensionId()), false);
            return;
        }

        final BlockPos min = r.min();
        final BlockPos max = r.max();

        final boolean enabled = flipSingle(player.getUUID(), regionName);

        ModNetwork.sendTo(player, new RegionLookPayload(enabled, regionName, dimKey, min, max));

        player.displayClientMessage(
                Component.literal((enabled ? "Showing " : "Hiding ") + regionName),
                false
        );
    }

    public static void toggleAll(ServerPlayer player) {
        if (player == null) return;
        if (player.level().isClientSide()) return;

        UUID id = player.getUUID();

        // Turning on "all" should disable any single toggles
        ACTIVE_SINGLE.remove(id);

        if (ACTIVE_ALL.remove(id)) {
            // disable
            ModNetwork.sendTo(player, new RegionLookAllPayload(false, player.level().dimension(), java.util.List.of()));
            player.displayClientMessage(Component.literal("Hiding all nearby regions"), false);
            return;
        }

        ACTIVE_ALL.add(id);

        // Snapshot using player's current chunk + a conservative radius (client will request exact refresh)
        int cx = player.chunkPosition().x;
        int cz = player.chunkPosition().z;
        int radius = 12;
        sendAllSnapshot(player, player.level(), player.level().dimension(), cx, cz, radius);

        player.displayClientMessage(Component.literal("Showing all nearby regions (rendered area)"), false);
    }

    /** Called by server payload handler when client wants refresh based on render distance. */
    public static void refreshAllFor(ServerPlayer player, RegionLookAllRequestPayload req) {
        if (player == null) return;
        if (player.level().isClientSide()) return;

        UUID id = player.getUUID();
        if (!ACTIVE_ALL.contains(id)) {
            return; // ignore refresh if not in "all" mode
        }

        ServerLevel level = player.level();

        // Ensure same dimension
        if (!level.dimension().equals(req.dimension())) {
            ModNetwork.sendTo(player, new RegionLookAllPayload(true, level.dimension(), java.util.List.of()));
            return;
        }

        int radius = clamp(req.radiusChunks(), 2, 32);
        sendAllSnapshot(player, level, req.dimension(), req.centerChunkX(), req.centerChunkZ(), radius);
    }

    private static void sendAllSnapshot(ServerPlayer player, ServerLevel level, ResourceKey<Level> dim,
                                        int centerChunkX, int centerChunkZ, int radiusChunks) {

        RegionRegistryData data = RegionRegistryData.get(level);

        // Convert chunk radius into block bounds (inclusive)
        int minBlockX = (centerChunkX - radiusChunks) << 4;
        int maxBlockX = ((centerChunkX + radiusChunks) << 4) + 15;
        int minBlockZ = (centerChunkZ - radiusChunks) << 4;
        int maxBlockZ = ((centerChunkZ + radiusChunks) << 4) + 15;

        // Y: wide so we don’t miss tall regions; still intersect properly
        int minY = -2048;
        int maxY = 2048;

        ArrayList<RegionLookAllPayload.Entry> out = new ArrayList<>();

        for (var r : data.listSorted()) {
            ResourceKey<Level> rDim = parseDimKey(r.dimensionId());
            if (rDim == null) continue;
            if (!rDim.equals(dim)) continue;

            BlockPos rMinP = r.min();
            BlockPos rMaxP = r.max();

            int rMinX = Math.min(rMinP.getX(), rMaxP.getX());
            int rMaxX = Math.max(rMinP.getX(), rMaxP.getX());
            int rMinY = Math.min(rMinP.getY(), rMaxP.getY());
            int rMaxY = Math.max(rMinP.getY(), rMaxP.getY());
            int rMinZ = Math.min(rMinP.getZ(), rMaxP.getZ());
            int rMaxZ = Math.max(rMinP.getZ(), rMaxP.getZ());

            if (!intersects(rMinX, rMaxX, minBlockX, maxBlockX)) continue;
            if (!intersects(rMinZ, rMaxZ, minBlockZ, maxBlockZ)) continue;
            if (!intersects(rMinY, rMaxY, minY, maxY)) continue;

            BlockPos min = new BlockPos(rMinX, rMinY, rMinZ);
            BlockPos max = new BlockPos(rMaxX, rMaxY, rMaxZ);
            out.add(new RegionLookAllPayload.Entry(r.name(), min, max));
        }

        ModNetwork.sendTo(player, new RegionLookAllPayload(true, dim, out));
    }

    private static boolean intersects(int aMin, int aMax, int bMin, int bMax) {
        return aMax >= bMin && bMax >= aMin;
    }

    private static int clamp(int v, int lo, int hi) {
        if (v < lo) return lo;
        return Math.min(v, hi);
    }

    private static ResourceKey<Level> parseDimKey(String dimId) {
        if (dimId == null || dimId.isBlank()) return null;
        try {
            ResourceLocation rl = ResourceLocation.parse(dimId);
            return ResourceKey.create(Registries.DIMENSION, rl);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean flipSingle(UUID playerId, String regionName) {
        Set<String> set = ACTIVE_SINGLE.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet());
        if (set.remove(regionName)) {
            return false;
        } else {
            set.add(regionName);
            return true;
        }
    }
}
