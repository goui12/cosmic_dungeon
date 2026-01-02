package net.goui.cosmicdungeon.playerclass.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public final class DungeoneerClassService {
    private DungeoneerClassService() {}

    public static void applyClassChange(ServerPlayer sp, String newClassId) {
        if (sp == null) return;

        newClassId = DungeoneerClassRegistry.clamp(newClassId);

        // Always set the class id first (server truth)
        ClassNbtUtil.setClassId(sp, newClassId);

        // Handle per-class state
        if (DungeoneerClassIds.METALMANCER.equals(newClassId)) {
            ClassNet.seedMetalmancerExtra(sp);
            return;
        }

        // Non-metalmancer: remove extra inventory mirror
        CompoundTag pd = sp.getPersistentData();
        CompoundTag root = pd.getCompoundOrEmpty(ClassData.ROOT_TAG).copy();
        root.remove(ClassData.KEY_EXTRA);
        pd.put(ClassData.ROOT_TAG, root);
    }
}
