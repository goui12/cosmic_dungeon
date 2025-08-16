package net.goui.cosmicdungeon.block.custom.amethyst;

import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.common.color.AmethystColor;
import net.goui.cosmicdungeon.common.properties.ModProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class ColoredBuddingAmethystBlock extends Block {

    public ColoredBuddingAmethystBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(
                this.defaultBlockState()
                        .setValue(ModProperties.COLOR, AmethystColor.PURPLE)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ModProperties.COLOR);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        AmethystColor color = state.getValue(ModProperties.COLOR);
        Direction dir = Direction.getRandom(rand);

        BlockPos targetPos = pos.relative(dir);
        BlockState targetState = level.getBlockState(targetPos);

        if (canGrow(targetState)) {
            BlockState nextStage = getNextGrowthStage(targetState, dir, color);
            if (nextStage != null) {
                level.setBlock(targetPos, nextStage, 3);
            }
        }
    }

    private boolean canGrow(BlockState state) {
        return state.isAir() ||
                state.is(ModBlocks.COLORED_AMETHYST_BUD_SMALL.get()) ||
                state.is(ModBlocks.COLORED_AMETHYST_BUD_MEDIUM.get()) ||
                state.is(ModBlocks.COLORED_AMETHYST_BUD_LARGE.get());
    }

    private BlockState getNextGrowthStage(BlockState current, Direction dir, AmethystColor color) {
        if (current.isAir()) {
            return ModBlocks.COLORED_AMETHYST_BUD_SMALL.get().defaultBlockState()
                    .setValue(BlockStateProperties.FACING, dir)
                    .setValue(BlockStateProperties.WATERLOGGED, false)
                    .setValue(ModProperties.COLOR, color);
        } else if (current.is(ModBlocks.COLORED_AMETHYST_BUD_SMALL.get())) {
            return ModBlocks.COLORED_AMETHYST_BUD_MEDIUM.get().defaultBlockState()
                    .setValue(BlockStateProperties.FACING, dir)
                    .setValue(BlockStateProperties.WATERLOGGED, current.getValue(BlockStateProperties.WATERLOGGED))
                    .setValue(ModProperties.COLOR, color);
        } else if (current.is(ModBlocks.COLORED_AMETHYST_BUD_MEDIUM.get())) {
            return ModBlocks.COLORED_AMETHYST_BUD_LARGE.get().defaultBlockState()
                    .setValue(BlockStateProperties.FACING, dir)
                    .setValue(BlockStateProperties.WATERLOGGED, current.getValue(BlockStateProperties.WATERLOGGED))
                    .setValue(ModProperties.COLOR, color);
        } else if (current.is(ModBlocks.COLORED_AMETHYST_BUD_LARGE.get())) {
            return ModBlocks.COLORED_AMETHYST_CLUSTER.get().defaultBlockState()
                    .setValue(BlockStateProperties.FACING, dir)
                    .setValue(BlockStateProperties.WATERLOGGED, current.getValue(BlockStateProperties.WATERLOGGED))
                    .setValue(ModProperties.COLOR, color);
        }
        return null;
    }
}
