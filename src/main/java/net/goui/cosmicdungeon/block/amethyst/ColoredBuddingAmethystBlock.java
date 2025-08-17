package net.goui.cosmicdungeon.block.amethyst;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public class ColoredBuddingAmethystBlock extends Block {
    private final Supplier<Block> smallBud;
    private final Supplier<Block> mediumBud;
    private final Supplier<Block> largeBud;
    private final Supplier<Block> cluster;
    private final int growthChance;

    public ColoredBuddingAmethystBlock(Properties props,
                                       Supplier<Block> smallBud,
                                       Supplier<Block> mediumBud,
                                       Supplier<Block> largeBud,
                                       Supplier<Block> cluster,
                                       int growthChance) {
        super(props.randomTicks().strength(1.5F).sound(SoundType.AMETHYST));
        this.smallBud  = smallBud;
        this.mediumBud = mediumBud;
        this.largeBud  = largeBud;
        this.cluster   = cluster;
        this.growthChance = Math.max(1, growthChance);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rnd) {
        if (rnd.nextInt(growthChance) != 0) return;

        Direction dir = Direction.getRandom(rnd);
        BlockPos target = pos.relative(dir);
        if (!canAttach(level, target, dir)) return;

        BlockState t = level.getBlockState(target);
        Block small  = smallBud.get();
        Block medium = mediumBud.get();
        Block large  = largeBud.get();
        Block clust  = cluster.get();

        BlockState next = null;

        if (t.isAir()) {
            next = oriented(small, dir);
        } else if (t.is(small)) {
            next = oriented(medium, dir);
        } else if (t.is(medium)) {
            next = oriented(large, dir);
        } else if (t.is(large)) {
            next = oriented(clust, dir);
        }

        if (next != null) level.setBlockAndUpdate(target, next);
    }

    private static boolean canAttach(LevelAccessor level, BlockPos toPos, Direction growDir) {
        BlockPos behind = toPos.relative(growDir.getOpposite());
        return Block.canSupportCenter(level, behind, growDir);
    }

    private static BlockState oriented(Block block, Direction dir) {
        return block.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, dir);
    }
}
