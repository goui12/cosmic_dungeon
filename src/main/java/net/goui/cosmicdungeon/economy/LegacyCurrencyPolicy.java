package net.goui.cosmicdungeon.economy;

/** Economy Internal (2026-08-18), 17ufIuIy0VhLmB_V-6sZ7sCaUCZuGZUkHrgJLVpEcS28:
 * denominations are display only; only the managed logical death drop is collectible money.
 * Old registered physical items remain evidence, not automatic account credit.
 */
public final class LegacyCurrencyPolicy {
    public enum Kind { ORDINARY, MANAGED_DEATH, LEGACY_REVIEW }
    private LegacyCurrencyPolicy(){}
    public static CurrencyDenomination denomination(String item) {
        if(item==null)return null;
        return switch(item) {
            case "cosmicdungeon:attunement_trace" -> CurrencyDenomination.TRACE;
            case "cosmicdungeon:attunement_mark" -> CurrencyDenomination.MARK;
            case "cosmicdungeon:attunement_seal" -> CurrencyDenomination.SEAL;
            case "cosmicdungeon:attunement_crown" -> CurrencyDenomination.CROWN;
            case "cosmicdungeon:attunement_anchor" -> CurrencyDenomination.ANCHOR;
            default -> null;
        };
    }
    public static Kind classify(String item,boolean deathMarked) {
        return deathMarked ? Kind.MANAGED_DEATH : denomination(item)!=null ? Kind.LEGACY_REVIEW : Kind.ORDINARY;
    }
    public static long nominalTrace(String item,long count) {
        var denomination=denomination(item);
        if(denomination==null || count<=0)throw new IllegalArgumentException("Not a positive legacy denomination stack");
        return Math.multiplyExact(count,denomination.traceValue());
    }
    // TODO(M03/M20, legacy recovery review): physical count is nominal evidence, never proof
    // of unpaid entitlement. Before any conversion inspect a consistent complete backup,
    // exact entity UUID/count/components/owner/target, prior merges/splits, held/Ender/nested
    // and unloaded storage, account ledger and prior pickup receipts. No deposit/discard
    // pair, name-based ownership, guessed payout or partial-backup reconstruction is safe.
    // Existing Chop inspect/preview covers only its reviewed single-owner case; duplicate,
    // foreign, overstacked and orphan escrow evidence must remain held for explicit review.
}
