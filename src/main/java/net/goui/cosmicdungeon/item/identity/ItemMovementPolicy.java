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
        // Unknown quick-move implementations may route between multiple external inventories.
        return !restricted || privateMenu;
    }
    public static boolean mayNest(boolean restricted, boolean portableContainer) {
        return !restricted || !portableContainer;
    }
}
