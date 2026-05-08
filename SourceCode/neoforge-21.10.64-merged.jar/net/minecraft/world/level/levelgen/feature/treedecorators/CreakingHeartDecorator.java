package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CreakingHeartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.CreakingHeartState;

public class CreakingHeartDecorator extends TreeDecorator {
    public static final MapCodec<CreakingHeartDecorator> CODEC = Codec.floatRange(0.0F, 1.0F)
        .fieldOf("probability")
        .xmap(CreakingHeartDecorator::new, p_379513_ -> p_379513_.probability);
    private final float probability;

    public CreakingHeartDecorator(float probability) {
        this.probability = probability;
    }

    @Override
    protected TreeDecoratorType<?> type() {
        return TreeDecoratorType.CREAKING_HEART;
    }

    @Override
    public void place(TreeDecorator.Context context) {
        RandomSource randomsource = context.random();
        List<BlockPos> list = context.logs();
        if (!list.isEmpty()) {
            if (!(randomsource.nextFloat() >= this.probability)) {
                List<BlockPos> list1 = new ArrayList<>(list);
                Util.shuffle(list1, randomsource);
                Optional<BlockPos> optional = list1.stream().filter(p_380105_ -> {
                    for (Direction direction : Direction.values()) {
                        if (!context.checkBlock(p_380105_.relative(direction), p_379435_ -> p_379435_.is(BlockTags.LOGS))) {
                            return false;
                        }
                    }

                    return true;
                }).findFirst();
                if (!optional.isEmpty()) {
                    context.setBlock(
                        optional.get(),
                        Blocks.CREAKING_HEART
                            .defaultBlockState()
                            .setValue(CreakingHeartBlock.STATE, CreakingHeartState.DORMANT)
                            .setValue(CreakingHeartBlock.NATURAL, true)
                    );
                }
            }
        }
    }
}
