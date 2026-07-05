package net.goui.cosmicdungeon.block.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.warden.Warden;

/**
 * Server-side spawn tuning that only applies to entities emitted by Cosmic Mob Spawners.
 * The marker is stored on the spawned entity, not the spawner block entity, so this does
 * not change world/chunk Cosmic Spawner save schemas.
 */
public final class CosmicSpawnerSpawnDefaults {
    private static final String APPLIED_VERSION_KEY = "cosmicdungeon:spawner_spawn_defaults_applied_version";
    private static final int APPLIED_VERSION = 1;
    private static final long WARDEN_DIG_COOLDOWN_TICKS = 1200L;
    private static final int MAX_STANDARD_SLIME_SIZE = 4;
    private static final ResourceLocation WARDEN_ID = ResourceLocation.withDefaultNamespace("warden");
    private static final ResourceLocation SLIME_ID = ResourceLocation.withDefaultNamespace("slime");
    private static final ResourceLocation MAGMA_CUBE_ID = ResourceLocation.withDefaultNamespace("magma_cube");

    private CosmicSpawnerSpawnDefaults() {}

    public static void applyIfNeeded(Entity entity) {
        if (entity == null || entity.getPersistentData().getIntOr(APPLIED_VERSION_KEY, 0) >= APPLIED_VERSION) {
            return;
        }

        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (WARDEN_ID.equals(entityId) && entity instanceof Warden warden) {
            applyWardenDigCooldown(warden);
        } else if ((SLIME_ID.equals(entityId) || MAGMA_CUBE_ID.equals(entityId)) && entity instanceof Slime slime) {
            applyMaxStandardSlimeSize(slime);
        }

        entity.getPersistentData().putInt(APPLIED_VERSION_KEY, APPLIED_VERSION);
    }

    private static void applyWardenDigCooldown(Warden warden) {
        warden.getBrain().setMemoryWithExpiry(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, WARDEN_DIG_COOLDOWN_TICKS);
    }

    private static void applyMaxStandardSlimeSize(Slime slime) {
        if (slime.getSize() < MAX_STANDARD_SLIME_SIZE) {
            slime.setSize(MAX_STANDARD_SLIME_SIZE, true);
        }
    }
}
