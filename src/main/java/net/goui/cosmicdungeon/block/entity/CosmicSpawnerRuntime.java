package net.goui.cosmicdungeon.block.entity;

import net.goui.cosmicdungeon.mixin.BaseSpawnerAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

/** Cosmic-only placement policy; native saved data, spawn events and delay selection remain intact. */
final class CosmicSpawnerRuntime {
    private CosmicSpawnerRuntime() {}
    static void tick(ServerLevel level, BlockPos pos, CosmicSpawnerBlockEntity owner,
            BaseSpawner spawner) {
        if (!level.getServer().isSpawnerBlockEnabled()
                || !level.hasNearbyAlivePlayer(pos.getX() + 0.5, pos.getY() + 0.5,
                        pos.getZ() + 0.5, owner.getSpawnerRequiredPlayerRange())) return;
        if (owner.getSpawnerDelayTicks() == -1) delay(spawner, level, pos);
        if (owner.getSpawnerDelayTicks() > 0) {
            var profile = net.goui.cosmicdungeon.dungeon.DungeonDifficultyEvents.profile(level);
            owner.tickSpawnDelay(profile == null ? 1 : profile.spawnDelay());
            return;
        }
        if (!owner.mayRetryPlacement(level.getGameTime())) return;
        SpawnData data = owner.currentSpawnData(); // delay(-1) may select a new weighted potential.
        boolean spawned = false;
        for (int i = 0; i < owner.getSpawnerSpawnCount(); i++) {
            // Ignore authored absolute Pos only for this attempt; never rewrite the saved NBT.
            var tag = data.getEntityToSpawn().copy();
            tag.remove("Pos");
            Entity entity = EntityType.loadEntityRecursive(tag, level, EntitySpawnReason.SPAWNER,
                    part -> {
                        part.snapTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                                level.random.nextFloat() * 360.0F, 0);
                        return part;
                    });
            if (entity == null) { delay(spawner, level, pos); return; }
            if (!entity.getType().getCategory().isFriendly() && level.getDifficulty() == Difficulty.PEACEFUL) return;
            if (entity instanceof Mob mob) {
                boolean basic = tag.size() == 1 && tag.getString("id").isPresent();
                EventHooks.finalizeMobSpawnSpawner(mob, level, level.getCurrentDifficultyAt(pos),
                        EntitySpawnReason.SPAWNER, null, spawner, basic);
                data.getEquipment().ifPresent(mob::equip);
            }
            // Apply actual size/equipment before testing the complete entity/passenger bounds.
            entity.getSelfAndPassengers().forEach(part -> {
                part.addTag(owner.oneShotSpawnTag());
                CosmicSpawnerSpawnDefaults.applyIfNeeded(part);
                owner.maintainTaggedEntity(part);
            });
            if (!CosmicSpawnerPlacement.place(level, entity, pos)) {
                owner.setSpawnBlocked(true);
                owner.deferPlacement(level.getGameTime());
                if (spawned) delay(spawner, level, pos);
                return;
            }
            owner.setSpawnBlocked(false);
            if (data.getCustomSpawnRules().isPresent()
                    && !data.getCustomSpawnRules().get().isValidPosition(entity.blockPosition(), level)) continue;
            if (entity instanceof Mob mob) {
                var event = new MobSpawnEvent.PositionCheck(mob, level, EntitySpawnReason.SPAWNER, spawner);
                NeoForge.EVENT_BUS.post(event);
                if (event.getResult() == MobSpawnEvent.PositionCheck.Result.FAIL) continue;
                // DEFAULT intentionally excludes entity collision. Block bounds were checked above.
                if (event.getResult() == MobSpawnEvent.PositionCheck.Result.DEFAULT
                        && data.getCustomSpawnRules().isEmpty()
                        && !mob.checkSpawnRules(level, EntitySpawnReason.SPAWNER)) continue;
            }
            if (!entity.getSelfAndPassengers().allMatch(part -> CosmicSpawnerPlacement.clear(level, part))) continue;
            int living = (int) entity.getSelfAndPassengers().filter(Entity::isAlive).count();
            if (!fitsCap(owner.getSpawnerMobCap(),
                    CosmicSpawnerEntities.count(level, owner.oneShotSpawnTag()), living)) {
                if (spawned) delay(spawner, level, pos);
                else owner.deferPlacement(level.getGameTime());
                return;
            }
            entity.getSelfAndPassengers().forEach(CosmicSpawnerAwareness::fresh);
            if (!level.tryAddFreshEntityWithPassengers(entity)) { delay(spawner, level, pos); return; }
            level.levelEvent(2004, pos, 0);
            level.gameEvent(entity, GameEvent.ENTITY_PLACE, entity.blockPosition());
            if (entity instanceof Mob mob) mob.spawnAnim();
            spawned = true;
        }
        if (spawned) delay(spawner, level, pos);
        else owner.deferPlacement(level.getGameTime());
    }
    static boolean fitsCap(int cap, int existing, int incoming) {
        return cap <= 0 || (long) existing + incoming <= cap;
    }
    private static void delay(BaseSpawner spawner, ServerLevel level, BlockPos pos) {
        ((BaseSpawnerAccess) spawner).cosmicdungeon$delay(level, pos);
    }
}
