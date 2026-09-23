package net.goui.cosmicdungeon.playerclass.dragoon.repair;

import java.util.Set;

/** Service-only raw-material compatibility; vendor marker identity is intentionally separate. */
public final class RepairMaterialRules {
    private RepairMaterialRules() {}
    private static final Set<String> RAW = Set.of("oak_planks", "spruce_planks", "cobblestone",
            "leather", "gold_ingot", "copper_ingot", "iron_ingot", "diamond", "netherite_ingot");

    public static String serviceKey(String itemId, boolean markerPresent, String validatedMarker,
                                    boolean pristine, boolean protectedIdentity) {
        if (!pristine || protectedIdentity) return null;
        // An invalid marker must never become eligible through the unmarked-material fallback.
        if (markerPresent) return validatedMarker;
        if (itemId == null || !itemId.startsWith("minecraft:")) return null;
        String path = itemId.substring("minecraft:".length());
        return RAW.contains(path) ? path : null;
    }
}
