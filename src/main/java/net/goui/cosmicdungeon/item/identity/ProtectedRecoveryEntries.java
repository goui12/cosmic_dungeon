package net.goui.cosmicdungeon.item.identity;

import net.minecraft.nbt.CompoundTag;
import java.util.ArrayList;

/** Copy-on-write NBT operations; preserve all item components and unknown unrelated records. */
public final class ProtectedRecoveryEntries {
    private ProtectedRecoveryEntries() {}
    public static CompoundTag remainder(CompoundTag original, int count) {
        if (!(original.get("item") instanceof CompoundTag item) || count < 1 || count > item.getIntOr("count", 1))
            throw new IllegalArgumentException("Invalid protected item remainder");
        var result = original.copy();
        result.getCompoundOrEmpty("item").putInt("count", count);
        return result;
    }
    public static boolean hasClaimable(CompoundTag pending, long currentScope) {
        for (String id : pending.keySet())
            if (ProtectedRecoveryRules.claimable(pending.getCompoundOrEmpty(id).getLongOr("run", -1), currentScope, false))
                return true;
        return false;
    }
    public static CompoundTag finish(CompoundTag pending, long runId, boolean successfulD1) {
        var next = pending.copy();
        for (String id : new ArrayList<>(next.keySet())) {
            var entry = next.getCompoundOrEmpty(id);
            switch (ProtectedRecoveryRules.finish(entry.getLongOr("run", -1), runId, successfulD1)) {
                case DISCARD -> next.remove(id);
                case RELEASE_OUTSIDE -> {
                    var released = entry.copy(); released.putLong("run", 0); next.put(id, released);
                }
                case KEEP -> {}
            }
        }
        return next;
    }
}
