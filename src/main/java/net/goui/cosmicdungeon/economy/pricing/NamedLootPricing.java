package net.goui.cosmicdungeon.economy.pricing;

import net.goui.cosmicdungeon.item.identity.D1LootCatalog;

/** Newest named-item table supplies the final unit price, inclusive of authored enchantments. */
public final class NamedLootPricing {
    private NamedLootPricing() {}
    public static VendorPrice quote(String identity, String actualBase, long configuredUnit, Long cap, int count) {
        var entry = D1LootCatalog.find(identity);
        if (entry == null || !entry.baseItem().equals(actualBase)) return new VendorPrice(0, "invalid_named_identity");
        if (configuredUnit < 0 || count < 1) return new VendorPrice(0, "unpriced_named:" + identity);
        try {
            var breakdown = VendorPriceBreakdown.calculate(configuredUnit, 0, 0, 0, cap, count);
            return new VendorPrice(breakdown.total(), "named_d1:" + identity, true, breakdown);
        } catch (ArithmeticException overflow) { return new VendorPrice(0, "price_overflow"); }
    }
}
