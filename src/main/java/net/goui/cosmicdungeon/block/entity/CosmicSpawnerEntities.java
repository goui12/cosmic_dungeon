package net.goui.cosmicdungeon.block.entity;

import java.util.*;
import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Server-only derived index. Native Tags remain authoritative, including legacy placed spawners.
 * Native visibility admission, tag edits, health changes and removal maintain exact alive counts.
 * Presets apply immediately on admission; later repair of authored equipment is fairly budgeted.
 */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class CosmicSpawnerEntities {
    private CosmicSpawnerEntities() {}
    private static final String PREFIX = "cosmic_spawner_";
    private static final Map<ServerLevel, SpawnerMembership<Entity>> LEVELS = new IdentityHashMap<>();
    private static final SpawnerMaintenanceQueue<CosmicSpawnerBlockEntity> MAINTENANCE = new SpawnerMaintenanceQueue<>();

    public static void addedOrRetagged(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level) || entity.isRemoved()
                || level.getEntity(entity.getUUID()) != entity) return;
        Set<String> tags = new LinkedHashSet<>();
        for (String tag : entity.getTags()) if (position(tag) != null) tags.add(tag);
        var index = LEVELS.get(level);
        if (tags.isEmpty()) {
            if (index != null) {
                index.remove(entity.getUUID(), entity);
                if (index.size() == 0) LEVELS.remove(level);
            }
            return;
        }
        if (index == null) { index = new SpawnerMembership<>(); LEVELS.put(level, index); }
        index.put(entity.getUUID(), entity, tags, entity.isAlive());
        // Admission is already bounded by native spawning/loading; never load the owning chunk.
        for (String tag : tags) {
            BlockPos pos = position(tag);
            if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof CosmicSpawnerBlockEntity spawner)
                spawner.maintainTaggedEntity(entity);
        }
    }
    public static void tagChanged(Entity entity, String tag) {
        if (tag.startsWith(PREFIX)) addedOrRetagged(entity);
    }
    public static void healthChanged(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        var index = LEVELS.get(level);
        if (index != null) index.alive(entity.getUUID(), entity, entity.isAlive());
    }
    public static void removed(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        var index = LEVELS.get(level);
        if (index != null) {
            index.remove(entity.getUUID(), entity);
            if (index.size() == 0) LEVELS.remove(level);
        }
    }
    public static int count(ServerLevel level, String tag) {
        var index = LEVELS.get(level); return index == null ? 0 : index.alive(tag);
    }
    public static void request(CosmicSpawnerBlockEntity spawner, ServerLevel level, String tag) {
        var index = LEVELS.get(level);
        MAINTENANCE.request(spawner, level.getServer().getTickCount(), index == null ? 0 : index.size(tag));
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        MAINTENANCE.run(event.getServer().getTickCount(), Config.SPAWNER_MAINTENANCE_BUDGET.get(), spawner -> {
            if (spawner.isRemoved() || !(spawner.getLevel() instanceof ServerLevel level)) return;
            var index = LEVELS.get(level);
            if (index == null) return;
            Entity entity = index.next(spawner.oneShotSpawnTag());
            if (entity != null && !entity.isRemoved()) spawner.maintainTaggedEntity(entity);
        });
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            LEVELS.remove(level);
            MAINTENANCE.removeIf(spawner -> spawner.getLevel() == level);
        }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { LEVELS.clear(); MAINTENANCE.clear(); }

    static BlockPos position(String tag) {
        if (!tag.startsWith(PREFIX)) return null;
        String[] parts = tag.substring(PREFIX.length()).split("_", -1);
        if (parts.length != 3) return null;
        try {
            int x = Integer.parseInt(parts[0]), y = Integer.parseInt(parts[1]), z = Integer.parseInt(parts[2]);
            if (!tag.equals(PREFIX + x + "_" + y + "_" + z)) return null;
            return new BlockPos(x, y, z);
        } catch (NumberFormatException ignored) { return null; }
    }
}
