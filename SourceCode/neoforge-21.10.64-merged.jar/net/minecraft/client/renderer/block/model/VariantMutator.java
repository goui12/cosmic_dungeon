package net.minecraft.client.renderer.block.model;

import com.mojang.math.Quadrant;
import java.util.function.UnaryOperator;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@FunctionalInterface
@OnlyIn(Dist.CLIENT)
public interface VariantMutator extends UnaryOperator<Variant> {
    VariantMutator.VariantProperty<Quadrant> X_ROT = Variant::withXRot;
    VariantMutator.VariantProperty<Quadrant> Y_ROT = Variant::withYRot;
    VariantMutator.VariantProperty<ResourceLocation> MODEL = Variant::withModel;
    VariantMutator.VariantProperty<Boolean> UV_LOCK = Variant::withUvLock;

    default VariantMutator then(VariantMutator mutator) {
        return p_405783_ -> mutator.apply(this.apply(p_405783_));
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface VariantProperty<T> {
        Variant apply(Variant variant, T value);

        default VariantMutator withValue(T value) {
            return p_405243_ -> this.apply(p_405243_, value);
        }
    }
}
