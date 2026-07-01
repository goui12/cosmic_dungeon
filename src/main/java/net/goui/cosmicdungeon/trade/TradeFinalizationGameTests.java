package net.goui.cosmicdungeon.trade;

import com.mojang.serialization.MapCodec;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class TradeFinalizationGameTests {
    private static final ResourceLocation ENVIRONMENT = id("trade_finalization");
    private static final ResourceLocation EMPTY_STRUCTURE = ResourceLocation.withDefaultNamespace("empty");

    private TradeFinalizationGameTests() {}

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(ENVIRONMENT, new TestEnvironmentDefinition.AllOf());
        register(event, environment, "item_only", TradeFinalizationGameTests::itemOnlyTradeCompletes);
        register(event, environment, "currency_only", TradeFinalizationGameTests::currencyOnlyTradeCompletes);
        register(event, environment, "mixed", TradeFinalizationGameTests::mixedTradeCompletes);
        register(event, environment, "full_inventory", TradeFinalizationGameTests::fullInventoryTradeFailsBeforeMutation);
        register(event, environment, "capacity_limit", TradeFinalizationGameTests::currencyCapacityTradeFailsBeforeMutation);
    }

    private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition> environment, String name, Consumer<GameTestHelper> test) {
        event.registerTest(id(name), new DirectGameTestInstance(test, data(environment)));
    }

    private static TestData<Holder<TestEnvironmentDefinition>> data(Holder<TestEnvironmentDefinition> environment) {
        return new TestData<>(environment, EMPTY_STRUCTURE, 20, 0, true, Rotation.NONE);
    }

    private static void itemOnlyTradeCompletes(GameTestHelper helper) {
        FakeParticipant a = participant(0, 1_000, 5);
        FakeParticipant b = participant(0, 1_000, 5);

        assertResult(helper, TradeFinalizationService.Result.SUCCESS, finalize(a, b, items(1), items(1)));
        assertState(helper, a, 1_000, 1);
        assertState(helper, b, 1_000, 1);
        helper.succeed();
    }

    private static void currencyOnlyTradeCompletes(GameTestHelper helper) {
        FakeParticipant a = participant(20, 100, 5);
        FakeParticipant b = participant(5, 50, 5);

        assertResult(helper, TradeFinalizationService.Result.SUCCESS, finalize(a, b, items(0), items(0)));
        assertState(helper, a, 85, 0);
        assertState(helper, b, 65, 0);
        helper.succeed();
    }

    private static void mixedTradeCompletes(GameTestHelper helper) {
        FakeParticipant a = participant(40, 100, 5);
        FakeParticipant b = participant(10, 50, 5);

        assertResult(helper, TradeFinalizationService.Result.SUCCESS, finalize(a, b, items(2), items(1)));
        assertState(helper, a, 70, 1);
        assertState(helper, b, 80, 2);
        helper.succeed();
    }

    private static void fullInventoryTradeFailsBeforeMutation(GameTestHelper helper) {
        FakeParticipant a = participant(0, 100, 0);
        FakeParticipant b = participant(0, 100, 0);

        assertResult(helper, TradeFinalizationService.Result.NOT_ENOUGH_INVENTORY_SPACE, finalize(a, b, items(1), items(1)));
        assertState(helper, a, 100, 0);
        assertState(helper, b, 100, 0);
        helper.succeed();
    }

    private static void currencyCapacityTradeFailsBeforeMutation(GameTestHelper helper) {
        FakeParticipant a = participant(0, 95, 5, 100);
        FakeParticipant b = participant(10, 50, 5, 1_000);

        assertResult(helper, TradeFinalizationService.Result.CANNOT_RECEIVE_CURRENCY, finalize(a, b, items(0), items(0)));
        assertState(helper, a, 95, 0);
        assertState(helper, b, 50, 0);
        helper.succeed();
    }

    private static TradeFinalizationService.Result finalize(FakeParticipant a, FakeParticipant b, FakeItems aOffer, FakeItems bOffer) {
        return TradeFinalizationService.finalizeTrade(a, b, aOffer, bOffer);
    }

    private static FakeParticipant participant(long offeredCurrency, long balance, int itemCapacity) {
        return participant(offeredCurrency, balance, itemCapacity, 1_000);
    }

    private static FakeParticipant participant(long offeredCurrency, long balance, int itemCapacity, long currencyCapacity) {
        return new FakeParticipant(offeredCurrency, balance, currencyCapacity, itemCapacity);
    }

    private static FakeItems items(int count) {
        return new FakeItems(count);
    }

    private static void assertResult(GameTestHelper helper, TradeFinalizationService.Result expected, TradeFinalizationService.Result actual) {
        if (actual != expected) {
            helper.fail("Expected " + expected + " but got " + actual);
        }
    }

    private static void assertState(GameTestHelper helper, FakeParticipant participant, long balance, int receivedItems) {
        if (participant.balanceTrace() != balance) {
            helper.fail("Expected balance " + balance + " but got " + participant.balanceTrace());
        }
        if (participant.receivedItems != receivedItems) {
            helper.fail("Expected received items " + receivedItems + " but got " + participant.receivedItems);
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CosmicDungeonMod.MOD_ID, "trade_finalization/" + path);
    }

    private static final class DirectGameTestInstance extends GameTestInstance {
        private final Consumer<GameTestHelper> test;

        private DirectGameTestInstance(Consumer<GameTestHelper> test, TestData<Holder<TestEnvironmentDefinition>> data) {
            super(data);
            this.test = test;
        }

        @Override
        public void run(GameTestHelper helper) {
            test.accept(helper);
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public MapCodec<? extends GameTestInstance> codec() {
            return (MapCodec) FunctionGameTestInstance.CODEC;
        }

        @Override
        protected net.minecraft.network.chat.MutableComponent typeDescription() {
            return net.minecraft.network.chat.Component.literal("direct cosmic dungeon trade finalization test");
        }
    }

    private static final class FakeParticipant implements TradeFinalizationService.TradeParticipant {
        private final long offeredCurrency;
        private final long capacity;
        private final int itemCapacity;
        private long balance;
        private int receivedItems;

        private FakeParticipant(long offeredCurrency, long balance, long capacity, int itemCapacity) {
            this.offeredCurrency = offeredCurrency;
            this.balance = balance;
            this.capacity = capacity;
            this.itemCapacity = itemCapacity;
        }

        @Override
        public long offeredCurrencyTrace() {
            return offeredCurrency;
        }

        @Override
        public long balanceTrace() {
            return balance;
        }

        @Override
        public long capacityTrace() {
            return capacity;
        }

        @Override
        public boolean tryWithdraw(long traceAmount) {
            if (traceAmount <= 0L || balance < traceAmount) return false;
            balance -= traceAmount;
            return true;
        }

        @Override
        public boolean tryDeposit(long traceAmount) {
            if (traceAmount <= 0L || traceAmount > capacity - balance) return false;
            balance += traceAmount;
            return true;
        }

        @Override
        public void setBalanceTrace(long traceAmount) {
            balance = traceAmount;
        }

        @Override
        public boolean canReceiveItems(TradeFinalizationService.OfferedItems items) {
            return items.canMoveInto(this);
        }

        @Override
        public void receiveItems(TradeFinalizationService.OfferedItems items) {
            items.moveInto(this);
        }
    }

    private static final class FakeItems implements TradeFinalizationService.OfferedItems {
        private int count;

        private FakeItems(int count) {
            this.count = count;
        }

        @Override
        public boolean canMoveInto(TradeFinalizationService.TradeParticipant receiver) {
            if (receiver instanceof FakeParticipant participant) {
                return participant.receivedItems + count <= participant.itemCapacity;
            }
            return count == 0;
        }

        @Override
        public void moveInto(TradeFinalizationService.TradeParticipant receiver) {
            if (receiver instanceof FakeParticipant participant) {
                participant.receivedItems += count;
                count = 0;
            }
        }
    }
}
