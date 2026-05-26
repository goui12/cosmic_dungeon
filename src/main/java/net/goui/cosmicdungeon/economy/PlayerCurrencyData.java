package net.goui.cosmicdungeon.economy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerCurrencyData extends SavedData {
    public static final String SAVE_ID = "cosmicdungeon_player_currency_v1";
    public static final long DEFAULT_CAPACITY_TRACE = 10_000L;

    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
    private static final Codec<Map<UUID, Long>> UUID_LONG_MAP_CODEC = Codec.unboundedMap(UUID_CODEC, Codec.LONG);

    private static final Codec<PlayerCurrencyData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            UUID_LONG_MAP_CODEC.optionalFieldOf("balances", Map.of()).forGetter(data -> data.balanceByPlayer),
            UUID_LONG_MAP_CODEC.optionalFieldOf("capacity_overrides", Map.of()).forGetter(data -> data.capacityOverrideByPlayer)
    ).apply(inst, PlayerCurrencyData::fromCodec));

    public static final SavedDataType<PlayerCurrencyData> TYPE = new SavedDataType<>(
            SAVE_ID,
            PlayerCurrencyData::new,
            CODEC
    );

    private final Map<UUID, Long> balanceByPlayer = new HashMap<>();
    private final Map<UUID, Long> capacityOverrideByPlayer = new HashMap<>();

    private PlayerCurrencyData() {}

    private static PlayerCurrencyData fromCodec(Map<UUID, Long> balances, Map<UUID, Long> overrides) {
        PlayerCurrencyData data = new PlayerCurrencyData();
        if (balances != null) data.balanceByPlayer.putAll(balances);
        if (overrides != null) data.capacityOverrideByPlayer.putAll(overrides);
        return data;
    }

    public static PlayerCurrencyData get(MinecraftServer server) {
        if (server == null) throw new IllegalArgumentException("server is null");
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public long getBalanceTrace(UUID playerId) {
        if (playerId == null) return 0L;
        return Math.max(0L, balanceByPlayer.getOrDefault(playerId, 0L));
    }

    public long getCapacityTrace(UUID playerId) {
        if (playerId == null) return DEFAULT_CAPACITY_TRACE;
        return Math.max(0L, capacityOverrideByPlayer.getOrDefault(playerId, DEFAULT_CAPACITY_TRACE));
    }

    public void setBalanceTrace(UUID playerId, long traceAmount) {
        if (playerId == null) return;
        long clamped = Math.max(0L, traceAmount);
        long capacity = getCapacityTrace(playerId);
        if (clamped > capacity) clamped = capacity;

        if (clamped == 0L) {
            if (balanceByPlayer.remove(playerId) != null) setDirty();
            return;
        }

        balanceByPlayer.put(playerId, clamped);
        setDirty();
    }

    public void setCapacityTrace(UUID playerId, long capacityTrace) {
        if (playerId == null) return;
        long clamped = Math.max(0L, capacityTrace);
        if (clamped == DEFAULT_CAPACITY_TRACE) {
            if (capacityOverrideByPlayer.remove(playerId) != null) setDirty();
        } else {
            capacityOverrideByPlayer.put(playerId, clamped);
            setDirty();
        }
        long balance = getBalanceTrace(playerId);
        if (balance > clamped) {
            setBalanceTrace(playerId, clamped);
        }
    }

    public boolean canDeposit(UUID playerId, long traceAmount) {
        if (playerId == null || traceAmount <= 0L) return false;
        long balance = getBalanceTrace(playerId);
        long capacity = getCapacityTrace(playerId);
        return balance <= capacity - traceAmount;
    }

    public boolean tryDeposit(UUID playerId, long traceAmount) {
        if (!canDeposit(playerId, traceAmount)) return false;
        long next = getBalanceTrace(playerId) + traceAmount;
        setBalanceTrace(playerId, next);
        return true;
    }

    public boolean tryWithdraw(UUID playerId, long traceAmount) {
        if (playerId == null || traceAmount <= 0L) return false;
        long balance = getBalanceTrace(playerId);
        if (balance < traceAmount) return false;
        setBalanceTrace(playerId, balance - traceAmount);
        return true;
    }

    public void clear(UUID playerId) {
        if (playerId == null) return;
        boolean removed = balanceByPlayer.remove(playerId) != null;
        removed |= capacityOverrideByPlayer.remove(playerId) != null;
        if (removed) setDirty();
    }
}
