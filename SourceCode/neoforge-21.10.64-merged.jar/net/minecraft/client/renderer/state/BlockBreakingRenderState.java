package net.minecraft.client.renderer.state;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockBreakingRenderState extends MovingBlockRenderState {
    public int progress;

    public BlockBreakingRenderState(ClientLevel level, BlockPos blockPos, int progress) {
        this.level = level;
        this.blockPos = blockPos;
        this.blockState = level.getBlockState(blockPos);
        this.progress = progress;
        this.biome = level.getBiome(blockPos);
    }
}
