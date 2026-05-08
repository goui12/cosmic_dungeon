package net.minecraft.gametest.framework;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;

public class GameTestBatchFactory {
    private static final int MAX_TESTS_PER_BATCH = 50;
    public static final GameTestBatchFactory.TestDecorator DIRECT = (p_396363_, p_396364_) -> Stream.of(
        new GameTestInfo(p_396363_, Rotation.NONE, p_396364_, RetryOptions.noRetries())
    );

    public static List<GameTestBatch> divideIntoBatches(
        Collection<Holder.Reference<GameTestInstance>> instances, GameTestBatchFactory.TestDecorator decorator, ServerLevel level
    ) {
        Map<Holder<TestEnvironmentDefinition>, List<GameTestInfo>> map = instances.stream()
            .flatMap(p_396367_ -> decorator.decorate((Holder.Reference<GameTestInstance>)p_396367_, level))
            .collect(Collectors.groupingBy(p_396368_ -> p_396368_.getTest().batch()));
        return map.entrySet().stream().flatMap(p_396359_ -> {
            Holder<TestEnvironmentDefinition> holder = p_396359_.getKey();
            List<GameTestInfo> list = p_396359_.getValue();
            return Streams.mapWithIndex(Lists.partition(list, 50).stream(), (p_396370_, p_396371_) -> toGameTestBatch(p_396370_, holder, (int)p_396371_));
        }).toList();
    }

    public static GameTestRunner.GameTestBatcher fromGameTestInfo() {
        return fromGameTestInfo(50);
    }

    public static GameTestRunner.GameTestBatcher fromGameTestInfo(int maxTests) {
        return p_351703_ -> {
            Map<Holder<TestEnvironmentDefinition>, List<GameTestInfo>> map = p_351703_.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(p_396372_ -> p_396372_.getTest().batch()));
            return map.entrySet()
                .stream()
                .flatMap(
                    p_396358_ -> {
                        Holder<TestEnvironmentDefinition> holder = p_396358_.getKey();
                        List<GameTestInfo> list = p_396358_.getValue();
                        return Streams.mapWithIndex(
                            Lists.partition(list, maxTests).stream(),
                            (p_396361_, p_396362_) -> toGameTestBatch(List.copyOf(p_396361_), holder, (int)p_396362_)
                        );
                    }
                )
                .toList();
        };
    }

    public static GameTestBatch toGameTestBatch(Collection<GameTestInfo> gameTestInfos, Holder<TestEnvironmentDefinition> environment, int index) {
        return new GameTestBatch(index, gameTestInfos, environment);
    }

    @FunctionalInterface
    public interface TestDecorator {
        Stream<GameTestInfo> decorate(Holder.Reference<GameTestInstance> instance, ServerLevel level);
    }
}
