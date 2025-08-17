package net.goui.cosmicdungeon.block.amethyst;

import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ColoredBudBlock extends AmethystClusterBlock {
    // vanilla-like sizes (height, width):
    // SMALL  = (3, 4), MEDIUM = (4, 5), LARGE = (5, 6)
    public static final int SMALL = 0, MEDIUM = 1, LARGE = 2;

    private ColoredBudBlock(BlockBehaviour.Properties props, int height, int width) {
        super(height, width, props); // AmethystClusterBlock handles FACING/WATERLOGGED/shapes
    }

    public static ColoredBudBlock small(BlockBehaviour.Properties props)  { return new ColoredBudBlock(props, 3, 8); }
    public static ColoredBudBlock medium(BlockBehaviour.Properties props) { return new ColoredBudBlock(props, 4, 9); }
    public static ColoredBudBlock large(BlockBehaviour.Properties props)  { return new ColoredBudBlock(props, 5, 10); }
}
