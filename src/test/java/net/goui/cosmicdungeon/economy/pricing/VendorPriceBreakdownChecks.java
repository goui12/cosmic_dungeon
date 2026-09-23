package net.goui.cosmicdungeon.economy.pricing;

import net.goui.cosmicdungeon.vendor.VendorSaleQuote;

public final class VendorPriceBreakdownChecks {
    private static int checks;
    private static void check(boolean value, String label) { checks++; if (!value) throw new AssertionError(label); }
    private static void rejects(Runnable action, String label) {
        boolean rejected = false;
        try { action.run(); } catch (ArithmeticException | IllegalArgumentException expected) { rejected = true; }
        check(rejected, label);
    }
    public static void main(String[] args) {
        var plain = VendorPriceBreakdown.calculate(20, 0, 0, 0, null, 3);
        check(plain.equals(new VendorPriceBreakdown(60, 0, 0, 0, 60)), "Plain stack quantity");
        var enchanted = VendorPriceBreakdown.calculate(20, 10, -5, 0, null, 2);
        check(enchanted.equals(new VendorPriceBreakdown(40, 20, -10, 0, 50)), "Enchantments and curses separated per stack");
        var floor = VendorPriceBreakdown.calculate(10, 2, -20, 0, null, 2);
        check(floor.total() == 0 && floor.adjustments() == 16, "Zero floor is explicit");
        var cap = VendorPriceBreakdown.calculate(20, 10, -5, 0, 7L, 3);
        check(cap.total() == 21 && cap.adjustments() == -54, "Conversion cap is explicit");
        var bucket = VendorPriceBreakdown.calculate(50, 0, 0, -47, null, 2);
        check(bucket.base() == 100 && bucket.adjustments() == -94 && bucket.total() == 6, "Milk container exploit cap preserved");
        var negativeCap = VendorPriceBreakdown.calculate(10, 0, 0, 0, -1L, 1);
        check(negativeCap.total() == 0 && negativeCap.adjustments() == -10, "Negative provenance cap cannot pay negative");
        check(VendorPriceBreakdown.calculate(0, 0, 0, 0, null, 1).total() == 0, "Approved zero still quotable");
        check(VendorPriceBreakdown.calculate(10, -2, 5, 0, null, 1).total() == 13, "Config override signs are preserved");
        var line = new VendorSaleQuote.Line<>(0, "stack", 50, enchanted, value -> value, String::equals);
        check(line.matches("stack", new VendorPrice(50, "catalog", true, enchanted)), "Unchanged breakdown commits");
        check(!line.matches("stack", new VendorPrice(50, "catalog", true, new VendorPriceBreakdown(50, 10, -10, 0, 50))),
                "Changed components invalidate even when final price is unchanged");
        check(!line.matches("stack", new VendorPrice(50, "restricted", false, enchanted)), "Revoked approval invalidates quote");
        rejects(() -> VendorPriceBreakdown.calculate(Long.MAX_VALUE, 1, 0, 0, null, 1), "Per-item sum overflow rejected");
        rejects(() -> VendorPriceBreakdown.calculate(Long.MAX_VALUE, 0, 0, 0, null, 2), "Stack multiplication overflow rejected");
        rejects(() -> VendorPriceBreakdown.calculate(1, 0, 0, 0, null, 0), "Empty stack rejected");
        rejects(() -> new VendorPriceBreakdown(10, 2, 0, 0, 11), "Inconsistent breakdown rejected");
        rejects(() -> new VendorPrice(51, "catalog", true, enchanted), "Quote total mismatch rejected");
        // Reconcile the existing bucket-before-enchant, zero-floor-then-cap ordering over a range of fixtures.
        for (long base : new long[]{0, 3, 50, 1000}) for (long ench : new long[]{0, 12})
            for (long curse : new long[]{0, -15, -100}) for (long delta : new long[]{0, -3})
                for (Long limit : new Long[]{null, 0L, 7L, 5000L}) {
                    long expected = Math.max(0, base + delta + ench + curse);
                    if (limit != null) expected = Math.min(expected, limit);
                    var result = VendorPriceBreakdown.calculate(base, ench, curse, delta, limit, 4);
                    check(result.total() == expected * 4, "Existing pricing ordering preserved");
                }
        System.out.println(checks + " vendor breakdown checks passed");
    }
}
