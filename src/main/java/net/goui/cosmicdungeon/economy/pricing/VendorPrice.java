package net.goui.cosmicdungeon.economy.pricing;

/** A rejected/unpriced item is different from an approved zero-Trace offer. */
public record VendorPrice(long traceValue, String debugSource, boolean approved, VendorPriceBreakdown breakdown) {
    public VendorPrice {
        if (breakdown.total() != traceValue) throw new IllegalArgumentException("Price total mismatch");
    }
    public VendorPrice(long traceValue, String debugSource, boolean approved) {
        this(traceValue, debugSource, approved, VendorPriceBreakdown.baseOnly(traceValue));
    }
    public VendorPrice(long traceValue, String debugSource) { this(traceValue, debugSource, false); }
}
