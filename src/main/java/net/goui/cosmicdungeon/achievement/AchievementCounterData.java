package net.goui.cosmicdungeon.achievement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AchievementCounterData extends SavedData {
    private static final String SAVE_ID = "cosmicdungeon_achievement_counters_v1";
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public record CounterRecord(UUID playerId, int bindingIdolReturns, int bindingIdolProvided, int vitalExchangeMask,
                                int d1MusicDiscMask, int genericCounter1, int genericCounter2) {
        public static final Codec<CounterRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUID_CODEC.fieldOf("player_id").forGetter(CounterRecord::playerId),
                Codec.INT.optionalFieldOf("binding_idol_returns", 0).forGetter(CounterRecord::bindingIdolReturns),
                Codec.INT.optionalFieldOf("binding_idol_provided", 0).forGetter(CounterRecord::bindingIdolProvided),
                Codec.INT.optionalFieldOf("vital_exchange_mask", 0).forGetter(CounterRecord::vitalExchangeMask),
                Codec.INT.optionalFieldOf("d1_music_disc_mask", 0).forGetter(CounterRecord::d1MusicDiscMask),
                Codec.INT.optionalFieldOf("generic_counter_1", 0).forGetter(CounterRecord::genericCounter1),
                Codec.INT.optionalFieldOf("generic_counter_2", 0).forGetter(CounterRecord::genericCounter2)
        ).apply(i, CounterRecord::new));
    }

    private record Persisted(List<CounterRecord> records) {
        private static final Codec<Persisted> CODEC = RecordCodecBuilder.create(i -> i.group(
                CounterRecord.CODEC.listOf().optionalFieldOf("records", List.of()).forGetter(Persisted::records)
        ).apply(i, Persisted::new));
    }

    private static final Codec<AchievementCounterData> CODEC = Persisted.CODEC.xmap(AchievementCounterData::fromPersisted, AchievementCounterData::toPersisted);
    public static final SavedDataType<AchievementCounterData> TYPE = new SavedDataType<>(SAVE_ID, AchievementCounterData::new, CODEC);

    private final Map<UUID, CounterRecord> counters = new HashMap<>();

    public static AchievementCounterData get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) throw new IllegalStateException("Overworld unavailable");
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public static AchievementCounterData get(ServerLevel level) { return get(level.getServer()); }

    private AchievementCounterData() {}

    private static AchievementCounterData fromPersisted(Persisted p) {
        AchievementCounterData d = new AchievementCounterData();
        for (CounterRecord rec : p.records()) d.counters.put(rec.playerId(), rec);
        return d;
    }

    private Persisted toPersisted() {
        List<CounterRecord> out = new ArrayList<>(counters.values());
        out.sort(Comparator.comparing(r -> r.playerId().toString()));
        return new Persisted(out);
    }

    public CounterRecord get(UUID playerId) {
        return counters.getOrDefault(playerId, new CounterRecord(playerId, 0, 0, 0, 0, 0, 0));
    }

    public void set(CounterRecord rec) { counters.put(rec.playerId(), rec); setDirty(); }

    public void reset(UUID playerId) { if (counters.remove(playerId) != null) setDirty(); }
}
