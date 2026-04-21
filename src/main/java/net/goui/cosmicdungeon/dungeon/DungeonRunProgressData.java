package net.goui.cosmicdungeon.dungeon;

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
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class DungeonRunProgressData extends SavedData {
    private static final String SAVE_ID = "cosmicdungeon_dungeon_progress_v1";
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public record BloomMaskRecord(long runId, UUID playerId, long mask) {
        public static final Codec<BloomMaskRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("run_id").forGetter(BloomMaskRecord::runId),
                UUID_CODEC.fieldOf("player_id").forGetter(BloomMaskRecord::playerId),
                Codec.LONG.fieldOf("mask").forGetter(BloomMaskRecord::mask)
        ).apply(i, BloomMaskRecord::new));
    }

    public record CompletionRecord(UUID playerId, String dungeonId, String difficulty, long completedAtEpochMillis) {
        public static final Codec<CompletionRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUID_CODEC.fieldOf("player_id").forGetter(CompletionRecord::playerId),
                Codec.STRING.fieldOf("dungeon_id").forGetter(CompletionRecord::dungeonId),
                Codec.STRING.fieldOf("difficulty").forGetter(CompletionRecord::difficulty),
                Codec.LONG.fieldOf("completed_at").forGetter(CompletionRecord::completedAtEpochMillis)
        ).apply(i, CompletionRecord::new));
    }

    private record Persisted(List<BloomMaskRecord> bloomMasks, List<CompletionRecord> completions) {
        private static final Codec<Persisted> CODEC = RecordCodecBuilder.create(i -> i.group(
                BloomMaskRecord.CODEC.listOf().optionalFieldOf("bloom_masks", List.of()).forGetter(Persisted::bloomMasks),
                CompletionRecord.CODEC.listOf().optionalFieldOf("completions", List.of()).forGetter(Persisted::completions)
        ).apply(i, Persisted::new));
    }

    private static final Codec<DungeonRunProgressData> CODEC = Persisted.CODEC.xmap(
            DungeonRunProgressData::fromPersisted,
            DungeonRunProgressData::toPersisted
    );

    public static final SavedDataType<DungeonRunProgressData> TYPE =
            new SavedDataType<>(SAVE_ID, DungeonRunProgressData::new, CODEC);

    public static DungeonRunProgressData get(ServerLevel anyLevel) {
        return get(anyLevel.getServer());
    }

    public static DungeonRunProgressData get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld is not available; cannot load DungeonRunProgressData.");
        }
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    private final Map<Long, Map<UUID, Long>> bloomMasks = new HashMap<>();
    private final Map<String, CompletionRecord> completions = new HashMap<>();

    private DungeonRunProgressData() {}

    private static DungeonRunProgressData fromPersisted(Persisted p) {
        DungeonRunProgressData d = new DungeonRunProgressData();
        for (BloomMaskRecord rec : p.bloomMasks()) {
            d.bloomMasks.computeIfAbsent(rec.runId(), k -> new HashMap<>()).put(rec.playerId(), rec.mask());
        }
        for (CompletionRecord rec : p.completions()) {
            d.completions.put(key(rec.playerId(), rec.dungeonId(), rec.difficulty()), rec);
        }
        return d;
    }

    private Persisted toPersisted() {
        List<BloomMaskRecord> bloomOut = new ArrayList<>();
        for (var e : bloomMasks.entrySet()) {
            long runId = e.getKey();
            for (var byPlayer : e.getValue().entrySet()) {
                bloomOut.add(new BloomMaskRecord(runId, byPlayer.getKey(), byPlayer.getValue()));
            }
        }
        bloomOut.sort(Comparator.comparingLong(BloomMaskRecord::runId).thenComparing(r -> r.playerId().toString()));

        List<CompletionRecord> compOut = new ArrayList<>(completions.values());
        compOut.sort(Comparator.comparing(CompletionRecord::dungeonId)
                .thenComparing(CompletionRecord::difficulty)
                .thenComparing(r -> r.playerId().toString()));

        return new Persisted(bloomOut, compOut);
    }

    private static String key(UUID playerId, String dungeonId, String difficulty) {
        return playerId + "|" + dungeonId.toLowerCase(Locale.ROOT) + "|" + difficulty.toUpperCase(Locale.ROOT);
    }

    public void setBloomMask(long runId, UUID playerId, long mask) {
        if (runId <= 0L || playerId == null) return;
        bloomMasks.computeIfAbsent(runId, k -> new HashMap<>()).put(playerId, mask);
        setDirty();
    }

    public long getBloomMask(long runId, UUID playerId) {
        if (runId <= 0L || playerId == null) return 0L;
        Map<UUID, Long> byPlayer = bloomMasks.get(runId);
        if (byPlayer == null) return 0L;
        return byPlayer.getOrDefault(playerId, 0L);
    }

    public void clearRun(long runId) {
        if (runId <= 0L) return;
        if (bloomMasks.remove(runId) != null) {
            setDirty();
        }
    }

    public void markCompleted(UUID playerId, String dungeonId, String difficulty) {
        if (playerId == null || dungeonId == null || dungeonId.isBlank()) return;

        String diff = (difficulty == null || difficulty.isBlank()) ? "NORMAL" : difficulty.toUpperCase(Locale.ROOT);
        CompletionRecord rec = new CompletionRecord(playerId, dungeonId, diff, System.currentTimeMillis());
        completions.put(key(playerId, dungeonId, diff), rec);
        setDirty();
    }

    public List<CompletionRecord> listCompletionsFor(UUID playerId) {
        if (playerId == null) return List.of();
        List<CompletionRecord> out = new ArrayList<>();
        for (CompletionRecord rec : completions.values()) {
            if (playerId.equals(rec.playerId())) {
                out.add(rec);
            }
        }
        out.sort(Comparator.comparing(CompletionRecord::dungeonId)
                .thenComparing(CompletionRecord::difficulty)
                .thenComparingLong(CompletionRecord::completedAtEpochMillis));
        return List.copyOf(out);
    }
}