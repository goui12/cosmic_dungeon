package net.goui.cosmicdungeon.playerclass.d1;

import net.minecraft.server.level.*;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.*;
import java.util.ArrayList;

public final class D1RocketAbilities {
    private D1RocketAbilities() {}

    public static boolean explode(FireworkRocketEntity rocket, ServerLevel level) {
        String id = D1AbilityIdentity.identify(rocket.getItem());
        var permission = D1ProjectileAccess.permission(rocket, rocket.getItem(), id);
        if (permission == D1CombatRules.Ammunition.VANILLA) return false;
        // Recognized D1 ammunition must not regain vanilla damage when its class/run gate fails.
        if (permission == D1CombatRules.Ammunition.DENIED) return true;
        var spell = D1AbilityConfig.get("pyroclast", id);
        if (spell == null) return true;
        var player = (ServerPlayer) rocket.getOwner();
        double radius = D1AbilityConfig.ROCKET_RADIUS.get(), power = spell.power().get();
        int limit = D1AbilityConfig.ROCKET_CANDIDATE_LIMIT.get();
        var candidates = new ArrayList<LivingEntity>();
        int[] visited = {0};
        level.getEntities().get(EntityTypeTest.forClass(LivingEntity.class),
                rocket.getBoundingBox().inflate(radius), target -> {
            visited[0]++;
            if (target.isAlive() && !target.isSpectator() && rocket.distanceToSqr(target) < radius * radius)
                candidates.add(target);
            return visited[0] >= limit ? AbortableIterationConsumer.Continuation.ABORT
                    : AbortableIterationConsumer.Continuation.CONTINUE;
        });
        Vec3 origin = rocket.position();
        for (var target : candidates) {
            double damage = D1CombatRules.rocketDamage(power, rocket.distanceTo(target), radius);
            if (damage <= 0) continue;
            boolean clear = false;
            for (int i = 0; i < 2; i++) {
                Vec3 point = new Vec3(target.getX(), target.getY(0.5 * i), target.getZ());
                if (level.clip(new ClipContext(origin, point, ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, rocket)).getType() == HitResult.Type.MISS) {
                    clear = true;
                    break;
                }
            }
            if (clear) target.hurtServer(level, rocket.damageSources().fireworks(rocket, player), (float) damage);
        }
        return true;
    }
    // TODO(M63, licensed TEST): verify crossbow stacks, multishot, shields, self/team damage,
    // walls and saturated candidate budgets. Native damage hooks remain authoritative. The
    // local iteration cap may omit targets in crowded scenes; it is not a nearest-all guarantee.
    // TODO(M63, D2+): Cinder Breeze/Whispered Cinder/Cinder Smash/Cinder Cleave and later
    // Ashwhisper/Boneflare/Doomscree/Soulshredder/Malice Reaver remain deferred. March 26
    // overview 16FD3wxi-Uen_DRzItDHdSrSZvkGNwa_r-ZeUYiswxoE puts only Cinderbite (4 stars,
    // 12 HP) and Cindermaul (5 stars, 15 HP) in D1. Preserve authored payload colors/shapes.
}
