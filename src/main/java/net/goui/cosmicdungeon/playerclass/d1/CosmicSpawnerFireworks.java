package net.goui.cosmicdungeon.playerclass.d1;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.mercenary.MercenaryBrain;
import net.goui.cosmicdungeon.mercenary.MercenaryEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/** Called only after the shared rocket's class, launch/run and spent-shot authorization. */
final class CosmicSpawnerFireworks {
    private static final int RADIUS = 5;
    private CosmicSpawnerFireworks() {}
    static void destroy(FireworkRocketEntity rocket, ServerLevel level, LivingEntity owner) {
        ServerPlayer actor = owner instanceof ServerPlayer player ? player
                : owner instanceof MercenaryEntity merc ? MercenaryBrain.hirer(merc) : null;
        if (actor == null || actor.level() != level || !actor.isAlive() || actor.isSpectator()) return;
        var explosion = new ServerExplosion(level, rocket, rocket.damageSources().fireworks(rocket, owner), null,
                rocket.position(), RADIUS, false, Explosion.BlockInteraction.DESTROY);
        if (NeoForge.EVENT_BUS.post(new ExplosionEvent.Start(level, explosion)).isCanceled()) return;
        var candidates = new LinkedHashSet<BlockPos>();
        BlockPos center = rocket.blockPosition();
        // At most 1,331 already-loaded cells per detonation. Snapshot visibility before removing any shield.
        for (BlockPos cursor : BlockPos.betweenClosed(center.offset(-RADIUS,-RADIUS,-RADIUS),
                center.offset(RADIUS,RADIUS,RADIUS))) {
            if (!level.hasChunkAt(cursor) || Vec3.atCenterOf(cursor).distanceToSqr(rocket.position()) > RADIUS * RADIUS
                    || !level.getBlockState(cursor).is(ModBlocks.COSMIC_MOB_SPAWNER.get())) continue;
            // A diagonal ray can cross a third chunk; require the whole small X/Z corridor to be loaded.
            if (!level.hasChunksAt(Math.min(center.getX(),cursor.getX()), Math.min(center.getZ(),cursor.getZ()),
                    Math.max(center.getX(),cursor.getX()), Math.max(center.getZ(),cursor.getZ()))) continue;
            var hit = level.clip(new ClipContext(rocket.position(), Vec3.atCenterOf(cursor),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, rocket));
            if (hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(cursor))
                candidates.add(cursor.immutable());
        }
        if (candidates.isEmpty()) return;
        var affected = new ArrayList<>(candidates);
        NeoForge.EVENT_BUS.post(new ExplosionEvent.Detonate(level, explosion, new ArrayList<>(), affected));
        // Event vetoes are honored; event-added terrain/duplicates cannot expand this ability's allowlist.
        for (BlockPos pos : new LinkedHashSet<>(affected)) {
            if (!candidates.contains(pos) || !level.hasChunkAt(pos) || !level.mayInteract(actor,pos)
                    || !level.getWorldBorder().isWithinBounds(pos)) continue;
            var state = level.getBlockState(pos);
            if (!state.is(ModBlocks.COSMIC_MOB_SPAWNER.get())
                    || !state.canEntityDestroy(level,pos,owner)) continue;
            if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level,pos,state,actor)).isCanceled()) continue;
            // Native explosion loot/removal hooks keep XP bottles and normal block-entity cleanup.
            if (level.getBlockState(pos).equals(state))
                state.onExplosionHit(level,pos,explosion,(stack,dropPos)->Block.popResource(level,dropPos,stack));
        }
    }
}
