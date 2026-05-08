package net.minecraft.world.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;

public interface SpawnPlacementTypes {
    SpawnPlacementType NO_RESTRICTIONS = (p_321554_, p_321832_, p_321540_) -> true;
    SpawnPlacementType IN_WATER = (p_445308_, p_445309_, p_445310_) -> {
        if (p_445310_ != null && p_445308_.getWorldBorder().isWithinBounds(p_445309_)) {
            BlockPos blockpos = p_445309_.above();
            return p_445308_.getFluidState(p_445309_).is(FluidTags.WATER) && !p_445308_.getBlockState(blockpos).isRedstoneConductor(p_445308_, blockpos);
        } else {
            return false;
        }
    };
    SpawnPlacementType IN_LAVA = (p_445311_, p_445312_, p_445313_) -> p_445313_ != null && p_445311_.getWorldBorder().isWithinBounds(p_445312_)
        ? p_445311_.getFluidState(p_445312_).is(FluidTags.LAVA)
        : false;
    SpawnPlacementType ON_GROUND = new SpawnPlacementType() {
        @Override
        public boolean isSpawnPositionOk(LevelReader level, BlockPos pos, @Nullable EntityType<?> entityType) {
            if (entityType != null && level.getWorldBorder().isWithinBounds(pos)) {
                BlockPos blockpos = pos.above();
                BlockPos blockpos1 = pos.below();
                BlockState blockstate = level.getBlockState(blockpos1);
                return !blockstate.isValidSpawn(level, blockpos1, entityType)
                    ? false
                    : this.isValidEmptySpawnBlock(level, pos, entityType) && this.isValidEmptySpawnBlock(level, blockpos, entityType);
            } else {
                return false;
            }
        }

        private boolean isValidEmptySpawnBlock(LevelReader level, BlockPos pos, EntityType<?> entityType) {
            BlockState blockstate = level.getBlockState(pos);
            return NaturalSpawner.isValidEmptySpawnBlock(level, pos, blockstate, blockstate.getFluidState(), entityType);
        }

        @Override
        public BlockPos adjustSpawnPosition(LevelReader level, BlockPos pos) {
            BlockPos blockpos = pos.below();
            return level.getBlockState(blockpos).isPathfindable(PathComputationType.LAND) ? blockpos : pos;
        }
    };
}
