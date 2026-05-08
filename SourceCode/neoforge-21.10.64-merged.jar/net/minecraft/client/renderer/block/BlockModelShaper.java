package net.minecraft.client.renderer.block;

import java.util.Map;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockModelShaper {
    private Map<BlockState, BlockStateModel> modelByStateCache = Map.of();
    private final ModelManager modelManager;

    public BlockModelShaper(ModelManager modelManager) {
        this.modelManager = modelManager;
    }

    @Deprecated
    public TextureAtlasSprite getParticleIcon(BlockState state) {
        return getParticleIcon(state, net.minecraft.world.level.EmptyBlockAndTintGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO);
    }

    public TextureAtlasSprite getParticleIcon(BlockState state, net.minecraft.world.level.BlockAndTintGetter level, net.minecraft.core.BlockPos pos) {
        var model = this.getBlockModel(state);
        return model.particleIcon(level, pos, state);
    }

    public BlockStateModel getBlockModel(BlockState state) {
        BlockStateModel blockstatemodel = this.modelByStateCache.get(state);
        if (blockstatemodel == null) {
            blockstatemodel = this.modelManager.getMissingBlockStateModel();
        }

        return blockstatemodel;
    }

    public ModelManager getModelManager() {
        return this.modelManager;
    }

    public void replaceCache(Map<BlockState, BlockStateModel> modelByStateCache) {
        this.modelByStateCache = modelByStateCache;
    }
}
