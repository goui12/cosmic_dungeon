package net.goui.cosmicdungeon.item.identity;

import java.util.UUID;

/** Canon decisions shared by death, pickup and automation adapters. */
public final class ItemLifecyclePolicy {
    private ItemLifecyclePolicy() {}
    public static boolean retain(boolean noDrop, boolean developer) { return noDrop && !developer; }
    public static boolean copyRetained(boolean keepEverything, boolean keepInventory, boolean spectator, boolean developer) {
        return !keepEverything && !keepInventory && !spectator && !developer;
    }
    public static boolean mayCollect(UUID owner, UUID collector) { return owner == null || owner.equals(collector); }
    public static boolean mayAutomate(boolean privateStorage, UUID owner) { return !privateStorage && owner == null; }
}
