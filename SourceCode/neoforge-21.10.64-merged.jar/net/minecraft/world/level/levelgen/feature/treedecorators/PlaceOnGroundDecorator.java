package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class PlaceOnGroundDecorator extends TreeDecorator {
    public static final MapCodec<PlaceOnGroundDecorator> CODEC = RecordCodecBuilder.mapCodec(
        p_393837_ -> p_393837_.group(
                ExtraCodecs.POSITIVE_INT.fieldOf("tries").orElse(128).forGetter(p_393560_ -> p_393560_.tries),
                ExtraCodecs.NON_NEGATIVE_INT.fieldOf("radius").orElse(2).forGetter(p_394200_ -> p_394200_.radius),
                ExtraCodecs.NON_NEGATIVE_INT.fieldOf("height").orElse(1).forGetter(p_394044_ -> p_394044_.height),
                BlockStateProvider.CODEC.fieldOf("block_state_provider").forGetter(p_393973_ -> p_393973_.blockStateProvider)
            )
            .apply(p_393837_, PlaceOnGroundDecorator::new)
    );
    private final int tries;
    private final int radius;
    private final int height;
    private final BlockStateProvider blockStateProvider;

    public PlaceOnGroundDecorator(int tries, int radius, int height, BlockStateProvider blockStateProvider) {
        this.tries = tries;
        this.radius = radius;
        this.height = height;
        this.blockStateProvider = blockStateProvider;
    }

    @Override
    protected TreeDecoratorType<?> type() {
        return TreeDecoratorType.PLACE_ON_GROUND;
    }

    @Override
    public void place(TreeDecorator.Context context) {
        List<BlockPos> list = TreeFeature.getLowestTrunkOrRootOfTree(context);
        if (!list.isEmpty()) {
            BlockPos blockpos = list.getFirst();
            int i = blockpos.getY();
            int j = blockpos.getX();
            int k = blockpos.getX();
            int l = blockpos.getZ();
            int i1 = blockpos.getZ();

            for (BlockPos blockpos1 : list) {
                if (blockpos1.getY() == i) {
                    j = Math.min(j, blockpos1.getX());
                    k = Math.max(k, blockpos1.getX());
                    l = Math.min(l, blockpos1.getZ());
                    i1 = Math.max(i1, blockpos1.getZ());
                }
            }

            RandomSource randomsource = context.random();
            BoundingBox boundingbox = new BoundingBox(j, i, l, k, i, i1).inflatedBy(this.radius, this.height, this.radius);
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

            for (int j1 = 0; j1 < this.tries; j1++) {
                blockpos$mutableblockpos.set(
                    randomsource.nextIntBetweenInclusive(boundingbox.minX(), boundingbox.maxX()),
                    randomsource.nextIntBetweenInclusive(boundingbox.minY(), boundingbox.maxY()),
                    randomsource.nextIntBetweenInclusive(boundingbox.minZ(), boundingbox.maxZ())
                );
                this.attemptToPlaceBlockAbove(context, blockpos$mutableblockpos);
            }
        }
    }

    private void attemptToPlaceBlockAbove(TreeDecorator.Context context, BlockPos pos) {
        BlockPos blockpos = pos.above();
        if (context.level().isStateAtPosition(blockpos, p_399461_ -> p_399461_.isAir() || p_399461_.is(Blocks.VINE))
            && context.checkBlock(pos, BlockBehaviour.BlockStateBase::isSolidRender)
            && context.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos).getY() <= blockpos.getY()) {
            context.setBlock(blockpos, this.blockStateProvider.getState(context.random(), blockpos));
        }
    }
}
