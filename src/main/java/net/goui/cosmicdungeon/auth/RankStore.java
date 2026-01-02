package net.goui.cosmicdungeon.auth;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Canonical Cosmic Dungeon rank store.
 * Source of truth. Default rank is DUNGEONEER when absent.
 *
 * Stored in overworld data storage so it persists for the save.
 */
public final class RankStore extends SavedData {

    public static final String SAVE_ID = "cosmicdungeon_ranks";

    private static final Codec<Map<String, Rank>> MAP_CODEC =
            Codec.unboundedMap(Codec.STRING, Rank.CODEC);

    public static final Codec<RankStore> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            MAP_CODEC.optionalFieldOf("ranks", Map.of()).forGetter(s -> s.ranksById)
    ).apply(inst, RankStore::fromCodec));

    public static final SavedDataType<RankStore> TYPE = new SavedDataType<>(
            SAVE_ID,
            RankStore::new,
            CODEC
    );

    private final Map<String, Rank> ranksById = new HashMap<>();

    public RankStore() {}

    private static RankStore fromCodec(Map<String, Rank> ranks) {
        RankStore s = new RankStore();
        s.ranksById.clear();
        if (ranks != null) s.ranksById.putAll(ranks);
        return s;
    }

    /** Preferred: get via server (dimension-agnostic). */
    public static RankStore get(MinecraftServer server) {
        if (server == null) throw new IllegalArgumentException("server is null");
        final ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    /** Convenience: get via any loaded level. */
    public static RankStore get(ServerLevel anyLevel) {
        if (anyLevel == null) throw new IllegalArgumentException("level is null");
        return get(anyLevel.getServer());
    }

    public Rank getRank(UUID playerId) {
        if (playerId == null) return Rank.DUNGEONEER;
        return ranksById.getOrDefault(playerId.toString(), Rank.DUNGEONEER);
    }

    public boolean isDeveloper(UUID playerId) {
        return getRank(playerId).isDeveloper();
    }

    public void setRank(UUID playerId, Rank rank) {
        if (playerId == null) return;
        if (rank == null) rank = Rank.DUNGEONEER;

        ranksById.put(playerId.toString(), rank);
        setDirty();
    }

    public void clearRank(UUID playerId) {
        if (playerId == null) return;
        if (ranksById.remove(playerId.toString()) != null) {
            setDirty();
        }
    }

    public Map<String, Rank> snapshotRaw() {
        return Map.copyOf(ranksById);
    }
}
