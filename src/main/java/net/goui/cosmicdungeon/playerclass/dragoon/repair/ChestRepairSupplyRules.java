package net.goui.cosmicdungeon.playerclass.dragoon.repair;

import java.util.Set;

/** Source-authorized raw materials only. Weapons never become kits merely by being in a chest. */
public final class ChestRepairSupplyRules {
    private static final Set<String> MATERIALS = Set.of("oak_planks", "spruce_planks", "cobblestone",
            "leather", "gold_ingot", "copper_ingot", "iron_ingot", "diamond", "netherite_ingot");
    private ChestRepairSupplyRules() {}

    public static boolean countFits(int count, int slotMaximum) {
        return count > 0 && count <= slotMaximum;
    }

    public static String key(String itemId, boolean marked, boolean restricted, boolean provenance,
                             boolean enchanted, boolean damaged) {
        if (marked || restricted || provenance || enchanted || damaged || itemId == null
                || !itemId.startsWith("minecraft:")) return null;
        String key = itemId.substring("minecraft:".length());
        return MATERIALS.contains(key) ? key : null;
    }
}
