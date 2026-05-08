package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class VillagerHostilesSensor extends NearestVisibleLivingEntitySensor {
    /** @deprecated Neo: Use the {@link net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps#ACCEPTABLE_VILLAGER_DISTANCES acceptable villager distances} data map instead */
    @Deprecated
    private static final ImmutableMap<EntityType<?>, Float> ACCEPTABLE_DISTANCE_FROM_HOSTILES = ImmutableMap.<EntityType<?>, Float>builder()
        .put(EntityType.DROWNED, 8.0F)
        .put(EntityType.EVOKER, 12.0F)
        .put(EntityType.HUSK, 8.0F)
        .put(EntityType.ILLUSIONER, 12.0F)
        .put(EntityType.PILLAGER, 15.0F)
        .put(EntityType.RAVAGER, 12.0F)
        .put(EntityType.VEX, 8.0F)
        .put(EntityType.VINDICATOR, 10.0F)
        .put(EntityType.ZOGLIN, 10.0F)
        .put(EntityType.ZOMBIE, 8.0F)
        .put(EntityType.ZOMBIE_VILLAGER, 8.0F)
        .build();

    @Override
    protected boolean isMatchingEntity(ServerLevel level, LivingEntity entity, LivingEntity target) {
        return this.isHostile(target) && this.isClose(entity, target);
    }

    private boolean isClose(LivingEntity attacker, LivingEntity target) {
        var acceptableDistanceFromHostile = target.getType().builtInRegistryHolder().getData(net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps.ACCEPTABLE_VILLAGER_DISTANCES);
        float f = acceptableDistanceFromHostile != null ? acceptableDistanceFromHostile.distance() : ACCEPTABLE_DISTANCE_FROM_HOSTILES.get(target.getType());
        return target.distanceToSqr(attacker) <= f * f;
    }

    @Override
    protected MemoryModuleType<LivingEntity> getMemory() {
        return MemoryModuleType.NEAREST_HOSTILE;
    }

    private boolean isHostile(LivingEntity entity) {
        var acceptableDistanceFromHostile = entity.getType().builtInRegistryHolder().getData(net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps.ACCEPTABLE_VILLAGER_DISTANCES);
        return acceptableDistanceFromHostile != null || ACCEPTABLE_DISTANCE_FROM_HOSTILES.containsKey(entity.getType());
    }
}
