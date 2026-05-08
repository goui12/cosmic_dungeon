package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.VegetationFeatures;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HangingMossBlock;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;

public class PaleMossDecorator extends TreeDecorator {
    public static final MapCodec<PaleMossDecorator> CODEC = RecordCodecBuilder.mapCodec(
        p_379602_ -> p_379602_.group(
                Codec.floatRange(0.0F, 1.0F).fieldOf("leaves_probability").forGetter(p_379568_ -> p_379568_.leavesProbability),
                Codec.floatRange(0.0F, 1.0F).fieldOf("trunk_probability").forGetter(p_379338_ -> p_379338_.trunkProbability),
                Codec.floatRange(0.0F, 1.0F).fieldOf("ground_probability").forGetter(p_379346_ -> p_379346_.groundProbability)
            )
            .apply(p_379602_, PaleMossDecorator::new)
    );
    private final float leavesProbability;
    private final float trunkProbability;
    private final float groundProbability;

    @Override
    protected TreeDecoratorType<?> type() {
        return TreeDecoratorType.PALE_MOSS;
    }

    public PaleMossDecorator(float leavesProbability, float trunkProbability, float groundProbability) {
        this.leavesProbability = leavesProbability;
        this.trunkProbability = trunkProbability;
        this.groundProbability = groundProbability;
    }

    @Override
    public void place(TreeDecorator.Context context) {
        RandomSource randomsource = context.random();
        WorldGenLevel worldgenlevel = (WorldGenLevel)context.level();
        List<BlockPos> list = Util.shuffledCopy(context.logs(), randomsource);
        if (!list.isEmpty()) {
            Mutable<BlockPos> mutable = new MutableObject<>(list.getFirst());
            list.forEach(p_379657_ -> {
                if (p_379657_.getY() < mutable.getValue().getY()) {
                    mutable.setValue(p_379657_);
                }
            });
            BlockPos blockpos = mutable.getValue();
            if (randomsource.nextFloat() < this.groundProbability) {
                worldgenlevel.registryAccess()
                    .lookup(Registries.CONFIGURED_FEATURE)
                    .flatMap(p_382782_ -> p_382782_.get(VegetationFeatures.PALE_MOSS_PATCH))
                    .ifPresent(
                        p_380064_ -> p_380064_.value()
                            .place(worldgenlevel, worldgenlevel.getLevel().getChunkSource().getGenerator(), randomsource, blockpos.above())
                    );
            }

            context.logs().forEach(p_382781_ -> {
                if (randomsource.nextFloat() < this.trunkProbability) {
                    BlockPos blockpos1 = p_382781_.below();
                    if (context.isAir(blockpos1)) {
                        addMossHanger(blockpos1, context);
                    }
                }
            });
            context.leaves().forEach(p_380012_ -> {
                if (randomsource.nextFloat() < this.leavesProbability) {
                    BlockPos blockpos1 = p_380012_.below();
                    if (context.isAir(blockpos1)) {
                        addMossHanger(blockpos1, context);
                    }
                }
            });
        }
    }

    private static void addMossHanger(BlockPos pos, TreeDecorator.Context context) {
        while (context.isAir(pos.below()) && !(context.random().nextFloat() < 0.5)) {
            context.setBlock(pos, Blocks.PALE_HANGING_MOSS.defaultBlockState().setValue(HangingMossBlock.TIP, false));
            pos = pos.below();
        }

        context.setBlock(pos, Blocks.PALE_HANGING_MOSS.defaultBlockState().setValue(HangingMossBlock.TIP, true));
    }
}
