package net.goui.cosmicdungeon.block.custom.amethyst;

import net.goui.cosmicdungeon.common.color.AmethystColor;
import net.goui.cosmicdungeon.common.properties.ModProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class ColoredAmethystBlock extends Block {
    public ColoredAmethystBlock(Properties p) { super(p); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(ModProperties.COLOR);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(ModProperties.COLOR, AmethystColor.PURPLE); // default
    }
}
