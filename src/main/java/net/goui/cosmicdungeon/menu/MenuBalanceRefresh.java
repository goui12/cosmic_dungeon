package net.goui.cosmicdungeon.menu;

/** One bounded scalar poll per open menu; sends only when the displayed account values change. */
public final class MenuBalanceRefresh {
    private long nextPoll = Long.MIN_VALUE;
    private long first, second;
    private boolean initialized;
    public boolean due(long now, int interval) {
        if (now < nextPoll) return false;
        nextPoll = now + Math.max(1, interval);
        return true;
    }
    public boolean changed(long first, long second) {
        if (initialized && this.first == first && this.second == second) return false;
        initialized = true; this.first = first; this.second = second; return true;
    }
}
