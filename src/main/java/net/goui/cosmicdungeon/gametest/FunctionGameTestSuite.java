package net.goui.cosmicdungeon.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers executable test functions before creating native, serializable test instances. */
public final class FunctionGameTestSuite {
    private static final ResourceLocation EMPTY_STRUCTURE = ResourceLocation.withDefaultNamespace("empty");

    private final ResourceLocation environmentId;
    private final DeferredRegister<Consumer<GameTestHelper>> functions;
    private final List<DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> tests = new ArrayList<>();

    public FunctionGameTestSuite(ResourceLocation environmentId) {
        this.environmentId = environmentId;
        this.functions = DeferredRegister.create(Registries.TEST_FUNCTION, environmentId.getNamespace());
    }

    public void add(ResourceLocation id, Consumer<GameTestHelper> test) {
        if (!id.getNamespace().equals(environmentId.getNamespace())) {
            throw new IllegalArgumentException("Test function must belong to the suite namespace: " + id);
        }
        tests.add(functions.register(id.getPath(), () -> test));
    }

    public void register(IEventBus eventBus) {
        // NeoForge only fires RegisterGameTestsEvent in development. Keep these functions there too.
        if (FMLEnvironment.isProduction()) {
            return;
        }
        functions.register(eventBus);
        eventBus.addListener(this::registerTests);
    }

    private void registerTests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(environmentId, new TestEnvironmentDefinition.AllOf());
        var data = new TestData<>(environment, EMPTY_STRUCTURE, 20, 0, true, Rotation.NONE);
        for (var test : tests) {
            event.registerTest(test.getId(), new FunctionGameTestInstance(test.getKey(), data));
        }
    }
}
