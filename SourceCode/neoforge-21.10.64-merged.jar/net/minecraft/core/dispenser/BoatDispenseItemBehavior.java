package net.minecraft.core.dispenser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.AbstractBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.Vec3;

public class BoatDispenseItemBehavior extends DefaultDispenseItemBehavior {
    private final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();
    private final EntityType<? extends AbstractBoat> type;

    public BoatDispenseItemBehavior(EntityType<? extends AbstractBoat> type) {
        this.type = type;
    }

    @Override
    public ItemStack execute(BlockSource blockSource, ItemStack item) {
        Direction direction = blockSource.state().getValue(DispenserBlock.FACING);
        ServerLevel serverlevel = blockSource.level();
        Vec3 vec3 = blockSource.center();
        double d0 = 0.5625 + this.type.getWidth() / 2.0;
        double d1 = vec3.x() + direction.getStepX() * d0;
        double d2 = vec3.y() + direction.getStepY() * 1.125F;
        double d3 = vec3.z() + direction.getStepZ() * d0;
        BlockPos blockpos = blockSource.pos().relative(direction);
        AbstractBoat abstractboat = this.type.create(serverlevel, EntitySpawnReason.DISPENSER);
        if (abstractboat != null) {
            EntityType.<AbstractBoat>createDefaultStackConfig(serverlevel, item, null).accept(abstractboat);
            abstractboat.setYRot(direction.toYRot());
            serverlevel.addFreshEntity(abstractboat);
        }
        double d4;
        if (canBoatInFluid(abstractboat, serverlevel.getFluidState(blockpos))) {
            d4 = 1.0;
        } else {
            if (!serverlevel.getBlockState(blockpos).isAir() || !canBoatInFluid(abstractboat, serverlevel.getFluidState(blockpos.below()))) {
                return this.defaultDispenseItemBehavior.dispense(blockSource, item);
            }

            d4 = 0.0;
        }

        if (abstractboat != null) {
            abstractboat.setInitialPos(d1, d2 + d4, d3);
            item.shrink(1);
        }

        return item;
    }

    private static boolean canBoatInFluid(@org.jetbrains.annotations.Nullable AbstractBoat boat, net.minecraft.world.level.material.FluidState fluid) {
        return boat != null ? boat.canBoatInFluid(fluid) : fluid.is(FluidTags.WATER);
    }

    @Override
    protected void playSound(BlockSource blockSource) {
        blockSource.level().levelEvent(1000, blockSource.pos(), 0);
    }
}
