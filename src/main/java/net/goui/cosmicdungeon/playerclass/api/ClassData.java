package net.goui.cosmicdungeon.playerclass.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * Per-player class system data layout in player persistent NBT:
 *
 * player.getPersistentData()["cosmicdungeon"] -> root
 * root["class_id"] -> string
 * root["extra"]    -> CompoundTag (Metalmancer extra slots)
 */
public final class ClassData {
    private ClassData() {}

    public static final String ROOT_TAG     = "cosmicdungeon";
    public static final String KEY_CLASS_ID = "class_id";
    public static final String KEY_EXTRA    = "extra";

    public static String getClassId(Player player) {
        CompoundTag root = player.getPersistentData().getCompoundOrEmpty(ROOT_TAG);
        return root.getStringOr(KEY_CLASS_ID, ClassKeys.CLASS_ID_NONE);
    }
}
