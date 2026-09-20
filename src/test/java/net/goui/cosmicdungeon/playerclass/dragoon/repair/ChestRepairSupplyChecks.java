package net.goui.cosmicdungeon.playerclass.dragoon.repair;

import java.util.List;

public final class ChestRepairSupplyChecks {
    private static int checks;
    private static void check(boolean ok, String label) {
        checks++;
        if (!ok) throw new AssertionError(label);
    }
    public static void main(String[] args) {
        check(ChestRepairSupplyRules.countFits(64,64),"Full ordinary stack is preserved");
        check(ChestRepairSupplyRules.countFits(99,99),"Explicit larger stack capacity is preserved");
        check(!ChestRepairSupplyRules.countFits(65,64),"Never clamp an oversized authored stack");
        check(!ChestRepairSupplyRules.countFits(0,64),"Empty count is not classified");
        check(!ChestRepairSupplyRules.countFits(-1,64),"Malformed negative count is held");
        for (String material : List.of("oak_planks", "spruce_planks", "cobblestone", "leather",
                "gold_ingot", "copper_ingot", "iron_ingot", "diamond", "netherite_ingot")) {
            check(material.equals(ChestRepairSupplyRules.key("minecraft:" + material, false, false,
                    false, false, false)), "Reviewed plain supply: " + material);
            for (int flags = 1; flags < 32; flags++)
                check(ChestRepairSupplyRules.key("minecraft:" + material, (flags & 1) != 0,
                        (flags & 2) != 0, (flags & 4) != 0, (flags & 8) != 0, (flags & 16) != 0) == null,
                        "Never overwrite marked/bound/provenance/enchanted/damaged " + material + " " + flags);
        }
        for (String item : List.of("minecraft:bow", "minecraft:crossbow", "minecraft:trident",
                "minecraft:mace", "minecraft:breeze_rod", "minecraft:diamond_sword",
                "minecraft:diamond_block", "minecraft:gold_nugget", "minecraft:iron_ore",
                "minecraft:birch_planks", "minecraft:netherite_scrap", "minecraft:stick",
                "cosmicdungeon:diamond", "other:iron_ingot", "diamond", "")) {
            check(ChestRepairSupplyRules.key(item, false, false, false, false, false) == null,
                    "Non-supply/ordinary weapon never becomes a kit: " + item);
        }
        check(ChestRepairSupplyRules.key(null, false, false, false, false, false) == null, "Missing identity held");
        System.out.println("D1 chest repair supply checks passed: " + checks);
    }
}
