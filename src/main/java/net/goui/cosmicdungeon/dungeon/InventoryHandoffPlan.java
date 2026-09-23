package net.goui.cosmicdungeon.dungeon;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import java.util.*;

/** Immutable D1 cleanup/claim decision in the existing pending-recovery save. */
public final class InventoryHandoffPlan {
    public static final Codec<InventoryHandoffPlan> CODEC = CompoundTag.CODEC.xmap(InventoryHandoffPlan::new, InventoryHandoffPlan::image);
    private final CompoundTag data;
    public InventoryHandoffPlan(CompoundTag image) {
        data = image.copy();
        if (data.getIntOr("version", 0) != 1) throw new IllegalArgumentException("Unknown inventory handoff version");
        id(); owner();
        for (String flag : List.of("keep", "exact_before", "world_ready"))
            if (data.getBoolean(flag).isEmpty()) throw new IllegalArgumentException("Missing handoff flag " + flag);
        if (data.getString("reason").isEmpty() || data.getString("key").isEmpty())
            throw new IllegalArgumentException("Missing handoff identity");
        if (run() <= 0 || !Set.of("cleanup", "claim").contains(kind()))
            throw new IllegalArgumentException("Invalid inventory handoff");
        for (String key : List.of("before", "after", "stored", "escrow", "ownership_before", "ownership_after", "stored_after"))
            if (!data.contains(key) || data.getCompound(key).isEmpty()) throw new IllegalArgumentException("Missing handoff image " + key);
        if (kind().equals("claim") && (!key().equals(owner() + "|" + run()) || keep() || !exactBefore()))
            throw new IllegalArgumentException("Invalid stored-inventory claim");
        if (kind().equals("cleanup") && !Set.of("COMPLETED", "ABANDONED", "KICKED", "LINK_DEAD", "STARTUP_ABORT", "MANUAL").contains(reason()))
            throw new IllegalArgumentException("Unknown cleanup outcome");
        var escrow = tag("escrow");
        if (!escrow.isEmpty()) {
            var entry = DungeonInventoryEscrowData.Entry.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, escrow).getOrThrow();
            if (entry.runId() != run() || !entry.playerId().equals(owner())) throw new IllegalArgumentException("Foreign escrow");
        }
        for (String field : List.of("ownership_before", "ownership_after"))
            if (!tag(field).isEmpty()) ChopOwnershipData.Entry.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag(field)).getOrThrow();
    }
    public static InventoryHandoffPlan create(UUID owner, long run, String kind, String reason,
            boolean keep, boolean exactBefore, CompoundTag before, CompoundTag after, CompoundTag stored,
            CompoundTag escrow, CompoundTag ownershipBefore, CompoundTag ownershipAfter, String key, CompoundTag storedAfter) {
        var n = new CompoundTag();
        n.putInt("version", 1); n.putString("id", UUID.randomUUID().toString()); n.putString("owner", owner.toString());
        n.putLong("run", run); n.putString("kind", kind); n.putString("reason", reason);
        n.putBoolean("keep", keep); n.putBoolean("exact_before", exactBefore); n.putBoolean("world_ready", false);
        n.put("before", before.copy()); n.put("after", after.copy()); n.put("stored", stored.copy());
        n.put("escrow", escrow.copy()); n.put("ownership_before", ownershipBefore.copy()); n.put("ownership_after", ownershipAfter.copy());
        n.putString("key", key); n.put("stored_after", storedAfter.copy());
        return new InventoryHandoffPlan(n);
    }
    /** Pure source selection shared by online/offline cleanup; no inventory is moved here. */
    public static InventoryHandoffPlan cleanup(UUID owner, long run, String reason, CompoundTag original,
            DungeonInventoryEscrowData.Entry escrow, CompoundTag starting, CompoundTag ownershipBefore,
            CompoundTag before, boolean online) {
        boolean startup = !starting.isEmpty();
        boolean success = !startup && reason.equals("COMPLETED");
        boolean keep = !startup && (success || escrow != null && escrow.outsideActive());
        var after = startup ? starting.getCompoundOrEmpty("inventory").copy()
                : escrow == null ? original.copy() : escrow.outsideInventory().copy();
        var stored = success ? escrow == null ? original.copy()
                : escrow.outsideActive() ? escrow.dungeonInventory().copy() : escrow.outsideInventory().copy() : new CompoundTag();
        var ownershipAfter = ownershipBefore.copy();
        if (startup) {
            if (starting.getCompound("inventory").isEmpty() || starting.getCompound("ownership").isEmpty())
                throw new IllegalArgumentException("Incomplete startup image");
            ownershipAfter = starting.getCompoundOrEmpty("ownership").copy();
            var expected = ownershipAfter.isEmpty() ? null : ChopOwnershipData.Entry.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, ownershipAfter).getOrThrow();
            var bound = ChopOwnershipData.entryImage(expected == null ? null : new ChopOwnershipData.Entry(expected.token(), run, false));
            if (!ownershipBefore.equals(ownershipAfter) && !ownershipBefore.equals(bound))
                throw new IllegalStateException("Startup Chop ownership differs from frozen entry");
        } else if (!ownershipBefore.isEmpty()) {
            var entry = ChopOwnershipData.Entry.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, ownershipBefore).getOrThrow();
            if (entry.runId() == run)
                ownershipAfter = ChopOwnershipData.entryImage(new ChopOwnershipData.Entry(UUID.randomUUID().toString(), 0, true));
        }
        return create(owner, run, "cleanup", startup ? "STARTUP_ABORT" : reason, keep, online, before,
                after, stored, DungeonInventoryEscrowData.image(escrow), ownershipBefore, ownershipAfter, "", new CompoundTag());
    }
    public CompoundTag image() { return data.copy(); }
    public CompoundTag tag(String key) { return data.getCompoundOrEmpty(key).copy(); }
    public UUID id() { return UUID.fromString(data.getStringOr("id", "")); }
    public UUID owner() { return UUID.fromString(data.getStringOr("owner", "")); }
    public long run() { return data.getLongOr("run", 0); }
    public String kind() { return data.getStringOr("kind", ""); }
    public String reason() { return data.getStringOr("reason", ""); }
    public String key() { return data.getStringOr("key", ""); }
    public boolean keep() { return data.getBooleanOr("keep", false); }
    public boolean exactBefore() { return data.getBooleanOr("exact_before", false); }
    public boolean worldReady() { return data.getBooleanOr("world_ready", false); }
    public InventoryHandoffPlan ready() { var n = image(); n.putBoolean("world_ready", true); return new InventoryHandoffPlan(n); }
    public CompoundTag receipt() {
        var n = new CompoundTag(); n.putString("id", id().toString()); n.putString("owner", owner().toString());
        n.putLong("run", run()); n.putString("kind", kind()); return n;
    }
    public boolean receipted(CompoundTag receipt) { return receipt().equals(receipt); }
    public boolean canApply(CompoundTag receipt, long completedRun, CompoundTag current) {
        if (receipted(receipt)) return true;
        return (!kind().equals("cleanup") || completedRun < run())
                && (!exactBefore() || tag("before").equals(current));
    }
}
