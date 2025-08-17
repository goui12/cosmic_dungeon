package net.goui.cosmicdungeon.block.amethyst;

import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ColoredClusterBlock extends AmethystClusterBlock {
    // vanilla cluster size: (height=7, width=9)
    public ColoredClusterBlock(BlockBehaviour.Properties props) {
        super(7, 11, props);
    }
}
