package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class AttachedToLogsDecorator extends TreeDecorator {
    public static final MapCodec<AttachedToLogsDecorator> CODEC = RecordCodecBuilder.mapCodec(
        p_409864_ -> p_409864_.group(
                Codec.floatRange(0.0F, 1.0F).fieldOf("probability").forGetter(p_410879_ -> p_410879_.probability),
                BlockStateProvider.CODEC.fieldOf("block_provider").forGetter(p_410021_ -> p_410021_.blockProvider),
                ExtraCodecs.nonEmptyList(Direction.CODEC.listOf()).fieldOf("directions").forGetter(p_410225_ -> p_410225_.directions)
            )
            .apply(p_409864_, AttachedToLogsDecorator::new)
    );
    private final float probability;
    private final BlockStateProvider blockProvider;
    private final List<Direction> directions;

    public AttachedToLogsDecorator(float probability, BlockStateProvider blockProvider, List<Direction> directions) {
        this.probability = probability;
        this.blockProvider = blockProvider;
        this.directions = directions;
    }

    @Override
    public void place(TreeDecorator.Context context) {
        RandomSource randomsource = context.random();

        for (BlockPos blockpos : Util.shuffledCopy(context.logs(), randomsource)) {
            Direction direction = Util.getRandom(this.directions, randomsource);
            BlockPos blockpos1 = blockpos.relative(direction);
            if (randomsource.nextFloat() <= this.probability && context.isAir(blockpos1)) {
                context.setBlock(blockpos1, this.blockProvider.getState(randomsource, blockpos1));
            }
        }
    }

    @Override
    protected TreeDecoratorType<?> type() {
        return TreeDecoratorType.ATTACHED_TO_LOGS;
    }
}
