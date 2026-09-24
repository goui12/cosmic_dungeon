package net.goui.cosmicdungeon.trade;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.gametest.FunctionGameTestSuite;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;

public final class TradeFinalizationGameTests {
    private static final ResourceLocation ENVIRONMENT = id("trade_finalization");

    private TradeFinalizationGameTests() {}

    private static final FunctionGameTestSuite SUITE = createSuite();

    public static void register(IEventBus eventBus) {
        SUITE.register(eventBus);
    }

    private static FunctionGameTestSuite createSuite() {
        var suite = new FunctionGameTestSuite(ENVIRONMENT);
        suite.add(id("item_only"), TradeFinalizationGameTests::itemOnlyTradeCompletes);
        suite.add(id("currency_only"), TradeFinalizationGameTests::currencyOnlyTradeCompletes);
        suite.add(id("mixed"), TradeFinalizationGameTests::mixedTradeCompletes);
        suite.add(id("full_inventory"), TradeFinalizationGameTests::fullInventoryTradeFailsBeforeMutation);
        suite.add(id("capacity_limit"), TradeFinalizationGameTests::currencyCapacityTradeFailsBeforeMutation);
        return suite;
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
        var result=TradeFinalizationService.validate(a.offeredCurrency,b.offeredCurrency,a.balance,b.balance,
                a.capacity-a.balance,b.capacity-b.balance,bOffer.count<=a.itemCapacity,aOffer.count<=b.itemCapacity);
        if(result!=TradeFinalizationService.Result.SUCCESS)return result;
        try{
            var ctor=net.goui.cosmicdungeon.economy.PlayerCurrencyData.class.getDeclaredConstructor();ctor.setAccessible(true);var data=ctor.newInstance();
            var first=new java.util.UUID(0,1);var second=new java.util.UUID(0,2);var id=java.util.UUID.randomUUID();
            data.setCapacityTrace(first,a.capacity);data.setCapacityTrace(second,b.capacity);
            data.setBalanceTrace(first,a.balance);data.setBalanceTrace(second,b.balance);
            var terms=new net.goui.cosmicdungeon.economy.AccountTransfer.Terms(first,second,a.offeredCurrency,b.offeredCurrency,"player_trade",0,"gametest");
            data.reserve(id,terms,1);data.commitTransfer(id,terms,2);
            a.balance=data.getBalanceTrace(first);b.balance=data.getBalanceTrace(second);
            a.receivedItems+=bOffer.count;b.receivedItems+=aOffer.count;aOffer.count=0;bOffer.count=0;return result;
        }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
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

    private static final class FakeParticipant {
        private final long offeredCurrency,capacity;private final int itemCapacity;
        private long balance;private int receivedItems;
        private FakeParticipant(long offeredCurrency,long balance,long capacity,int itemCapacity){
            this.offeredCurrency=offeredCurrency;this.balance=balance;this.capacity=capacity;this.itemCapacity=itemCapacity;
        }
        private long balanceTrace(){return balance;}
    }
    private static final class FakeItems {private int count;private FakeItems(int count){this.count=count;}}
}
