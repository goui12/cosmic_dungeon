package net.minecraft.client.renderer.item.properties.numeric;

import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class NeedleDirectionHelper {
    private final boolean wobble;

    protected NeedleDirectionHelper(boolean wobble) {
        this.wobble = wobble;
    }

    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        if (owner == null) {
            owner = stack.getEntityRepresentation();
        }

        if (owner == null) {
            return 0.0F;
        } else {
            if (level == null && owner != null && owner.level() instanceof ClientLevel clientlevel) {
                level = clientlevel;
            }

            return level == null ? 0.0F : this.calculate(stack, level, seed, owner);
        }
    }

    protected abstract float calculate(ItemStack stack, ClientLevel level, int seed, @Nullable ItemOwner owner);

    protected boolean wobble() {
        return this.wobble;
    }

    protected NeedleDirectionHelper.Wobbler newWobbler(float scale) {
        return this.wobble ? standardWobbler(scale) : nonWobbler();
    }

    public static NeedleDirectionHelper.Wobbler standardWobbler(final float scale) {
        return new NeedleDirectionHelper.Wobbler() {
            private float rotation;
            private float deltaRotation;
            private long lastUpdateTick;

            @Override
            public float rotation() {
                return this.rotation;
            }

            @Override
            public boolean shouldUpdate(long p_387925_) {
                return this.lastUpdateTick != p_387925_;
            }

            @Override
            public void update(long p_387682_, float p_388243_) {
                this.lastUpdateTick = p_387682_;
                float f = Mth.positiveModulo(p_388243_ - this.rotation + 0.5F, 1.0F) - 0.5F;
                this.deltaRotation += f * 0.1F;
                this.deltaRotation = this.deltaRotation * scale;
                this.rotation = Mth.positiveModulo(this.rotation + this.deltaRotation, 1.0F);
            }
        };
    }

    public static NeedleDirectionHelper.Wobbler nonWobbler() {
        return new NeedleDirectionHelper.Wobbler() {
            private float targetValue;

            @Override
            public float rotation() {
                return this.targetValue;
            }

            @Override
            public boolean shouldUpdate(long p_388006_) {
                return true;
            }

            @Override
            public void update(long p_388810_, float p_387609_) {
                this.targetValue = p_387609_;
            }
        };
    }

    @OnlyIn(Dist.CLIENT)
    public interface Wobbler {
        float rotation();

        boolean shouldUpdate(long gameTime);

        void update(long gameTime, float targetValue);
    }
}
