package net.goui.cosmicdungeon.dungeon.d1;

import com.mojang.serialization.Codec;
import net.goui.cosmicdungeon.transaction.SavedDataProof;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import java.util.*;

/**
 * New, independent instance-scoped storage. Never imports the old lifetime music/bell counters.
 * The existing dungeon run and player progression save IDs remain unchanged.
 */
public final class D1RunData extends SavedData {
    private record State(long runId, Map<String, List<String>> unique,
                         Map<String, Integer> counts, Map<String, Long> bellTicks) {
        static final Codec<State> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("run_id").forGetter(State::runId),
                Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf()).optionalFieldOf("unique", Map.of()).forGetter(State::unique),
                Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("counts", Map.of()).forGetter(State::counts),
                Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("bell_ticks", Map.of()).forGetter(State::bellTicks)
        ).apply(i, (id, unique, counts, ticks) -> new State(id, new HashMap<>(unique), new HashMap<>(counts), new HashMap<>(ticks))));
    }
    private static final Codec<D1RunData> CODEC = RecordCodecBuilder.create(i -> i.group(
            State.CODEC.listOf().optionalFieldOf("runs", List.of()).forGetter((D1RunData d) -> List.copyOf(d.runs.values())),
            Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf()).optionalFieldOf("achievement_credits", Map.of())
                    .forGetter((D1RunData d) -> d.achievementCredits),
            WatsonOutcome.CODEC.listOf().optionalFieldOf("watson_outcomes", List.of()).forGetter(d -> List.copyOf(d.outcomes.values())),
            WatsonReceipt.MAP_CODEC.optionalFieldOf("watson_receipts", Map.of()).forGetter(d -> d.watsonReceipts)
    ).apply(i, D1RunData::load));
    private static final SavedDataType<D1RunData> TYPE = new SavedDataType<>("cosmicdungeon_d1_objectives_v1", D1RunData::new, CODEC);
    private final Map<Long, State> runs = new HashMap<>();
    // Permanent earned entitlements, not progress. Bounded to supported replayable achievements per UUID.
    // Never delete after projection: advancements save separately and a crash may replay this safely.
    private final Map<String, List<String>> achievementCredits = new HashMap<>();
    private final Map<Long, WatsonOutcome> outcomes = new TreeMap<>();
    private final Map<UUID, Long> outcomeOwners = new HashMap<>();
    private final Map<UUID, WatsonReceipt> watsonReceipts = new HashMap<>();
    private MinecraftServer server;
    private D1RunData() {}
    public static D1RunData get(MinecraftServer server) {
        SavedDataProof.validate(server, "cosmicdungeon_d1_objectives_v1", CODEC);
        var data = server.overworld().getDataStorage().computeIfAbsent(TYPE); data.server = server; return data;
    }
    public boolean flushVerified() { return SavedDataProof.save(server, "cosmicdungeon_d1_objectives_v1", CODEC, this); }
    private static D1RunData load(List<State> states, Map<String, List<String>> credits,
            List<WatsonOutcome> outcomes, Map<UUID, WatsonReceipt> receipts) {
        D1RunData data = new D1RunData();
        credits.forEach((owner, ids) -> data.achievementCredits.put(owner, List.copyOf(ids)));
        for (State s : states) {
            if (s.runId() <= 0 || data.runs.putIfAbsent(s.runId(), s) != null)
                throw new IllegalArgumentException("Invalid or duplicate D1 objective run");
        }
        WatsonReceipt.validate(receipts); data.watsonReceipts.putAll(receipts);
        for (WatsonOutcome outcome : outcomes) data.indexOutcome(outcome);
        return data;
    }
    private State state(long runId) {
        if (runId <= 0) throw new IllegalArgumentException("An instance run ID is required");
        return runs.computeIfAbsent(runId, id -> new State(id, new HashMap<>(), new HashMap<>(), new HashMap<>()));
    }
    public boolean recordUnique(long runId, String key, String value) {
        State state = state(runId);
        List<String> old = state.unique().getOrDefault(key, List.of());
        if (old.contains(value)) return false;
        List<String> next = new ArrayList<>(old);
        next.add(value);
        state.unique().put(key, List.copyOf(next));
        setDirty();
        return true;
    }
    public void removeUnique(long runId,String key,String value){
        var state=runs.get(runId);if(state==null)return;
        var next=new ArrayList<>(state.unique().getOrDefault(key,List.of()));
        if(next.remove(value)){state.unique().put(key,List.copyOf(next));setDirty();}
    }
    public void setValue(long runId, String key, String value) {
        var state = state(runId);
        List<String> next = value == null || value.isEmpty() ? List.of() : List.of(value);
        if (!state.unique().getOrDefault(key, List.of()).equals(next)) {
            state.unique().put(key, next); setDirty();
        }
    }
    public List<String> values(long runId, String key) {
        State state = runs.get(runId);
        return state == null ? List.of() : List.copyOf(state.unique().getOrDefault(key, List.of()));
    }
    public int count(long runId, String key) {
        State state = runs.get(runId);
        return state == null ? 0 : Math.max(0, state.counts().getOrDefault(key, 0));
    }
    public void setCount(long runId, String key, int amount) {
        State state = state(runId);
        int value = Math.max(0, amount);
        if (state.counts().getOrDefault(key, 0) != value) {
            state.counts().put(key, value); setDirty();
        }
    }
    public int recordKill(long runId, UUID player) {
        String key = "kills:" + player;
        int old = count(runId, key);
        int next = old == Integer.MAX_VALUE ? old : old + 1;
        setCount(runId, key, next);
        return next;
    }
    public int strikeBell(long runId, String bell, long now, long windowTicks) {
        int count = D1ObjectiveRules.strike(state(runId).bellTicks(), bell, now, windowTicks);
        setDirty(); return count;
    }
    public boolean creditShared(long runId, String achievement, Collection<UUID> recipients) {
        if (recipients.isEmpty() || values(runId, "awarded").contains(achievement)) return false;
        recordUnique(runId, "awarded", achievement);
        for (UUID owner : recipients) {
            var next = new ArrayList<>(achievementCredits.getOrDefault(owner.toString(), List.of()));
            if (!next.contains(achievement)) { next.add(achievement); achievementCredits.put(owner.toString(), List.copyOf(next)); }
        }
        setDirty(); return true;
    }
    public boolean creditPersonal(long runId, String achievement, UUID owner) {
        if (!recordUnique(runId, "awarded:" + owner, achievement)) return false;
        var next = new ArrayList<>(achievementCredits.getOrDefault(owner.toString(), List.of()));
        if (!next.contains(achievement)) { next.add(achievement); achievementCredits.put(owner.toString(), List.copyOf(next)); }
        setDirty(); return true;
    }
    public List<String> achievementCredits(UUID owner) {
        return List.copyOf(achievementCredits.getOrDefault(owner.toString(), List.of()));
    }
    public void clearRun(long runId) { if (runs.remove(runId) != null) setDirty(); }
    public WatsonOutcome outcome(long runId) { return outcomes.get(runId); }
    public WatsonOutcome outcomeFor(UUID owner) {
        Long run = outcomeOwners.get(owner); return run == null ? null : outcomes.get(run);
    }
    public List<Long> outcomeRuns() { return List.copyOf(outcomes.keySet()); }
    public WatsonReceipt lastWatson(UUID owner) { return watsonReceipts.get(owner); }
    public boolean sealed(long runId) { return outcomes.containsKey(runId) || !values(runId, "watson_outcome").isEmpty(); }
    private void indexOutcome(WatsonOutcome outcome) {
        if (outcomes.containsKey(outcome.run())) throw new IllegalArgumentException("Duplicate Watson run");
        var marker = values(outcome.run(), "watson_outcome");
        if (!marker.isEmpty() && !marker.equals(List.of(outcome.success() ? "success" : "failure")))
            throw new IllegalArgumentException("Watson outcome marker disagrees with decision");
        for (UUID owner : outcome.owners()) {
            if (outcomeOwners.containsKey(owner) || !outcome.receipt(owner).shouldApply(watsonReceipts.get(owner)))
                throw new IllegalArgumentException("Overlapping or retired Watson decision");
        }
        outcomes.put(outcome.run(), outcome); outcome.owners().forEach(owner -> outcomeOwners.put(owner, outcome.run()));
    }
    public void beginOutcome(WatsonOutcome outcome) {
        if (sealed(outcome.run()) || outcome.ready() || !outcome.acknowledged().isEmpty())
            throw new IllegalStateException("Watson outcome already decided or not fresh");
        indexOutcome(outcome);
        setValue(outcome.run(), "watson_outcome", outcome.success() ? "success" : "failure"); setDirty();
    }
    private WatsonOutcome requireOutcome(WatsonOutcome expected) {
        var current = outcomes.get(expected.run());
        if (current == null || !current.sameDecision(expected)) throw new IllegalStateException("Watson decision changed");
        return current;
    }
    public void acknowledgeOutcome(WatsonOutcome expected, UUID owner) {
        var next = requireOutcome(expected).acknowledge(owner); outcomes.put(next.run(), next); setDirty();
    }
    public void readyOutcome(WatsonOutcome expected) {
        var next = requireOutcome(expected).markReady(); outcomes.put(next.run(), next); setDirty();
    }
    public boolean readyOutcomeVerified(WatsonOutcome expected) {
        var previous = requireOutcome(expected); readyOutcome(expected);
        if (flushVerified()) return true;
        outcomes.put(previous.run(), previous); setDirty(); return false;
    }
    /** Called only after verified run retirement. Pending outcomes survive ordinary objective resets. */
    public void retireOutcome(long runId) {
        var outcome = outcomes.get(runId); if (outcome == null) return;
        if (!outcome.ready()) throw new IllegalStateException("Watson outcome is not settled");
        for (UUID owner : outcome.owners()) {
            var receipt = outcome.receipt(owner);
            if (!receipt.shouldApply(watsonReceipts.get(owner))) throw new IllegalStateException("Watson cursor already retired");
        }
        for (UUID owner : outcome.owners()) { watsonReceipts.put(owner, outcome.receipt(owner)); outcomeOwners.remove(owner); }
        outcomes.remove(runId); setDirty();
    }
    public boolean retireOutcomeVerified(long runId) {
        var outcome = outcomes.get(runId); if (outcome == null) return true;
        var previous = new HashMap<>(watsonReceipts);
        retireOutcome(runId);
        if (flushVerified()) return true;
        watsonReceipts.clear(); watsonReceipts.putAll(previous); indexOutcome(outcome); setDirty(); return false;
    }
}
