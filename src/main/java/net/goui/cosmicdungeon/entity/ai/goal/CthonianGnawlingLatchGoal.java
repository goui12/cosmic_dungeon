package net.goui.cosmicdungeon.entity.ai.goal;

import net.goui.cosmicdungeon.entity.CthonianGnawlingEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class CthonianGnawlingLatchGoal extends Goal {
    private final CthonianGnawlingEntity gnawling;

    public CthonianGnawlingLatchGoal(CthonianGnawlingEntity gnawling) {
        this.gnawling = gnawling;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = gnawling.getTarget();
        return gnawling.isLatched() || (target != null && target.isAlive());
    }

    @Override
    public boolean canContinueToUse() {
        if (gnawling.isLatched()) return true;
        LivingEntity target = gnawling.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public void tick() {
        if (gnawling.isLatched()) {
            return;
        }

        LivingEntity target = gnawling.getTarget();
        if (target == null || !target.isAlive()) return;

        gnawling.getLookControl().setLookAt(target, 180.0F, 180.0F);
        gnawling.getMoveControl().setWantedPosition(target.getX(), target.getY() + 0.35D, target.getZ(), 1.25D);

        if (gnawling.distanceToSqr(target) <= 1.44D) {
            gnawling.latchTo(target);
        }
    }
}
