package net.goui.cosmicdungeon.faction;

import com.mojang.serialization.Codec;
import net.goui.cosmicdungeon.dungeon.d1.WatsonReceipt;
import net.goui.cosmicdungeon.transaction.SavedDataProof;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerFactionData extends SavedData {
    public static final String SAVE_ID = "cosmicdungeon_player_factions_v1";

    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
    private static final Codec<Map<String, Integer>> FACTION_VALUE_CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT);
    private static final Codec<Map<UUID, Map<String, Integer>>> PLAYER_FACTION_MAP_CODEC = Codec.unboundedMap(UUID_CODEC, FACTION_VALUE_CODEC);

    private static final Codec<PlayerFactionData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            PLAYER_FACTION_MAP_CODEC.optionalFieldOf("player_factions", Map.of()).forGetter(data -> data.valuesByPlayer),
            WatsonReceipt.MAP_CODEC.optionalFieldOf("watson_receipts", Map.of()).forGetter(data -> data.watsonReceipts)
    ).apply(inst, PlayerFactionData::fromCodec));

    public static final SavedDataType<PlayerFactionData> TYPE = new SavedDataType<>(SAVE_ID, PlayerFactionData::new, CODEC);

    private final Map<UUID, Map<String, Integer>> valuesByPlayer = new HashMap<>();

    private PlayerFactionData() {}

    private final Map<UUID, WatsonReceipt> watsonReceipts = new HashMap<>();
    private MinecraftServer server;
    private static PlayerFactionData fromCodec(Map<UUID, Map<String, Integer>> values, Map<UUID, WatsonReceipt> receipts) {
        WatsonReceipt.validate(receipts);
        PlayerFactionData data = new PlayerFactionData();
        data.watsonReceipts.putAll(receipts);
        if (values != null) {
            for (Map.Entry<UUID, Map<String, Integer>> entry : values.entrySet()) {
                data.valuesByPlayer.put(entry.getKey(), new HashMap<>(entry.getValue()));
            }
        }
        return data;
    }

    public static PlayerFactionData get(MinecraftServer server) {
        if (server == null) throw new IllegalArgumentException("server is null");
        SavedDataProof.validate(server, SAVE_ID, CODEC);
        ServerLevel overworld = server.overworld();
        var data = overworld.getDataStorage().computeIfAbsent(TYPE); data.server = server; return data;
    }

    public boolean flushVerified() { return SavedDataProof.save(server, SAVE_ID, CODEC, this); }
    public void applyWatson(WatsonReceipt receipt, long bonus) {
        if (bonus < 0 || bonus > 400) throw new IllegalArgumentException("Invalid Watson faction bonus");
        if (!receipt.shouldApply(watsonReceipts.get(receipt.owner()))) return;
        if (receipt.success()) setValue(receipt.owner(), FactionDefinitions.NPC_ID,
                (int) Math.min(100L, getValue(receipt.owner(), FactionDefinitions.NPC_ID) + bonus));
        watsonReceipts.put(receipt.owner(), receipt); setDirty();
    }

    public int getValue(UUID playerId, ResourceLocation factionId) {
        if (playerId == null || factionId == null) return 0;
        FactionDefinition definition = FactionDefinitions.get(factionId);
        if (definition == null) return 0;

        Map<String, Integer> perFaction = valuesByPlayer.get(playerId);
        if (perFaction == null) return definition.startingValue();

        Integer stored = perFaction.get(factionId.toString());
        return stored == null ? definition.startingValue() : definition.clamp(stored);
    }

    public void setValue(UUID playerId, ResourceLocation factionId, int value) {
        if (playerId == null || factionId == null) return;
        FactionDefinition definition = FactionDefinitions.get(factionId);
        if (definition == null) return;

        int clamped = definition.clamp(value);
        int start = definition.startingValue();

        Map<String, Integer> perFaction = valuesByPlayer.computeIfAbsent(playerId, k -> new HashMap<>());
        String key = factionId.toString();

        if (clamped == start) {
            if (perFaction.remove(key) != null) setDirty();
            if (perFaction.isEmpty() && valuesByPlayer.remove(playerId) != null) setDirty();
            return;
        }

        Integer prev = perFaction.put(key, clamped);
        if (prev == null || prev != clamped) setDirty();
    }

    public Map<String, Integer> getStoredValues(UUID playerId) {
        Map<String, Integer> perFaction = valuesByPlayer.get(playerId);
        return perFaction == null ? Map.of() : Map.copyOf(perFaction);
    }
}
