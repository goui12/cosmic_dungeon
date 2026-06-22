package net.goui.cosmicdungeon.economy;

public record CurrencyAmount(long totalTrace) {
    public CurrencyAmount {
        if (totalTrace < 0L) totalTrace = 0L;
    }

    public static CurrencyAmount ofTrace(long totalTrace) {
        return new CurrencyAmount(totalTrace);
    }

    public static CurrencyAmount of(long amount, CurrencyDenomination denomination) {
        if (denomination == null) return new CurrencyAmount(0L);
        return new CurrencyAmount(denomination.toTrace(amount));
    }

    public CurrencyAmount addTrace(long delta) {
        if (delta <= 0L) return this;
        return new CurrencyAmount(Math.addExact(totalTrace, delta));
    }

    public CurrencyAmount subtractTrace(long delta) {
        if (delta <= 0L) return this;
        if (delta >= totalTrace) return new CurrencyAmount(0L);
        return new CurrencyAmount(totalTrace - delta);
    }

    public String formatNormalized() {
        long remaining = totalTrace;

        long anchors = remaining / CurrencyDenomination.ANCHOR.traceValue();
        remaining %= CurrencyDenomination.ANCHOR.traceValue();

        long crowns = remaining / CurrencyDenomination.CROWN.traceValue();
        remaining %= CurrencyDenomination.CROWN.traceValue();

        long seals = remaining / CurrencyDenomination.SEAL.traceValue();
        remaining %= CurrencyDenomination.SEAL.traceValue();

        long marks = remaining / CurrencyDenomination.MARK.traceValue();
        remaining %= CurrencyDenomination.MARK.traceValue();

        long traces = remaining;

        StringBuilder formatted = new StringBuilder();
        appendDenomination(formatted, anchors, "A");
        appendDenomination(formatted, crowns, "C");
        appendDenomination(formatted, seals, "S");
        appendDenomination(formatted, marks, "M");
        appendDenomination(formatted, traces, "T");
        return formatted.isEmpty() ? "0T" : formatted.toString();
    }

    private static void appendDenomination(StringBuilder formatted, long amount, String suffix) {
        if (amount <= 0L) return;
        if (!formatted.isEmpty()) formatted.append(' ');
        formatted.append(amount).append(suffix);
    }
}
