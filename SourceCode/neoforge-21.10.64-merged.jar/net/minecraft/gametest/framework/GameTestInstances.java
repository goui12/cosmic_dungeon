package net.minecraft.gametest.framework;

import java.util.function.Consumer;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public interface GameTestInstances {
    ResourceKey<GameTestInstance> ALWAYS_PASS = create("always_pass");

    static void bootstrap(BootstrapContext<GameTestInstance> context) {
        HolderGetter<Consumer<GameTestHelper>> holdergetter = context.lookup(Registries.TEST_FUNCTION);
        HolderGetter<TestEnvironmentDefinition> holdergetter1 = context.lookup(Registries.TEST_ENVIRONMENT);
        context.register(
            ALWAYS_PASS,
            new FunctionGameTestInstance(
                BuiltinTestFunctions.ALWAYS_PASS,
                new TestData<>(holdergetter1.getOrThrow(GameTestEnvironments.DEFAULT_KEY), ResourceLocation.withDefaultNamespace("empty"), 1, 1, false)
            )
        );
    }

    private static ResourceKey<GameTestInstance> create(String key) {
        return ResourceKey.create(Registries.TEST_INSTANCE, ResourceLocation.withDefaultNamespace(key));
    }
}
