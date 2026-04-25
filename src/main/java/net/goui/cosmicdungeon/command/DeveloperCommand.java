package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.goui.cosmicdungeon.auth.OpUtil;
import net.goui.cosmicdungeon.auth.PasswordStore;
import net.goui.cosmicdungeon.auth.Rank;
import net.goui.cosmicdungeon.auth.RankStore;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class DeveloperCommand {
    private DeveloperCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("developer")
                        .executes(ctx -> {
                            ctx.getSource().sendFailure(Component.literal("Usage: /developer <password>. Type /rank help.")
                                    .withStyle(ChatFormatting.RED));
                            return 0;
                        })
                        .then(Commands.argument("password", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    CommandSourceStack src = ctx.getSource();
                                    ServerPlayer p = src.getPlayer();
                                    if (p == null) {
                                        src.sendFailure(Component.literal("Console must use /rank <name> <rank> <password>."));
                                        return 0;
                                    }

                                    MinecraftServer server = p.level().getServer();
                                    if (server == null) return 0;

                                    PasswordStore pw = PasswordStore.get(server);
                                    String password = StringArgumentType.getString(ctx, "password");
                                    if (!pw.matches(password)) {
                                        src.sendFailure(Component.literal("Invalid password. Type /rank help.")
                                                .withStyle(ChatFormatting.RED));
                                        return 0;
                                    }

                                    RankStore store = RankStore.get(server);
                                    store.setRank(p.getUUID(), Rank.DEVELOPER);
                                    OpUtil.setOperator(server, p, true);

                                    src.sendSuccess(() -> Component.literal("You are now DEVELOPER.")
                                            .withStyle(ChatFormatting.GREEN), true);
                                    return 1;
                                })
                        )
        );
    }
}
