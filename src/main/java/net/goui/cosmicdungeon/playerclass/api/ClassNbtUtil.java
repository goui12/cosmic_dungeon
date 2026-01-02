package net.goui.cosmicdungeon.playerclass.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * Central utilities for reading/writing player-class data in player persistent NBT.
 *
 * Use these instead of hardcoding "cosmicdungeon", "class_id", etc. all over.
 */
public final class ClassNbtUtil {
    private ClassNbtUtil() {}

    /**
     * Returns the root class-data compound for this player, and ensures it is
     * actually attached under player.getPersistentData()[ROOT_TAG].
     */
    public static CompoundTag getOrCreateRoot(Player player) {
        CompoundTag pd = player.getPersistentData();

        // Get an existing or empty root tag
        CompoundTag root = pd.getCompoundOrEmpty(ClassData.ROOT_TAG);

        // Make sure it's actually stored back on the player PD
        // (getCompoundOrEmpty creates a new tag if missing, but does not attach it).
        pd.put(ClassData.ROOT_TAG, root);

        return root;
    }

    /**
     * Returns the current class ID for this player, falling back to CLASS_ID_NONE.
     */
    public static String getClassId(Player player) {
        CompoundTag root = getOrCreateRoot(player);
        return root.getStringOr(ClassData.KEY_CLASS_ID, ClassKeys.CLASS_ID_NONE);
    }

    /**
     * Sets the current class ID for this player.
     */
    public static void setClassId(Player player, String classId) {
        CompoundTag root = getOrCreateRoot(player);
        root.putString(ClassData.KEY_CLASS_ID, classId);
        // No need to re-put root; getOrCreateRoot already attached it.
    }

    /**
     * Clears the current class, back to "none".
     */
    public static void clearClass(Player player) {
        setClassId(player, ClassKeys.CLASS_ID_NONE);
    }

    /**
     * Checks if the player has this class ID active.
     */
    public static boolean hasClass(Player player, String classId) {
        return classId.equals(getClassId(player));
    }

    /**
     * Convenience for Metalmancer checks.
     */
    public static boolean isMetalmancer(Player player) {
        return hasClass(player, ClassKeys.CLASS_ID_METALMANCER);
    }

    /**
     * Gets (and attaches) the "extra" subtag used for extra inventory / slots.
     *
     * Layout:
     *   cosmicdungeon -> extra
     */
    public static CompoundTag getOrCreateExtra(Player player) {
        CompoundTag root = getOrCreateRoot(player);
        CompoundTag extra = root.getCompoundOrEmpty(ClassData.KEY_EXTRA);
        root.put(ClassData.KEY_EXTRA, extra);
        return extra;
    }
}
