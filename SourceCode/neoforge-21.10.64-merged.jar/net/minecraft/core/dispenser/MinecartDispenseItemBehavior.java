package net.minecraft.core.dispenser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

public class MinecartDispenseItemBehavior extends DefaultDispenseItemBehavior {
    private final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();
    private final EntityType<? extends AbstractMinecart> entityType;

    public MinecartDispenseItemBehavior(EntityType<? extends AbstractMinecart> entityType) {
        this.entityType = entityType;
    }

    @Override
    public ItemStack execute(BlockSource blockSource, ItemStack item) {
        Direction direction = blockSource.state().getValue(DispenserBlock.FACING);
        ServerLevel serverlevel = blockSource.level();
        Vec3 vec3 = blockSource.center();
        double d0 = vec3.x() + direction.getStepX() * 1.125;
        double d1 = Math.floor(vec3.y()) + direction.getStepY();
        double d2 = vec3.z() + direction.getStepZ() * 1.125;
        BlockPos blockpos = blockSource.pos().relative(direction);
        BlockState blockstate = serverlevel.getBlockState(blockpos);
        double d3;
        if (blockstate.is(BlockTags.RAILS)) {
            if (getRailShape(blockstate, serverlevel, blockpos).isSlope()) {
                d3 = 0.6;
            } else {
                d3 = 0.1;
            }
        } else {
            if (!blockstate.isAir()) {
                return this.defaultDispenseItemBehavior.dispense(blockSource, item);
            }

            BlockState blockstate1 = serverlevel.getBlockState(blockpos.below());
            if (!blockstate1.is(BlockTags.RAILS)) {
                return this.defaultDispenseItemBehavior.dispense(blockSource, item);
            }

            if (direction != Direction.DOWN && getRailShape(blockstate1, serverlevel, blockpos.below()).isSlope()) {
                d3 = -0.4;
            } else {
                d3 = -0.9;
            }
        }

        Vec3 vec31 = new Vec3(d0, d1 + d3, d2);
        AbstractMinecart abstractminecart = AbstractMinecart.createMinecart(
            serverlevel, vec31.x, vec31.y, vec31.z, this.entityType, EntitySpawnReason.DISPENSER, item, null
        );
        if (abstractminecart != null) {
            serverlevel.addFreshEntity(abstractminecart);
            item.shrink(1);
        }

        return item;
    }

    private static RailShape getRailShape(BlockState state, ServerLevel level, BlockPos pos) {
        return state.getBlock() instanceof BaseRailBlock baserailblock ? baserailblock.getRailDirection(state, level, pos, null) : RailShape.NORTH_SOUTH;
    }

    @Override
    protected void playSound(BlockSource blockSource) {
        blockSource.level().levelEvent(1000, blockSource.pos(), 0);
    }
}
