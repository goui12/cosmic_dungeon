package net.minecraft.client.renderer.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CompositeModel implements ItemModel {
    private final List<ItemModel> models;

    public CompositeModel(List<ItemModel> models) {
        this.models = models;
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
        renderState.ensureCapacity(this.models.size());

        for (ItemModel itemmodel : this.models) {
            itemmodel.update(renderState, stack, itemModelResolver, displayContext, level, owner, seed);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked(List<ItemModel.Unbaked> models) implements ItemModel.Unbaked {
        public static final MapCodec<CompositeModel.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_388702_ -> p_388702_.group(ItemModels.CODEC.listOf().fieldOf("models").forGetter(CompositeModel.Unbaked::models))
                .apply(p_388702_, CompositeModel.Unbaked::new)
        );

        @Override
        public MapCodec<CompositeModel.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            for (ItemModel.Unbaked itemmodel$unbaked : this.models) {
                itemmodel$unbaked.resolveDependencies(resolver);
            }
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context) {
            return new CompositeModel(this.models.stream().map(p_387424_ -> p_387424_.bake(context)).toList());
        }
    }
}
