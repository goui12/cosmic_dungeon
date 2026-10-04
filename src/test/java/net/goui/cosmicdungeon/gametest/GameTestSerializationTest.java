package net.goui.cosmicdungeon.gametest;

import com.mojang.serialization.Lifecycle;
import java.util.Set;
import java.util.stream.Stream;
import net.goui.cosmicdungeon.dungeon.DungeonInstanceGameTests;
import net.goui.cosmicdungeon.trade.TradeFinalizationGameTests;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** Exercises the actual registry element codec used during client configuration. */
public final class GameTestSerializationTest {
    private int checks;

    @org.junit.jupiter.api.Test
    public void registryPayloadRoundTrips() throws Exception {
        var environments = new MappedRegistry<TestEnvironmentDefinition>(Registries.TEST_ENVIRONMENT, Lifecycle.stable());
        var tests = new MappedRegistry<GameTestInstance>(Registries.TEST_INSTANCE, Lifecycle.stable());
        var event = new RegisterGameTestsEvent(environments, tests);
        // Use the real suites without requiring a mod event bus or opening a client/server.
        for (Class<?> owner : new Class<?>[]{TradeFinalizationGameTests.class, DungeonInstanceGameTests.class}) {
            var field = owner.getDeclaredField("SUITE");
            field.setAccessible(true);
            var registration = FunctionGameTestSuite.class.getDeclaredMethod("registerTests", RegisterGameTestsEvent.class);
            registration.setAccessible(true);
            registration.invoke(field.get(null), event);
        }
        var expected = Set.of("trade_finalization/item_only", "trade_finalization/currency_only",
                "trade_finalization/mixed", "trade_finalization/full_inventory", "trade_finalization/capacity_limit",
                "slot_mapping", "legacy_run_codec", "farrows_chop_target_codec", "inventory_escrow_codec",
                "startup_plan_one_player", "startup_plan_four_players", "startup_plan_six_players",
                "startup_plan_duplicate_classes", "startup_definition_integrity", "startup_plan_party_counts",
                "startup_plan_blank_entry", "class_selector_ready_eligibility", "startup_plan_rejects_none");
        require(tests.keySet().stream().map(id -> id.getPath()).collect(java.util.stream.Collectors.toSet()).equals(expected),
                "Every existing test ID remains registered");
        require(environments.size() == 2, "Both test environments remain registered");
        var lookup = HolderLookup.Provider.create(Stream.of(environments));
        var ops = RegistryOps.create(NbtOps.INSTANCE, lookup);
        for (var id : tests.keySet()) {
            var test = tests.getValue(id);
            require(net.minecraft.core.registries.BuiltInRegistries.TEST_FUNCTION.getValue(id) != null,
                    "Executable test function registered for " + id);
            require(test instanceof FunctionGameTestInstance, "Native instance for " + id);
            var encoded = GameTestInstance.DIRECT_CODEC.encodeStart(ops, test).getOrThrow();
            var decoded = GameTestInstance.DIRECT_CODEC.parse(ops, encoded).getOrThrow();
            require(GameTestInstance.DIRECT_CODEC.encodeStart(ops, decoded).getOrThrow().equals(encoded),
                    "Registry payload round trip for " + id);
            require(decoded.structure().equals(test.structure()) && decoded.maxTicks() == 20
                    && decoded.setupTicks() == 0 && decoded.required()
                    && decoded.batch().equals(test.batch()), "Test metadata retained for " + id);
            require(((net.minecraft.nbt.CompoundTag) encoded).getString("function").orElseThrow().equals(id.toString()),
                    "Registered function identity retained for " + id);
        }
        var oldBrokenInstance = new IncompatibleTestInstance(new net.minecraft.gametest.framework.TestData<>(
                environments.listElements().findFirst().orElseThrow(),
                net.minecraft.resources.ResourceLocation.withDefaultNamespace("empty"),
                20, 0, true, net.minecraft.world.level.block.Rotation.NONE));
        org.junit.jupiter.api.Assertions.assertThrows(ClassCastException.class,
                () -> GameTestInstance.DIRECT_CODEC.encodeStart(ops, oldBrokenInstance).getOrThrow());
        checks++;
        System.out.println("GameTest serialization: " + checks + " checks passed.");
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }
    private static final class IncompatibleTestInstance extends GameTestInstance {
        private IncompatibleTestInstance(net.minecraft.gametest.framework.TestData<
                net.minecraft.core.Holder<TestEnvironmentDefinition>> data) {
            super(data);
        }

        @Override
        public void run(net.minecraft.gametest.framework.GameTestHelper helper) {
            throw new AssertionError("The serialization fixture must never execute as gameplay");
        }

        @Override
        public com.mojang.serialization.MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected net.minecraft.network.chat.MutableComponent typeDescription() {
            return net.minecraft.network.chat.Component.literal("incompatible codec regression fixture");
        }
    }

}
