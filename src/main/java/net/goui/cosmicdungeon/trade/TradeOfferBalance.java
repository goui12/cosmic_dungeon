package net.goui.cosmicdungeon.trade;

/** Offers are previews until confirmation. Account holds are already excluded by the server. */
public final class TradeOfferBalance {
    private TradeOfferBalance() {}
    public static long remaining(long availableBeforeOffer, long offered) {
        return Math.max(0L, Math.max(0L, availableBeforeOffer) - Math.max(0L, offered));
    }
}
