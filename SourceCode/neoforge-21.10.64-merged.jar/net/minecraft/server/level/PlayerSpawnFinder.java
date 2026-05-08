package net.minecraft.server.level;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class PlayerSpawnFinder {
    private static final EntityDimensions PLAYER_DIMENSIONS = EntityType.PLAYER.getDimensions();
    private static final int ABSOLUTE_MAX_ATTEMPTS = 1024;
    private final ServerLevel level;
    private final BlockPos spawnSuggestion;
    private final int radius;
    private final int candidateCount;
    private final int coprime;
    private final int offset;
    private int nextCandidateIndex;
    private final CompletableFuture<Vec3> finishedFuture = new CompletableFuture<>();

    private PlayerSpawnFinder(ServerLevel level, BlockPos spawnSuggestion, int radius) {
        this.level = level;
        this.spawnSuggestion = spawnSuggestion;
        this.radius = radius;
        long i = radius * 2L + 1L;
        this.candidateCount = (int)Math.min(1024L, i * i);
        this.coprime = getCoprime(this.candidateCount);
        this.offset = RandomSource.create().nextInt(this.candidateCount);
    }

    public static CompletableFuture<Vec3> findSpawn(ServerLevel level, BlockPos pos) {
        if (level.dimensionType().hasSkyLight() && level.getServer().getWorldData().getGameType() != GameType.ADVENTURE) {
            int i = Math.max(0, level.getGameRules().getInt(GameRules.RULE_SPAWN_RADIUS));
            int j = Mth.floor(level.getWorldBorder().getDistanceToBorder(pos.getX(), pos.getZ()));
            if (j < i) {
                i = j;
            }

            if (j <= 1) {
                i = 1;
            }

            PlayerSpawnFinder playerspawnfinder = new PlayerSpawnFinder(level, pos, i);
            playerspawnfinder.scheduleNext();
            return playerspawnfinder.finishedFuture;
        } else {
            return CompletableFuture.completedFuture(fixupSpawnHeight(level, pos));
        }
    }

    private void scheduleNext() {
        int i = this.nextCandidateIndex++;
        if (i < this.candidateCount) {
            int j = (this.offset + this.coprime * i) % this.candidateCount;
            int k = j % (this.radius * 2 + 1);
            int l = j / (this.radius * 2 + 1);
            int i1 = this.spawnSuggestion.getX() + k - this.radius;
            int j1 = this.spawnSuggestion.getZ() + l - this.radius;
            this.scheduleCandidate(i1, j1, i, () -> {
                BlockPos blockpos = getOverworldRespawnPos(this.level, i1, j1);
                return blockpos != null && noCollisionNoLiquid(this.level, blockpos) ? Optional.of(Vec3.atBottomCenterOf(blockpos)) : Optional.empty();
            });
        } else {
            this.scheduleCandidate(
                this.spawnSuggestion.getX(), this.spawnSuggestion.getZ(), i, () -> Optional.of(fixupSpawnHeight(this.level, this.spawnSuggestion))
            );
        }
    }

    private static Vec3 fixupSpawnHeight(CollisionGetter collisionGetter, BlockPos pos) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = pos.mutable();

        while (!noCollisionNoLiquid(collisionGetter, blockpos$mutableblockpos) && blockpos$mutableblockpos.getY() < collisionGetter.getMaxY()) {
            blockpos$mutableblockpos.move(Direction.UP);
        }

        blockpos$mutableblockpos.move(Direction.DOWN);

        while (noCollisionNoLiquid(collisionGetter, blockpos$mutableblockpos) && blockpos$mutableblockpos.getY() > collisionGetter.getMinY()) {
            blockpos$mutableblockpos.move(Direction.DOWN);
        }

        blockpos$mutableblockpos.move(Direction.UP);
        return Vec3.atBottomCenterOf(blockpos$mutableblockpos);
    }

    private static boolean noCollisionNoLiquid(CollisionGetter collisionGetter, BlockPos pos) {
        return collisionGetter.noCollision(null, PLAYER_DIMENSIONS.makeBoundingBox(pos.getBottomCenter()), true);
    }

    private static int getCoprime(int candidateCount) {
        return candidateCount <= 16 ? candidateCount - 1 : 17;
    }

    private void scheduleCandidate(int x, int z, int index, Supplier<Optional<Vec3>> calculator) {
        if (!this.finishedFuture.isDone()) {
            int i = SectionPos.blockToSectionCoord(x);
            int j = SectionPos.blockToSectionCoord(z);
            this.level
                .getChunkSource()
                .addTicketAndLoadWithRadius(TicketType.SPAWN_SEARCH, new ChunkPos(i, j), 0)
                .whenCompleteAsync((p_433344_, p_433338_) -> {
                    if (p_433338_ == null) {
                        try {
                            Optional<Vec3> optional = calculator.get();
                            if (optional.isPresent()) {
                                this.finishedFuture.complete(optional.get());
                            } else {
                                this.scheduleNext();
                            }
                        } catch (Exception exception) {
                            p_433338_ = exception;
                        }
                    }

                    if (p_433338_ != null) {
                        CrashReport crashreport = CrashReport.forThrowable(p_433338_, "Searching for spawn");
                        CrashReportCategory crashreportcategory = crashreport.addCategory("Spawn Lookup");
                        crashreportcategory.setDetail("Origin", this.spawnSuggestion::toString);
                        crashreportcategory.setDetail("Radius", () -> Integer.toString(this.radius));
                        crashreportcategory.setDetail("Candidate", () -> "[" + x + "," + z + "]");
                        crashreportcategory.setDetail("Progress", () -> index + " out of " + this.candidateCount);
                        this.finishedFuture.completeExceptionally(new ReportedException(crashreport));
                    }
                }, this.level.getServer());
        }
    }

    @Nullable
    protected static BlockPos getOverworldRespawnPos(ServerLevel level, int x, int z) {
        boolean flag = level.dimensionType().hasCeiling();
        LevelChunk levelchunk = level.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
        int i = flag
            ? level.getChunkSource().getGenerator().getSpawnHeight(level)
            : levelchunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x & 15, z & 15);
        if (i < level.getMinY()) {
            return null;
        } else {
            int j = levelchunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
            if (j <= i && j > levelchunk.getHeight(Heightmap.Types.OCEAN_FLOOR, x & 15, z & 15)) {
                return null;
            } else {
                BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

                for (int k = i + 1; k >= level.getMinY(); k--) {
                    blockpos$mutableblockpos.set(x, k, z);
                    BlockState blockstate = level.getBlockState(blockpos$mutableblockpos);
                    if (!blockstate.getFluidState().isEmpty()) {
                        break;
                    }

                    if (Block.isFaceFull(blockstate.getCollisionShape(level, blockpos$mutableblockpos), Direction.UP)) {
                        return blockpos$mutableblockpos.above().immutable();
                    }
                }

                return null;
            }
        }
    }

    @Nullable
    public static BlockPos getSpawnPosInChunk(ServerLevel level, ChunkPos chunkPos) {
        if (SharedConstants.debugVoidTerrain(chunkPos)) {
            return null;
        } else {
            for (int i = chunkPos.getMinBlockX(); i <= chunkPos.getMaxBlockX(); i++) {
                for (int j = chunkPos.getMinBlockZ(); j <= chunkPos.getMaxBlockZ(); j++) {
                    BlockPos blockpos = getOverworldRespawnPos(level, i, j);
                    if (blockpos != null) {
                        return blockpos;
                    }
                }
            }

            return null;
        }
    }
}
