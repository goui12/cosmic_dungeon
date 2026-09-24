package net.goui.cosmicdungeon.client.economy;

import java.util.Objects;
import java.util.function.LongSupplier;

/** Client presentation timer; accepted account snapshots remain the source of truth. */
public final class CurrencyHudVisibility {
    private static final long DISPLAY_NANOS = 5_000_000_000L;

    private final LongSupplier clock;
    private boolean initialized;
    private boolean received;
    private long balance;
    private long receivedAt;

    public CurrencyHudVisibility(LongSupplier clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    public void update(long confirmedBalance) {
        // The initial snapshot is a baseline, not a new receipt of the entire wallet.
        if (initialized && confirmedBalance > balance) {
            receivedAt = clock.getAsLong();
            received = true;
        }
        balance = confirmedBalance;
        initialized = true;
    }

    public boolean visible() {
        if (!received) {
            return false;
        }
        // Elapsed monotonic time handles nanoTime wraparound and does not pause in menus.
        long elapsed = clock.getAsLong() - receivedAt;
        return elapsed >= 0 && elapsed < DISPLAY_NANOS;
    }

    public void clear() {
        initialized = false;
        received = false;
        balance = 0;
        receivedAt = 0;
    }
}
