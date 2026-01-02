package net.goui.cosmicdungeon.playerclass.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * Central definition for all per-player "class system" data.
 *
 * Layout under player.getPersistentData()[ROOT_TAG]:
 *
 * cosmicdungeon: {
 *   class_id: "none" | "metalmancer" | ...
 *   extra: { ... }   // Metalmancer 3-slot extra hotbar, satchel, etc.
 *   // future: other top-level keys as needed
 * }
 */
public final class ClassData {
    private ClassData() {}

    /**
     * Root compound under Player persistent data where all class info lives:
     *
     *   player.getPersistentData().getCompound(ROOT_TAG)
     */
    public static final String ROOT_TAG = "cosmicdungeon";

    /** Key for the active class ID stored as a string. */
    public static final String KEY_CLASS_ID = "class_id";

    /**
     * Key for "extra inventory" / extra slots subtree.
     * This is where the Metalmancer 3-slot extra bar lives.
     */
    public static final String KEY_EXTRA = "extra";

    /**
     * Canonical class ID values. Use these instead of hard-coded strings.
     */
    public static final String CLASS_ID_NONE        = "none";
    public static final String CLASS_ID_METALMANCER = "metalmancer";

    /**
     * Read the current classId for a player from persistent data.
     * Falls back to "none" if tag is missing or empty.
     */
    public static String getClassId(Player player) {
        CompoundTag root = player.getPersistentData().getCompoundOrEmpty(ROOT_TAG);
        return root.getStringOr(KEY_CLASS_ID, CLASS_ID_NONE);
    }

    /**
     * Low-level helper if you already have the player's persistent-data compound.
     * Expects the compound you pass in to be player.getPersistentData(), not ROOT_TAG directly.
     */
    public static String getClassId(CompoundTag playerPersistentData) {
        CompoundTag root = playerPersistentData.getCompoundOrEmpty(ROOT_TAG);
        return root.getStringOr(KEY_CLASS_ID, CLASS_ID_NONE);
    }
}
