package net.goui.cosmicdungeon.entity.ai.goal;

import net.goui.cosmicdungeon.entity.CrystalCreeperEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Mostly vanilla creeper swell logic, adapted for CrystalCreeperEntity.
 */
public class CrystalCreeperSwellGoal extends Goal {

    private final CrystalCreeperEntity creeper;
    private LivingEntity target;

    public CrystalCreeperSwellGoal(CrystalCreeperEntity creeper) {
        this.creeper = creeper;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity livingentity = this.creeper.getTarget();
        return livingentity != null || this.creeper.getSwell() > 0;
    }

    @Override
    public void start() {
        this.creeper.getNavigation().stop();
        this.target = this.creeper.getTarget();
    }

    @Override
    public void stop() {
        this.target = null;
    }

    @Override
    public void tick() {
        if (this.target == null) {
            this.creeper.setSwellDir(-1);
            return;
        }

        double distanceSq = this.creeper.distanceToSqr(this.target);
        if (distanceSq > 49.0D || !this.creeper.getSensing().hasLineOfSight(this.target)) {
            this.creeper.setSwellDir(-1);
        } else if (distanceSq < 9.0D) {
            this.creeper.setSwellDir(1);
        } else {
            this.creeper.setSwellDir(-1);
        }
    }
}
