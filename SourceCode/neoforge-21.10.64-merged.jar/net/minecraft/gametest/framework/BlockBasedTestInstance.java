package net.minecraft.gametest.framework;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TestBlock;
import net.minecraft.world.level.block.entity.TestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.TestBlockMode;

public class BlockBasedTestInstance extends GameTestInstance {
    public static final MapCodec<BlockBasedTestInstance> CODEC = RecordCodecBuilder.mapCodec(
        p_397683_ -> p_397683_.group(TestData.CODEC.forGetter(GameTestInstance::info)).apply(p_397683_, BlockBasedTestInstance::new)
    );

    public BlockBasedTestInstance(TestData<Holder<TestEnvironmentDefinition>> info) {
        super(info);
    }

    @Override
    public void run(GameTestHelper helper) {
        BlockPos blockpos = this.findStartBlock(helper);
        TestBlockEntity testblockentity = helper.getBlockEntity(blockpos, TestBlockEntity.class);
        testblockentity.trigger();
        helper.onEachTick(() -> {
            List<BlockPos> list = this.findTestBlocks(helper, TestBlockMode.ACCEPT);
            if (list.isEmpty()) {
                helper.fail(Component.translatable("test_block.error.missing", TestBlockMode.ACCEPT.getDisplayName()));
            }

            boolean flag = list.stream().map(p_400878_ -> helper.getBlockEntity(p_400878_, TestBlockEntity.class)).anyMatch(TestBlockEntity::hasTriggered);
            if (flag) {
                helper.succeed();
            } else {
                this.forAllTriggeredTestBlocks(helper, TestBlockMode.FAIL, p_397155_ -> helper.fail(Component.literal(p_397155_.getMessage())));
                this.forAllTriggeredTestBlocks(helper, TestBlockMode.LOG, TestBlockEntity::trigger);
            }
        });
    }

    private void forAllTriggeredTestBlocks(GameTestHelper helper, TestBlockMode mode, Consumer<TestBlockEntity> onTrigger) {
        for (BlockPos blockpos : this.findTestBlocks(helper, mode)) {
            TestBlockEntity testblockentity = helper.getBlockEntity(blockpos, TestBlockEntity.class);
            if (testblockentity.hasTriggered()) {
                onTrigger.accept(testblockentity);
                testblockentity.reset();
            }
        }
    }

    private BlockPos findStartBlock(GameTestHelper helper) {
        List<BlockPos> list = this.findTestBlocks(helper, TestBlockMode.START);
        if (list.isEmpty()) {
            helper.fail(Component.translatable("test_block.error.missing", TestBlockMode.START.getDisplayName()));
        }

        if (list.size() != 1) {
            helper.fail(Component.translatable("test_block.error.too_many", TestBlockMode.START.getDisplayName()));
        }

        return list.getFirst();
    }

    private List<BlockPos> findTestBlocks(GameTestHelper helper, TestBlockMode mode) {
        List<BlockPos> list = new ArrayList<>();
        helper.forEveryBlockInStructure(p_397203_ -> {
            BlockState blockstate = helper.getBlockState(p_397203_);
            if (blockstate.is(Blocks.TEST_BLOCK) && blockstate.getValue(TestBlock.MODE) == mode) {
                list.add(p_397203_.immutable());
            }
        });
        return list;
    }

    @Override
    public MapCodec<BlockBasedTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.translatable("test_instance.type.block_based");
    }
}
