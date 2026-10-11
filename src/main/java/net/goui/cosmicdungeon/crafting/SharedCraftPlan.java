package net.goui.cosmicdungeon.crafting;

import com.mojang.serialization.Codec;
import java.util.*;
import net.goui.cosmicdungeon.dungeon.metrics.RunMetrics;
import net.minecraft.nbt.*;

/** Immutable multi-owner decision. Unknown NBT survives supported-schema transitions. */
public final class SharedCraftPlan {
    public static final Codec<SharedCraftPlan> CODEC = CompoundTag.CODEC.xmap(SharedCraftPlan::new, SharedCraftPlan::image);
    private final CompoundTag image;
    public SharedCraftPlan(CompoundTag source) {
        if (source.sizeInBytes() > 4 * 1024 * 1024) throw new IllegalArgumentException("Crafting image exceeds4MiB");
        image = source.copy();
        if (!(image.get("schema") instanceof IntTag) || image.getIntOr("schema", 0) != 1)
            throw new IllegalArgumentException("Unsupported shared crafting schema");
        id(); recipient();
        if (!(image.get("run") instanceof LongTag) || run() <= 0 || !(image.get("sequence") instanceof LongTag)
                || sequence() <= 0 || !(image.get("at") instanceof LongTag) || at() < 0
                || !(image.get("donation_before") instanceof LongTag) || donationBefore() < 0
                || !(image.get("donation_after") instanceof LongTag) || donationAfter() < donationBefore()
                || donationAfter() - donationBefore() > 64)
            throw new IllegalArgumentException("Invalid crafting identity/cursor");
        for (String field : List.of("recipe", "skill", "recipient_name")) text(image.getStringOr(field, ""), 128);
        for (String flag : List.of("committed", "metrics_ack"))
            if (image.getBoolean(flag).isEmpty()) throw new IllegalArgumentException("Missing crafting decision");
        for (String key : List.of("owners", "donated", "output"))
            if (!(image.get(key) instanceof CompoundTag)) throw new IllegalArgumentException("Missing crafting image");
        var owners = owners();
        if (owners.isEmpty() || owners.size() > 6 || !owners.contains(recipient())) throw new IllegalArgumentException("Crafting roster");
        int total = 0;
        for (UUID owner : owners) {
            var state = owner(owner);
            if (state.getBoolean("ack").isEmpty()) throw new IllegalArgumentException("Missing owner acknowledgement");
            for (String key : List.of("before", "after", "debits"))
                if (!(state.get(key) instanceof CompoundTag)) throw new IllegalArgumentException("Missing owner inventory");
            for (var debit : debits(owner)) {
                if (debit.slot() < 0 || debit.slot() >= 128 || debit.count() <= 0 || debit.count() > 64)
                    throw new IllegalArgumentException("Crafting debit bounds");
                text(debit.item(), 128); text(debit.ingredient(), 128); total += debit.count();
            }
            if (!owner.equals(recipient()) && debits(owner).isEmpty()) throw new IllegalArgumentException("Non-donating participant");
        }
        if (total < 1 || total > 64) throw new IllegalArgumentException("Crafting quantity");
        long donatedUnits = owners.stream().filter(o -> !o.equals(recipient())).flatMap(o -> debits(o).stream()).mapToLong(Debit::count).sum();
        if (donationAfter() - donationBefore() != donatedUnits) throw new IllegalArgumentException("Donation cursor delta");
        if (!donated().keySet().equals(new HashSet<>(owners.stream().filter(o -> !o.equals(recipient())).toList())))
            throw new IllegalArgumentException("Donation roster differs");
        if (donated().values().stream().anyMatch(v -> v <= donationBefore() || v > donationAfter())
                || new HashSet<>(donated().values()).size() != donated().size()
                || (!donated().isEmpty() && Collections.max(donated().values()) != donationAfter()))
            throw new IllegalArgumentException("Donation cursor range");
        if (metricsAcknowledged() && (!committed() || !ownersAcknowledged())) throw new IllegalArgumentException("Premature metrics acknowledgement");
    }
    public record Debit(int slot, String item, String ingredient, int count) {}
    private static void text(String value, int max) {
        if (value.isBlank() || value.length() > max || value.chars().anyMatch(c -> c < 32))
            throw new IllegalArgumentException("Crafting text");
    }
    public UUID id() { return UUID.fromString(image.getStringOr("id", "")); }
    public UUID recipient() { return UUID.fromString(image.getStringOr("recipient", "")); }
    public long run() { return image.getLongOr("run", 0); }
    public long sequence() { return image.getLongOr("sequence", 0); }
    public long at() { return image.getLongOr("at", 0); }
    public long donationBefore() { return image.getLongOr("donation_before", 0); }
    public long donationAfter() { return image.getLongOr("donation_after", 0); }
    public String recipe() { return image.getStringOr("recipe", ""); }
    public String skill() { return image.getStringOr("skill", ""); }
    public String recipientName() { return image.getStringOr("recipient_name", ""); }
    public boolean committed() { return image.getBooleanOr("committed", false); }
    public boolean metricsAcknowledged() { return image.getBooleanOr("metrics_ack", false); }
    public List<UUID> owners() { return tag("owners").keySet().stream().map(UUID::fromString).sorted().toList(); }
    private CompoundTag owner(UUID owner) { return tag("owners").getCompound(owner.toString()).orElseThrow(); }
    public CompoundTag inventory(UUID owner, boolean after) { return owner(owner).getCompoundOrEmpty(after ? "after" : "before").copy(); }
    public List<Debit> debits(UUID owner) {
        var debits = owner(owner).getCompoundOrEmpty("debits");
        var result = new ArrayList<Debit>();
        for (String slot : debits.keySet()) {
            if (!slot.equals(Integer.toString(Integer.parseInt(slot)))) throw new IllegalArgumentException("Noncanonical debit slot");
            var row = debits.getCompound(slot).orElseThrow();
            if (!(row.get("count") instanceof IntTag)) throw new IllegalArgumentException("Missing debit count");
            result.add(new Debit(Integer.parseInt(slot), row.getStringOr("item", ""), row.getStringOr("ingredient", ""), row.getIntOr("count", 0)));
        }
        result.sort(Comparator.comparingInt(Debit::slot)); return List.copyOf(result);
    }
    public Map<UUID, Long> donated() {
        var result = new TreeMap<UUID, Long>(); var data = tag("donated");
        for (String key : data.keySet()) {
            if (!(data.get(key) instanceof LongTag)) throw new IllegalArgumentException("Donation rank shape");
            result.put(UUID.fromString(key), data.getLongOr(key, -1));
        }
        return Map.copyOf(result);
    }
    public boolean acknowledged(UUID owner) { return owner(owner).getBooleanOr("ack", false); }
    public boolean ownersAcknowledged() { return owners().stream().allMatch(this::acknowledged); }
    public boolean complete() { return ownersAcknowledged() && (!committed() || metricsAcknowledged()); }
    public CompoundTag image() { return image.copy(); }
    public CompoundTag tag(String key) { return image.getCompoundOrEmpty(key).copy(); }
    public SharedCraftPlan commit() {
        if (owners().stream().anyMatch(this::acknowledged)) throw new IllegalStateException("Cancelled craft cannot commit");
        var next = image(); next.putBoolean("committed", true); return new SharedCraftPlan(next);
    }
    public SharedCraftPlan acknowledge(UUID owner) {
        var next = image(); var owners = next.getCompoundOrEmpty("owners"); var state = owner(owner);
        state.putBoolean("ack", true); owners.put(owner.toString(), state); next.put("owners", owners); return new SharedCraftPlan(next);
    }
    public SharedCraftPlan acknowledgeMetrics() {
        var next = image(); next.putBoolean("metrics_ack", true); return new SharedCraftPlan(next);
    }
    public CompoundTag reservation(UUID owner) {
        owner(owner);
        var result = image(); result.putBoolean("committed", false); result.putBoolean("metrics_ack", false);
        var owners = result.getCompoundOrEmpty("owners");
        for (String key : owners.keySet()) owners.getCompoundOrEmpty(key).putBoolean("ack", false);
        result.put("owners", owners); result.putString("owner", owner.toString()); return result;
    }
    public CompoundTag receipt(UUID owner) {
        owner(owner); var result = new CompoundTag(); result.putInt("schema", 1);
        result.putString("id", id().toString()); result.putString("owner", owner.toString()); result.putLong("run", run());
        result.putLong("sequence", sequence()); result.putBoolean("committed", committed()); return result;
    }
    public boolean recoverable(UUID owner, CompoundTag custody, CompoundTag receipt) {
        if (receipt(owner).equals(receipt)) return custody.isEmpty();
        return !acknowledged(owner) && (custody.equals(reservation(owner)) || (!committed() && custody.isEmpty()));
    }
    public List<RunMetrics.ResourceEvent> events() {
        var result = new ArrayList<RunMetrics.ResourceEvent>();
        for (UUID owner : owners()) {
            var quantities = new TreeMap<String, Long>();
            for (var debit : debits(owner)) quantities.merge(debit.item(), (long)debit.count(), Long::sum);
            quantities.forEach((item, quantity) -> result.add(new RunMetrics.ResourceEvent(id(), owner, recipient(), skill(),
                    item, quantity, owner.equals(recipient()), at())));
        }
        return List.copyOf(result);
    }
}
