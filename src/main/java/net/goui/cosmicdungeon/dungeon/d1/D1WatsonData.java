package net.goui.cosmicdungeon.dungeon.d1;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Developer-authored template location; each leased instance gets its own Watson. */
public final class D1WatsonData extends SavedData {
    private static final Codec<D1WatsonData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("template_dimension", "").forGetter((D1WatsonData d) -> d.dimension),
            Codec.LONG.optionalFieldOf("position", 0L).forGetter((D1WatsonData d) -> d.position)
    ).apply(i, D1WatsonData::load));
    private static final SavedDataType<D1WatsonData> TYPE =
            new SavedDataType<>("cosmicdungeon_d1_watson_v1", D1WatsonData::new, CODEC);
    private String dimension = "";
    private long position;
    private D1WatsonData() {}
    private static D1WatsonData load(String dimension, long position) {
        var data = new D1WatsonData(); data.dimension = dimension; data.position = position; return data;
    }
    public static D1WatsonData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
    public boolean configured() { return !dimension.isEmpty(); }
    public String dimension() { return dimension; }
    public BlockPos pos() { return BlockPos.of(position); }
    /** Resolve the same authored template point in any leased D1 copy. */
    public net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> instanceDimension(int slot) {
        if (!configured() || slot < 1 || slot > net.goui.cosmicdungeon.dungeon.DungeonInstanceSlots.SLOT_COUNT) return null;
        return net.goui.cosmicdungeon.dungeon.DungeonInstanceSlots.mapping(
                net.goui.cosmicdungeon.dungeon.DungeonDefinitions.DUNGEON_1, slot).entrySet().stream()
                .filter(e -> e.getKey().location().toString().equals(dimension))
                .map(java.util.Map.Entry::getValue).findFirst().orElse(null);
    }
    public boolean matches(int slot, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> actual,
                           BlockPos position) {
        return actual != null && actual.equals(instanceDimension(slot)) && pos().equals(position);
    }
    public void set(String dimension, BlockPos pos) {
        this.dimension = dimension; this.position = pos.asLong(); setDirty();
    }
    public void clear() { dimension = ""; setDirty(); }
}
