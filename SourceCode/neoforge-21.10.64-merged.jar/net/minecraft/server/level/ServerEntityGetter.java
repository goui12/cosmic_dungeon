package net.minecraft.server.level;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.phys.AABB;

public interface ServerEntityGetter extends EntityGetter {
    ServerLevel getLevel();

    @Nullable
    default Player getNearestPlayer(TargetingConditions targetingConditions, LivingEntity source) {
        return this.getNearestEntity(this.players(), targetingConditions, source, source.getX(), source.getY(), source.getZ());
    }

    @Nullable
    default Player getNearestPlayer(TargetingConditions targetingConditions, LivingEntity source, double x, double y, double z) {
        return this.getNearestEntity(this.players(), targetingConditions, source, x, y, z);
    }

    @Nullable
    default Player getNearestPlayer(TargetingConditions targetingConditions, double x, double y, double z) {
        return this.getNearestEntity(this.players(), targetingConditions, null, x, y, z);
    }

    @Nullable
    default <T extends LivingEntity> T getNearestEntity(
        Class<? extends T> entityClass,
        TargetingConditions targetingConditions,
        @Nullable LivingEntity source,
        double x,
        double y,
        double z,
        AABB area
    ) {
        return this.getNearestEntity(this.getEntitiesOfClass(entityClass, area, p_376664_ -> true), targetingConditions, source, x, y, z);
    }

    @Nullable
    default LivingEntity getNearestEntity(
        TagKey<EntityType<?>> tag,
        TargetingConditions targetingConditions,
        @Nullable LivingEntity source,
        double x,
        double y,
        double z,
        AABB area
    ) {
        double d0 = Double.MAX_VALUE;
        LivingEntity livingentity = null;

        for (LivingEntity livingentity1 : this.getEntitiesOfClass(LivingEntity.class, area, p_437182_ -> p_437182_.getType().is(tag))) {
            if (targetingConditions.test(this.getLevel(), source, livingentity1)) {
                double d1 = livingentity1.distanceToSqr(x, y, z);
                if (d1 < d0) {
                    d0 = d1;
                    livingentity = livingentity1;
                }
            }
        }

        return livingentity;
    }

    @Nullable
    default <T extends LivingEntity> T getNearestEntity(
        List<? extends T> entities, TargetingConditions targetingConditions, @Nullable LivingEntity source, double x, double y, double z
    ) {
        double d0 = -1.0;
        T t = null;

        for (T t1 : entities) {
            if (targetingConditions.test(this.getLevel(), source, t1)) {
                double d1 = t1.distanceToSqr(x, y, z);
                if (d0 == -1.0 || d1 < d0) {
                    d0 = d1;
                    t = t1;
                }
            }
        }

        return t;
    }

    default List<Player> getNearbyPlayers(TargetingConditions targetingConditions, LivingEntity source, AABB area) {
        List<Player> list = new ArrayList<>();

        for (Player player : this.players()) {
            if (area.contains(player.getX(), player.getY(), player.getZ()) && targetingConditions.test(this.getLevel(), source, player)) {
                list.add(player);
            }
        }

        return list;
    }

    default <T extends LivingEntity> List<T> getNearbyEntities(Class<T> entityClass, TargetingConditions targetingConditions, LivingEntity source, AABB area) {
        List<T> list = this.getEntitiesOfClass(entityClass, area, p_376145_ -> true);
        List<T> list1 = new ArrayList<>();

        for (T t : list) {
            if (targetingConditions.test(this.getLevel(), source, t)) {
                list1.add(t);
            }
        }

        return list1;
    }
}
