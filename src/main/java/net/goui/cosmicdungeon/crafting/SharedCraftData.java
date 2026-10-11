package net.goui.cosmicdungeon.crafting;

import com.mojang.serialization.Codec;
import java.util.*;
import net.goui.cosmicdungeon.transaction.SavedDataProof;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.*;

/** Pending inventory/metrics decisions and committed per-crafter fuel. One writer and at most one transaction per run/owner. */
public final class SharedCraftData extends SavedData {
    public static final String ID = "cosmicdungeon_shared_crafts_v1";
    public static final Codec<SharedCraftData> CODEC = CompoundTag.CODEC.xmap(SharedCraftData::new, SharedCraftData::image);
    private static final SavedDataType<SharedCraftData> TYPE = new SavedDataType<>(ID, SharedCraftData::new, CODEC);
    private final CompoundTag extra;
    private final Map<UUID, SharedCraftPlan> plans = new LinkedHashMap<>();
    private final Map<UUID, UUID> owners = new HashMap<>();
    private final Map<Long, UUID> runs = new HashMap<>();
    private final Map<UUID, CompoundTag> fuel = new HashMap<>();
    private MinecraftServer server;
    private SharedCraftData() { extra = new CompoundTag(); extra.putInt("schema", 1); }
    public SharedCraftData(CompoundTag image) {
        extra = image.copy();
        if (!(image.get("schema") instanceof IntTag) || !Set.of(1, 2).contains(image.getIntOr("schema", 0))
                || image.contains("pending") && !(image.get("pending") instanceof CompoundTag))
            throw new IllegalArgumentException("Unsupported shared crafting journal; original retained");
        if (image.getIntOr("schema", 0) == 2) {
            if (!(image.get(BrewingFuel.KEY) instanceof CompoundTag)) throw new IllegalArgumentException("Missing brewing ledger");
            var ledger = image.getCompoundOrEmpty(BrewingFuel.KEY);
            for (String key : ledger.keySet()) {
                UUID owner = UUID.fromString(key);
                if (!key.equals(owner.toString())) throw new IllegalArgumentException("Noncanonical fuel owner");
                var state = ledger.getCompound(key).orElseThrow(); BrewingFuel.validate(state); fuel.put(owner, state.copy());
            }
        }
        var pending = image.getCompoundOrEmpty("pending");
        if (pending.size() > 32) throw new IllegalArgumentException("Too many pending crafts");
        for (String key : pending.keySet()) {
            var plan = new SharedCraftPlan(pending.getCompound(key).orElseThrow());
            if (!key.equals(plan.id().toString())) throw new IllegalArgumentException("Crafting journal identity");
            if (plan.hasFuel()) {
                if (image.getIntOr("schema", 0) != 2 || !fuel(plan.recipient()).equals(plan.fuel().getCompoundOrEmpty(plan.committed() ? "after" : "before")))
                    throw new IllegalArgumentException("Fuel ledger differs from pending decision");
            }
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
    public CompoundTag fuel(UUID owner) { return fuel.getOrDefault(owner, BrewingFuel.empty()).copy(); }
    public boolean fuelMatches(SharedCraftPlan plan) {
        return !plan.hasFuel() || fuel(plan.recipient()).equals(plan.fuel().getCompoundOrEmpty("before"));
    }
    public SharedCraftPlan pending(UUID owner) { return plans.get(owners.get(owner)); }
    public SharedCraftPlan pendingRun(long run) { return plans.get(runs.get(run)); }
    public List<SharedCraftPlan> pending() { return List.copyOf(plans.values()); }
    public CompoundTag image() {
        var result = extra.copy(); var pending = new CompoundTag();
        plans.forEach((id, plan) -> pending.put(id.toString(), plan.image())); result.put("pending", pending);
        if (result.getIntOr("schema", 0) == 2) {
            var ledger = new CompoundTag(); fuel.forEach((owner, state) -> ledger.put(owner.toString(), state.copy()));
            result.put(BrewingFuel.KEY, ledger);
        }
        return result;
    }
    public void reserve(SharedCraftPlan plan) {
        if (plans.size() >= 32 || plan.committed() || plan.owners().stream().anyMatch(plan::acknowledged))
            throw new IllegalStateException("Invalid crafting reservation");
        if (!fuelMatches(plan)) throw new IllegalStateException("Stale brewing fuel reservation");
        if (plan.hasFuel() && extra.getIntOr("schema", 0) == 1 && extra.contains(BrewingFuel.KEY))
            throw new IllegalStateException("Legacy unknown field collides with brewing ledger; retained");
        add(plan);
        if (plan.hasFuel()) extra.putInt("schema", 2);
        setDirty();
    }
    public void commit(UUID id) {
        var previous = Objects.requireNonNull(plans.get(id));
        if (previous.committed() || !fuelMatches(previous)) throw new IllegalStateException("Fuel/decision already changed");
        var committed = previous.commit();
        if (previous.hasFuel()) fuel.put(previous.recipient(), previous.fuel().getCompoundOrEmpty("after").copy());
        plans.put(id, committed); setDirty();
    }
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
