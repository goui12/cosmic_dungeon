package net.goui.cosmicdungeon.dungeon.d1;

import com.mojang.serialization.Codec;
import net.goui.cosmicdungeon.npc.tamsin.TamsinTaxProgress;
import net.minecraft.nbt.*;
import java.util.*;

/** Immutable six-Bloom decision. The existing D1 objective save owns pending decisions. */
public final class WatsonOutcome {
    public static final Codec<WatsonOutcome> CODEC = CompoundTag.CODEC.xmap(WatsonOutcome::new, WatsonOutcome::image);
    public static final List<String> BLOOMS = List.of(
            "cosmicdungeon:bloom_of_quiet_assurance", "cosmicdungeon:bloom_of_gentle_lies",
            "cosmicdungeon:bloom_of_waning_mercy", "cosmicdungeon:bloom_of_constricting_bonds",
            "cosmicdungeon:bloom_of_unspoken_resignation", "cosmicdungeon:bloom_of_elegy");
    private final CompoundTag data;
    public WatsonOutcome(CompoundTag image) {
        data = image.copy();
        if (data.getIntOr("version", 0) != 1 || run() <= 0) throw new IllegalArgumentException("Unknown Watson decision version/run");
        id();
        if (data.getBoolean("success").isEmpty() || data.getBoolean("ready").isEmpty())
            throw new IllegalArgumentException("Missing Watson decision flags");
        var owners = owners();
        if (owners.isEmpty() || owners.size() > 6 || new HashSet<>(owners).size() != owners.size())
            throw new IllegalArgumentException("Invalid Watson roster");
        if (!data.getCompoundOrEmpty("members").keySet().equals(owners.stream().map(UUID::toString).collect(java.util.stream.Collectors.toSet())))
            throw new IllegalArgumentException("Watson member images differ from roster");
        var found = new HashSet<String>();
        for (UUID owner : owners) {
            var m = member(owner);
            for (String key : List.of("before", "after", "tax_before", "tax_after"))
                if (m.getCompound(key).isEmpty()) throw new IllegalArgumentException("Missing Watson member image " + key);
            for (String key : List.of("kills", "collected", "lifetime_lesser", "rounded"))
                if (m.getInt(key).isEmpty() || m.getIntOr(key, -1) < 0) throw new IllegalArgumentException("Invalid Watson reward " + key);
            if (m.getIntOr("lifetime_lesser", 0) > m.getIntOr("collected", 0)
                    || m.getIntOr("rounded", 0) != D1ObjectiveRules.roundedLesserBlooms(m.getIntOr("collected", 0))
                    || m.getLong("faction_bonus").isEmpty() || m.getLongOr("faction_bonus", -1) < 0
                    || m.getLongOr("faction_bonus", -1) > 400)
                throw new IllegalArgumentException("Invalid frozen Watson rewards");
            if (!consume(m.getCompoundOrEmpty("before"), found).equals(m.getCompoundOrEmpty("after")))
                throw new IllegalArgumentException("Watson replacement is not the exact Bloom deduction");
            var tax = success() ? TamsinTaxProgress.successfulImage(m.getCompoundOrEmpty("tax_before"), run()) : m.getCompoundOrEmpty("tax_before");
            if (!tax.equals(m.getCompoundOrEmpty("tax_after"))) throw new IllegalArgumentException("Watson Tax projection differs");
        }
        if (success() != (found.size() == BLOOMS.size())) throw new IllegalArgumentException("Watson outcome differs from physical Blooms");
        var ack = acknowledged();
        if (!new HashSet<>(owners).containsAll(ack) || ack.size() != strings("acknowledged").size()
                || ready() && ack.size() != owners.size()) throw new IllegalArgumentException("Invalid Watson acknowledgements");
    }
    /** Freeze each source before touching players; first distinct registered Bloom costs exactly one item. */
    public static WatsonOutcome create(long run, List<UUID> owners, Map<UUID, CompoundTag> inventories,
            Map<UUID, CompoundTag> taxes, Map<UUID, int[]> counters, int factionPerBloom) {
        if (factionPerBloom < 0 || factionPerBloom > 100) throw new IllegalArgumentException("Invalid configured faction gain");
        var n = new CompoundTag(); n.putInt("version", 1); n.putLong("run", run);
        n.putString("id", UUID.randomUUID().toString()); n.putBoolean("ready", false);
        var roster = new ListTag(); owners.forEach(owner -> roster.add(StringTag.valueOf(owner.toString()))); n.put("owners", roster);
        n.put("acknowledged", new ListTag()); var members = new CompoundTag(); var found = new HashSet<String>();
        for (UUID owner : owners) {
            var m = new CompoundTag(); var before = Objects.requireNonNull(inventories.get(owner));
            m.put("before", before.copy()); m.put("after", consume(before, found));
            m.put("tax_before", Objects.requireNonNull(taxes.get(owner)).copy());
            int[] count = Objects.requireNonNull(counters.get(owner)); int rounded = D1ObjectiveRules.roundedLesserBlooms(count[1]);
            m.putInt("kills", count[0]); m.putInt("collected", count[1]); m.putInt("lifetime_lesser", count[2]);
            m.putInt("rounded", rounded); m.putLong("faction_bonus", (long) (rounded - count[1]) * factionPerBloom);
            members.put(owner.toString(), m);
        }
        boolean success = found.size() == BLOOMS.size(); n.putBoolean("success", success);
        for (UUID owner : owners) {
            var m = members.getCompoundOrEmpty(owner.toString()); var tax = m.getCompoundOrEmpty("tax_before");
            m.put("tax_after", success ? TamsinTaxProgress.successfulImage(tax, run) : tax.copy());
        }
        n.put("members", members); return new WatsonOutcome(n);
    }
    private static CompoundTag consume(CompoundTag source, Set<String> found) {
        var after = source.copy(); var items = source.getList("Items").orElseThrow(() -> new IllegalArgumentException("Missing native inventory list"));
        var remaining = new ListTag(); var slots = new HashSet<Integer>();
        for (Tag value : items) {
            if (!(value instanceof CompoundTag item) || item.getInt("Slot").isEmpty()
                    || item.getString("id").isEmpty() || item.getInt("count").isEmpty()
                    || item.getIntOr("count", 0) < 1 || !slots.add(item.getIntOr("Slot", -1)))
                throw new IllegalArgumentException("Malformed native inventory entry");
            var next = item.copy(); String id = item.getStringOr("id", "");
            if (BLOOMS.contains(id) && found.add(id)) {
                int count = item.getIntOr("count", 0) - 1;
                if (count == 0) continue;
                next.putInt("count", count);
            }
            remaining.add(next);
        }
        after.put("Items", remaining); return after;
    }
    private List<String> strings(String key) {
        var list = data.getList(key).orElseThrow(() -> new IllegalArgumentException("Missing Watson list " + key));
        var values = new ArrayList<String>();
        for (Tag value : list) values.add(value.asString().orElseThrow(() -> new IllegalArgumentException("Malformed Watson list")));
        return List.copyOf(values);
    }
    public CompoundTag image() { return data.copy(); }
    public UUID id() { return UUID.fromString(data.getStringOr("id", "")); }
    public long run() { return data.getLongOr("run", 0); }
    public boolean success() { return data.getBooleanOr("success", false); }
    public boolean ready() { return data.getBooleanOr("ready", false); }
    public boolean permitsCleanup(String reason) { return ready() && reason.equals(success() ? "COMPLETED" : "ABANDONED"); }
    public List<UUID> owners() { return strings("owners").stream().map(UUID::fromString).toList(); }
    public Set<UUID> acknowledged() { return new HashSet<>(strings("acknowledged").stream().map(UUID::fromString).toList()); }
    public CompoundTag member(UUID owner) {
        return data.getCompoundOrEmpty("members").getCompound(owner.toString()).orElseThrow().copy();
    }
    public WatsonReceipt receipt(UUID owner) {
        if (!owners().contains(owner)) throw new IllegalArgumentException("Foreign Watson owner");
        return new WatsonReceipt(owner, run(), id(), success());
    }
    public WatsonOutcome acknowledge(UUID owner) {
        receipt(owner); var n = image(); var ack = n.getList("acknowledged").orElseThrow();
        if (!acknowledged().contains(owner)) ack.add(StringTag.valueOf(owner.toString()));
        return new WatsonOutcome(n);
    }
    public WatsonOutcome markReady() { var n = image(); n.putBoolean("ready", true); return new WatsonOutcome(n); }
    public boolean sameDecision(WatsonOutcome other) {
        var a = image(); var b = other.image(); a.remove("acknowledged"); a.remove("ready"); b.remove("acknowledged"); b.remove("ready");
        return a.equals(b);
    }
    public boolean canApply(UUID owner, CompoundTag receipt, long cleanupRun, CompoundTag inventory, CompoundTag tax) {
        if (receipt(owner).matches(receipt)) return true;
        if (acknowledged().contains(owner) || cleanupRun >= run()) return false;
        if (!receipt.isEmpty() && !receipt(owner).shouldApply(WatsonReceipt.CODEC.parse(NbtOps.INSTANCE, receipt).getOrThrow())) return false;
        var m = member(owner);
        return m.getCompoundOrEmpty("before").equals(inventory) && m.getCompoundOrEmpty("tax_before").equals(tax);
    }
}
