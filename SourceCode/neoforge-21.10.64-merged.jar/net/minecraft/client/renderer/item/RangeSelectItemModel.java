package net.minecraft.client.renderer.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RangeSelectItemModel implements ItemModel {
    private static final int LINEAR_SEARCH_THRESHOLD = 16;
    private final RangeSelectItemModelProperty property;
    private final float scale;
    private final float[] thresholds;
    private final ItemModel[] models;
    private final ItemModel fallback;

    RangeSelectItemModel(RangeSelectItemModelProperty property, float scale, float[] thresholds, ItemModel[] models, ItemModel fallback) {
        this.property = property;
        this.thresholds = thresholds;
        this.models = models;
        this.fallback = fallback;
        this.scale = scale;
    }

    private static int lastIndexLessOrEqual(float[] thresholds, float value) {
        if (thresholds.length < 16) {
            for (int k = 0; k < thresholds.length; k++) {
                if (thresholds[k] > value) {
                    return k - 1;
                }
            }

            return thresholds.length - 1;
        } else {
            int i = Arrays.binarySearch(thresholds, value);
            if (i < 0) {
                int j = ~i;
                return j - 1;
            } else {
                return i;
            }
        }
    }

    @Override
    public void update(
        ItemStackRenderState renderState,
        ItemStack stack,
        ItemModelResolver itemModelResolver,
        ItemDisplayContext displayContext,
        @Nullable ClientLevel level,
        @Nullable ItemOwner owner,
        int seed
    ) {
        renderState.appendModelIdentityElement(this);
        float f = this.property.get(stack, level, owner, seed) * this.scale;
        ItemModel itemmodel;
        if (Float.isNaN(f)) {
            itemmodel = this.fallback;
        } else {
            int i = lastIndexLessOrEqual(this.thresholds, f);
            itemmodel = i == -1 ? this.fallback : this.models[i];
        }

        itemmodel.update(renderState, stack, itemModelResolver, displayContext, level, owner, seed);
    }

    @OnlyIn(Dist.CLIENT)
    public record Entry(float threshold, ItemModel.Unbaked model) {
        public static final Codec<RangeSelectItemModel.Entry> CODEC = RecordCodecBuilder.create(
            p_388203_ -> p_388203_.group(
                    Codec.FLOAT.fieldOf("threshold").forGetter(RangeSelectItemModel.Entry::threshold),
                    ItemModels.CODEC.fieldOf("model").forGetter(RangeSelectItemModel.Entry::model)
                )
                .apply(p_388203_, RangeSelectItemModel.Entry::new)
        );
        public static final Comparator<RangeSelectItemModel.Entry> BY_THRESHOLD = Comparator.comparingDouble(RangeSelectItemModel.Entry::threshold);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked(RangeSelectItemModelProperty property, float scale, List<RangeSelectItemModel.Entry> entries, Optional<ItemModel.Unbaked> fallback)
        implements ItemModel.Unbaked {
        public static final MapCodec<RangeSelectItemModel.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_387435_ -> p_387435_.group(
                    RangeSelectItemModelProperties.MAP_CODEC.forGetter(RangeSelectItemModel.Unbaked::property),
                    Codec.FLOAT.optionalFieldOf("scale", 1.0F).forGetter(RangeSelectItemModel.Unbaked::scale),
                    RangeSelectItemModel.Entry.CODEC.listOf().fieldOf("entries").forGetter(RangeSelectItemModel.Unbaked::entries),
                    ItemModels.CODEC.optionalFieldOf("fallback").forGetter(RangeSelectItemModel.Unbaked::fallback)
                )
                .apply(p_387435_, RangeSelectItemModel.Unbaked::new)
        );

        @Override
        public MapCodec<RangeSelectItemModel.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext p_388005_) {
            float[] afloat = new float[this.entries.size()];
            ItemModel[] aitemmodel = new ItemModel[this.entries.size()];
            List<RangeSelectItemModel.Entry> list = new ArrayList<>(this.entries);
            list.sort(RangeSelectItemModel.Entry.BY_THRESHOLD);

            for (int i = 0; i < list.size(); i++) {
                RangeSelectItemModel.Entry rangeselectitemmodel$entry = list.get(i);
                afloat[i] = rangeselectitemmodel$entry.threshold;
                aitemmodel[i] = rangeselectitemmodel$entry.model.bake(p_388005_);
            }

            ItemModel itemmodel = this.fallback.<ItemModel>map(p_387030_ -> p_387030_.bake(p_388005_)).orElse(p_388005_.missingItemModel());
            return new RangeSelectItemModel(this.property, this.scale, afloat, aitemmodel, itemmodel);
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver p_387826_) {
            this.fallback.ifPresent(p_387900_ -> p_387900_.resolveDependencies(p_387826_));
            this.entries.forEach(p_387986_ -> p_387986_.model.resolveDependencies(p_387826_));
        }
    }
}
