package net.goui.cosmicdungeon.dungeon.d1;

import com.mojang.serialization.Codec;
import net.goui.cosmicdungeon.transaction.SavedDataProof;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import java.util.*;

/** Lifetime totals are independent of objective reset and use UUIDs, never player names. */
public final class D1LifetimeData extends SavedData {
    public record Totals(long spectralBlooms, long lesserBlooms, long completions, long lastCompletedRun, long successfulKills) {
        public static final Totals EMPTY = new Totals(0, 0, 0, 0, 0);
        private static final Codec<Totals> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.optionalFieldOf("spectral_blooms", 0L).forGetter(Totals::spectralBlooms),
                Codec.LONG.optionalFieldOf("lesser_blooms", 0L).forGetter(Totals::lesserBlooms),
                Codec.LONG.optionalFieldOf("completions", 0L).forGetter(Totals::completions),
                Codec.LONG.optionalFieldOf("last_completed_run", 0L).forGetter(Totals::lastCompletedRun),
                Codec.LONG.optionalFieldOf("successful_kills", 0L).forGetter(Totals::successfulKills)
        ).apply(i, Totals::new));
    }
    private static final Codec<D1LifetimeData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING.xmap(UUID::fromString, UUID::toString), Totals.CODEC)
                    .optionalFieldOf("players", Map.of()).forGetter((D1LifetimeData d) -> d.players),
            WatsonReceipt.MAP_CODEC.optionalFieldOf("watson_receipts", Map.of()).forGetter(d -> d.watsonReceipts)
    ).apply(i, D1LifetimeData::load));
    private static final SavedDataType<D1LifetimeData> TYPE =
            new SavedDataType<>("cosmicdungeon_d1_lifetime_v1", D1LifetimeData::new, CODEC);
    private final Map<UUID, Totals> players = new HashMap<>();
    private D1LifetimeData() {}
    private final Map<UUID, WatsonReceipt> watsonReceipts = new HashMap<>();
    private MinecraftServer server;
    private static D1LifetimeData load(Map<UUID, Totals> players, Map<UUID, WatsonReceipt> receipts) {
        WatsonReceipt.validate(receipts);
        D1LifetimeData data = new D1LifetimeData(); data.players.putAll(players); data.watsonReceipts.putAll(receipts); return data;
    }
    public static D1LifetimeData get(MinecraftServer server) {
        SavedDataProof.validate(server, "cosmicdungeon_d1_lifetime_v1", CODEC);
        var data = server.overworld().getDataStorage().computeIfAbsent(TYPE); data.server = server; return data;
    }
    public boolean flushVerified() { return SavedDataProof.save(server, "cosmicdungeon_d1_lifetime_v1", CODEC, this); }
    public void applyWatson(WatsonReceipt receipt, int kills, int lesser) {
        if (kills < 0 || lesser < 0) throw new IllegalArgumentException("Negative Watson lifetime reward");
        if (!receipt.shouldApply(watsonReceipts.get(receipt.owner()))) return;
        if (receipt.success()) {
            Totals old = totals(receipt.owner());
            if (receipt.run() <= old.lastCompletedRun()) throw new IllegalStateException("Legacy completion has no matching Watson receipt");
            players.put(receipt.owner(), new Totals(D1ObjectiveRules.saturatingAdd(old.spectralBlooms(), 6),
                    D1ObjectiveRules.saturatingAdd(old.lesserBlooms(), lesser),
                    D1ObjectiveRules.saturatingAdd(old.completions(), 1), receipt.run(),
                    D1ObjectiveRules.saturatingAdd(old.successfulKills(), kills)));
        }
        watsonReceipts.put(receipt.owner(), receipt); setDirty();
    }
    public Totals totals(UUID player) { return players.getOrDefault(player, Totals.EMPTY); }
    public void recordLesserBlooms(UUID player, int amount) {
        if (amount <= 0) return;
        Totals old = totals(player);
        players.put(player, new Totals(old.spectralBlooms(), D1ObjectiveRules.saturatingAdd(old.lesserBlooms(), amount),
                old.completions(), old.lastCompletedRun(), old.successfulKills()));
        setDirty();
    }
    public boolean complete(UUID player, long runId) { return complete(player, runId, 0); }
    public boolean complete(UUID player, long runId, int runKills) {
        Totals old = totals(player);
        if (runId <= old.lastCompletedRun()) return false;
        players.put(player, new Totals(D1ObjectiveRules.saturatingAdd(old.spectralBlooms(), 6),
                old.lesserBlooms(), D1ObjectiveRules.saturatingAdd(old.completions(), 1), runId,
                D1ObjectiveRules.saturatingAdd(old.successfulKills(), Math.max(0, runKills))));
        setDirty(); return true;
    }
}
