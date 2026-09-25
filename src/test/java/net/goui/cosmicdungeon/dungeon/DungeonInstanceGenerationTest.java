package net.goui.cosmicdungeon.dungeon;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class DungeonInstanceGenerationTest {
    @SuppressWarnings("unchecked")
    private Codec<DungeonRunRegistryData> codec() throws Exception {
        var field = DungeonRunRegistryData.class.getDeclaredField("CODEC");
        field.setAccessible(true);
        return (Codec<DungeonRunRegistryData>) field.get(null);
    }

    private DungeonRunRegistryData empty() throws Exception {
        CompoundTag legacy = new CompoundTag();
        legacy.putLong("next_run_id", 42);
        legacy.put("runs", new net.minecraft.nbt.ListTag());
        return codec().parse(NbtOps.INSTANCE, legacy).getOrThrow();
    }

    private DungeonRunRegistryData roundTrip(DungeonRunRegistryData data) throws Exception {
        var codec = codec();
        return codec.parse(NbtOps.INSTANCE, codec.encodeStart(NbtOps.INSTANCE, data).getOrThrow()).getOrThrow();
    }

    @Test void reservationSurvivesRestartAndCancellationNeverReusesAnId() throws Exception {
        var data = empty();
        var first = data.allocateInstance(DungeonDefinitions.DUNGEON_1, 1);
        assertEquals(42, first.runId());
        assertEquals(DungeonRunState.PREPARING, first.stateEnum());
        assertTrue(data.isSlotOccupied(1));
        data = roundTrip(data);
        assertTrue(data.isSlotOccupied(1));
        assertTrue(data.removeRun(first.runId()));
        data = roundTrip(data);
        var second = data.allocateInstance(DungeonDefinitions.DUNGEON_1, 1);
        assertEquals(43, second.runId());
        assertTrue(java.util.Collections.disjoint(first.dungeonDimensionIds(), second.dungeonDimensionIds()));
    }

    @Test void storedRunDimensionsRouteToTheirOwnGenerationAndPreserveLegacySlots() throws Exception {
        var data = empty();
        var run = data.allocateInstance(DungeonDefinitions.DUNGEON_1, 2);
        var mapping = DungeonInstanceSlots.mapping(DungeonDefinitions.DUNGEON_1, run);
        assertEquals(Set.copyOf(run.dungeonDimensionIds()), mapping.values().stream()
                .map(key -> key.location().toString()).collect(java.util.stream.Collectors.toSet()));
        mapping.values().forEach(key -> assertEquals(2, DungeonInstanceSlots.slotOf(key).orElseThrow()));
        var legacy = run.withInstance(2, DungeonInstanceSlots.dimensionIds(2));
        assertEquals(DungeonInstanceSlots.mapping(DungeonDefinitions.DUNGEON_1, 2),
                DungeonInstanceSlots.mapping(DungeonDefinitions.DUNGEON_1, legacy));
        assertThrows(IllegalArgumentException.class, () -> DungeonInstanceSlots.mapping(DungeonDefinitions.DUNGEON_1,
                run.withInstance(2, DungeonDefinitions.DUNGEON_1.dimensionIds())));
        var invalid = ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath("cosmicdungeon", "dungeon_instance_02_not_a_run"));
        assertTrue(DungeonInstanceSlots.slotOf(invalid).isEmpty());
    }

    @Test void registrationConsumesOnlyItsReservationWithoutAllocatingAnotherId() throws Exception {
        var data = empty();
        var run = data.allocateInstance(DungeonDefinitions.DUNGEON_1, 1);
        var player = UUID.randomUUID();
        assertEquals(-1, data.startRun(run.runId(), Level.OVERWORLD, 0, DungeonDefinitions.DUNGEON_1, 2,
                run.dungeonDimensionIds(), List.of(player), List.of()));
        assertEquals(run.runId(), data.startRun(run.runId(), Level.OVERWORLD, 0, DungeonDefinitions.DUNGEON_1, 1,
                run.dungeonDimensionIds(), List.of(player), List.of()));
        assertEquals(DungeonRunState.ACTIVE, data.getRun(run.runId()).orElseThrow().stateEnum());
        assertEquals(43, roundTrip(data).allocateInstance(DungeonDefinitions.DUNGEON_1, 2).runId());
    }

    @Test void oldSavesDefaultToNoRetirementAndInvalidMarkersFailClosed() throws Exception {
        var data = empty();
        assertFalse(data.retiring(42));
        var run = data.allocateInstance(DungeonDefinitions.DUNGEON_1, 1);
        var tag = (CompoundTag) codec().encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        var markers = new net.minecraft.nbt.ListTag();
        markers.add(net.minecraft.nbt.LongTag.valueOf(run.runId()));
        tag.put("retiring_instances", markers);
        assertTrue(codec().parse(NbtOps.INSTANCE, tag).getOrThrow().retiring(run.runId()));
        markers.add(net.minecraft.nbt.LongTag.valueOf(999));
        assertThrows(RuntimeException.class, () -> codec().parse(NbtOps.INSTANCE, tag).getOrThrow());
    }
    @Test void onlyEmptyPreparationsMayUseTheNoInventoryCleanupPath() throws Exception {
        var data = empty();
        var run = data.allocateInstance(DungeonDefinitions.DUNGEON_1, 1);
        assertTrue(run.unusedPreparation());
        var malformed = new DungeonRunRegistryData.RunRecord(run.runId(), run.dungeonId(), run.selectorDimensionId(),
                run.selectorPosLong(), run.dungeonDimensionIds(), run.instanceSlot(), run.state(), run.resetReason(),
                run.startedAtEpochMillis(), List.of(UUID.randomUUID()), List.of(), List.of());
        assertFalse(malformed.unusedPreparation());
        var tag = (CompoundTag) codec().encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        var records = new net.minecraft.nbt.ListTag();
        records.add(DungeonRunRegistryData.RunRecord.CODEC.encodeStart(NbtOps.INSTANCE, malformed).getOrThrow());
        tag.put("runs", records);
        assertThrows(RuntimeException.class, () -> codec().parse(NbtOps.INSTANCE, tag).getOrThrow());
    }

    @Test void tenReservationsEnforceCapacityAndOccupiedSlotsCannotConsumeIds() throws Exception {
        var data = empty();
        for (int slot = 1; slot <= DungeonInstanceSlots.SLOT_COUNT; slot++)
            data.allocateInstance(DungeonDefinitions.DUNGEON_1, slot);
        assertTrue(data.firstAvailableSlot().isEmpty());
        assertThrows(IllegalStateException.class, () -> data.allocateInstance(DungeonDefinitions.DUNGEON_1, 1));
        assertEquals(52, roundTrip(data).nextInstanceRunId());
    }

    @Test void exhaustedCounterFailsInsteadOfWrappingOrReusingIds() throws Exception {
        var tag = (CompoundTag) codec().encodeStart(NbtOps.INSTANCE, empty()).getOrThrow();
        tag.putLong("next_run_id", Long.MAX_VALUE);
        var data = codec().parse(NbtOps.INSTANCE, tag).getOrThrow();
        assertThrows(IllegalStateException.class, () -> data.allocateInstance(DungeonDefinitions.DUNGEON_1, 1));
        assertEquals(Long.MAX_VALUE, data.nextInstanceRunId());
    }

}
