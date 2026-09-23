package net.goui.cosmicdungeon.item.identity;

/** Pure boundary rules. A private destination is the same player's inventory/ender chest
 * or a specifically reviewed owner-preserving service, never arbitrary shared storage. */
public final class ItemMovementPolicy {
    private ItemMovementPolicy() {}
    public static boolean mayDrop(boolean noDrop) { return !noDrop; }
    public static boolean mayInsert(boolean restricted, boolean privateDestination) {
        return !restricted || privateDestination;
    }
    public static boolean mayShift(boolean restricted, boolean sourcePrivate, boolean privateMenu) {
        return mayShift(restricted, sourcePrivate, privateMenu, false);
    }
    public static boolean mayShift(boolean restricted, boolean sourcePrivate, boolean privateMenu,
                                   boolean reviewedClassChestPickup) {
        // The narrow exception proves a chest-to-owner destination, never the reverse route.
        return !restricted || privateMenu || (!sourcePrivate && reviewedClassChestPickup);
    }
    public static boolean mayNest(boolean restricted, boolean portableContainer) {
        return !restricted || !portableContainer;
    }
}
