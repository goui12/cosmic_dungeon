package net.goui.cosmicdungeon.block.entity;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** Each mob wakes independently. No spawner-wide combat flag can activate later waves. */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class CosmicSpawnerAwareness {
    public static final String AWAKE = "cosmicdungeon:spawner_awake";
    public static final double DETECTION_RANGE = 8.0;
    private CosmicSpawnerAwareness() {}
    static boolean managed(Entity entity) {
        return entity.getTags().stream().anyMatch(tag -> CosmicSpawnerEntities.position(tag) != null);
    }
    static void fresh(Entity entity) {
        if (entity instanceof Mob mob && managed(entity)) {
            entity.getPersistentData().putBoolean(AWAKE, false);
            mob.setTarget(null);
        }
    }
    static boolean dormant(Mob mob) {
        if (!CosmicSpawnerEntities.tracked(mob)) return false;
        var data = mob.getPersistentData();
        // Legacy actively fighting mobs keep their encounter when first seen after an update.
        return !awake(data, mob.getTarget() != null);
    }
    static boolean awake(net.minecraft.nbt.CompoundTag data, boolean legacyHasTarget) {
        if (!data.contains(AWAKE)) data.putBoolean(AWAKE, legacyHasTarget);
        return data.getBooleanOr(AWAKE, false);
    }
    static boolean detects(double distanceSquared, boolean visible, boolean eligible) {
        return eligible && visible && distanceSquared <= DETECTION_RANGE * DETECTION_RANGE;
    }
    public static boolean waitForPlayer(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level) || !dormant(mob)) return false;
        if ((mob.tickCount + mob.getId()) % 10 == 0) {
            // Bounded local search, no pathfinding and no scan of other spawned mobs.
            for (Player player : level.getEntitiesOfClass(Player.class,
                    mob.getBoundingBox().inflate(DETECTION_RANGE), p -> p.isAlive()
                            && !p.isSpectator() && !p.isCreative())) {
                if (detects(mob.distanceToSqr(player), mob.hasLineOfSight(player), true)) {
                    mob.getPersistentData().putBoolean(AWAKE, true);
                    return false;
                }
            }
        }
        if (mob.getTarget() != null) mob.setTarget(null);
        mob.getNavigation().stop();
        mob.setSpeed(0);
        mob.setZza(0);
        mob.setXxa(0);
        mob.setJumping(false);
        return true;
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void targeting(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Mob mob && mob.level() instanceof ServerLevel
                && event.getNewAboutToBeSetTarget() != null && dormant(mob))
            event.setNewAboutToBeSetTarget(null);
    }
    @SubscribeEvent
    public static void damaged(LivingDamageEvent.Post event) {
        if (event.getNewDamage() > 0 && event.getSource().getEntity() instanceof LivingEntity
                && event.getEntity() instanceof Mob mob && mob.level() instanceof ServerLevel && CosmicSpawnerEntities.tracked(mob))
            mob.getPersistentData().putBoolean(AWAKE, true);
    }
}
