package net.minecraft.client.data.models;

import java.util.List;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.SingleVariant;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.client.renderer.block.model.VariantMutator;
import net.minecraft.client.resources.model.WeightedVariants;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record MultiVariant(WeightedList<Variant> variants, WeightedList<net.neoforged.neoforge.client.model.generators.blockstate.CustomBlockStateModelBuilder> customBlockStateModels) {
    /**
     * @param customBlockStateModels Neo-added parameter to allow using custom blockstate models with MultiVariantGenerator and MultiPartGenerator (only used during datagen)
     */
    public MultiVariant(WeightedList<Variant> variants, WeightedList<net.neoforged.neoforge.client.model.generators.blockstate.CustomBlockStateModelBuilder> customBlockStateModels) {
        if (!customBlockStateModels.isEmpty()) {
            if (!variants.isEmpty()) {
                throw new IllegalArgumentException("Cannot specify both a variant list and a custom blockstate model");
            }
            this.variants = variants;
            this.customBlockStateModels = customBlockStateModels;
        } else
        if (variants.isEmpty()) {
            throw new IllegalArgumentException("Variant list must contain at least one element");
        } else {
            this.variants = variants;
            this.customBlockStateModels = customBlockStateModels;
        }
    }

    public MultiVariant(WeightedList<Variant> variants) {
        this(variants, WeightedList.of());
    }

    // Neo: convenience functions for datagen with custom blockstate models
    public static MultiVariant of(WeightedList<net.neoforged.neoforge.client.model.generators.blockstate.CustomBlockStateModelBuilder> customBlockStateModels) {
        return new MultiVariant(WeightedList.of(), customBlockStateModels);
    }

    public static MultiVariant of(net.neoforged.neoforge.client.model.generators.blockstate.CustomBlockStateModelBuilder customBlockStateModel) {
        return new MultiVariant(WeightedList.of(), WeightedList.of(customBlockStateModel));
    }

    public MultiVariant with(VariantMutator other) {
        if (!this.customBlockStateModels.isEmpty()) {
            return new MultiVariant(this.variants, this.customBlockStateModels.map(model -> model.with(other)));
        }
        return new MultiVariant(this.variants.map(other));
    }

    /**
     * Applies the given unbaked mutator.
     * <p>
     * If this multi variant consists of plain Vanilla {@linkplain Variant variants}, each variant will be
     * temporarily converted to a {@link SingleVariant.Unbaked}, mutated and then converted back to a plain variant.
     */
    public MultiVariant with(net.neoforged.neoforge.client.model.generators.blockstate.UnbakedMutator mutator) {
        if (!this.customBlockStateModels.isEmpty()) {
            return new MultiVariant(this.variants, this.customBlockStateModels.map(model -> model.with(mutator)));
        }
        return new MultiVariant(this.variants.map(v -> mutator.apply(new SingleVariant.Unbaked(v)).variant()));
    }

    public BlockStateModel.Unbaked toUnbaked() {
        if (!this.customBlockStateModels.isEmpty()) {
            var builders = this.customBlockStateModels.unwrap();
            if (builders.size() == 1) {
                return builders.getFirst().value().toUnbaked();
            }
            return new WeightedVariants.Unbaked(this.customBlockStateModels.map(net.neoforged.neoforge.client.model.generators.blockstate.CustomBlockStateModelBuilder::toUnbaked));
        }
        List<Weighted<Variant>> list = this.variants.unwrap();
        return (BlockStateModel.Unbaked)(list.size() == 1
            ? new SingleVariant.Unbaked(list.getFirst().value())
            : new WeightedVariants.Unbaked(this.variants.map(SingleVariant.Unbaked::new)));
    }
}
