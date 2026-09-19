package net.goui.cosmicdungeon.economy.pricing;

/** Exact whole-stack amounts supplied by the server, including zero floors and conversion caps. */
public record VendorPriceBreakdown(long base, long enchantments, long curses, long adjustments, long total) {
    public VendorPriceBreakdown {
        if (total < 0 || Math.addExact(Math.addExact(base, enchantments), Math.addExact(curses, adjustments)) != total)
            throw new IllegalArgumentException("Price components must equal the nonnegative total");
    }
    public static VendorPriceBreakdown baseOnly(long total) {
        return new VendorPriceBreakdown(total, 0, 0, 0, total);
    }
    public static VendorPriceBreakdown calculate(long base, long enchantments, long curses,
                                                long baseAdjustment, Long conversionCap, int count) {
        if (count < 1 || base < 0) throw new IllegalArgumentException("Invalid stack or base");
        long ordinary = Math.addExact(Math.addExact(base, enchantments), curses);
        long value = Math.max(0, Math.addExact(ordinary, baseAdjustment));
        if (conversionCap != null) value = Math.min(value, Math.max(0, conversionCap));
        long adjustment = Math.subtractExact(value, ordinary);
        return new VendorPriceBreakdown(Math.multiplyExact(base, count), Math.multiplyExact(enchantments, count),
                Math.multiplyExact(curses, count), Math.multiplyExact(adjustment, count), Math.multiplyExact(value, count));
    }
}
