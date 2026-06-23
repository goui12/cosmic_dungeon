package net.goui.cosmicdungeon.playerclass.api;

/** Item with an intrinsic class requirement independent of stack attunement metadata. */
public interface ClassBoundItem {
    /** Canonical required class id from {@link ClassKeys}. */
    String requiredClassId();
}
