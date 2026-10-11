package net.goui.cosmicdungeon.crafting;

import com.mojang.serialization.Codec;
import java.util.*;
import net.goui.cosmicdungeon.transaction.SavedDataProof;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.*;

/** Pending inventory/metrics decisions only. One writer and at most one transaction per run/owner. */
public final class SharedCraftData extends SavedData {
    public static final String ID = "cosmicdungeon_shared_crafts_v1";
    public static final Codec<SharedCraftData> CODEC = CompoundTag.CODEC.xmap(SharedCraftData::new, SharedCraftData::image);
    private static final SavedDataType<SharedCraftData> TYPE = new SavedDataType<>(ID, SharedCraftData::new, CODEC);
    private final CompoundTag extra;
    private final Map<UUID, SharedCraftPlan> plans = new LinkedHashMap<>();
    private final Map<UUID, UUID> owners = new HashMap<>();
    private final Map<Long, UUID> runs = new HashMap<>();
    private MinecraftServer server;
    private SharedCraftData() { extra = new CompoundTag(); extra.putInt("schema", 1); }
    public SharedCraftData(CompoundTag image) {
        extra = image.copy();
        if (!(image.get("schema") instanceof IntTag) || image.getIntOr("schema", 0) != 1
                || image.contains("pending") && !(image.get("pending") instanceof CompoundTag))
            throw new IllegalArgumentException("Unsupported shared crafting journal; original retained");
        var pending = image.getCompoundOrEmpty("pending");
        if (pending.size() > 32) throw new IllegalArgumentException("Too many pending crafts");
        for (String key : pending.keySet()) {
            var plan = new SharedCraftPlan(pending.getCompound(key).orElseThrow());
            if (!key.equals(plan.id().toString())) throw new IllegalArgumentException("Crafting journal identity");
            add(plan);
        }
    }
    public static SharedCraftData get(MinecraftServer server) {
        SavedDataProof.validate(server, ID, CODEC);
        var data = server.overworld().getDataStorage().computeIfAbsent(TYPE); data.server = server; return data;
    }
    private void add(SharedCraftPlan plan) {
        if (plans.containsKey(plan.id()) || runs.containsKey(plan.run()) || plan.owners().stream().anyMatch(owners::containsKey))
            throw new IllegalStateException("Crafting owner or run is already reserved");
        plans.put(plan.id(), plan); runs.put(plan.run(), plan.id()); plan.owners().forEach(owner -> owners.put(owner, plan.id()));
    }
    public SharedCraftPlan pending(UUID owner) { return plans.get(owners.get(owner)); }
    public SharedCraftPlan pendingRun(long run) { return plans.get(runs.get(run)); }
    public List<SharedCraftPlan> pending() { return List.copyOf(plans.values()); }
    public CompoundTag image() {
        var result = extra.copy(); var pending = new CompoundTag();
        plans.forEach((id, plan) -> pending.put(id.toString(), plan.image())); result.put("pending", pending); return result;
    }
    public void reserve(SharedCraftPlan plan) {
        if (plans.size() >= 32 || plan.committed() || plan.owners().stream().anyMatch(plan::acknowledged))
            throw new IllegalStateException("Invalid crafting reservation");
        add(plan); setDirty();
    }
    public void commit(UUID id) { plans.put(id, Objects.requireNonNull(plans.get(id)).commit()); setDirty(); }
    public void acknowledge(UUID id, UUID owner) { plans.put(id, Objects.requireNonNull(plans.get(id)).acknowledge(owner)); setDirty(); }
    public void acknowledgeMetrics(UUID id) { plans.put(id, Objects.requireNonNull(plans.get(id)).acknowledgeMetrics()); setDirty(); }
    public void retire(UUID id) {
        var plan = Objects.requireNonNull(plans.get(id));
        if (!plan.complete()) throw new IllegalStateException("Crafting receipts or metrics still pending");
        plans.remove(id); runs.remove(plan.run()); plan.owners().forEach(owners::remove); setDirty();
    }
    public boolean retireVerified(UUID id) {
        var plan = Objects.requireNonNull(plans.get(id)); retire(id);
        if (flushVerified()) return true;
        add(plan); setDirty(); return false;
    }
    public boolean flushVerified() { return SavedDataProof.save(server, ID, CODEC, this); }
}
