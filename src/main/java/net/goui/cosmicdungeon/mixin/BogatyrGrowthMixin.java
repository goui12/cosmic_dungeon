package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.playerclass.bogatyr.BogatyrGrowthAccess;
import net.minecraft.world.entity.AgeableMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AgeableMob.class)
public abstract class BogatyrGrowthMixin implements BogatyrGrowthAccess {
    @Shadow protected int forcedAge;
    @Shadow protected int forcedAgeTimer;
    @Shadow public abstract int getAge();
    @Shadow public abstract void setAge(int age);
    @Override public void cosmicdungeon$growByTicks(int ticks) {
        int age = getAge();
        if (age >= 0 || ticks <= 0) return;
        int gained = (int)Math.min(-(long)age, ticks);
        setAge(age + gained);
        forcedAge = (int)Math.min(Integer.MAX_VALUE, (long)Math.max(0, forcedAge) + gained);
        if (forcedAgeTimer == 0) forcedAgeTimer = 40;
        if (getAge() == 0) setAge(forcedAge);
    }
}
