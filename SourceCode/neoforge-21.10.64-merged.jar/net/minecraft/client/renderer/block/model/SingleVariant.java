package net.minecraft.client.renderer.block.model;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SingleVariant implements BlockStateModel {
    private final BlockModelPart model;

    public SingleVariant(BlockModelPart model) {
        this.model = model;
    }

    @Override
    public void collectParts(RandomSource random, List<BlockModelPart> output) {
        output.add(this.model);
    }

    @Override
    public Object createGeometryKey(net.minecraft.world.level.BlockAndTintGetter level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state, RandomSource random) {
        return this;
    }

    @Override
    public TextureAtlasSprite particleIcon() {
        return this.model.particleIcon();
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked(Variant variant) implements BlockStateModel.Unbaked {
        public static final com.mojang.serialization.MapCodec<SingleVariant.Unbaked> MAP_CODEC = Variant.MAP_CODEC.xmap(SingleVariant.Unbaked::new, SingleVariant.Unbaked::variant);
        public static final Codec<SingleVariant.Unbaked> CODEC = MAP_CODEC.codec();

        @Override
        public BlockStateModel bake(ModelBaker p_410856_) {
            return new SingleVariant(this.variant.bake(p_410856_));
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver p_410724_) {
            this.variant.resolveDependencies(p_410724_);
        }
    }
}
