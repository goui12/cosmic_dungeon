package net.minecraft.world.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractBoat;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class BoatItem extends Item {
    private final EntityType<? extends AbstractBoat> entityType;

    public BoatItem(EntityType<? extends AbstractBoat> entityType, Item.Properties properties) {
        super(properties);
        this.entityType = entityType;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        HitResult hitresult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hitresult.getType() == HitResult.Type.MISS) {
            return InteractionResult.PASS;
        } else {
            Vec3 vec3 = player.getViewVector(1.0F);
            double d0 = 5.0;
            List<Entity> list = level.getEntities(
                player, player.getBoundingBox().expandTowards(vec3.scale(5.0)).inflate(1.0), EntitySelector.CAN_BE_PICKED
            );
            if (!list.isEmpty()) {
                Vec3 vec31 = player.getEyePosition();

                for (Entity entity : list) {
                    AABB aabb = entity.getBoundingBox().inflate(entity.getPickRadius());
                    if (aabb.contains(vec31)) {
                        return InteractionResult.PASS;
                    }
                }
            }

            if (hitresult.getType() == HitResult.Type.BLOCK) {
                AbstractBoat abstractboat = this.getBoat(level, hitresult, itemstack, player);
                if (abstractboat == null) {
                    return InteractionResult.FAIL;
                } else {
                    abstractboat.setYRot(player.getYRot());
                    if (!level.noCollision(abstractboat, abstractboat.getBoundingBox())) {
                        return InteractionResult.FAIL;
                    } else {
                        if (!level.isClientSide()) {
                            level.addFreshEntity(abstractboat);
                            level.gameEvent(player, GameEvent.ENTITY_PLACE, hitresult.getLocation());
                            itemstack.consume(1, player);
                        }

                        player.awardStat(Stats.ITEM_USED.get(this));
                        return InteractionResult.SUCCESS;
                    }
                }
            } else {
                return InteractionResult.PASS;
            }
        }
    }

    @Nullable
    private AbstractBoat getBoat(Level level, HitResult hitResult, ItemStack stack, Player player) {
        AbstractBoat abstractboat = this.entityType.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (abstractboat != null) {
            Vec3 vec3 = hitResult.getLocation();
            abstractboat.setInitialPos(vec3.x, vec3.y, vec3.z);
            if (level instanceof ServerLevel serverlevel) {
                EntityType.<AbstractBoat>createDefaultStackConfig(serverlevel, stack, player).accept(abstractboat);
            }
        }

        return abstractboat;
    }
}
