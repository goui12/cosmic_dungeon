package net.goui.cosmicdungeon.dungeon;

import com.mojang.serialization.Codec;
import net.goui.cosmicdungeon.dungeon.d1.D1StoredInventoryData;
import net.minecraft.nbt.*;
import java.nio.file.*;
import java.util.*;

/** Native codec/save-cut fixtures. These do not launch Minecraft or simulate a licensed TEST run. */
public final class InventoryHandoffChecks {
    private static int checks;
    private static final UUID OWNER = new UUID(0, 2801), OTHER = new UUID(0, 2802), TOKEN = new UUID(0, 2803);
    private static void check(boolean value, String why) { checks++; if (!value) throw new AssertionError(why); }
    private interface Work { void run() throws Exception; }
    private static void reject(Work work, String why) {
        boolean rejected = false; try { work.run(); } catch (Exception expected) { rejected = true; }
        check(rejected, why);
    }
    @SuppressWarnings("unchecked") private static <T> Codec<T> codec(Class<T> type) throws Exception {
        var field = type.getDeclaredField("CODEC"); field.setAccessible(true); return (Codec<T>) field.get(null);
    }
    private static <T> CompoundTag encode(Class<T> type, T data) throws Exception {
        return (CompoundTag) codec(type).encodeStart(NbtOps.INSTANCE, data).getOrThrow();
    }
    private static <T> T reload(Class<T> type, T data) throws Exception { return codec(type).parse(NbtOps.INSTANCE, encode(type, data)).getOrThrow(); }
    private static CompoundTag inventory(String name, int count) {
        var item = new CompoundTag(); item.putByte("Slot", (byte) 0); item.putString("id", "minecraft:paper"); item.putInt("count", count);
        var custom = new CompoundTag(); custom.putString("authored", name);
        var components = new CompoundTag(); components.put("minecraft:custom_data", custom); item.put("components", components);
        var items = new ListTag(); items.add(item); var result = new CompoundTag(); result.put("Items", items); return result;
    }
    private static CompoundTag empty() { return new CompoundTag(); }
    private static CompoundTag ownership(long run) { return ChopOwnershipData.entryImage(new ChopOwnershipData.Entry(TOKEN.toString(), run, false)); }
    private static InventoryHandoffPlan cleanup(String reason, boolean outside, boolean online) {
        var escrow = new DungeonInventoryEscrowData.Entry(28, OWNER, inventory("dungeon", 8), inventory("outside", 17), outside);
        return InventoryHandoffPlan.cleanup(OWNER, 28, reason, inventory("original", 11), escrow, empty(), ownership(28),
                online ? inventory(outside ? "current village" : "current dungeon", 23) : empty(), online);
    }
    private static void outcomes() throws Exception {
        for (boolean online : List.of(false, true)) for (boolean outside : List.of(false, true))
            for (String reason : List.of("COMPLETED", "ABANDONED", "KICKED", "LINK_DEAD")) {
                var p = cleanup(reason, outside, online); boolean success = reason.equals("COMPLETED");
                check(p.keep() == (success || outside), "Current inventory policy " + reason + outside + online);
                check(p.exactBefore() == online, "Offline cleanup uses its saved outcome without inventing a live source");
                check(p.tag("after").equals(inventory("outside", 17)), "Failure replacement preserves outside components/count");
                check(p.tag("stored").equals(success ? inventory(outside ? "dungeon" : "outside", outside ? 8 : 17) : empty()), "Success stores exactly the other inventory");
                var owner = ChopOwnershipData.Entry.CODEC.parse(NbtOps.INSTANCE, p.tag("ownership_after")).getOrThrow();
                check(owner.deliver() && owner.runId() == 0 && !owner.token().equals(TOKEN.toString()), "One new Raw entitlement invalidates the old run token");
                check(InventoryHandoffPlan.CODEC.parse(NbtOps.INSTANCE, p.image()).getOrThrow().image().equals(p.image()), "Exact decision round trip");
            }
        var fresh = InventoryHandoffPlan.cleanup(OWNER, 28, "ABANDONED", inventory("original", 11), null, empty(), empty(), empty(), false);
        check(!fresh.keep() && fresh.tag("after").equals(inventory("original", 11)), "No Chop escrow restores original on failure");
        var successful = InventoryHandoffPlan.cleanup(OWNER, 28, "COMPLETED", inventory("original", 11), null, empty(), empty(), empty(), false);
        check(successful.keep() && successful.tag("stored").equals(inventory("original", 11)), "Success without travel preserves original stash");
        var starting = empty(); starting.put("inventory", inventory("full pre-entry including Raw", 1)); starting.put("ownership", ownership(0));
        for (long binding : List.of(0L, 28L)) {
            var p = InventoryHandoffPlan.cleanup(OWNER, 28, "ABANDONED", inventory("regular snapshot excludes Chop", 10), null,
                    starting, ownership(binding), inventory("partly entered", 1), true);
            check(p.reason().equals("STARTUP_ABORT") && !p.keep(), "Incomplete entry always rolls back");
            check(p.tag("after").equals(starting.getCompoundOrEmpty("inventory")), "Startup restores full pre-entry inventory");
            check(p.tag("ownership_after").equals(ownership(0)), "Startup restores exact old token; no extra Raw entitlement");
        }
        reject(() -> InventoryHandoffPlan.cleanup(OWNER, 28, "ABANDONED", empty(), null, starting, ownership(99), empty(), false), "Foreign startup ownership held");
        var legacy = empty(); legacy.put("inventory", inventory("legacy", 1));
        reject(() -> InventoryHandoffPlan.cleanup(OWNER, 28, "ABANDONED", empty(), null, legacy, empty(), empty(), false), "Missing startup entitlement held");
    }
    private static void codecs() throws Exception {
        var d = codec(PendingDungeonRecoveryData.class).parse(NbtOps.INSTANCE, empty()).getOrThrow();
        var plan = cleanup("COMPLETED", false, true); d.begin(plan);
        reject(() -> d.begin(cleanup("ABANDONED", false, true)), "Pending decision cannot be overwritten");
        reject(() -> d.acknowledge(plan), "Unverified world handoff cannot acknowledge");
        var wrong = cleanup("COMPLETED", false, true);
        reject(() -> d.worldReady(wrong), "Wrong operation cannot finalize");
        d.worldReady(plan); var restored = reload(PendingDungeonRecoveryData.class, d);
        check(restored.handoff(OWNER).worldReady() && restored.durable(OWNER, 28), "Offline durable handoff survives restart");
        restored.acknowledge(plan); restored = reload(PendingDungeonRecoveryData.class, restored);
        check(restored.handoff(OWNER) == null && restored.completed(OWNER) == 28, "Receipt watermark survives journal removal");
        var completed = restored;
        reject(() -> completed.begin(plan), "Repeated cleanup cannot replay an acknowledged run");
        var future = plan.image(); future.putInt("version", 2);
        reject(() -> new InventoryHandoffPlan(future), "Future version held");
        var missing = plan.image(); missing.remove("after");
        reject(() -> new InventoryHandoffPlan(missing), "Missing replacement image held");
        var badFlag = plan.image(); badFlag.putString("keep", "yes");
        reject(() -> new InventoryHandoffPlan(badFlag), "Malformed outcome flag held");
        var foreign = plan.image(); foreign.putString("owner", OTHER.toString());
        reject(() -> new InventoryHandoffPlan(foreign), "Foreign escrow owner held");
        check(plan.canApply(empty(), 0, plan.tag("before")), "Fresh cleanup accepts its exact source");
        check(!plan.canApply(empty(), 28, plan.tag("before")), "Stale world cleanup cannot overwrite acknowledged owner history");
        check(!plan.canApply(empty(), 0, inventory("unexpected items", 5)), "Changed owner source held");
        check(plan.canApply(plan.receipt(), 28, inventory("later player state", 1)), "Matching owner receipt suppresses repeated delivery");
        var sameId = plan.image(); sameId.put("after", inventory("tampered", 1));
        reject(() -> d.worldReady(new InventoryHandoffPlan(sameId)), "Same UUID with changed decision cannot finalize");
        var receipt = plan.receipt(); receipt.putLong("run", 99);
        check(!plan.receipted(receipt), "Different run receipt cannot suppress recovery");
        var copy = plan.image(); copy.getCompoundOrEmpty("before").putString("mutated", "yes");
        check(!plan.tag("before").contains("mutated"), "Decision image is immutable");
        var legacyRecord = new PendingDungeonRecoveryData.RecoveryRecord(OWNER, 27, "dungeon_1", "COMPLETED", inventory("old", 3));
        var legacy = codec(PendingDungeonRecoveryData.class).parse(NbtOps.INSTANCE, empty()).getOrThrow(); legacy.put(legacyRecord);
        var migrated = reload(PendingDungeonRecoveryData.class, legacy);
        check(migrated.get(OWNER).orElseThrow().inventoryNbt().equals(legacyRecord.inventoryNbt()) && migrated.handoff(OWNER) == null,
                "Legacy unreceipted records preserved without automatic replay");
        reject(() -> migrated.begin(plan), "Legacy evidence prevents a new conflicting decision");
        var invalid = encode(PendingDungeonRecoveryData.class, d);
        invalid.getCompoundOrEmpty("handoffs").put(OTHER.toString(), plan.image());
        reject(() -> codec(PendingDungeonRecoveryData.class).parse(NbtOps.INSTANCE, invalid).getOrThrow(), "Mismatched journal map key held");
    }
    private static void stored() throws Exception {
        var d = codec(D1StoredInventoryData.class).parse(NbtOps.INSTANCE, empty()).getOrThrow();
        d.stash(28, OWNER, inventory("other", 64)); d.stash(29, OWNER, inventory("later", 5)); d.stash(1, OTHER, inventory("foreign", 2));
        check(d.first(OWNER).equals(OWNER + "|28") && d.first(OTHER).equals(OTHER + "|1"), "Claim selects oldest run for its owner");
        var copy = d.image(OWNER + "|28"); copy.putString("changed", "yes");
        check(!d.image(OWNER + "|28").contains("changed"), "Stored image defensively copied");
        d.applyClaim(OWNER + "|28", inventory("other", 64), inventory("other", 40));
        d = reload(D1StoredInventoryData.class, d);
        check(d.image(OWNER + "|28").equals(inventory("other", 40)), "Partial remainder preserves components and count");
        d.applyClaim(OWNER + "|28", inventory("other", 64), inventory("other", 40));
        check(d.image(OWNER + "|28").equals(inventory("other", 40)), "Repeated claim remainder is idempotent");
        var data = d;
        reject(() -> data.applyClaim(OWNER + "|28", inventory("wrong", 2), empty()), "Unexpected remainder held");
        data.applyClaim(OWNER + "|28", inventory("other", 40), empty());
        check(data.first(OWNER).equals(OWNER + "|29") && data.hasPending(OTHER), "Retiring one claim preserves later/foreign belongings");
        reject(() -> data.stash(28, OWNER, inventory("other", 64)), "Retired stash cannot be resurrected");
        reject(() -> data.stash(29, OWNER, inventory("changed", 5)), "Conflicting success stash held");
        reject(() -> D1StoredInventoryData.run(OWNER + "|0"), "Invalid legacy stash run held");
        var legacy = empty(); var pending = empty(); pending.put(OWNER + "|10", inventory("old enchantments", 3)); legacy.put("pending", pending);
        var migrated = codec(D1StoredInventoryData.class).parse(NbtOps.INSTANCE, legacy).getOrThrow();
        check(migrated.image(OWNER + "|10").equals(inventory("old enchantments", 3)), "Old pending-only stored inventory loads losslessly");
    }
    private static void interruptions(String kind, boolean outside) throws Exception {
        var before = inventory("current", 5); var after = inventory("received", 12); var storedBefore = inventory("other", 64); var storedAfter = inventory("other", 57);
        var plan = kind.equals("claim") ? InventoryHandoffPlan.create(OWNER, 28, "claim", "", false, true, before, after,
                storedBefore, empty(), empty(), empty(), OWNER + "|28", storedAfter) : cleanup(kind, outside, true);
        var expected = plan.keep() ? plan.tag("before") : plan.tag("after");
        Path temp = Files.createTempFile("inventory-handoff-cuts-", ".dat");
        try {
            // A decision, independent world side effects, owner write, remainder/ack can reach disk separately.
            for (int cut = 0; cut <= 10; cut++) {
                var journal = codec(PendingDungeonRecoveryData.class).parse(NbtOps.INSTANCE, empty()).getOrThrow(); journal.begin(plan);
                var player = empty(); player.put("inventory", plan.tag("before"));
                boolean savedOwner = cut >= 7;
                if (savedOwner) { player.put("inventory", expected); player.put("receipt", plan.receipt()); }
                if (cut >= 5 && !kind.equals("claim") || cut >= 9) journal.worldReady(plan);
                if (cut == 10) journal.acknowledge(plan);
                var disk = empty(); disk.put("journal", encode(PendingDungeonRecoveryData.class, journal)); disk.put("player", player);
                NbtIo.writeCompressed(disk, temp);
                disk = NbtIo.readCompressed(temp, NbtAccounter.unlimitedHeap());
                var loaded = codec(PendingDungeonRecoveryData.class).parse(NbtOps.INSTANCE, disk.get("journal")).getOrThrow();
                player = disk.getCompoundOrEmpty("player"); var pending = loaded.handoff(OWNER); int deliveries = savedOwner ? 1 : 0;
                if (pending != null) {
                    if (!pending.receipted(player.getCompoundOrEmpty("receipt"))) {
                        check(player.getCompoundOrEmpty("inventory").equals(pending.tag("before")), "Unreceipted native owner retains frozen source");
                        player.put("inventory", expected); player.put("receipt", pending.receipt()); deliveries++;
                    }
                    loaded.worldReady(pending); loaded.acknowledge(pending);
                }
                check(deliveries == 1, kind + " cut " + cut + " owner receives exactly once");
                check(player.getCompoundOrEmpty("inventory").equals(expected), kind + " exact outcome after recovery");
                check(reload(PendingDungeonRecoveryData.class, loaded).handoff(OWNER) == null, kind + " acknowledgement survives restart");
                if (!kind.equals("claim")) check(loaded.completed(OWNER) == 28, "Cleanup replay prevented after player receipt");
            }
        } finally { Files.deleteIfExists(temp); }
    }
    private static void startup() throws Exception {
        var run = new DungeonRunRegistryData.RunRecord(28, "dungeon_1", "minecraft:overworld", 0,
                List.of("cosmicdungeon:test_slot"), 1, "ACTIVE", "", 1, List.of(OWNER, OTHER), List.of(),
                List.of(new DungeonPlayerRunSnapshot(OWNER, inventory("owner", 1)), new DungeonPlayerRunSnapshot(OTHER, inventory("other", 2))));
        var root = empty(); root.putLong("next_run_id", 29); var runs = new ListTag();
        runs.add(DungeonRunRegistryData.RunRecord.CODEC.encodeStart(NbtOps.INSTANCE, run).getOrThrow()); root.put("runs", runs);
        var d = codec(DungeonRunRegistryData.class).parse(NbtOps.INSTANCE, root).getOrThrow();
        check(!d.starting(28), "Legacy active runs are not treated as unfinished entry");
        var owners = empty();
        for (UUID owner : List.of(OWNER, OTHER)) {
            var image = empty(); image.put("inventory", inventory(owner.toString(), 3)); image.put("ownership", empty()); owners.put(owner.toString(), image);
        }
        d.prepareStartup(28, owners); d = reload(DungeonRunRegistryData.class, d);
        check(d.starting(28) && d.startupImage(28, OWNER).equals(owners.getCompoundOrEmpty(OWNER.toString())), "Full startup roster survives native codec round trip");
        check(d.isSlotOccupied(1), "Unfinished startup reserves its instance");
        var saved = d;
        reject(() -> saved.removePlayer(28, OWNER), "Partial member removal cannot invalidate startup rollback");
        var wrong = root.copy(); var startup = empty(); startup.put("28", empty()); wrong.put("startup", startup);
        reject(() -> codec(DungeonRunRegistryData.class).parse(NbtOps.INSTANCE, wrong).getOrThrow(), "Missing startup participants fail closed");
        var classes = List.of("bogatyr", "dragoon", "judicator", "pyroclast", "theurgist", "venefex");
        for (int size = 3; size <= 6; size++) {
            final int occupied = size;
            var plan = DungeonStartupSchematicPlan.buildPlan(classes.subList(0, size));
            check(plan.requests().size() == 36, "Every D1 party size keeps all 36 authored paste operations");
            for (int slot = 1; slot <= 6; slot++) {
                final int target = slot;
                var entries = plan.requests().stream().filter(p -> p.logicalSlot() == target).toList();
                check(entries.size() == 6, "Each logical slot has entry plus five chest pastes");
                check(entries.stream().allMatch(p -> p.classId().equals(target <= occupied ? classes.get(target - 1) : "blankslot")),
                        "Class/blank mapping stays with locked roster order");
            }
        }
    }
    public static void main(String[] args) throws Exception {
        outcomes(); codecs(); stored(); startup();
        var saved = empty(); saved.put("respawn", inventory("respawn fixture", 1));
        check(!net.goui.cosmicdungeon.transaction.PlayerSaveProof.matchesLocation(saved, empty()),
                "Inventory/location readback also verifies native respawn binding");
        check(net.goui.cosmicdungeon.transaction.PlayerSaveProof.matchesLocation(saved, saved.copy()),
                "Unchanged native respawn passes exact readback");
        for (String kind : List.of("COMPLETED", "ABANDONED", "KICKED", "LINK_DEAD", "claim"))
            for (boolean outside : List.of(false, true)) interruptions(kind, outside);
        System.out.println("Inventory handoff checks passed: " + checks);
    }
}
