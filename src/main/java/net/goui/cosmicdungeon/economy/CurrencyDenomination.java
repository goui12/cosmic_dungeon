package net.goui.cosmicdungeon.economy;

import java.util.Locale;

public enum CurrencyDenomination {
    TRACE("trace", 1L),
    MARK("mark", 10L),
    SEAL("seal", 100L),
    CROWN("crown", 1_000L),
    ANCHOR("anchor", 10_000L);

    private final String id;
    private final long traceValue;

    CurrencyDenomination(String id, long traceValue) {
        this.id = id;
        this.traceValue = traceValue;
    }

    public String id() { return id; }

    public long traceValue() { return traceValue; }

    public long toTrace(long amount) {
        if (amount <= 0L) return 0L;
        return Math.multiplyExact(amount, traceValue);
    }

    public String displayName(long amount) {
        String suffix = amount == 1L ? "" : "s";
        return amount + " " + name().toLowerCase(Locale.ROOT) + suffix;
    }

    public static CurrencyDenomination fromId(String raw) {
        if (raw == null) return null;
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (CurrencyDenomination value : values()) {
            if (value.id.equals(normalized) || value.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return value;
            }
        }
        return null;
    }
}
