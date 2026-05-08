package net.minecraft.util;

public class BinaryAnimator {
    private final int animationLength;
    private final BinaryAnimator.EasingFunction easingFunction;
    private int ticks;
    private int ticksOld;

    public BinaryAnimator(int animationLength, BinaryAnimator.EasingFunction easingFunction) {
        this.animationLength = animationLength;
        this.easingFunction = easingFunction;
    }

    public BinaryAnimator(int animationLength) {
        this(animationLength, p_362335_ -> p_362335_);
    }

    public void tick(boolean condition) {
        this.ticksOld = this.ticks;
        if (condition) {
            if (this.ticks < this.animationLength) {
                this.ticks++;
            }
        } else if (this.ticks > 0) {
            this.ticks--;
        }
    }

    public float getFactor(float partialTick) {
        float f = Mth.lerp(partialTick, (float)this.ticksOld, (float)this.ticks) / this.animationLength;
        return this.easingFunction.apply(f);
    }

    public interface EasingFunction {
        float apply(float ticks);
    }
}
