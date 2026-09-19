package net.goui.cosmicdungeon.npc.tamsin;

import java.util.Set;

/** Frozen Tax allowlist: Doc 1dIuaeMFMZaaWo7AS1zQ51kXSbdPkb9tmUm864MBs2Q0 (2026-08-19).
 * Future catalogue additions must NOT silently become tax payments. No price/class/durability gate. */
public final class TamsinTaxRules {
    private TamsinTaxRules() {}
    public static final Set<String> ELIGIBLE = Set.of(
            "recovered_spyglass", "can_opener", "squared_mallet", "limb_lopper", "the_adjuster",
            "skeleton_key", "brutes_key", "severance_pay", "salvaged_leggings", "perforated_chestplate",
            "discarded_helm", "resoled_boots", "cold_comfort", "second_thought", "last_resort",
            "web_cautery", "fibril", "lash_of_the_crumbling_front", "dead_reckoning", "loophole",
            "triptych", "traitors_enfilade", "ranseur_of_the_fallen_dragoon");
    public static boolean qualifies(long campRun, long successRun, boolean receiptPresent) {
        return campRun > 0 && successRun >= campRun && !receiptPresent;
    }
    public static boolean confirmation(boolean session, boolean eligible, String expected, String supplied,
                                       long now, long expires, boolean exactStack) {
        return session && eligible && expected != null && !expected.isEmpty() && expected.equals(supplied)
                && now < expires && exactStack;
    }
}
