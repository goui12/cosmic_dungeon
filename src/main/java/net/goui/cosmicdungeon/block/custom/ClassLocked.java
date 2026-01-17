package net.goui.cosmicdungeon.block.custom;

/** Implemented by blocks that require a specific player class to interact/open. */
public interface ClassLocked {
    /** Canonical class id from ClassKeys, e.g. "judicator". */
    String requiredClassId();
}
