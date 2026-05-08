package net.minecraft.world.level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface EntityGetter {
    /**
     * Gets all entities within the specified AABB excluding the one passed into it.
     */
    List<Entity> getEntities(@Nullable Entity entity, AABB area, Predicate<? super Entity> predicate);

    <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> entityTypeTest, AABB bounds, Predicate<? super T> predicate);

    default <T extends Entity> List<T> getEntitiesOfClass(Class<T> entityClass, AABB area, Predicate<? super T> filter) {
        return this.getEntities(EntityTypeTest.forClass(entityClass), area, filter);
    }

    List<? extends Player> players();

    /**
     * Will get all entities within the specified AABB excluding the one passed into it. Args: entityToExclude, aabb
     */
    default List<Entity> getEntities(@Nullable Entity entity, AABB area) {
        return this.getEntities(entity, area, EntitySelector.NO_SPECTATORS);
    }

    default boolean isUnobstructed(@Nullable Entity p_entity, VoxelShape shape) {
        if (shape.isEmpty()) {
            return true;
        } else {
            for (Entity entity : this.getEntities(p_entity, shape.bounds())) {
                if (!entity.isRemoved()
                    && entity.blocksBuilding
                    && (p_entity == null || !entity.isPassengerOfSameVehicle(p_entity))
                    && Shapes.joinIsNotEmpty(shape, Shapes.create(entity.getBoundingBox()), BooleanOp.AND)) {
                    return false;
                }
            }

            return true;
        }
    }

    default <T extends Entity> List<T> getEntitiesOfClass(Class<T> entityClass, AABB area) {
        return this.getEntitiesOfClass(entityClass, area, EntitySelector.NO_SPECTATORS);
    }

    default List<VoxelShape> getEntityCollisions(@Nullable Entity p_entity, AABB collisionBox) {
        if (collisionBox.getSize() < 1.0E-7) {
            return List.of();
        } else {
            Predicate<Entity> predicate = p_entity == null ? EntitySelector.CAN_BE_COLLIDED_WITH : EntitySelector.NO_SPECTATORS.and(p_entity::canCollideWith);
            List<Entity> list = this.getEntities(p_entity, collisionBox.inflate(1.0E-7), predicate);
            if (list.isEmpty()) {
                return List.of();
            } else {
                Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(list.size());

                for (Entity entity : list) {
                    builder.add(Shapes.create(entity.getBoundingBox()));
                }

                return builder.build();
            }
        }
    }

    @Nullable
    default Player getNearestPlayer(double x, double y, double z, double distance, @Nullable Predicate<Entity> predicate) {
        double d0 = -1.0;
        Player player = null;

        for (Player player1 : this.players()) {
            if (predicate == null || predicate.test(player1)) {
                double d1 = player1.distanceToSqr(x, y, z);
                if ((distance < 0.0 || d1 < distance * distance) && (d0 == -1.0 || d1 < d0)) {
                    d0 = d1;
                    player = player1;
                }
            }
        }

        return player;
    }

    @Nullable
    default Player getNearestPlayer(Entity entity, double distance) {
        return this.getNearestPlayer(entity.getX(), entity.getY(), entity.getZ(), distance, false);
    }

    @Nullable
    default Player getNearestPlayer(double x, double y, double z, double distance, boolean creativePlayers) {
        Predicate<Entity> predicate = creativePlayers ? EntitySelector.NO_CREATIVE_OR_SPECTATOR : EntitySelector.NO_SPECTATORS;
        return this.getNearestPlayer(x, y, z, distance, predicate);
    }

    default boolean hasNearbyAlivePlayer(double x, double y, double z, double distance) {
        for (Player player : this.players()) {
            if (EntitySelector.NO_SPECTATORS.test(player) && EntitySelector.LIVING_ENTITY_STILL_ALIVE.test(player)) {
                double d0 = player.distanceToSqr(x, y, z);
                if (distance < 0.0 || d0 < distance * distance) {
                    return true;
                }
            }
        }

        return false;
    }

    @Nullable
    default Player getPlayerByUUID(UUID uniqueId) {
        for (int i = 0; i < this.players().size(); i++) {
            Player player = this.players().get(i);
            if (uniqueId.equals(player.getUUID())) {
                return player;
            }
        }

        return null;
    }
}
