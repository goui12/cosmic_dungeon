package net.goui.cosmicdungeon.playerclass.dragoon;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.particle.ModParticleTypes;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.api.ClassKeys;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class DragoonPassiveEvents {
    private static final int PARTICLES_PER_ARC = 10;
    private static final ThreadLocal<Boolean> CHAINING = ThreadLocal.withInitial(() -> false);

    private DragoonPassiveEvents() {}
    public static boolean chaining() { return CHAINING.get(); }


    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (CHAINING.get()) return;
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        if (!(event.getEntity() instanceof Mob initialTarget)) return;
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer dragoon)) return;
        if (!ClassKeys.CLASS_ID_DRAGOON.equals(ClassData.getClassId(dragoon))) return;
        boolean trident = source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.ThrownTrident
                || source.getDirectEntity() == dragoon && dragoon.getMainHandItem().is(net.minecraft.world.item.Items.TRIDENT);
        if (!trident) return;
        float damage = (float)(event.getNewDamage() * Config.CHAIN_DAMAGE.get());
        if (damage <= 0.0F || dragoon.getRandom().nextDouble() >= Config.CHAIN_CHANCE.get()) return;

        List<Mob> targets = collectTargets(level, dragoon, initialTarget);
        if (targets.isEmpty()) return;

        CHAINING.set(true);
        try {
            LivingEntity from = dragoon;
            for (Mob target : targets) {
                spawnLightningArc(level, from, target);
                target.hurtServer(level, source, damage);
                from = target;
            }
        } finally {
            CHAINING.set(false);
        }
    }

    private static List<Mob> collectTargets(ServerLevel level, ServerPlayer dragoon, Mob initialTarget) {
        double range=Config.CHAIN_RADIUS.get();
        return level.getEntitiesOfClass(Mob.class, dragoon.getBoundingBox().inflate(range), mob ->
                mob.isAlive() && mob != initialTarget && mob instanceof net.minecraft.world.entity.monster.Enemy
                        && !mob.isAlliedTo(dragoon) && dragoon.distanceToSqr(mob)<=range*range
                        && dragoon.hasLineOfSight(mob))
                .stream().sorted(java.util.Comparator.comparingDouble(dragoon::distanceToSqr))
                .limit(Config.CHAIN_TARGET_LIMIT.get()).toList();
    }

    @SubscribeEvent
    public static void passiveRepair(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !ClassKeys.CLASS_ID_DRAGOON.equals(ClassData.getClassId(player))
                || !player.isAlive() || player.isSpectator()
                || player.level().getServer().overworld().getGameTime() % (20L*Config.REPAIR_POLL_SECONDS.get()) != 0) return;
        for (var slot : new net.minecraft.world.entity.EquipmentSlot[]{
                net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET}) {
            var stack=player.getItemBySlot(slot);
            double cost=Config.PASSIVE_REPAIR_HEALTH_COST.get();
            if (player.getHealth() <= player.getMaxHealth()*Config.PASSIVE_REPAIR_HEALTH_GATE.get()
                    || player.getHealth() <= cost) break;
            if (!net.goui.cosmicdungeon.playerclass.dragoon.repair.DragoonRepairRules.isSupportedDamagedItem(stack)) continue;
            stack.setDamageValue(stack.getDamageValue()-1);
            player.setHealth((float)(player.getHealth()-cost));
            player.getInventory().setChanged();
        }
    }

    private static void spawnLightningArc(ServerLevel level, LivingEntity from, LivingEntity to) {
        Vec3 start = from.position().add(0.0D, from.getBbHeight() * 0.55D, 0.0D);
        Vec3 end = to.position().add(0.0D, to.getBbHeight() * 0.55D, 0.0D);
        Vec3 delta = end.subtract(start);
        for (int i = 1; i <= PARTICLES_PER_ARC; i++) {
            double t = i / (double) (PARTICLES_PER_ARC + 1);
            Vec3 pos = start.add(delta.scale(t));
            double arch = Math.sin(Math.PI * t) * 0.22D;
            double jitterX = (level.random.nextDouble() - 0.5D) * 0.08D;
            double jitterZ = (level.random.nextDouble() - 0.5D) * 0.08D;
            level.sendParticles(ModParticleTypes.DRAGOON_LIGHTNING.get(),
                    pos.x + jitterX, pos.y + arch, pos.z + jitterZ,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }
}
