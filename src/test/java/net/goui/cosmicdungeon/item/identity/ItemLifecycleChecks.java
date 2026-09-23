package net.goui.cosmicdungeon.item.identity;

import java.util.UUID;

public final class ItemLifecycleChecks {
    private static int checks;
    private static void check(boolean ok, String label) { checks++; if (!ok) throw new AssertionError(label); }
    public static void main(String[] args) {
        check(ItemLifecyclePolicy.retain(true, false), "Protected gear survives death");
        check(!ItemLifecyclePolicy.retain(false, false), "Ordinary gear still drops");
        check(!ItemLifecyclePolicy.retain(true, true), "Developer testing bypass remains");
        for (int mask = 0; mask < 16; mask++) {
            boolean keepAll = (mask & 1) != 0, keepInventory = (mask & 2) != 0;
            boolean spectator = (mask & 4) != 0, developer = (mask & 8) != 0;
            check(ItemLifecyclePolicy.copyRetained(keepAll, keepInventory, spectator, developer) == (mask == 0),
                    "Only an otherwise empty death clone copies retained gear: " + mask);
        }
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID other = UUID.fromString("00000000-0000-0000-0000-000000000002");
        check(ItemLifecyclePolicy.mayCollect(null, owner), "Unowned encounter loot remains available");
        check(ItemLifecyclePolicy.mayCollect(owner, owner), "Owner can recover droppable equipment");
        check(!ItemLifecyclePolicy.mayCollect(owner, other), "World drop cannot bypass gear trade");
        check(ItemLifecyclePolicy.mayCollect(UUID.fromString(owner.toString()), owner), "Reloaded owner value matches");
        check(!ItemLifecyclePolicy.mayCollect(owner, null), "Missing collector fails closed");
        check(ItemLifecyclePolicy.mayAutomate(false, null), "Ordinary unowned trap supplies still move");
        check(!ItemLifecyclePolicy.mayAutomate(true, null), "Bound container contents cannot be automated");
        check(!ItemLifecyclePolicy.mayAutomate(false, owner), "Owner-only gear cannot be laundered by hopper/mob");
        check(!ItemLifecyclePolicy.mayAutomate(true, owner), "Both restrictions remain effective");
        System.out.println(checks + " item lifecycle checks passed");
    }
}
