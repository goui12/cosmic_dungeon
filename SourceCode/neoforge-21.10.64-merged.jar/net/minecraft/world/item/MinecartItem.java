package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

public class MinecartItem extends Item {
    private final EntityType<? extends AbstractMinecart> type;

    public MinecartItem(EntityType<? extends AbstractMinecart> type, Item.Properties properties) {
        super(properties);
        this.type = type;
    }

    /**
     * Called when this item is used when targeting a Block
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos blockpos = context.getClickedPos();
        BlockState blockstate = level.getBlockState(blockpos);
        if (!blockstate.is(BlockTags.RAILS)) {
            return InteractionResult.FAIL;
        } else {
            ItemStack itemstack = context.getItemInHand();
            RailShape railshape = blockstate.getBlock() instanceof BaseRailBlock
                    ? ((BaseRailBlock)blockstate.getBlock()).getRailDirection(blockstate, level, blockpos, null)
                : RailShape.NORTH_SOUTH;
            double d0 = 0.0;
            if (railshape.isSlope()) {
                d0 = 0.5;
            }

            Vec3 vec3 = new Vec3(blockpos.getX() + 0.5, blockpos.getY() + 0.0625 + d0, blockpos.getZ() + 0.5);
            AbstractMinecart abstractminecart = AbstractMinecart.createMinecart(
                level, vec3.x, vec3.y, vec3.z, this.type, EntitySpawnReason.DISPENSER, itemstack, context.getPlayer()
            );
            if (abstractminecart == null) {
                return InteractionResult.FAIL;
            } else {
                if (AbstractMinecart.useExperimentalMovement(level)) {
                    for (Entity entity : level.getEntities(null, abstractminecart.getBoundingBox())) {
                        if (entity instanceof AbstractMinecart) {
                            return InteractionResult.FAIL;
                        }
                    }
                }

                if (level instanceof ServerLevel serverlevel) {
                    serverlevel.addFreshEntity(abstractminecart);
                    serverlevel.gameEvent(
                        GameEvent.ENTITY_PLACE, blockpos, GameEvent.Context.of(context.getPlayer(), serverlevel.getBlockState(blockpos.below()))
                    );
                }

                itemstack.shrink(1);
                return InteractionResult.SUCCESS;
            }
        }
    }
}
