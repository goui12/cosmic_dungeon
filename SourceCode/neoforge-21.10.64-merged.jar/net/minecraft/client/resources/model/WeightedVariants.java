package net.minecraft.client.resources.model;

import java.util.List;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WeightedVariants implements BlockStateModel, net.neoforged.neoforge.client.model.DynamicBlockStateModel {
    private final WeightedList<BlockStateModel> list;
    private final TextureAtlasSprite particleIcon;

    public WeightedVariants(WeightedList<BlockStateModel> list) {
        this.list = list;
        BlockStateModel blockstatemodel = list.unwrap().getFirst().value();
        this.particleIcon = blockstatemodel.particleIcon();
    }

    @Override
    public TextureAtlasSprite particleIcon() {
        return this.particleIcon;
    }

    // Neo: Implement our overload so child models can have custom logic
    @Override
    public void collectParts(net.minecraft.world.level.BlockAndTintGetter level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state, RandomSource p_409649_, List<BlockModelPart> p_410123_) {
        this.list.getRandomOrThrow(p_409649_).collectParts(level, pos, state, p_409649_, p_410123_);
    }

    @Override
    public Object createGeometryKey(net.minecraft.world.level.BlockAndTintGetter level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state, RandomSource random) {
        return this.list.getRandomOrThrow(random).createGeometryKey(level, pos, state, random);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked(WeightedList<BlockStateModel.Unbaked> entries) implements BlockStateModel.Unbaked {
        @Override
        public BlockStateModel bake(ModelBaker p_410875_) {
            return new WeightedVariants(this.entries.map(p_409707_ -> p_409707_.bake(p_410875_)));
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver p_409900_) {
            this.entries.unwrap().forEach(p_409719_ -> p_409719_.value().resolveDependencies(p_409900_));
        }
    }
}
