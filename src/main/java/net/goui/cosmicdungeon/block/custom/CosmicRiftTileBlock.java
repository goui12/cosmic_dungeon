package net.goui.cosmicdungeon.block.custom;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.client.rift.RiftConfigScreen;
import net.goui.cosmicdungeon.rift.RiftRegistryData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CosmicRiftTileBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    // Safety cap (64x64)
    private static final int MAX_BREAK_TILES = 64 * 64;

    // Prevent recursive destruction loops
    private static final ThreadLocal<Boolean> BREAKING_WHOLE =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    /* ===================== TELEPORT CONFIG ===================== */

    private static final long TELEPORT_COOLDOWN_TICKS = 12;
    private static final Map<UUID, Long> NEXT_ALLOWED_TELEPORT = new ConcurrentHashMap<>();

    private static final int SAFE_SEARCH_RADIUS = 6;
    private static final int SAFE_SEARCH_UP = 4;
    private static final int SAFE_SEARCH_DOWN = 2;

    public CosmicRiftTileBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            RiftConfigScreen.openForClickedTile(pos);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Correct 1.21.10 override.
     * This is reliably called for thin/raised shapes like your 1/16 tile.
     */
    @Override
    protected void entityInside(BlockState state,
                                Level level,
                                BlockPos pos,
                                Entity entity,
                                InsideBlockEffectApplier effectApplier,
                                boolean unknownFlag) {
        super.entityInside(state, level, pos, entity, effectApplier, unknownFlag);
        entity.fallDistance = 0.0F;

        if (level.isClientSide()) return;
        if (!(level instanceof ServerLevel sl)) return;
        if (!(entity instanceof ServerPlayer sp)) return;

        // Optional: avoid midair triggers (but still works with the 1/16 raised tile)
        if (!sp.onGround()) return;

        // Important for 1/16 raised tile: the player's blockPosition() is often the block BELOW.
        // So accept if the player is standing on the block below this tile OR on this tile.
        BlockPos feet = sp.blockPosition();
        if (!feet.equals(pos) && !feet.above().equals(pos)) return;

        tryTeleportFromTile(sl, pos, sp);
    }

    private static void tryTeleportFromTile(ServerLevel currentLevel, BlockPos steppedTile, ServerPlayer sp) {
        long now = currentLevel.getGameTime();
        UUID id = sp.getUUID();

        long nextOk = NEXT_ALLOWED_TELEPORT.getOrDefault(id, 0L);
        if (now < nextOk) return;

        RiftRegistryData data = RiftRegistryData.get(currentLevel);

        OptionalLong anchorOpt = data.getAnchorForTile(steppedTile);
        if (anchorOpt.isEmpty()) return;

        var portalOpt = data.getPortal(anchorOpt.getAsLong());
        if (portalOpt.isEmpty()) return;

        String destinationName = portalOpt.get().destinationName();
        if (destinationName == null || destinationName.isBlank()) return;

        var destOpt = data.getDestination(destinationName);
        if (destOpt.isEmpty()) return;

        RiftRegistryData.DestinationRecord dest = destOpt.get();

        ResourceLocation dimId;
        try {
            dimId = ResourceLocation.parse(dest.dimensionId());
        } catch (Exception e) {
            return;
        }

        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimId);
        ServerLevel targetLevel = currentLevel.getServer().getLevel(dimKey);
        if (targetLevel == null) return;

        BlockPos rawTarget = dest.pos();

        // Ensure chunk loaded (force-load)
        targetLevel.getChunk(rawTarget);

        BlockPos safe = findSafeTeleportPos(targetLevel, rawTarget);
        if (safe == null) return;

        double tx = safe.getX() + 0.5D;
        double ty = safe.getY();
        double tz = safe.getZ() + 0.5D;

        boolean ok = sp.teleportTo(
                targetLevel,
                tx, ty, tz,
                Set.of(),
                sp.getYRot(),
                sp.getXRot(),
                true
        );

        if (!ok) return;
// Custom rift teleport SFX (play in BOTH dimensions)
        currentLevel.playSound(
                null,
                steppedTile,
                net.goui.cosmicdungeon.sound.ModSounds.RIFT_TELEPORT.get(),
                net.minecraft.sounds.SoundSource.AMBIENT,
                1.0F,
                0.95F + currentLevel.getRandom().nextFloat() * 0.10F
        );

        targetLevel.playSound(
                null,
                safe,
                net.goui.cosmicdungeon.sound.ModSounds.RIFT_TELEPORT.get(),
                net.minecraft.sounds.SoundSource.AMBIENT,
                1.0F,
                0.95F + targetLevel.getRandom().nextFloat() * 0.10F
        );

        // Cooldown based on the target world's time base
        NEXT_ALLOWED_TELEPORT.put(id, targetLevel.getGameTime() + TELEPORT_COOLDOWN_TICKS);
    }

    private static BlockPos findSafeTeleportPos(ServerLevel level, BlockPos preferred) {
        if (isStandable(level, preferred)) return preferred;

        int baseX = preferred.getX();
        int baseY = preferred.getY();
        int baseZ = preferred.getZ();

        for (int r = 1; r <= SAFE_SEARCH_RADIUS; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;

                    for (int dy = -SAFE_SEARCH_DOWN; dy <= SAFE_SEARCH_UP; dy++) {
                        BlockPos p = new BlockPos(baseX + dx, baseY + dy, baseZ + dz);
                        if (isStandable(level, p)) return p;
                    }
                }
            }
        }

        return null;
    }

    private static boolean isStandable(ServerLevel level, BlockPos pos) {
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState floor = level.getBlockState(pos.below());

        if (!feet.getCollisionShape(level, pos).isEmpty()) return false;
        if (!head.getCollisionShape(level, pos.above()).isEmpty()) return false;

        FluidState ff = feet.getFluidState();
        FluidState hf = head.getFluidState();
        if (!ff.isEmpty()) return false;
        if (!hf.isEmpty()) return false;

        if (floor.getCollisionShape(level, pos.below()).isEmpty()) return false;

        return true;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()
                && level instanceof ServerLevel serverLevel
                && !Boolean.TRUE.equals(BREAKING_WHOLE.get())) {

            BREAKING_WHOLE.set(Boolean.TRUE);
            try {
                breakConnectedRift(serverLevel, pos, player);
            } finally {
                BREAKING_WHOLE.set(Boolean.FALSE);
            }
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    private static void breakConnectedRift(ServerLevel level, BlockPos origin, Player player) {
        Block riftTile = ModBlocks.COSMIC_RIFT_TILE.get();
        int y = origin.getY();

        LongOpenHashSet visited = new LongOpenHashSet();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();

        long start = origin.asLong();
        visited.add(start);
        queue.enqueue(start);

        while (!queue.isEmpty() && visited.size() <= MAX_BREAK_TILES) {
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

                if (level.getBlockState(next).getBlock() != riftTile) continue;

                visited.add(nk);
                queue.enqueue(nk);
            }
        }

        RiftRegistryData.get(level).onRiftTilesBroken(visited);

        for (long packed : visited) {
            BlockPos p = BlockPos.of(packed);
            boolean drop = p.equals(origin);
            level.destroyBlock(p, drop, player);
        }
    }
}
