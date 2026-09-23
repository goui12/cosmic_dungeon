package net.goui.cosmicdungeon.dungeon.d1;

import com.mojang.serialization.Codec;
import net.goui.cosmicdungeon.dungeon.DungeonDefinitions;
import net.goui.cosmicdungeon.dungeon.DungeonInstanceSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import java.nio.file.Files;
import java.util.*;

public final class WatsonPlacementChecks {
    private static int checks;
    private static void check(boolean ok, String label) { checks++; if (!ok) throw new AssertionError(label); }
    @SuppressWarnings("unchecked")
    private static <T> Codec<T> codec(Class<T> type) throws Exception {
        var field = type.getDeclaredField("CODEC"); field.setAccessible(true);
        return (Codec<T>)field.get(null);
    }
    private static <T> T restart(Codec<T> codec, T data) throws Exception {
        var path = Files.createTempFile("d1-watson-placement-", ".nbt");
        try {
            NbtIo.writeCompressed((CompoundTag)codec.encodeStart(NbtOps.INSTANCE, data).getOrThrow(), path);
            return codec.parse(NbtOps.INSTANCE,
                    NbtIo.readCompressed(path, NbtAccounter.create(1024 * 1024))).getOrThrow();
        } finally { Files.deleteIfExists(path); }
    }
    public static void main(String[] args) throws Exception {
        var codec = codec(D1WatsonData.class);
        var empty = codec.parse(NbtOps.INSTANCE, new CompoundTag()).getOrThrow();
        check(!empty.configured() && empty.instanceDimension(1) == null, "No fabricated spawn without authoring");
        BlockPos pos = new BlockPos(-123, -40, 456);
        for (var template : DungeonDefinitions.DUNGEON_1.dimensions()) {
            var legacy = new CompoundTag();
            legacy.putString("template_dimension", template.location().toString());
            legacy.putLong("position", pos.asLong());
            var data = restart(codec, codec.parse(NbtOps.INSTANCE, legacy).getOrThrow());
            check(data.pos().equals(pos) && data.dimension().equals(template.location().toString()),
                    "Existing authored template/coordinate fields survive unchanged");
            for (int slot = 1; slot <= DungeonInstanceSlots.SLOT_COUNT; slot++) {
                var target = DungeonInstanceSlots.mapping(DungeonDefinitions.DUNGEON_1, slot).get(template);
                check(data.matches(slot, target, pos), "Each instance remembers same template position");
                check(!data.matches(slot, target, pos.above()), "Old position cannot reclaim placement");
                check(!data.matches(slot, net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION,
                        net.minecraft.resources.ResourceLocation.withDefaultNamespace("overworld")), pos), "Global overworld cannot impersonate instance");
                int other = slot == DungeonInstanceSlots.SLOT_COUNT ? 1 : slot + 1;
                check(!data.matches(slot, DungeonInstanceSlots.mapping(DungeonDefinitions.DUNGEON_1, other).get(template), pos),
                        "Different party instance stays separate");
            }
            check(data.instanceDimension(0) == null && data.instanceDimension(11) == null, "Invalid slots have no placement");
            var moved = pos.offset(10, 2, -10); data.set(template.location().toString(), moved);
            data = restart(codec, data);
            check(data.matches(2, data.instanceDimension(2), moved) && !data.matches(2, data.instanceDimension(2), pos),
                    "Reauthored placement persists and excludes old position");
            data.clear();
            check(!restart(codec, data).configured(), "Clear persists without inventing coordinates");
        }
        var runCodec = codec(D1RunData.class);
        var runs = runCodec.parse(NbtOps.INSTANCE, new CompoundTag()).getOrThrow();
        UUID one = new UUID(0, 51), two = new UUID(0, 52);
        runs.setValue(101, "watson_entity", one.toString());
        runs.setValue(102, "watson_entity", two.toString());
        runs.setCount(101, "test_objective", 6);
        runs.setValue(101, "watson_entity", "retired");
        runs = restart(runCodec, runs);
        check(runs.values(101, "watson_entity").equals(List.of("retired")), "Retired unloaded Watson cannot be read as unclaimed");
        check(runs.values(102, "watson_entity").equals(List.of(two.toString())), "Retiring one instance preserves another Watson");
        check(runs.count(101, "test_objective") == 6, "Placement does not reset run progress");
        runs.clearRun(101); runs = restart(runCodec, runs);
        check(runs.values(101, "watson_entity").isEmpty() && runs.values(102, "watson_entity").equals(List.of(two.toString())),
                "Normal run reset retains other instance owner");
        System.out.println(checks + " Watson template placement/save checks passed");
    }
}
