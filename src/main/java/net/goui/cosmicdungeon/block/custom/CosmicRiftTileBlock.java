package net.goui.cosmicdungeon.block.custom;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.goui.cosmicdungeon.auth.Authority;
import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.dungeon.DungeonRunRegistryData;
import net.goui.cosmicdungeon.dungeon.DungeonWorldSnapshotService;
import net.goui.cosmicdungeon.network.ModNetwork;
import net.goui.cosmicdungeon.network.RiftPayloads;
import net.goui.cosmicdungeon.rift.RiftRegistryData;
import net.goui.cosmicdungeon.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundSource;
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
    private static final int MAX_BREAK_TILES = 64 * 64;
    private static final ThreadLocal<Boolean> BREAKING_WHOLE = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final long TELEPORT_COOLDOWN_TICKS = 12L;
    private static final Map<UUID, Long> NEXT_ALLOWED_TELEPORT = new ConcurrentHashMap<>();
    private static final int SAFE_SEARCH_RADIUS = 6;
    private static final int SAFE_SEARCH_UP = 4;
    private static final int SAFE_SEARCH_DOWN = 2;
    private static final int CONFIG_MAX_DIST = 16;

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
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel sl)) return InteractionResult.SUCCESS;

        if (!Authority.isDeveloper(sp)) {
            sp.displayClientMessage(Component.literal("You do not have permission to configure rifts."), true);
            return InteractionResult.SUCCESS;
        }

        if (sp.blockPosition().distManhattan(pos) > CONFIG_MAX_DIST) {
            sp.displayClientMessage(Component.literal("Too far from rift to configure."), true);
            return InteractionResult.SUCCESS;
        }

        RiftRegistryData data = RiftRegistryData.get(sl);
        OptionalLong anchorOpt = data.getAnchorForTile(pos);

        BlockPos anchor;
        String name = "";
        String dest = "";
        boolean resetTrigger = false;

        if (anchorOpt.isEmpty()) {
            anchor = pos;
        } else {
            anchor = BlockPos.of(anchorOpt.getAsLong());
            var portal = data.getPortal(anchor.asLong()).orElse(null);
            if (portal != null) {
                name = portal.portalName() == null ? "" : portal.portalName();
                dest = portal.destinationName() == null ? "" : portal.destinationName();
                resetTrigger = portal.resetTrigger();
            }
        }

        ModNetwork.sendTo(sp, new RiftPayloads.S2C_RiftConfig(
                pos,
                anchor,
                name,
                dest,
                resetTrigger,
                data.listDestinationNamesSorted()
        ));

        return InteractionResult.SUCCESS;
    }

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

        if (!sp.onGround()) return;

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

        RiftRegistryData.PortalRecord portal = portalOpt.get();

        String destinationName = portal.destinationName();
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

        currentLevel.playSound(
                null,
                steppedTile,
                ModSounds.RIFT_TELEPORT.get(),
                SoundSource.AMBIENT,
                1.0F,
                0.95F + currentLevel.getRandom().nextFloat() * 0.10F
        );

        targetLevel.playSound(
                null,
                safe,
                ModSounds.RIFT_TELEPORT.get(),
                SoundSource.AMBIENT,
                1.0F,
                0.95F + targetLevel.getRandom().nextFloat() * 0.10F
        );

        NEXT_ALLOWED_TELEPORT.put(id, now + TELEPORT_COOLDOWN_TICKS);

        if (portal.resetTrigger()) {
            var runs = DungeonRunRegistryData.get(currentLevel.getServer());
            boolean shouldReset = runs.markPlayerExitedAndShouldReset(currentLevel.getServer(), currentLevel.dimension(), id);

            if (shouldReset) {
                var result = DungeonWorldSnapshotService.resetToLatest(currentLevel.getServer(), currentLevel.dimension());
                if (result instanceof DungeonWorldSnapshotService.SnapshotResult.Ok okResult) {
                    runs.clearRunsForDimension(currentLevel.dimension());

                    var server = currentLevel.getServer();
                    if (server != null) {
                        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
                            if (Authority.isDeveloper(online)) {
                                online.sendSystemMessage(Component.literal(
                                        "Dungeon reset complete for " + currentLevel.dimension().location() +
                                                " -> " + okResult.snapshotId()
                                ));
                            }
                        }
                    }
                } else {
                    DungeonWorldSnapshotService.SnapshotResult.Error err =
                            (DungeonWorldSnapshotService.SnapshotResult.Error) result;

                    var server = currentLevel.getServer();
                    if (server != null) {
                        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
                            if (Authority.isDeveloper(online)) {
                                online.sendSystemMessage(Component.literal(
                                        "Dungeon reset failed for " + currentLevel.dimension().location() +
                                                ": " + err.message()
                                ));
                            }
                        }
                    }
                }
            }
        }
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

        return !floor.getCollisionShape(level, pos.below()).isEmpty();
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
            long packed = queue.dequeueLong();
            BlockPos p = BlockPos.of(packed);

            if (p.getY() != y) continue;
            if (!level.getBlockState(p).is(riftTile)) continue;

            tryEnqueueNeighbor(level, p.north(), y, riftTile, visited, queue);
            tryEnqueueNeighbor(level, p.south(), y, riftTile, visited, queue);
            tryEnqueueNeighbor(level, p.east(), y, riftTile, visited, queue);
            tryEnqueueNeighbor(level, p.west(), y, riftTile, visited, queue);
        }

        if (!visited.isEmpty()) {
            RiftRegistryData.get(level).onRiftTilesBroken(visited);

            for (long packed : visited) {
                BlockPos p = BlockPos.of(packed);
                if (level.getBlockState(p).is(riftTile)) {
                    level.destroyBlock(p, false, player);
                }
            }
        }
    }

    private static void tryEnqueueNeighbor(ServerLevel level,
                                           BlockPos pos,
                                           int fixedY,
                                           Block riftTile,
                                           LongOpenHashSet visited,
                                           LongArrayFIFOQueue queue) {
        if (pos.getY() != fixedY) return;
        if (!level.getBlockState(pos).is(riftTile)) return;

        long packed = pos.asLong();
        if (visited.add(packed)) {
            queue.enqueue(packed);
        }
    }
}