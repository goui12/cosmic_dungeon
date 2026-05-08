package net.minecraft.world.entity.projectile;

import javax.annotation.Nullable;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@FunctionalInterface
public interface ProjectileDeflection {
    ProjectileDeflection NONE = (p_320379_, p_320626_, p_320122_) -> {};
    ProjectileDeflection REVERSE = (p_436549_, p_436550_, p_436551_) -> {
        float f = 170.0F + p_436551_.nextFloat() * 20.0F;
        p_436549_.setDeltaMovement(p_436549_.getDeltaMovement().scale(-0.5));
        p_436549_.setYRot(p_436549_.getYRot() + f);
        p_436549_.yRotO += f;
        p_436549_.hasImpulse = true;
    };
    ProjectileDeflection AIM_DEFLECT = (p_436555_, p_436556_, p_436557_) -> {
        if (p_436556_ != null) {
            Vec3 vec3 = p_436556_.getLookAngle().normalize();
            p_436555_.setDeltaMovement(vec3);
            p_436555_.hasImpulse = true;
        }
    };
    ProjectileDeflection MOMENTUM_DEFLECT = (p_436552_, p_436553_, p_436554_) -> {
        if (p_436553_ != null) {
            Vec3 vec3 = p_436553_.getDeltaMovement().normalize();
            p_436552_.setDeltaMovement(vec3);
            p_436552_.hasImpulse = true;
        }
    };

    void deflect(Projectile projectile, @Nullable Entity entity, RandomSource random);
}
