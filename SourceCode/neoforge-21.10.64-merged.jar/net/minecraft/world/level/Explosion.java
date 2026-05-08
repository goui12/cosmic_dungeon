package net.minecraft.world.level;

import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

public interface Explosion {
    static DamageSource getDefaultDamageSource(Level level, @Nullable Entity source) {
        return level.damageSources().explosion(source, getIndirectSourceEntity(source));
    }

    @Nullable
    static LivingEntity getIndirectSourceEntity(@Nullable Entity source) {
        return switch (source) {
            case PrimedTnt primedtnt -> primedtnt.getOwner();
            case LivingEntity livingentity -> livingentity;
            case Projectile projectile when projectile.getOwner() instanceof LivingEntity livingentity1 -> livingentity1;
            case null, default -> null;
        };
    }

    ServerLevel level();

    Explosion.BlockInteraction getBlockInteraction();

    @Nullable
    LivingEntity getIndirectSourceEntity();

    @Nullable
    Entity getDirectSourceEntity();

    float radius();

    Vec3 center();

    boolean canTriggerBlocks();

    boolean shouldAffectBlocklikeEntities();

    public static enum BlockInteraction {
        KEEP(false),
        DESTROY(true),
        DESTROY_WITH_DECAY(true),
        TRIGGER_BLOCK(false);

        private final boolean shouldAffectBlocklikeEntities;

        private BlockInteraction(boolean shouldAffectBlocklikeEntities) {
            this.shouldAffectBlocklikeEntities = shouldAffectBlocklikeEntities;
        }

        public boolean shouldAffectBlocklikeEntities() {
            return this.shouldAffectBlocklikeEntities;
        }
    }
}
