package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import net.goui.cosmicdungeon.auth.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class DungeoneerCommand {
    private DungeoneerCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("dungeoneer")
                        .requires(AccessPolicy::requireDeveloperOrConsole)
                        .executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();
                            ServerPlayer p = src.getPlayer();
                            if (p == null) {
                                src.sendFailure(Component.literal("Console must use /rank for a target."));
                                return 0;
                            }

                            MinecraftServer server = p.level().getServer();
                            if (server == null) return 0;

                            RankStore store = RankStore.get(server);
                            store.setRank(p.getUUID(), Rank.DUNGEONEER);

                            // Apply deop immediately
                            OpUtil.setOperator(server, p, false);

                            src.sendSuccess(() -> Component.literal("You are now DUNGEONEER.")
                                    .withStyle(ChatFormatting.GREEN), true);
                            return 1;
                        })
        );
    }
}
