package net.minecraft.util;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;

public class SpawnUtil {
    public static <T extends Mob> Optional<T> trySpawnMob(
        EntityType<T> entityType,
        EntitySpawnReason spawnReason,
        ServerLevel level,
        BlockPos pos,
        int attempts,
        int range,
        int yOffset,
        SpawnUtil.Strategy strategy,
        boolean checkCollision
    ) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = pos.mutable();

        for (int i = 0; i < attempts; i++) {
            int j = Mth.randomBetweenInclusive(level.random, -range, range);
            int k = Mth.randomBetweenInclusive(level.random, -range, range);
            blockpos$mutableblockpos.setWithOffset(pos, j, yOffset, k);
            if (level.getWorldBorder().isWithinBounds(blockpos$mutableblockpos)
                && moveToPossibleSpawnPosition(level, yOffset, blockpos$mutableblockpos, strategy)
                && (
                    !checkCollision
                        || level.noCollision(
                            entityType.getSpawnAABB(
                                blockpos$mutableblockpos.getX() + 0.5, blockpos$mutableblockpos.getY(), blockpos$mutableblockpos.getZ() + 0.5
                            )
                        )
                )) {
                T t = (T)entityType.create(level, null, blockpos$mutableblockpos, spawnReason, false, false);
                if (t != null) {
                    if (net.neoforged.neoforge.event.EventHooks.checkSpawnPosition(t, level, spawnReason)) {
                        level.addFreshEntityWithPassengers(t);
                        t.playAmbientSound();
                        return Optional.of(t);
                    }

                    t.discard();
                }
            }
        }

        return Optional.empty();
    }

    private static boolean moveToPossibleSpawnPosition(ServerLevel level, int yOffset, BlockPos.MutableBlockPos pos, SpawnUtil.Strategy strategy) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos().set(pos);
        BlockState blockstate = level.getBlockState(blockpos$mutableblockpos);

        for (int i = yOffset; i >= -yOffset; i--) {
            pos.move(Direction.DOWN);
            blockpos$mutableblockpos.setWithOffset(pos, Direction.UP);
            BlockState blockstate1 = level.getBlockState(pos);
            if (strategy.canSpawnOn(level, pos, blockstate1, blockpos$mutableblockpos, blockstate)) {
                pos.move(Direction.UP);
                return true;
            }

            blockstate = blockstate1;
        }

        return false;
    }

    public interface Strategy {
        @Deprecated
        SpawnUtil.Strategy LEGACY_IRON_GOLEM = (p_289751_, p_289752_, p_289753_, p_289754_, p_289755_) -> !p_289753_.is(Blocks.COBWEB)
                && !p_289753_.is(Blocks.CACTUS)
                && !p_289753_.is(Blocks.GLASS_PANE)
                && !(p_289753_.getBlock() instanceof StainedGlassPaneBlock)
                && !(p_289753_.getBlock() instanceof StainedGlassBlock)
                && !(p_289753_.getBlock() instanceof LeavesBlock)
                && !p_289753_.is(Blocks.CONDUIT)
                && !p_289753_.is(Blocks.ICE)
                && !p_289753_.is(Blocks.TNT)
                && !p_289753_.is(Blocks.GLOWSTONE)
                && !p_289753_.is(Blocks.BEACON)
                && !p_289753_.is(Blocks.SEA_LANTERN)
                && !p_289753_.is(Blocks.FROSTED_ICE)
                && !p_289753_.is(Blocks.TINTED_GLASS)
                && !p_289753_.is(Blocks.GLASS)
            ? (p_289755_.isAir() || p_289755_.liquid()) && (p_289753_.isSolid() || p_289753_.is(Blocks.POWDER_SNOW))
            : false;
        SpawnUtil.Strategy ON_TOP_OF_COLLIDER = (p_359662_, p_359663_, p_359664_, p_359665_, p_359666_) -> p_359666_.getCollisionShape(p_359662_, p_359665_)
                .isEmpty()
            && Block.isFaceFull(p_359664_.getCollisionShape(p_359662_, p_359663_), Direction.UP);
        SpawnUtil.Strategy ON_TOP_OF_COLLIDER_NO_LEAVES = (p_379064_, p_379065_, p_379066_, p_379067_, p_379068_) -> p_379068_.getCollisionShape(
                    p_379064_, p_379067_
                )
                .isEmpty()
            && !p_379066_.is(BlockTags.LEAVES)
            && Block.isFaceFull(p_379066_.getCollisionShape(p_379064_, p_379065_), Direction.UP);

        boolean canSpawnOn(ServerLevel level, BlockPos targetPos, BlockState targetState, BlockPos attemptedPos, BlockState attemptedState);
    }
}
