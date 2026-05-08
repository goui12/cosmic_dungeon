package net.minecraft.gametest.framework;

import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;

public class TestFinder implements TestInstanceFinder, TestPosFinder {
    static final TestInstanceFinder NO_FUNCTIONS = Stream::empty;
    static final TestPosFinder NO_STRUCTURES = Stream::empty;
    private final TestInstanceFinder testInstanceFinder;
    private final TestPosFinder testPosFinder;
    private final CommandSourceStack source;

    @Override
    public Stream<BlockPos> findTestPos() {
        return this.testPosFinder.findTestPos();
    }

    public static TestFinder.Builder builder() {
        return new TestFinder.Builder();
    }

    TestFinder(CommandSourceStack source, TestInstanceFinder testInstanceFinder, TestPosFinder testPosFinder) {
        this.source = source;
        this.testInstanceFinder = testInstanceFinder;
        this.testPosFinder = testPosFinder;
    }

    public CommandSourceStack source() {
        return this.source;
    }

    @Override
    public Stream<Holder.Reference<GameTestInstance>> findTests() {
        return this.testInstanceFinder.findTests();
    }

    public static class Builder {
        private final UnaryOperator<Supplier<Stream<Holder.Reference<GameTestInstance>>>> testFinderWrapper;
        private final UnaryOperator<Supplier<Stream<BlockPos>>> structureBlockPosFinderWrapper;

        public Builder() {
            this.testFinderWrapper = p_329857_ -> p_329857_;
            this.structureBlockPosFinderWrapper = p_329858_ -> p_329858_;
        }

        private Builder(UnaryOperator<Supplier<Stream<Holder.Reference<GameTestInstance>>>> testFinderWrapper, UnaryOperator<Supplier<Stream<BlockPos>>> structureBlockPosFinderWrapper) {
            this.testFinderWrapper = testFinderWrapper;
            this.structureBlockPosFinderWrapper = structureBlockPosFinderWrapper;
        }

        public TestFinder.Builder createMultipleCopies(int count) {
            return new TestFinder.Builder(createCopies(count), createCopies(count));
        }

        private static <Q> UnaryOperator<Supplier<Stream<Q>>> createCopies(int count) {
            return p_396490_ -> {
                List<Q> list = new LinkedList<>();
                List<Q> list1 = ((Stream)p_396490_.get()).toList();

                for (int i = 0; i < count; i++) {
                    list.addAll(list1);
                }

                return list::stream;
            };
        }

        private TestFinder build(CommandSourceStack source, TestInstanceFinder instanceFinder, TestPosFinder posFinder) {
            return new TestFinder(
                source, this.testFinderWrapper.apply(instanceFinder::findTests)::get, this.structureBlockPosFinderWrapper.apply(posFinder::findTestPos)::get
            );
        }

        public TestFinder radius(CommandContext<CommandSourceStack> context, int radius) {
            CommandSourceStack commandsourcestack = context.getSource();
            BlockPos blockpos = BlockPos.containing(commandsourcestack.getPosition());
            return this.build(
                commandsourcestack, TestFinder.NO_FUNCTIONS, () -> StructureUtils.findTestBlocks(blockpos, radius, commandsourcestack.getLevel())
            );
        }

        public TestFinder nearest(CommandContext<CommandSourceStack> context) {
            CommandSourceStack commandsourcestack = context.getSource();
            BlockPos blockpos = BlockPos.containing(commandsourcestack.getPosition());
            return this.build(
                commandsourcestack, TestFinder.NO_FUNCTIONS, () -> StructureUtils.findNearestTest(blockpos, 15, commandsourcestack.getLevel()).stream()
            );
        }

        public TestFinder allNearby(CommandContext<CommandSourceStack> context) {
            CommandSourceStack commandsourcestack = context.getSource();
            BlockPos blockpos = BlockPos.containing(commandsourcestack.getPosition());
            return this.build(commandsourcestack, TestFinder.NO_FUNCTIONS, () -> StructureUtils.findTestBlocks(blockpos, 250, commandsourcestack.getLevel()));
        }

        public TestFinder lookedAt(CommandContext<CommandSourceStack> context) {
            CommandSourceStack commandsourcestack = context.getSource();
            return this.build(
                commandsourcestack,
                TestFinder.NO_FUNCTIONS,
                () -> StructureUtils.lookedAtTestPos(
                    BlockPos.containing(commandsourcestack.getPosition()), commandsourcestack.getPlayer().getCamera(), commandsourcestack.getLevel()
                )
            );
        }

        public TestFinder failedTests(CommandContext<CommandSourceStack> context, boolean onlyRequired) {
            return this.build(
                context.getSource(),
                () -> FailedTestTracker.getLastFailedTests().filter(p_396483_ -> !onlyRequired || p_396483_.value().required()),
                TestFinder.NO_STRUCTURES
            );
        }

        public TestFinder byResourceSelection(CommandContext<CommandSourceStack> context, Collection<Holder.Reference<GameTestInstance>> collection) {
            return this.build(context.getSource(), collection::stream, TestFinder.NO_STRUCTURES);
        }

        public TestFinder failedTests(CommandContext<CommandSourceStack> context) {
            return this.failedTests(context, false);
        }
    }
}
