package net.goui.cosmicdungeon.auth;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class OpUtil {
    private OpUtil() {}

    public static void setOperator(MinecraftServer server, ServerPlayer player, boolean shouldBeOp) {
        if (server == null || player == null) return;

        if (shouldBeOp) {
            server.getPlayerList().op(player.nameAndId());
        } else {
            server.getPlayerList().deop(player.nameAndId());
        }
    }
}
