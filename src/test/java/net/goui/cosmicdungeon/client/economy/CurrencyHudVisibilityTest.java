package net.goui.cosmicdungeon.client.economy;

import java.util.function.LongSupplier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class CurrencyHudVisibilityTest {
    @Test
    public void existingOwnerRevisionAndBalanceSnapshotChecksRemainValid() {
        net.goui.cosmicdungeon.economy.BalanceDisplayChecks.main(new String[0]);
    }

    @Test
    public void firstSnapshotAndFiveSecondBoundary() {
        var clock = new ManualClock();
        var hud = new CurrencyHudVisibility(clock);
        assertFalse(hud.visible());
        hud.update(100);
        assertFalse(hud.visible(), "Login wallet is not a receipt");
        hud.update(101);
        assertTrue(hud.visible());
        clock.advance(4_999_999_999L);
        assertTrue(hud.visible());
        clock.advance(1);
        assertFalse(hud.visible(), "Hide exactly five seconds after the confirmed gain");
    }

    @Test
    public void successiveGainsRefreshButUnchangedOrLowerBalancesDoNot() {
        var clock = new ManualClock();
        var hud = new CurrencyHudVisibility(clock);
        hud.update(0);
        hud.update(10);
        clock.advance(3_000_000_000L);
        hud.update(11);
        clock.advance(3_000_000_000L);
        assertTrue(hud.visible(), "A second gain starts a fresh five seconds");
        hud.update(11);
        hud.update(5);
        clock.advance(2_000_000_000L);
        assertFalse(hud.visible(), "Spending and reservation-only snapshots cannot extend the timer");
        hud.update(5);
        hud.update(4);
        assertFalse(hud.visible(), "Non-gains cannot reopen the HUD");
        hud.update(6);
        assertTrue(hud.visible(), "A gain after spending can reopen it");
    }

    @Test
    public void disconnectAndReconnectClearTheReceiptAndBaseline() {
        var clock = new ManualClock();
        var hud = new CurrencyHudVisibility(clock);
        hud.update(0);
        hud.update(10);
        assertTrue(hud.visible());
        hud.clear();
        assertFalse(hud.visible());
        hud.update(1000);
        assertFalse(hud.visible(), "A different session must seed its own balance baseline");
        hud.update(1001);
        assertTrue(hud.visible());
    }

    @Test
    public void balanceAndMonotonicClockBoundariesRemainSafe() {
        var clock = new ManualClock();
        clock.advance(Long.MAX_VALUE - 1_000_000_000L);
        var hud = new CurrencyHudVisibility(clock);
        hud.update(Long.MAX_VALUE - 1);
        hud.update(Long.MAX_VALUE);
        clock.advance(4_999_999_999L);
        assertTrue(hud.visible(), "nanoTime may wrap while the receipt is visible");
        clock.advance(1);
        assertFalse(hud.visible());
        hud.update(Long.MAX_VALUE);
        assertFalse(hud.visible(), "Repeated maximum balance is not a new gain");
    }

    private static final class ManualClock implements LongSupplier {
        private long now;

        private void advance(long nanos) {
            now += nanos;
        }

        @Override
        public long getAsLong() {
            return now;
        }
    }
}
