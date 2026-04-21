// file: src/main/java/net/goui/cosmicdungeon/block/custom/CosmicRiftPlacerBlock.java
package net.goui.cosmicdungeon.block.custom;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.goui.cosmicdungeon.auth.Authority;
import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.rift.RiftRegistryData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class CosmicRiftPlacerBlock extends Block {

    private static final int MAX_DIM = 64;
    private static final int MAX_TILES = MAX_DIM * MAX_DIM;

    public CosmicRiftPlacerBlock(Properties props) {
        super(props);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (level.isClientSide()) return;

        // Permission gate: only Developer can place/form rifts
        if (placer instanceof ServerPlayer sp && !Authority.isDeveloper(sp)) {
            level.removeBlock(pos, false);
            sp.displayClientMessage(Component.literal("You do not have permission to place a Cosmic Rift."), true);
            return;
        }

        FillResult cavity = findConnectedReplaceableCavity(level, pos);

        // Note: your FillResult never sets valid=false. Keep behavior as you had it:
        // "too large" => fallback 4x4, otherwise stamp full cavity.
        if (cavity.tooLarge) {
            if (placer instanceof Player player) {
                player.displayClientMessage(
                        Component.literal("Space is either wider or longer than 64 tiles. Defaulting to 4x4."),
                        true
                );
            }
            stampFallback4x4(level, pos);
            return;
        }

        BlockState tile = ModBlocks.COSMIC_RIFT_TILE.get().defaultBlockState();
        for (long packed : cavity.positions) {
            BlockPos p = BlockPos.of(packed);
            level.setBlock(p, tile, Block.UPDATE_ALL);
        }

        if (level instanceof ServerLevel sl) {
            // Anchor is the min corner in the cavity bounding box, same as your original.
            BlockPos anchor = new BlockPos(cavity.minX, pos.getY(), cavity.minZ);
            RiftRegistryData.get(sl).registerPortalWithTiles(sl, anchor, cavity.positions);
        }
    }

    private static void stampFallback4x4(Level level, BlockPos placerPos) {
        BlockPos origin = placerPos.offset(-1, 0, -1);

        // Validate area
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                BlockPos p = origin.offset(col, 0, row);
                if (p.equals(placerPos)) continue;

                BlockState existing = level.getBlockState(p);
                if (!existing.canBeReplaced()) {
                    level.removeBlock(placerPos, false);
                    return;
                }
            }
        }

        BlockState tile = ModBlocks.COSMIC_RIFT_TILE.get().defaultBlockState();
        LongOpenHashSet placed = new LongOpenHashSet();

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                BlockPos p = origin.offset(col, 0, row);
                level.setBlock(p, tile, Block.UPDATE_ALL);
                placed.add(p.asLong());
            }
        }

        if (level instanceof ServerLevel sl) {
            RiftRegistryData.get(sl).registerPortalWithTiles(sl, origin, placed);
        }
    }

    private static FillResult findConnectedReplaceableCavity(Level level, BlockPos start) {
        final int y = start.getY();

        LongOpenHashSet visited = new LongOpenHashSet();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();

        long startKey = start.asLong();
        visited.add(startKey);
        queue.enqueue(startKey);

        int minX = start.getX(), maxX = start.getX();
        int minZ = start.getZ(), maxZ = start.getZ();

        while (!queue.isEmpty()) {
            long curKey = queue.dequeueLong();
            BlockPos cur = BlockPos.of(curKey);

            for (int i = 0; i < 4; i++) {
                BlockPos next = switch (i) {
                    case 0 -> cur.east();
                    case 1 -> cur.west();
                    case 2 -> cur.south();
                    default -> cur.north();
                };

                if (next.getY() != y) continue;

                long nk = next.asLong();
                if (visited.contains(nk)) continue;

                BlockState existing = level.getBlockState(next);
                if (!existing.canBeReplaced()) continue;

                visited.add(nk);
                queue.enqueue(nk);

                int x = next.getX();
                int z = next.getZ();

                if (x < minX) minX = x;
                if (x > maxX) maxX = x;
                if (z < minZ) minZ = z;
                if (z > maxZ) maxZ = z;

                int width = (maxX - minX) + 1;
                int length = (maxZ - minZ) + 1;

                if (width > MAX_DIM || length > MAX_DIM || visited.size() > MAX_TILES) {
                    return FillResult.tooLarge(visited, minX, maxX, minZ, maxZ);
                }
            }
        }

        return FillResult.ok(visited, minX, maxX, minZ, maxZ);
    }

    private static final class FillResult {
        final boolean tooLarge;
        final LongOpenHashSet positions;
        final int minX, maxX, minZ, maxZ;

        private FillResult(boolean tooLarge, LongOpenHashSet positions, int minX, int maxX, int minZ, int maxZ) {
            this.tooLarge = tooLarge;
            this.positions = positions;
            this.minX = minX; this.maxX = maxX;
            this.minZ = minZ; this.maxZ = maxZ;
        }

        static FillResult ok(LongOpenHashSet positions, int minX, int maxX, int minZ, int maxZ) {
            return new FillResult(false, positions, minX, maxX, minZ, maxZ);
        }

        static FillResult tooLarge(LongOpenHashSet positions, int minX, int maxX, int minZ, int maxZ) {
            return new FillResult(true, positions, minX, maxX, minZ, maxZ);
        }
    }
}
