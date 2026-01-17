package net.goui.cosmicdungeon.playerclass.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public final class ClassNbtUtil {
    private ClassNbtUtil() {}

    public static CompoundTag getOrCreateRoot(Player player) {
        CompoundTag pd = player.getPersistentData();
        CompoundTag root = pd.getCompoundOrEmpty(ClassData.ROOT_TAG);
        pd.put(ClassData.ROOT_TAG, root);
        return root;
    }

    public static String getClassId(Player player) {
        CompoundTag root = getOrCreateRoot(player);
        return root.getStringOr(ClassData.KEY_CLASS_ID, ClassKeys.CLASS_ID_NONE);
    }

    public static void setClassId(Player player, String classId) {
        // Normalize + clamp to known values (prevents typos, hacked packets, future drift)
        classId = ClassKeys.clamp(classId);

        CompoundTag root = getOrCreateRoot(player);
        root.putString(ClassData.KEY_CLASS_ID, classId);
    }

    public static boolean hasClass(Player player, String classId) {
        return classId != null && classId.equals(getClassId(player));
    }

    public static boolean isMetalmancer(Player player) {
        return hasClass(player, ClassKeys.CLASS_ID_METALMANCER);
    }

    public static CompoundTag getOrCreateExtra(Player player) {
        CompoundTag root = getOrCreateRoot(player);
        CompoundTag extra = root.getCompoundOrEmpty(ClassData.KEY_EXTRA);
        root.put(ClassData.KEY_EXTRA, extra);
        return extra;
    }
}
