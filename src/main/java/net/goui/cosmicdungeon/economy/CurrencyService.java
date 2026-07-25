package net.goui.cosmicdungeon.economy;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.goui.cosmicdungeon.dungeon.FarrowsChopTravelService;

public final class CurrencyService {
    private CurrencyService() {}

    public static long getBalanceTrace(ServerPlayer player) {
        PlayerCurrencyData data = getData(player);
        return data == null ? 0L : data.getBalanceTrace(player.getUUID());
    }

    public static String getFormattedBalance(ServerPlayer player) {
        return CurrencyAmount.ofTrace(getBalanceTrace(player)).formatNormalized();
    }

    public static boolean tryDeposit(ServerPlayer player, long traceAmount) {
        if (FarrowsChopTravelService.isOutsideEscrow(player)) return false;
        PlayerCurrencyData data = getData(player);
        return data != null && data.tryDeposit(player.getUUID(), traceAmount);
    }

    public static boolean tryWithdraw(ServerPlayer player, long traceAmount) {
        if (FarrowsChopTravelService.isOutsideEscrow(player)) return false;
        PlayerCurrencyData data = getData(player);
        return data != null && data.tryWithdraw(player.getUUID(), traceAmount);
    }

    public static boolean canDeposit(ServerPlayer player, long traceAmount) {
        if (FarrowsChopTravelService.isOutsideEscrow(player)) return false;
        PlayerCurrencyData data = getData(player);
        return data != null && data.canDeposit(player.getUUID(), traceAmount);
    }

    public static long getCapacity(ServerPlayer player) {
        PlayerCurrencyData data = getData(player);
        return data == null ? PlayerCurrencyData.DEFAULT_CAPACITY_TRACE : data.getCapacityTrace(player.getUUID());
    }

    public static void setBalanceTrace(ServerPlayer player, long traceAmount) {
        PlayerCurrencyData data = getData(player);
        if (data != null) data.setBalanceTrace(player.getUUID(), traceAmount);
    }

    public static void setCapacity(ServerPlayer player, long capacityTrace) {
        PlayerCurrencyData data = getData(player);
        if (data != null) data.setCapacityTrace(player.getUUID(), capacityTrace);
    }

    public static void clear(ServerPlayer player) {
        PlayerCurrencyData data = getData(player);
        if (data != null) data.clear(player.getUUID());
    }

    private static PlayerCurrencyData getData(ServerPlayer player) {
        if (player == null) return null;
        MinecraftServer server = player.level().getServer();
        if (server == null) return null;
        return PlayerCurrencyData.get(server);
    }
}
