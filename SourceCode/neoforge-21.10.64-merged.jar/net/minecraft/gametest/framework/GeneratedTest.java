package net.minecraft.gametest.framework;

import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public record GeneratedTest(
    Map<ResourceLocation, TestData<ResourceKey<TestEnvironmentDefinition>>> tests,
    ResourceKey<Consumer<GameTestHelper>> functionKey,
    Consumer<GameTestHelper> function
) {
    public GeneratedTest(
        Map<ResourceLocation, TestData<ResourceKey<TestEnvironmentDefinition>>> p_397881_, ResourceLocation p_397734_, Consumer<GameTestHelper> p_397974_
    ) {
        this(p_397881_, ResourceKey.create(Registries.TEST_FUNCTION, p_397734_), p_397974_);
    }

    public GeneratedTest(ResourceLocation p_397873_, TestData<ResourceKey<TestEnvironmentDefinition>> p_397065_, Consumer<GameTestHelper> p_397393_) {
        this(Map.of(p_397873_, p_397065_), p_397873_, p_397393_);
    }
}
