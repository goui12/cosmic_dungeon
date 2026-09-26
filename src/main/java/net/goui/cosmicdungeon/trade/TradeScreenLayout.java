package net.goui.cosmicdungeon.trade;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import net.goui.cosmicdungeon.economy.BalanceDisplayView;

/** Shared presentation coordinates only; slot identities and transaction rules are unchanged. */
public final class TradeScreenLayout {
    private TradeScreenLayout() {}
    public static final int WIDTH = 300, HEIGHT = 238;
    public static final int SLOT_X = 9, SLOT_STEP = 18;
    public static final int OTHER_ITEMS_Y = 49, OWN_ITEMS_Y = 103;
    public static final int OTHER_CURRENCY_Y = 69, OWN_CURRENCY_Y = 123;
    public static final int INVENTORY_Y = 158, HOTBAR_Y = 216;
    public static final int CURRENCY_X = 9, ICON_SIZE = 16;
    public static final int ACCEPT_X = 274, OTHER_ACCEPT_Y = 49, OWN_ACCEPT_Y = 103;
    public static final int DENY_X = 274, DENY_Y = 123;

    public record CurrencyCell(int index, int x, int width, long count) {
        public boolean contains(int mouseX) { return mouseX >= x && mouseX < x + width; }
    }

    /** Inventory's icon + 18px text offset + 4px trailing gap, with full integer counts. */
    public static List<CurrencyCell> currencyCells(long trace, ToIntFunction<String> textWidth) {
        long[] counts = BalanceDisplayView.denominations(trace);
        List<CurrencyCell> cells = new ArrayList<>(counts.length);
        int x = CURRENCY_X;
        for (int i = 0; i < counts.length; i++) {
            int width = 22 + textWidth.applyAsInt(Long.toString(counts[i]));
            cells.add(new CurrencyCell(i, x, width, counts[i]));
            x += width;
        }
        return List.copyOf(cells);
    }
}
