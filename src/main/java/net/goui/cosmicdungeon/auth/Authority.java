package net.goui.cosmicdungeon.auth;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class Authority {
    private Authority() {}

    public static Rank getRank(ServerPlayer player) {
        if (player == null) return Rank.DUNGEONEER;
        return RankStore.get(player.level()).getRank(player.getUUID());
    }

    public static boolean isDeveloper(ServerPlayer player) {
        return getRank(player).isDeveloper();
    }

    /** Pure predicate: console allowed OR developer. NO CHAT/SPAM HERE. */
    public static boolean isDeveloperOrConsole(CommandSourceStack src) {
        if (src == null) return false;
        ServerPlayer p = src.getPlayer();
        if (p == null) return true; // console / rcon / command block
        return isDeveloper(p);
    }

    /** Call this only from executes() when denying. */
    public static int deny(CommandSourceStack src) {
        if (src != null) {
            src.sendFailure(Component.literal("You do not have permission to use this command.")
                    .withStyle(ChatFormatting.RED));
        }
        return 0;
    }
}
