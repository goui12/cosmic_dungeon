package net.goui.cosmicdungeon.dungeon;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.effect.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

final class DungeonDifficultyTest {
    @SuppressWarnings("unchecked")
    private Codec<DungeonRunRegistryData> codec() throws Exception {
        var f = DungeonRunRegistryData.class.getDeclaredField("CODEC"); f.setAccessible(true);
        return (Codec<DungeonRunRegistryData>)f.get(null);
    }
    private DungeonRunRegistryData registry() throws Exception {
        var n = new CompoundTag(); n.putLong("next_run_id", 1); n.put("runs", new ListTag());
        return codec().parse(NbtOps.INSTANCE, n).getOrThrow();
    }
    private DungeonRunRegistryData.RunRecord roundTrip(DungeonRunRegistryData.RunRecord run) {
        return DungeonRunRegistryData.RunRecord.CODEC.parse(NbtOps.INSTANCE,
                DungeonRunRegistryData.RunRecord.CODEC.encodeStart(NbtOps.INSTANCE, run).getOrThrow()).getOrThrow();
    }
    @Test void approvedDefaultsAreIndependentAndLegacyNormalMeansHard() {
        assertEquals(DungeonDifficulty.HARD, DungeonDifficulty.parse("NORMAL").orElseThrow());
        assertTrue(DungeonDifficulty.parse("impossible").isEmpty());
        for (var tier : DungeonDifficulty.values()) {
            var p = tier.defaults();
            assertEquals(List.of(.5, 1., 1.5, 2.).get(tier.ordinal()), p.health());
            assertEquals(p.health(), p.damage()); assertEquals(p.health(), p.spawnCount());
            assertEquals(1, p.health() * p.spawnDelay(), .000001);
        }
        assertEquals(.001, DungeonDifficulty.INSANE.defaults().blindnessChance());
        assertEquals(100, DungeonDifficulty.INSANE.defaults().blindnessTicks());
        assertEquals(1.1, DungeonDifficulty.RIDICULOUS.defaults().armorWear());
    }
    @Test void profilesRejectMalformedDataWithoutDiscardingRun() throws Exception {
        var profile = (CompoundTag)DungeonDifficulty.Profile.CODEC.encodeStart(NbtOps.INSTANCE,
                DungeonDifficulty.INSANE.defaults()).getOrThrow();
        profile.putDouble("health", Double.NaN);
        assertTrue(DungeonDifficulty.Profile.CODEC.parse(NbtOps.INSTANCE, profile).isError());
        profile.putDouble("health", 1.5); profile.putString("tier", "UNKNOWN");
        assertThrows(RuntimeException.class, () -> DungeonDifficulty.Profile.CODEC.parse(NbtOps.INSTANCE, profile).getOrThrow());
    }
    @Test void oldRunWithoutProfileLoadsBaselineAndRetainsRosterDimensionsAndSnapshots() throws Exception {
        var run = registry().allocateInstance(DungeonDefinitions.DUNGEON_1, 1);
        var encoded = (CompoundTag)DungeonRunRegistryData.RunRecord.CODEC.encodeStart(NbtOps.INSTANCE, run).getOrThrow();
        encoded.remove("difficulty_profile");
        var restored = DungeonRunRegistryData.RunRecord.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        assertEquals(run, restored);
        assertEquals(DungeonDifficulty.Profile.LEGACY, restored.difficulty());
    }
    @Test void profileSurvivesRunCopiesAndRoundTrips() throws Exception {
        var profile = new DungeonDifficulty.Profile("INSANE", 1.25, 1.7, 1.9, .8, .6, .02, 123, 1.3);
        var run = registry().allocateInstance(DungeonDefinitions.DUNGEON_1, 1).withDifficulty(profile);
        assertEquals(profile, roundTrip(run).difficulty());
        assertEquals(profile, roundTrip(run.withState(DungeonRunState.ACTIVE, null)).difficulty());
        assertEquals(profile, roundTrip(run.withCompletionExited(UUID.randomUUID())).difficulty());
        assertEquals(profile, roundTrip(run.withInstance(1, run.dungeonDimensionIds())).difficulty());
    }
    @Test void simultaneousInstancesAndStartingRegistryKeepSeparateProfiles() throws Exception {
        var data = registry(); var a = data.allocateInstance(DungeonDefinitions.DUNGEON_1, 1);
        var b = data.allocateInstance(DungeonDefinitions.DUNGEON_1, 2);
        var encoded = (CompoundTag)codec().encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        var list = new ListTag();
        list.add(DungeonRunRegistryData.RunRecord.CODEC.encodeStart(NbtOps.INSTANCE, a.withDifficulty(DungeonDifficulty.EASY.defaults())).getOrThrow());
        list.add(DungeonRunRegistryData.RunRecord.CODEC.encodeStart(NbtOps.INSTANCE, b.withDifficulty(DungeonDifficulty.RIDICULOUS.defaults())).getOrThrow());
        encoded.put("runs", list); data = codec().parse(NbtOps.INSTANCE, encoded).getOrThrow();
        var dimA = DungeonInstanceSlots.mapping(DungeonDefinitions.DUNGEON_1, a).get(DungeonDefinitions.DUNGEON_1.primaryDimension());
        var dimB = DungeonInstanceSlots.mapping(DungeonDefinitions.DUNGEON_1, b).get(DungeonDefinitions.DUNGEON_1.primaryDimension());
        assertEquals(DungeonDifficulty.EASY, data.difficultyForDimension(dimA).difficulty());
        assertEquals(DungeonDifficulty.RIDICULOUS, data.difficultyForDimension(dimB).difficulty());
        assertNull(data.difficultyForDimension(Level.OVERWORLD));
        assertNull(data.difficultyForDimension(DungeonDefinitions.DUNGEON_1.primaryDimension()));
        var owner = UUID.randomUUID();
        assertEquals(a.runId(), data.startRun(a.runId(), Level.OVERWORLD, 0, DungeonDefinitions.DUNGEON_1, 1,
                a.dungeonDimensionIds(), List.of(owner), List.of()));
        assertEquals(DungeonDifficulty.EASY, data.getRun(a.runId()).orElseThrow().difficulty().difficulty());
        assertEquals(DungeonDifficulty.EASY, data.getRun(a.runId()).orElseThrow().withoutPlayer(owner).difficulty().difficulty());
    }
    @Test void spawnCountsNeverDuplicateBossesOrEnableDisabledSpawns() {
        for (var tier : DungeonDifficulty.values()) {
            assertEquals(1, tier.defaults().count(20, true));
            assertEquals(0, tier.defaults().count(0, false));
            assertEquals(0, tier.defaults().count(0, true));
            assertTrue(tier.defaults().count(1, false) >= 1);
        }
        assertEquals(2, DungeonDifficulty.EASY.defaults().count(4, false));
        assertEquals(6, DungeonDifficulty.INSANE.defaults().count(4, false));
        assertEquals(8, DungeonDifficulty.RIDICULOUS.defaults().count(4, false));
    }
    @Test void countdownClockScalesInitialAndLaterDelaysWithoutRewritingAuthoredValues() {
        for (var tier : DungeonDifficulty.values()) {
            var clock = new DungeonDifficulty.Clock();
            int elapsed = 0;
            for (int tick = 0; tick < 20; tick++) elapsed += clock.step(tier.defaults().spawnDelay());
            assertEquals((int)(20 * tier.defaults().health()), elapsed);
        }
        var easy = new DungeonDifficulty.Clock();
        assertEquals(0, easy.step(2)); assertEquals(1, easy.step(2));
        assertEquals(20, new DungeonDifficulty.Clock().step(.05));
    }
    @Test void fractionalArmorWearDoesNotDoubleEverySmallHit() {
        var p = DungeonDifficulty.RIDICULOUS.defaults();
        assertEquals(1, p.wear(1, .5)); assertEquals(2, p.wear(1, .05));
        assertEquals(11, p.wear(10, .5)); assertEquals(0, p.wear(0, 0));
        int total = 0; for (int n = 0; n < 1000; n++) total += p.wear(1, (n + .5) / 1000);
        assertEquals(1100, total);
    }
    @Test void effectScalingCopiesInputAndKeepsInfiniteEffectsInfinite() {
        var effect = new MobEffectInstance(MobEffects.POISON, 100, 2, true, false, false);
        var shortEffect = effect.withScaledDuration((float)DungeonDifficulty.EASY.defaults().harmfulDuration());
        assertEquals(100, effect.getDuration()); assertEquals(50, shortEffect.getDuration());
        assertEquals(2, shortEffect.getAmplifier()); assertTrue(shortEffect.isAmbient());
        assertFalse(shortEffect.isVisible()); assertFalse(shortEffect.showIcon());
        assertEquals(-1, new MobEffectInstance(MobEffects.POISON, -1).withScaledDuration(.5f).getDuration());
    }
    @Test void completionTiersAreCumulativeAndUnknownOrOtherDungeonsGrantNothing() {
        var id = UUID.randomUUID();
        for (var tier : DungeonDifficulty.values()) {
            var earned = DungeonCompletionAwards.earned(List.of(new DungeonRunProgressData.CompletionRecord(id, "dungeon_1", tier.name(), 123)));
            assertEquals(new HashSet<>(tier.completedTiers()), earned);
        }
        assertEquals(Set.of(DungeonDifficulty.EASY, DungeonDifficulty.HARD),
                DungeonCompletionAwards.earned(List.of(new DungeonRunProgressData.CompletionRecord(id, "dungeon_1", "NORMAL", 123))));
        assertTrue(DungeonCompletionAwards.earned(List.of(
                new DungeonRunProgressData.CompletionRecord(id, "dungeon_2", "RIDICULOUS", 123),
                new DungeonRunProgressData.CompletionRecord(id, "dungeon_1", "UNKNOWN", 123))).isEmpty());
    }
    @Test void unsuccessfulHandoffsNeverTouchCompletionHistoryOrAwards() {
        for (String reason : List.of("ABANDONED", "KICKED", "LINK_DEAD", "STARTUP_ABORT", "MANUAL")) {
            var plan = InventoryHandoffPlan.create(UUID.randomUUID(), 12, "cleanup", reason, false, false,
                    new CompoundTag(), new CompoundTag(), new CompoundTag(), new CompoundTag(),
                    new CompoundTag(), new CompoundTag(), "", new CompoundTag()).withDifficulty(DungeonDifficulty.RIDICULOUS);
            assertDoesNotThrow(() -> DungeonCompletionAwards.completed(null, plan));
        }
    }
    @Test void frozenHandoffTierSurvivesOfflineRecoveryAndKeepsInventoryImages() {
        var original = new CompoundTag(); original.putString("evidence", "authored stack");
        var plan = InventoryHandoffPlan.cleanup(UUID.randomUUID(), 12, "COMPLETED", original, null,
                new CompoundTag(), new CompoundTag(), new CompoundTag(), false);
        assertEquals(DungeonDifficulty.HARD, plan.difficulty());
        var chosen = plan.withDifficulty(DungeonDifficulty.EASY);
        var restored = InventoryHandoffPlan.CODEC.parse(NbtOps.INSTANCE,
                InventoryHandoffPlan.CODEC.encodeStart(NbtOps.INSTANCE, chosen.ready()).getOrThrow()).getOrThrow();
        assertEquals(DungeonDifficulty.EASY, restored.difficulty()); assertTrue(restored.worldReady());
        assertEquals(plan.tag("stored"), restored.tag("stored")); assertEquals(plan.receipt(), restored.receipt());
        var bad = chosen.image(); bad.putString("difficulty", "UNKNOWN");
        assertThrows(IllegalArgumentException.class, () -> new InventoryHandoffPlan(bad));
    }
}
