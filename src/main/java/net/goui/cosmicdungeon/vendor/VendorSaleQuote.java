package net.goui.cosmicdungeon.vendor;

import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.function.BiPredicate;

/** One menu-local, single-use quote. Nothing is removed or reserved while it is displayed. */
public final class VendorSaleQuote<T> {
    public record Line<T>(int slot, T stack, long trace,
                          net.goui.cosmicdungeon.economy.pricing.VendorPriceBreakdown breakdown,
                          UnaryOperator<T> copy, BiPredicate<T,T> equal) {
        public Line {
            if (trace != breakdown.total()) throw new IllegalArgumentException("Quote breakdown mismatch");
            stack = copy.apply(stack);
        }
        public Line(int slot, T stack, long trace, UnaryOperator<T> copy, BiPredicate<T,T> equal) {
            this(slot, stack, trace, net.goui.cosmicdungeon.economy.pricing.VendorPriceBreakdown.baseOnly(trace), copy, equal);
        }
        @Override public T stack() { return copy.apply(stack); }
        public boolean matches(T current, net.goui.cosmicdungeon.economy.pricing.VendorPrice price) {
            return price.approved() && breakdown.equals(price.breakdown()) && matches(current, price.traceValue());
        }
        public boolean matches(T current, long currentTrace) {
            return currentTrace == trace && equal.test(stack, current);
        }
    }
    private final String token = UUID.randomUUID().toString();
    private final int vendorEntityId;
    private final long createdAt;
    private final int lifetime;
    private final List<Line<T>> lines;
    private final long total;
    private boolean consumed;

    public VendorSaleQuote(int vendorEntityId, long createdAt, int lifetime, List<Line<T>> lines) {
        this.vendorEntityId = vendorEntityId;
        this.createdAt = createdAt;
        this.lifetime = lifetime;
        this.lines = List.copyOf(lines);
        if (lines.isEmpty() || lines.size() > 41) throw new IllegalArgumentException("Invalid sale size");
        var slots = new java.util.HashSet<Integer>();
        long sum = 0;
        for (Line<T> line : lines) {
            if (line.slot() < 0 || line.slot() >= 41 || !slots.add(line.slot()) || line.trace() < 0)
                throw new IllegalArgumentException("Invalid sale line");
            sum = Math.addExact(sum, line.trace());
        }
        total = sum;
    }
    public String token() { return token; }
    public int vendorEntityId() { return vendorEntityId; }
    public List<Line<T>> lines() { return lines; }
    public long total() { return total; }
    public boolean consume(String suppliedToken, long now) {
        if (consumed || !token.equals(suppliedToken)) return false;
        consumed = true;
        return now >= createdAt && now - createdAt < lifetime;
    }
}
