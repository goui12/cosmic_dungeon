package net.goui.cosmicdungeon.npc.tamsin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import java.util.*;

/** Separate overworld save: never reset with dungeon runs or inventory snapshots. */
public final class TamsinData extends SavedData {
    public record Binding(String dimension, long selector) {
        private static final Codec<Binding> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("dimension").forGetter(Binding::dimension),
                Codec.LONG.fieldOf("selector").forGetter(Binding::selector)
        ).apply(i, Binding::new));
    }
    private static final Codec<TamsinData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.listOf().optionalFieldOf("accepted_players", List.of()).forGetter(d -> List.copyOf(d.accepted)),
            Codec.unboundedMap(Codec.STRING, Binding.CODEC).optionalFieldOf("bindings", Map.of()).forGetter(d -> d.bindings)
    ).apply(i, TamsinData::load));
    private static final SavedDataType<TamsinData> TYPE =
            new SavedDataType<>("cosmicdungeon_d1_tamsin_v1", TamsinData::new, CODEC);
    private final Set<String> accepted = new HashSet<>();
    private final Map<String, Binding> bindings = new HashMap<>();
    private TamsinData() {}
    private static TamsinData load(List<String> players, Map<String, Binding> bindings) {
        var data = new TamsinData();
        data.accepted.addAll(players); data.bindings.putAll(bindings); return data;
    }
    public static TamsinData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
    public boolean accepted(UUID player) { return accepted.contains(player.toString()); }
    public boolean accept(UUID player) {
        if (!accepted.add(player.toString())) return false;
        setDirty(); return true;
    }
    public Binding binding(UUID npc) { return bindings.get(npc.toString()); }
    public void bind(UUID npc, Binding binding) { bindings.put(npc.toString(), binding); setDirty(); }
    public boolean unbind(UUID npc) {
        if (bindings.remove(npc.toString()) == null) return false;
        setDirty(); return true;
    }
    public int bindingCount() { return bindings.size(); }
}
