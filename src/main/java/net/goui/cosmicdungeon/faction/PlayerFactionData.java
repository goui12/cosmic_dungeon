package net.goui.cosmicdungeon.faction;

import com.mojang.serialization.Codec;
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
            PLAYER_FACTION_MAP_CODEC.optionalFieldOf("player_factions", Map.of()).forGetter(data -> data.valuesByPlayer)
    ).apply(inst, PlayerFactionData::fromCodec));

    public static final SavedDataType<PlayerFactionData> TYPE = new SavedDataType<>(SAVE_ID, PlayerFactionData::new, CODEC);

    private final Map<UUID, Map<String, Integer>> valuesByPlayer = new HashMap<>();

    private PlayerFactionData() {}

    private static PlayerFactionData fromCodec(Map<UUID, Map<String, Integer>> values) {
        PlayerFactionData data = new PlayerFactionData();
        if (values != null) {
            for (Map.Entry<UUID, Map<String, Integer>> entry : values.entrySet()) {
                data.valuesByPlayer.put(entry.getKey(), new HashMap<>(entry.getValue()));
            }
        }
        return data;
    }

    public static PlayerFactionData get(MinecraftServer server) {
        if (server == null) throw new IllegalArgumentException("server is null");
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(TYPE);
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
