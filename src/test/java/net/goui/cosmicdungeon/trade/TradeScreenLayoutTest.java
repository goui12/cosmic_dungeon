package net.goui.cosmicdungeon.trade;

import net.goui.cosmicdungeon.economy.BalanceDisplayView;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static net.goui.cosmicdungeon.trade.TradeScreenLayout.*;

final class TradeScreenLayoutTest {
    @Test void allRowsFitMinimumVanillaGuiWithoutSlotOrCurrencyOverlap() {
        assertTrue(WIDTH <= 320 && HEIGHT <= 240);
        assertTrue(OTHER_ITEMS_Y + 17 < OTHER_CURRENCY_Y);
        assertTrue(OTHER_CURRENCY_Y + ICON_SIZE + 8 < OWN_ITEMS_Y - 1);
        assertTrue(OWN_ITEMS_Y + 17 < OWN_CURRENCY_Y);
        assertTrue(OWN_CURRENCY_Y + ICON_SIZE + 12 < INVENTORY_Y - 1);
        assertTrue(INVENTORY_Y + 2 * SLOT_STEP + 17 < HOTBAR_Y - 1);
        assertTrue(HOTBAR_Y + 17 < HEIGHT);
        assertTrue(SLOT_X + 8 * SLOT_STEP + 17 < 181); // player preview
    }

    @Test void fullIntegerAmountsFitBesideIconsIncludingMaximumBalance() {
        for (long amount : new long[]{0, 9, 10, 9999, 123456789, Long.MAX_VALUE}) {
            var cells = currencyCells(amount, text -> text.length() * 6);
            assertEquals(5, cells.size());
            int end = CURRENCY_X;
            long reconstructed = 0;
            long[] units = {10000, 1000, 100, 10, 1};
            for (var cell : cells) {
                assertEquals(end, cell.x());
                assertTrue(cell.width() >= ICON_SIZE + 6);
                assertTrue(cell.contains(cell.x() + 18));
                assertFalse(cell.contains(cell.x() + cell.width()));
                reconstructed = Math.addExact(reconstructed, Math.multiplyExact(cell.count(), units[cell.index()]));
                end = cell.x() + cell.width();
            }
            assertEquals(amount, reconstructed);
            assertTrue(end + 4 < DENY_X); // largest count cannot cover Cancel
        }
    }

    @Test void offerCountsUseInventoryDenominationsAndRemainIndependent() {
        var own = currencyCells(12345, text -> text.length() * 6);
        var other = currencyCells(67890, text -> text.length() * 6);
        assertArrayEquals(BalanceDisplayView.denominations(12345), own.stream().mapToLong(CurrencyCell::count).toArray());
        assertArrayEquals(new long[]{6, 7, 8, 9, 0}, other.stream().mapToLong(CurrencyCell::count).toArray());
        assertNotEquals(own, other);
    }
}
