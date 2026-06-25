package net.goui.cosmicdungeon.playerclass.dragoon;

import net.goui.cosmicdungeon.CosmicDungeonMod;
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
    private static final double CHANCE = 0.03D;
    private static final double RANGE = 3.0D;
    private static final int MAX_TARGETS = 7;
    private static final int PARTICLES_PER_ARC = 10;
    private static final ThreadLocal<Boolean> CHAINING = ThreadLocal.withInitial(() -> false);

    private DragoonPassiveEvents() {}

    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (CHAINING.get()) return;
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        if (!(event.getEntity() instanceof Mob initialTarget)) return;
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer dragoon)) return;
        if (!ClassKeys.CLASS_ID_DRAGOON.equals(ClassData.getClassId(dragoon))) return;
        float damage = event.getNewDamage();
        if (damage <= 0.0F || dragoon.getRandom().nextDouble() >= CHANCE) return;

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
        AABB box = dragoon.getBoundingBox().inflate(RANGE, RANGE, RANGE);
        List<Mob> nearby = level.getEntitiesOfClass(Mob.class, box, mob ->
                mob.isAlive()
                        && mob != initialTarget
                        && !mob.isAlliedTo(dragoon)
                        && dragoon.distanceToSqr(mob) <= RANGE * RANGE
        );
        for (int i = nearby.size() - 1; i > 0; i--) {
            Collections.swap(nearby, i, dragoon.getRandom().nextInt(i + 1));
        }
        List<Mob> ordered = new ArrayList<>(Math.min(MAX_TARGETS, nearby.size() + 1));
        Set<Mob> used = new HashSet<>();
        ordered.add(initialTarget);
        used.add(initialTarget);
        for (Mob mob : nearby) {
            if (ordered.size() >= MAX_TARGETS) break;
            if (used.add(mob)) ordered.add(mob);
        }
        return ordered;
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
