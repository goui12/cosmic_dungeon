package net.goui.cosmicdungeon.entity.ai.goal;

import net.goui.cosmicdungeon.entity.CrystalCreeperEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;

import java.util.EnumSet;
import java.util.List;

public class CrystalCreeperEatAmethystGoal extends Goal {

    private final CrystalCreeperEntity creeper;
    private final double speedModifier;
    private ItemEntity targetItem;
    private int eatTicks;

    public CrystalCreeperEatAmethystGoal(CrystalCreeperEntity creeper, double speedModifier) {
        this.creeper = creeper;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.creeper.getStage() >= 3) {
            return false;
        }

        double radius = 12.0D;
        List<ItemEntity> list = this.creeper.level().getEntitiesOfClass(
                ItemEntity.class,
                this.creeper.getBoundingBox().inflate(radius),
                item -> item.isAlive()
                        && !item.level().isClientSide()
                        // TODO: Replace with your Cosmic Dungeon amethyst bud item
                        && item.getItem().is(Items.AMETHYST_SHARD)
        );

        if (list.isEmpty()) {
            return false;
        }

        list.sort((a, b) -> {
            double da = this.creeper.distanceToSqr(a);
            double db = this.creeper.distanceToSqr(b);
            return Double.compare(da, db);
        });

        this.targetItem = list.get(0);
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.targetItem == null || !this.targetItem.isAlive()) {
            return false;
        }
        if (this.eatTicks > 0) {
            return true;
        }
        double distSqr = this.creeper.distanceToSqr(this.targetItem);
        return distSqr < 16.0D; // 4 blocks
    }

    @Override
    public void start() {
        this.eatTicks = 0;
        this.creeper.setEating(false);
    }

    @Override
    public void stop() {
        this.creeper.getNavigation().stop();
        this.creeper.setEating(false);
        this.targetItem = null;
        this.eatTicks = 0;
    }

    @Override
    public void tick() {
        if (this.targetItem == null) {
            return;
        }

        this.creeper.getLookControl().setLookAt(this.targetItem, 30.0F, 30.0F);

        double distSqr = this.creeper.distanceToSqr(this.targetItem);
        if (distSqr > 4.0D && this.eatTicks == 0) {
            // Walk toward item
            this.creeper.getNavigation().moveTo(this.targetItem, this.speedModifier);
        } else {
            // Close enough to "eat"
            this.creeper.getNavigation().stop();
            this.eatTicks++;

            if (this.eatTicks == 1) {
                this.creeper.setEating(true);
            }

            // Let the animation run for ~20 ticks (1 second)
            if (this.eatTicks >= 20) {
                if (this.targetItem.isAlive()) {
                    this.targetItem.getItem().shrink(1);
                    if (this.targetItem.getItem().isEmpty()) {
                        this.targetItem.discard();
                    }
                }
                this.creeper.onEatAmethyst();
                this.creeper.setEating(false);
                this.eatTicks = 0;
            }
        }
    }
}
