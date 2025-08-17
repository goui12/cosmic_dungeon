package net.goui.cosmicdungeon.classsystem;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public final class ClassData {
    private static final String KEY = "cosmicdungeon_class";

    public static PlayerClass get(ServerPlayer p) {
        CompoundTag tag = p.getPersistentData();
        // ✅ use .orElse("") to safely unwrap
        String v = tag.getString(KEY).orElse("");
        var pc = PlayerClass.from(v);
        return pc != null ? pc : null;
    }

    public static void set(ServerPlayer p, PlayerClass cls) {
        p.getPersistentData().putString(KEY, cls.name());
        p.refreshDimensions(); // harmless ping; ensures some things resync. Optional.
    }
}
