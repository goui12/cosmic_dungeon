package net.goui.cosmicdungeon.economy.pricing;

/** Pure decision layer. Item facts come from server components, never player-facing names. */
public final class ItemTransferPolicy {
    public enum Action { TRADE, VENDOR_SALE }
    public enum Rejection { NONE, EMPTY, PROTECTED, NO_TRADE, NO_SALE, INVALID_PROVENANCE,
        UNCLASSIFIED_EQUIPMENT, INVALID_REPAIR_COMPONENT }
    public record Facts(boolean empty, boolean protectedItem, boolean noTrade, boolean noSale,
                        boolean provenancePresent, boolean provenanceValid, boolean needsProvenance,
                        boolean repairMarked, boolean repairValid) {}
    private ItemTransferPolicy() {}
    public static Rejection evaluate(Action action, Facts facts) {
        if (facts.empty()) return Rejection.EMPTY;
        if (facts.protectedItem()) return Rejection.PROTECTED;
        if (action == Action.TRADE && facts.noTrade()) return Rejection.NO_TRADE;
        if (action == Action.VENDOR_SALE && facts.noSale()) return Rejection.NO_SALE;
        if (facts.provenancePresent() && !facts.provenanceValid()) return Rejection.INVALID_PROVENANCE;
        if (facts.repairMarked() && (!facts.repairValid() || facts.provenancePresent()))
            return Rejection.INVALID_REPAIR_COMPONENT;
        if (facts.needsProvenance() && !facts.repairMarked() && !facts.provenanceValid())
            return Rejection.UNCLASSIFIED_EQUIPMENT;
        return Rejection.NONE;
    }
}
