package net.goui.cosmicdungeon.door;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public class DoorPassageTracker {

    // Per-player cooldown to avoid repeats while standing in the doorway
    private static final Map<UUID, Entry> RECENT = new HashMap<>();
    private static final int COOLDOWN_TICKS = 12; // ~0.6s at 20 TPS

    private record Entry(BlockPos lowerDoorPos, int ticks) {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        var server = e.getServer();

        for (ServerLevel level : server.getAllLevels()) {
            for (ServerPlayer p : level.players()) {
                // Tick down cooldown for this player
                RECENT.compute(p.getUUID(), (id, prev) -> {
                    if (prev == null) return null;
                    int t = prev.ticks() - 1;
                    return (t <= 0) ? null : new Entry(prev.lowerDoorPos(), t);
                });

                // Robust: check the player's AABB for any OPEN door column they overlap
                BlockPos inside = findOpenDoorBaseIntersecting(level, p);
                if (inside != null) {
                    Entry prev = RECENT.get(p.getUUID());
                    if (prev == null || !prev.lowerDoorPos().equals(inside)) {
                        // Count on ENTER
                        DoorPassageData data = DoorPassageData.get(level);
                        data.increment(level, inside);

// Close if we've reached / exceeded the limit
                        Integer limit = data.getLimit(level, inside);
                        if (limit != null && limit > 0) {
                            int total = data.get(level, inside);
                            if (total >= limit) {
                                BlockState bs = level.getBlockState(inside);
                                if (bs.getBlock() instanceof DoorBlock door) {
                                    door.setOpen(p, level, bs, inside, false); // shuts both halves + plays sound
                                }
                            }
                        }

                        RECENT.put(p.getUUID(), new Entry(inside, COOLDOWN_TICKS));

                    }
                }
            }
        }
    }

    /** Scan the player's AABB for an open door; return its LOWER-half pos if found, else null. */
    private static BlockPos findOpenDoorBaseIntersecting(ServerLevel level, ServerPlayer p) {
        AABB box = p.getBoundingBox().inflate(0.001); // avoid precision edge cases

        int minX = Mth.floor(box.minX);
        int maxX = Mth.floor(box.maxX);
        int minY = Mth.floor(box.minY);
        int maxY = Mth.floor(box.maxY);
        int minZ = Mth.floor(box.minZ);
        int maxZ = Mth.floor(box.maxZ);

        BlockPos.MutableBlockPos cur = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    cur.set(x, y, z);
                    BlockState st = level.getBlockState(cur);
                    if (!(st.getBlock() instanceof DoorBlock)) continue;

                    // Normalize to lower half
                    BlockPos base = (st.getOptionalValue(DoorBlock.HALF).orElse(DoubleBlockHalf.LOWER) == DoubleBlockHalf.UPPER)
                            ? cur.below()
                            : cur.immutable();

                    BlockState baseSt = level.getBlockState(base);
                    if (!(baseSt.getBlock() instanceof DoorBlock)) continue;

                    // Only consider OPEN doors
                    if (!baseSt.getOptionalValue(DoorBlock.OPEN).orElse(Boolean.FALSE)) continue;

                    // Ensure the player's XZ is actually inside the door's 1x1 column
                    double cx = (p.getX() + p.xOld) * 0.5; // mid-tick center helps with fast movement
                    double cz = (p.getZ() + p.zOld) * 0.5;
                    if (insideXZ(cx, cz, base)) {
                        return base;
                    }
                }
            }
        }
        return null;
    }

    private static boolean insideXZ(double px, double pz, BlockPos base) {
        double minX = base.getX(), minZ = base.getZ();
        double maxX = minX + 1.0, maxZ = minZ + 1.0;
        // Inclusive edges so skimming the block border still counts
        return (px >= minX && px <= maxX && pz >= minZ && pz <= maxZ);
    }

    @SubscribeEvent
    public static void onDoorBroken(BlockEvent.BreakEvent e) {
        if (!(e.getLevel() instanceof Level level)) return;

        BlockPos hit = e.getPos();
        BlockState st = level.getBlockState(hit);
        if (!(st.getBlock() instanceof DoorBlock)) return;

        // Normalize to the lower half and keep it effectively final
        final BlockPos basePos =
                st.getOptionalValue(DoorBlock.HALF).orElse(DoubleBlockHalf.LOWER) == DoubleBlockHalf.UPPER
                        ? hit.below()
                        : hit;

        DoorPassageData.get(level).remove(level, basePos);

        // Clear any cooldown entry that was tied to this door
        RECENT.entrySet().removeIf(en -> en.getValue().lowerDoorPos().equals(basePos));
    }

}
