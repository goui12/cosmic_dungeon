package net.goui.cosmicdungeon.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PlayerProgressionData extends SavedData {
    public static final String SAVE_ID = "cosmicdungeon_player_progression_v1";

    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
    private static final Codec<Set<String>> FLAG_SET_CODEC = Codec.STRING.listOf().xmap(HashSet::new, java.util.ArrayList::new);

    private record Entry(int d1LesserBloomsBest,
                         boolean d1CompletedWithAtLeast3LesserBlooms,
                         int lesserBlooms,
                         int cavernResidue,
                         boolean villageAccessUnlocked,
                         int npcUnlockTierD1,
                         int npcUnlockTierD2,
                         Set<String> flags) {
        private static final Codec<Entry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.INT.optionalFieldOf("d1_lesser_blooms_best", 0).forGetter(Entry::d1LesserBloomsBest),
                Codec.BOOL.optionalFieldOf("d1_completed_with_at_least_3_lesser_blooms", false).forGetter(Entry::d1CompletedWithAtLeast3LesserBlooms),
                Codec.INT.optionalFieldOf("lesser_blooms", 0).forGetter(Entry::lesserBlooms),
                Codec.INT.optionalFieldOf("cavern_residue", 0).forGetter(Entry::cavernResidue),
                Codec.BOOL.optionalFieldOf("village_access_unlocked", false).forGetter(Entry::villageAccessUnlocked),
                Codec.INT.optionalFieldOf("npc_unlock_tier_d1", 0).forGetter(Entry::npcUnlockTierD1),
                Codec.INT.optionalFieldOf("npc_unlock_tier_d2", 0).forGetter(Entry::npcUnlockTierD2),
                FLAG_SET_CODEC.optionalFieldOf("flags", Set.of()).forGetter(Entry::flags),
                Codec.INT.optionalFieldOf("d1_torch_flowers_best", 0).forGetter(e -> 0),
                Codec.BOOL.optionalFieldOf("d1_completed_with_at_least_3_torch_flowers", false).forGetter(e -> false)
        ).apply(inst, (best, completed, lesserBlooms, cavernResidue, village, d1Tier, d2Tier, flags, legacyBest, legacyCompleted) -> {
            int migratedBest = Math.max(clampLesserBloomsBest(best), clampLesserBloomsBest(legacyBest));
            boolean migratedCompleted = completed || legacyCompleted || migratedBest >= 3;
            return new Entry(migratedBest, migratedCompleted, lesserBlooms, cavernResidue, migratedCompleted || village, clampTier(d1Tier), clampTier(d2Tier), Set.copyOf(flags));
        }));
    }

    private static final Codec<Map<UUID, Entry>> DATA_CODEC = Codec.unboundedMap(UUID_CODEC, Entry.CODEC);

    private static final Codec<PlayerProgressionData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            DATA_CODEC.optionalFieldOf("player_progression", Map.of()).forGetter(d -> d.byPlayer)
    ).apply(inst, PlayerProgressionData::fromCodec));

    public static final SavedDataType<PlayerProgressionData> TYPE = new SavedDataType<>(SAVE_ID, PlayerProgressionData::new, CODEC);

    private final Map<UUID, Entry> byPlayer = new HashMap<>();

    private PlayerProgressionData() {}

    private static PlayerProgressionData fromCodec(Map<UUID, Entry> values) {
        PlayerProgressionData data = new PlayerProgressionData();
        if (values != null) data.byPlayer.putAll(values);
        return data;
    }

    public static PlayerProgressionData get(MinecraftServer server) {
        if (server == null) throw new IllegalArgumentException("server is null");
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public int getD1LesserBloomsBest(UUID playerId) { return getEntry(playerId).d1LesserBloomsBest(); }
    public boolean isD1CompletedWithAtLeast3LesserBlooms(UUID playerId) { return getEntry(playerId).d1CompletedWithAtLeast3LesserBlooms(); }
    public int getLesserBlooms(UUID playerId) { return getEntry(playerId).lesserBlooms(); }
    public int getCavernResidue(UUID playerId) { return getEntry(playerId).cavernResidue(); }
    public boolean isVillageAccessUnlocked(UUID playerId) { return getEntry(playerId).villageAccessUnlocked(); }
    public int getNpcUnlockTierD1(UUID playerId) { return getEntry(playerId).npcUnlockTierD1(); }
    public int getNpcUnlockTierD2(UUID playerId) { return getEntry(playerId).npcUnlockTierD2(); }
    public Set<String> getFlags(UUID playerId) { return Set.copyOf(getEntry(playerId).flags()); }

    public void setD1LesserBloomsBest(UUID playerId, int value) {
        mutate(playerId, e -> with(e, clampLesserBloomsBest(value), e.d1CompletedWithAtLeast3LesserBlooms(), e.lesserBlooms(), e.cavernResidue(), e.villageAccessUnlocked(), e.npcUnlockTierD1(), e.npcUnlockTierD2(), e.flags()));
    }

    public void setD1Completed(UUID playerId, boolean completed) {
        mutate(playerId, e -> with(e, e.d1LesserBloomsBest(), completed, e.lesserBlooms(), e.cavernResidue(), completed || e.villageAccessUnlocked(), e.npcUnlockTierD1(), e.npcUnlockTierD2(), e.flags()));
    }

    public void setVillageAccessUnlocked(UUID playerId, boolean unlocked) {
        mutate(playerId, e -> with(e, e.d1LesserBloomsBest(), e.d1CompletedWithAtLeast3LesserBlooms(), e.lesserBlooms(), e.cavernResidue(), unlocked, e.npcUnlockTierD1(), e.npcUnlockTierD2(), e.flags()));
    }

    public void setLesserBlooms(UUID playerId, int value) {
        int clamped = Math.max(0, value);
        mutate(playerId, e -> with(e, e.d1LesserBloomsBest(), e.d1CompletedWithAtLeast3LesserBlooms(), clamped, e.cavernResidue(), e.villageAccessUnlocked(), tierFromLesserBlooms(clamped), e.npcUnlockTierD2(), e.flags()));
    }

    public void setCavernResidue(UUID playerId, int value) {
        mutate(playerId, e -> with(e, e.d1LesserBloomsBest(), e.d1CompletedWithAtLeast3LesserBlooms(), e.lesserBlooms(), Math.max(0, value), e.villageAccessUnlocked(), e.npcUnlockTierD1(), e.npcUnlockTierD2(), e.flags()));
    }

    private interface EntryMutator { Entry apply(Entry entry); }

    private void mutate(UUID playerId, EntryMutator mutator) {
        if (playerId == null || mutator == null) return;
        Entry prev = getEntry(playerId);
        Entry next = mutator.apply(prev);
        if (!prev.equals(next)) {
            byPlayer.put(playerId, next);
            setDirty();
        }
    }

    private Entry getEntry(UUID playerId) {
        if (playerId == null) return defaults();
        return byPlayer.getOrDefault(playerId, defaults());
    }

    private static Entry defaults() { return new Entry(0, false, 0, 0, false, 0, 0, Set.of()); }
    private static Entry with(Entry prev, int best, boolean completed, int lesserBlooms, int cavernResidue, boolean village, int d1Tier, int d2Tier, Set<String> flags) {
        return new Entry(best, completed, lesserBlooms, cavernResidue, village, clampTier(d1Tier), clampTier(d2Tier), Set.copyOf(flags));
    }

    static int tierFromLesserBlooms(int lesserBlooms) {
        if (lesserBlooms >= 20) return 4;
        if (lesserBlooms >= 15) return 3;
        if (lesserBlooms >= 10) return 2;
        if (lesserBlooms >= 5) return 1;
        return 0;
    }

    private static int clampLesserBloomsBest(int value) { return Math.max(0, Math.min(6, value)); }
    private static int clampTier(int value) { return Math.max(0, Math.min(4, value)); }
}
