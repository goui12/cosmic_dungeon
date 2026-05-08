package net.minecraft.world.entity.ai.behavior;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class ValidateNearbyPoi {
    private static final int MAX_DISTANCE = 16;

    public static BehaviorControl<LivingEntity> create(Predicate<Holder<PoiType>> poiValidator, MemoryModuleType<GlobalPos> poiPosMemory) {
        return BehaviorBuilder.create(
            p_259215_ -> p_259215_.group(p_259215_.present(poiPosMemory)).apply(p_259215_, p_259498_ -> (p_448950_, p_448951_, p_448952_) -> {
                GlobalPos globalpos = p_259215_.get(p_259498_);
                BlockPos blockpos = globalpos.pos();
                if (p_448950_.dimension() == globalpos.dimension() && blockpos.closerToCenterThan(p_448951_.position(), 16.0)) {
                    ServerLevel serverlevel = p_448950_.getServer().getLevel(globalpos.dimension());
                    if (serverlevel == null || !serverlevel.getPoiManager().exists(blockpos, poiValidator)) {
                        p_259498_.erase();
                    } else if (bedIsOccupied(serverlevel, blockpos, p_448951_)) {
                        p_259498_.erase();
                        if (!bedIsOccupiedByVillager(serverlevel, blockpos)) {
                            p_448950_.getPoiManager().release(blockpos);
                            p_448950_.debugSynchronizers().updatePoi(blockpos);
                        }
                    }

                    return true;
                } else {
                    return false;
                }
            })
        );
    }

    private static boolean bedIsOccupied(ServerLevel level, BlockPos pos, LivingEntity entity) {
        BlockState blockstate = level.getBlockState(pos);
        return blockstate.is(BlockTags.BEDS) && blockstate.getValue(BedBlock.OCCUPIED) && !entity.isSleeping();
    }

    private static boolean bedIsOccupiedByVillager(ServerLevel level, BlockPos pos) {
        List<Villager> list = level.getEntitiesOfClass(Villager.class, new AABB(pos), LivingEntity::isSleeping);
        return !list.isEmpty();
    }
}
