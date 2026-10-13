package net.goui.cosmicdungeon.client.screen;

/** Bounded input wait only; unchanged/throttled replies never alter server-confirmed readiness. */
final class ReadyPendingState {
    static final int TIMEOUT_TICKS = 60;
    private int remaining;
    void begin() { remaining = TIMEOUT_TICKS; }
    void acknowledge() { remaining = 0; }
    void tick() { if (remaining > 0) remaining--; }
    boolean pending() { return remaining > 0; }
}
