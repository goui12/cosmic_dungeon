package net.minecraft.world.phys.shapes;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;

public class MinecartCollisionContext extends EntityCollisionContext {
    @Nullable
    private BlockPos ingoreBelow;
    @Nullable
    private BlockPos slopeIgnore;

    protected MinecartCollisionContext(AbstractMinecart minecart, boolean canStandOnFluid) {
        super(minecart, canStandOnFluid, false);
        this.setupContext(minecart);
    }

    private void setupContext(AbstractMinecart minecart) {
        BlockPos blockpos = minecart.getCurrentBlockPosOrRailBelow();
        BlockState blockstate = minecart.level().getBlockState(blockpos);
        boolean flag = BaseRailBlock.isRail(blockstate);
        if (flag) {
            this.ingoreBelow = blockpos.below();
            RailShape railshape = ((BaseRailBlock)blockstate.getBlock()).getRailDirection(blockstate, minecart.level(), blockpos, minecart);
            if (railshape.isSlope()) {
                this.slopeIgnore = switch (railshape) {
                    case ASCENDING_EAST -> blockpos.east();
                    case ASCENDING_WEST -> blockpos.west();
                    case ASCENDING_NORTH -> blockpos.north();
                    case ASCENDING_SOUTH -> blockpos.south();
                    default -> null;
                };
            }
        }
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, CollisionGetter collisionGetter, BlockPos pos) {
        return !pos.equals(this.ingoreBelow) && !pos.equals(this.slopeIgnore)
            ? super.getCollisionShape(state, collisionGetter, pos)
            : Shapes.empty();
    }
}
