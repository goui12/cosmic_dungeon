package net.goui.cosmicdungeon.dungeon.d1;

import com.mojang.serialization.Codec;
import net.goui.cosmicdungeon.faction.*;
import net.goui.cosmicdungeon.progression.PlayerProgressionData;
import net.minecraft.nbt.*;
import java.nio.file.*;
import java.util.*;

/** Production decisions/codecs and native compressed save cuts; no Minecraft launch or player bootstrap. */
public final class WatsonOutcomeChecks {
    private static int checks;
    private static final UUID A = new UUID(0, 2901), B = new UUID(0, 2902), C = new UUID(0, 2903);
    private static final List<UUID> OWNERS = List.of(A, B, C);
    private static void check(boolean ok, String why) { checks++; if (!ok) throw new AssertionError(why); }
    private interface Work { void run() throws Exception; }
    private static void reject(Work work, String why) {
        boolean rejected = false; try { work.run(); } catch (Exception expected) { rejected = true; }
        check(rejected, why);
    }
    @SuppressWarnings("unchecked") private static <T> Codec<T> codec(Class<T> type) throws Exception {
        var field = type.getDeclaredField("CODEC"); field.setAccessible(true); return (Codec<T>) field.get(null);
    }
    private static <T> T empty(Class<T> type) throws Exception { return codec(type).parse(NbtOps.INSTANCE, new CompoundTag()).getOrThrow(); }
    private static <T> CompoundTag encode(Class<T> type, T value) throws Exception {
        return (CompoundTag) codec(type).encodeStart(NbtOps.INSTANCE, value).getOrThrow();
    }
    private static <T> T reload(Class<T> type, T value) throws Exception { return codec(type).parse(NbtOps.INSTANCE, encode(type, value)).getOrThrow(); }
    private static CompoundTag item(int slot, String id, int count) {
        var item = new CompoundTag(); item.putByte("Slot", (byte) slot); item.putString("id", id); item.putInt("count", count);
        var custom = new CompoundTag(); custom.putString("authored", "preserve counts/components");
        var components = new CompoundTag(); components.put("minecraft:custom_data", custom); item.put("components", components);
        return item;
    }
    private static CompoundTag inventory(int member, int blooms) {
        var list = new ListTag();
        for (int i = 0; i < 6; i++) if (i < blooms && i % 3 == member) list.add(item(i, WatsonOutcome.BLOOMS.get(i), i == 0 ? 3 : 1));
        // Duplicate registered ID and a similarly named vanilla flower are not distinct objective items.
        if (member == 1) list.add(item(6, WatsonOutcome.BLOOMS.getFirst(), 4));
        list.add(item(7, "minecraft:torchflower", 12)); list.add(item(8, "minecraft:paper", 64));
        var result = new CompoundTag(); result.put("Items", list); return result;
    }
    private static WatsonOutcome plan(int blooms, int collected) {
        var inventories = new LinkedHashMap<UUID, CompoundTag>(); var taxes = new LinkedHashMap<UUID, CompoundTag>();
        var counters = new LinkedHashMap<UUID, int[]>();
        for (int i = 0; i < OWNERS.size(); i++) {
            UUID owner = OWNERS.get(i); inventories.put(owner, inventory(i, blooms));
            var tax = new CompoundTag();
            if (i == 0) tax.putLong("camp_run", 28);
            if (i == 2) { tax.putLong("camp_run", 30); tax.putString("extra", "keep"); }
            taxes.put(owner, tax); counters.put(owner, new int[]{7 + i, collected, Math.max(0, collected - i)});
        }
        return WatsonOutcome.create(29, OWNERS, inventories, taxes, counters, 2);
    }
    private static void decisions() throws Exception {
        for (int bloomCount = 0; bloomCount <= 6; bloomCount++) for (int collected : new int[]{0, 1, 5, 6, Integer.MAX_VALUE}) {
            var p = plan(bloomCount, collected);
            // The extra copy on B supplies Bloom 0 even when none were assigned to A.
            check(p.success() == (bloomCount == 6), "Only six distinct registered Blooms succeed");
            check(WatsonOutcome.CODEC.parse(NbtOps.INSTANCE, p.image()).getOrThrow().image().equals(p.image()), "Exact outcome round trip");
            for (UUID owner : OWNERS) {
                var m = p.member(owner);
                check(m.getIntOr("rounded", -1) == D1ObjectiveRules.roundedLesserBlooms(collected), "Rounding frozen per owner");
                check(m.getLongOr("faction_bonus", -1) == 2L * (D1ObjectiveRules.roundedLesserBlooms(collected) - collected), "Faction modifier frozen once");
                check(p.canApply(owner, new CompoundTag(), 28, m.getCompoundOrEmpty("before"), m.getCompoundOrEmpty("tax_before")), "Exact source accepted");
                check(!p.canApply(owner, new CompoundTag(), 29, m.getCompoundOrEmpty("before"), m.getCompoundOrEmpty("tax_before")), "Later cleanup blocks stale hand-in");
                var changed = m.getCompoundOrEmpty("before").copy(); changed.putString("unexpected", "inventory");
                check(!p.canApply(owner, new CompoundTag(), 0, changed, m.getCompoundOrEmpty("tax_before")), "Unknown player inventory held");
                check(p.canApply(owner, p.receipt(owner).image(), 30, changed, new CompoundTag()), "Owner receipt suppresses replacement after later play");
            }
            check(p.member(A).getCompoundOrEmpty("tax_after").getLongOr("success_run", 0) == (p.success() ? 29 : 0), "Tax requires earlier personal camp and success");
            check(!p.member(B).getCompoundOrEmpty("tax_after").contains("success_run"), "Peer camp never grants Tax");
            check(!p.member(C).getCompoundOrEmpty("tax_after").contains("success_run"), "Future camp cannot backfill old success");
            reject(p::markReady, "Cannot ready before every owner save");
        }
        var p = plan(6, 6); var afterA = p.member(A).getCompoundOrEmpty("after").getListOrEmpty("Items");
        check(afterA.getCompoundOrEmpty(0).getIntOr("count", 0) == 2, "Only one unit removed from first component-bearing stack");
        var afterB = p.member(B).getCompoundOrEmpty("after").getListOrEmpty("Items");
        check(afterB.getCompoundOrEmpty(0).getIntOr("count", 0) == 4, "Duplicate registered Bloom stack remains untouched");
        check(afterB.getCompoundOrEmpty(0).getCompoundOrEmpty("components").equals(item(0, "", 1).getCompoundOrEmpty("components")), "Custom components remain exact");
        var changed = p.image(); changed.putInt("version", 2); reject(() -> new WatsonOutcome(changed), "Unknown future format held");
        var wrongSuccess = p.image(); wrongSuccess.putBoolean("success", false); reject(() -> new WatsonOutcome(wrongSuccess), "Invented failure rejected");
        var missing = p.image(); missing.getCompoundOrEmpty("members").getCompoundOrEmpty(A.toString()).remove("after");
        reject(() -> new WatsonOutcome(missing), "Incomplete inventory evidence held");
        var tampered = p.image(); tampered.getCompoundOrEmpty("members").getCompoundOrEmpty(A.toString()).put("after", inventory(0, 6));
        reject(() -> new WatsonOutcome(tampered), "Missing physical deduction rejected");
        var duplicate = p.image(); duplicate.getListOrEmpty("owners").add(StringTag.valueOf(A.toString()));
        reject(() -> new WatsonOutcome(duplicate), "Duplicate roster held");
        var foreignAck = p.image(); foreignAck.getListOrEmpty("acknowledged").add(StringTag.valueOf(new UUID(0, 99).toString()));
        reject(() -> new WatsonOutcome(foreignAck), "Foreign receipt cannot acknowledge");
        var corruptCount = p.image(); corruptCount.getCompoundOrEmpty("members").getCompoundOrEmpty(A.toString()).putInt("lifetime_lesser", 7);
        reject(() -> new WatsonOutcome(corruptCount), "Lifetime cannot exceed actual run collection");
        var immutable = p.member(A); immutable.putInt("kills", 999);
        check(p.member(A).getIntOr("kills", 0) == 7, "Member image is immutable");
        reject(() -> p.receipt(new UUID(0, 99)), "Foreign player gets no receipt");
    }
    private static void fullRoster() throws Exception {
        var owners = new ArrayList<UUID>(); var inventories = new LinkedHashMap<UUID, CompoundTag>();
        var taxes = new LinkedHashMap<UUID, CompoundTag>(); var counters = new LinkedHashMap<UUID, int[]>();
        for (int i = 0; i < 6; i++) {
            UUID owner = new UUID(0, 2950 + i); owners.add(owner);
            var inventory = new CompoundTag(); var items = new ListTag();
            for (int slot = 0; slot < 41; slot++)
                items.add(item(slot, slot == 40 ? WatsonOutcome.BLOOMS.get(i) : "minecraft:stone", slot == 40 ? 1 : 64));
            inventory.put("Items", items); inventories.put(owner, inventory);
            var tax = new CompoundTag(); tax.putLong("camp_run", 1); tax.putLong("success_run", 2);
            tax.putString("receipt", "already paid"); taxes.put(owner, tax);
            counters.put(owner, new int[]{1, 0, 0});
        }
        var p = WatsonOutcome.create(29, owners, inventories, taxes, counters, 100);
        check(p.success() && p.owners().equals(owners), "All six owners and offhand Blooms included");
        check(!p.permitsCleanup("COMPLETED") && !p.permitsCleanup("ABANDONED"), "Neither exit reason can bypass input receipts");
        for (UUID owner : owners) {
            var m = p.member(owner); var after = m.getCompoundOrEmpty("after").getListOrEmpty("Items");
            check(after.size() == 40 && after.getCompoundOrEmpty(39).getIntOr("count", 0) == 64, "Full inventory loses only its exact offhand Bloom");
            check(m.getCompoundOrEmpty("tax_after").equals(taxes.get(owner)), "Existing Tax payment and eligibility survive");
            p = p.acknowledge(owner);
            check(!p.canApply(owner, new CompoundTag(), 0, m.getCompoundOrEmpty("before"), m.getCompoundOrEmpty("tax_before")), "Acknowledged input with rolled-back player file held");
        }
        p = p.markReady();
        check(p.permitsCleanup("COMPLETED"), "Settled success hands off to success cleanup");
        for (String reason : List.of("ABANDONED", "KICKED", "LINK_DEAD", "MANUAL"))
            check(!p.permitsCleanup(reason), "Success cannot be replaced by " + reason);
        var failed = plan(5, 1); for (UUID owner : OWNERS) failed = failed.acknowledge(owner);
        failed = failed.markReady();
        check(failed.permitsCleanup("ABANDONED") && !failed.permitsCleanup("COMPLETED"), "Settled failure cannot award success");
        var badTax = plan(6, 1); var source = badTax.member(A); var changed = source.getCompoundOrEmpty("tax_before").copy();
        changed.putLong("camp_run", 1);
        check(!badTax.canApply(A, new CompoundTag(), 0, source.getCompoundOrEmpty("before"), changed), "Unexpected Tax proof held before input overwrite");
    }
    private static void journals() throws Exception {
        var d = empty(D1RunData.class); var p = plan(6, 1);
        check(d.outcomeFor(A) == null && d.outcomeRuns().isEmpty(), "Old objective shape has no pending outcome");
        d.beginOutcome(p); check(d.sealed(29) && d.outcomeFor(A).id().equals(p.id()), "One outcome locks the roster");
        var mismatched = encode(D1RunData.class, d);
        var badMarker = new ListTag(); badMarker.add(StringTag.valueOf("failure"));
        mismatched.getListOrEmpty("runs").getCompoundOrEmpty(0).getCompoundOrEmpty("unique").put("watson_outcome", badMarker);
        reject(() -> codec(D1RunData.class).parse(NbtOps.INSTANCE, mismatched).getOrThrow(), "Conflicting legacy marker cannot override the decision");
        var journal = d; reject(() -> journal.beginOutcome(plan(6, 1)), "Outcome cannot be replaced");
        d.clearRun(29); check(d.outcome(29) != null && d.sealed(29), "Ordinary objective reset cannot erase pending outcome");
        reject(() -> journal.readyOutcome(p), "Missing input acknowledgement blocks readiness");
        for (UUID owner : OWNERS) d.acknowledgeOutcome(p, owner);
        d = reload(D1RunData.class, d); check(d.outcome(29).acknowledged().size() == 3, "Owner acknowledgements survive native codec");
        d.readyOutcome(p); check(d.outcome(29).ready(), "All receipts allow readiness");
        var originalReady = d.outcome(29).image();
        d.acknowledgeOutcome(p, A); check(d.outcome(29).image().equals(originalReady), "Repeated acknowledgement does not regress readiness");
        var altered = p.image(); altered.putString("id", UUID.randomUUID().toString());
        var active = d; reject(() -> active.acknowledgeOutcome(new WatsonOutcome(altered), A), "Another decision cannot acknowledge");
        d.retireOutcome(29); d = reload(D1RunData.class, d);
        check(d.outcomeRuns().isEmpty() && d.outcomeFor(A) == null && d.lastWatson(A).equals(p.receipt(A)), "Retirement keeps bounded per-owner cursors");
        var retired = d; reject(() -> retired.beginOutcome(p), "Retired run cannot replay its Bloom deductions");
        var oldFlag = empty(D1RunData.class); oldFlag.setValue(29, "watson_outcome", "success");
        check(oldFlag.sealed(29) && oldFlag.outcome(29) == null, "Legacy outcome remains visibly held without invented input proof");
        reject(() -> oldFlag.beginOutcome(p), "Legacy outcome cannot be auto-adopted");
        var invalid = encode(D1RunData.class, d);
        invalid.getCompoundOrEmpty("watson_receipts").put(B.toString(), p.receipt(A).image());
        reject(() -> codec(D1RunData.class).parse(NbtOps.INSTANCE, invalid).getOrThrow(), "Foreign cursor map key rejected");
    }
    private static void projections() throws Exception {
        var p = plan(6, 6); var receipt = p.receipt(A);
        var lifetime = empty(D1LifetimeData.class); lifetime.recordLesserBlooms(A, 9); lifetime.complete(A, 28, 3);
        var progression = empty(PlayerProgressionData.class); progression.setCavernResidue(A, 47); progression.setLesserBlooms(A, 5);
        var factions = empty(PlayerFactionData.class); factions.setValue(A, FactionDefinitions.JHW_ID, 123); factions.setValue(A, FactionDefinitions.NPC_ID, 98);
        for (int repeat = 0; repeat < 3; repeat++) {
            lifetime.applyWatson(receipt, 7, 6); progression.applyWatson(receipt, 10); factions.applyWatson(receipt, 8);
            lifetime = reload(D1LifetimeData.class, lifetime); progression = reload(PlayerProgressionData.class, progression); factions = reload(PlayerFactionData.class, factions);
            check(lifetime.totals(A).equals(new D1LifetimeData.Totals(12, 15, 2, 29, 10)), "Actual successful statistics credited exactly once");
            check(progression.getLesserBlooms(A) == 15 && progression.getNpcUnlockTierD1(A) == 3 && progression.isVillageAccessUnlocked(A), "Rounded contributions/unlocks credited once");
            check(progression.getCavernResidue(A) == 47, "Unrelated progression survives");
            check(factions.getValue(A, FactionDefinitions.NPC_ID) == 100 && factions.getValue(A, FactionDefinitions.JHW_ID) == 123, "NPC clamp leaves legacy JHW faction unchanged");
        }
        var lt = lifetime; var pg = progression; var fa = factions;
        var wrong = new WatsonReceipt(A, 29, UUID.randomUUID(), true); var stale = new WatsonReceipt(A, 28, UUID.randomUUID(), true);
        for (var r : List.of(wrong, stale)) {
            reject(() -> lt.applyWatson(r, 9, 9), "Wrong/stale lifetime receipt held");
            reject(() -> pg.applyWatson(r, 10), "Wrong/stale progression receipt held");
            reject(() -> fa.applyWatson(r, 4), "Wrong/stale faction receipt held");
        }
        var fail = new WatsonReceipt(A, 30, UUID.randomUUID(), false);
        lifetime.applyWatson(fail, 999, 999); progression.applyWatson(fail, 999); factions.applyWatson(fail, 400);
        check(lifetime.totals(A).equals(new D1LifetimeData.Totals(12, 15, 2, 29, 10)) && progression.getLesserBlooms(A) == 15, "Failure grants no statistics or unlock contribution");
        check(factions.getValue(A, FactionDefinitions.NPC_ID) == 100, "Failure grants no rounding faction");
        var oldCompletion = empty(D1LifetimeData.class); oldCompletion.complete(A, 29);
        reject(() -> oldCompletion.applyWatson(receipt, 7, 6), "Legacy already-completed run needs review instead of double credit");
        var huge = new CompoundTag(); var players = new CompoundTag(); var values = new CompoundTag();
        for (String key : List.of("spectral_blooms", "lesser_blooms", "completions", "successful_kills")) values.putLong(key, Long.MAX_VALUE - 1);
        players.put(A.toString(), values); huge.put("players", players);
        var saturated = codec(D1LifetimeData.class).parse(NbtOps.INSTANCE, huge).getOrThrow(); saturated.applyWatson(receipt, 7, 6);
        check(saturated.totals(A).spectralBlooms() == Long.MAX_VALUE && saturated.totals(A).lesserBlooms() == Long.MAX_VALUE
                && saturated.totals(A).successfulKills() == Long.MAX_VALUE && saturated.totals(A).completions() == Long.MAX_VALUE, "Lifetime arithmetic saturates");
        var maxProgress = empty(PlayerProgressionData.class); maxProgress.setLesserBlooms(A, Integer.MAX_VALUE - 1);
        maxProgress.applyWatson(receipt, Integer.MAX_VALUE); check(maxProgress.getLesserBlooms(A) == Integer.MAX_VALUE, "Progression addition never overflows");
        var unrelated = new CompoundTag(); unrelated.put("watson_receipts", new CompoundTag());
        unrelated.getCompoundOrEmpty("watson_receipts").put(B.toString(), receipt.image());
        for (Class<?> type : List.of(D1LifetimeData.class, PlayerProgressionData.class, PlayerFactionData.class))
            reject(() -> codec(type).parse(NbtOps.INSTANCE, unrelated).getOrThrow(), "Projection rejects foreign receipt owner " + type.getSimpleName());
    }
    private static final class Cut extends RuntimeException {}
    private static final class Disk implements AutoCloseable {
        final Path path = Files.createTempDirectory("cosmic-watson-");
        int cut, writes;
        Disk(int cut) throws Exception { this.cut = cut; }
        CompoundTag read(String key) throws Exception { return NbtIo.readCompressed(path.resolve(key + ".dat"), NbtAccounter.create(4 * 1024 * 1024)); }
        void initial(String key, CompoundTag tag) throws Exception { NbtIo.writeCompressed(tag, path.resolve(key + ".dat")); }
        void save(String key, CompoundTag tag) throws Exception { initial(key, tag); if (++writes == cut) throw new Cut(); }
        <T> T load(String key, Class<T> type) throws Exception { return codec(type).parse(NbtOps.INSTANCE, read(key)).getOrThrow(); }
        <T> void save(String key, Class<T> type, T value) throws Exception { save(key, encode(type, value)); }
        public void close() throws Exception { try (var paths = Files.walk(path)) { for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(p); } }
    }
    private static void settle(Disk disk, List<String> order) throws Exception {
        var data = disk.load("journal", D1RunData.class); var p = data.outcome(29); if (p == null) return;
        if (!p.ready()) {
            for (UUID owner : p.owners()) if (!p.acknowledged().contains(owner)) {
                var player = disk.read(owner.toString()); var m = p.member(owner); var receipt = player.getCompoundOrEmpty("receipt");
                check(p.canApply(owner, receipt, 0, player.getCompoundOrEmpty("inventory"), player.getCompoundOrEmpty("tax")), "Crash recovery accepts exact saved input or its receipt");
                if (!p.receipt(owner).matches(receipt)) {
                    player.put("inventory", m.getCompoundOrEmpty("after")); player.put("tax", m.getCompoundOrEmpty("tax_after"));
                    player.put("receipt", p.receipt(owner).image());
                }
                disk.save(owner.toString(), player);
                data.acknowledgeOutcome(p, owner); disk.save("journal", D1RunData.class, data); p = data.outcome(29);
            }
            var lt = disk.load("lifetime", D1LifetimeData.class); var pg = disk.load("progression", PlayerProgressionData.class); var fa = disk.load("faction", PlayerFactionData.class);
            for (UUID owner : p.owners()) {
                var m = p.member(owner); var receipt = p.receipt(owner);
                lt.applyWatson(receipt, m.getIntOr("kills", 0), m.getIntOr("lifetime_lesser", 0));
                pg.applyWatson(receipt, m.getIntOr("rounded", 0)); fa.applyWatson(receipt, m.getLongOr("faction_bonus", 0));
            }
            // Native storage can finish independent files in any order.
            for (String file : order) switch (file) {
                case "lifetime" -> disk.save(file, D1LifetimeData.class, lt);
                case "progression" -> disk.save(file, PlayerProgressionData.class, pg);
                case "faction" -> disk.save(file, PlayerFactionData.class, fa);
                default -> throw new AssertionError(file);
            }
            data.readyOutcome(p); disk.save("journal", D1RunData.class, data);
        }
        data.clearRun(29); disk.save("journal", D1RunData.class, data);
        data.retireOutcome(29); disk.save("journal", D1RunData.class, data);
    }
    private static void interruptions() throws Exception {
        var orders = List.of(List.of("lifetime", "progression", "faction"), List.of("lifetime", "faction", "progression"),
                List.of("progression", "lifetime", "faction"), List.of("progression", "faction", "lifetime"),
                List.of("faction", "lifetime", "progression"), List.of("faction", "progression", "lifetime"));
        for (boolean success : List.of(false, true)) for (var order : orders) for (int cut = 0; cut <= 13; cut++) {
            var p = plan(success ? 6 : 5, 6);
            try (var disk = new Disk(cut)) {
                var journal = empty(D1RunData.class);
                disk.initial("journal", encode(D1RunData.class, journal));
                disk.initial("lifetime", encode(D1LifetimeData.class, empty(D1LifetimeData.class)));
                disk.initial("progression", encode(PlayerProgressionData.class, empty(PlayerProgressionData.class)));
                disk.initial("faction", encode(PlayerFactionData.class, empty(PlayerFactionData.class)));
                for (UUID owner : OWNERS) {
                    var player = new CompoundTag(); var m = p.member(owner);
                    player.put("inventory", m.getCompoundOrEmpty("before")); player.put("tax", m.getCompoundOrEmpty("tax_before"));
                    player.putString("unrelated", "preserved"); disk.initial(owner.toString(), player);
                }
                try {
                    if (cut != 0) { journal.beginOutcome(p); disk.save("journal", D1RunData.class, journal); settle(disk, order); }
                } catch (Cut expected) { /* discard every in-memory object, then reopen all independent native files */ }
                disk.cut = -1; settle(disk, order); settle(disk, order);
                var lt = disk.load("lifetime", D1LifetimeData.class); var pg = disk.load("progression", PlayerProgressionData.class);
                var fa = disk.load("faction", PlayerFactionData.class); var finished = disk.load("journal", D1RunData.class);
                check(finished.outcomeRuns().isEmpty(), "Interrupted outcome eventually retires");
                for (int i = 0; i < OWNERS.size(); i++) {
                    UUID owner = OWNERS.get(i); var player = disk.read(owner.toString());
                    boolean awarded = cut != 0 && success;
                    check(lt.totals(owner).completions() == (awarded ? 1 : 0)
                            && lt.totals(owner).spectralBlooms() == (awarded ? 6 : 0)
                            && lt.totals(owner).successfulKills() == (awarded ? 7 + i : 0)
                            && lt.totals(owner).lesserBlooms() == (awarded ? 6 - i : 0), "All cut points preserve exactly one success-only lifetime award");
                    check(pg.getLesserBlooms(owner) == (awarded ? 10 : 0) && fa.getValue(owner, FactionDefinitions.NPC_ID) == (awarded ? 8 : 0), "Independent projection writes recover once");
                    check(player.getCompoundOrEmpty("inventory").equals(p.member(owner).getCompoundOrEmpty(cut == 0 ? "before" : "after")), "No cut loses or duplicates Bloom consumption");
                    check(player.getStringOr("unrelated", "").equals("preserved"), "Player fields outside the decision survive");
                    check((finished.lastWatson(owner) != null) == (cut != 0), "Retirement retains cursor only after a committed decision");
                }
            }
        }
    }
    public static void main(String[] args) throws Exception {
        decisions(); fullRoster(); journals(); projections(); interruptions();
        System.out.println("Watson outcome checks passed: " + checks);
    }
}
