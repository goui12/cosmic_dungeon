package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.goui.cosmicdungeon.auth.OpUtil;
import net.goui.cosmicdungeon.auth.PasswordStore;
import net.goui.cosmicdungeon.auth.Rank;
import net.goui.cosmicdungeon.auth.RankStore;
import net.goui.cosmicdungeon.dungeon.DungeonTravelRouter;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

import java.util.Collection;
import java.util.Locale;
import java.util.UUID;

public final class RankCommand {
    private RankCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("rank")
                        .executes(ctx -> sendHelpHint(ctx.getSource()))
                        .then(Commands.literal("help")
                                .executes(ctx -> sendHelp(ctx.getSource())))
                        .then(Commands.literal("password")
                                .then(Commands.argument("oldPassword", StringArgumentType.string())
                                        .then(Commands.argument("newPassword", StringArgumentType.string())
                                                .executes(ctx -> {
                                                    CommandSourceStack src = ctx.getSource();
                                                    PasswordStore pw = PasswordStore.get(src.getServer());

                                                    String oldPw = StringArgumentType.getString(ctx, "oldPassword");
                                                    String newPw = StringArgumentType.getString(ctx, "newPassword");

                                                    if (!pw.matches(oldPw)) {
                                                        src.sendFailure(Component.literal("Old password is incorrect. Type /rank help.")
                                                                .withStyle(ChatFormatting.RED));
                                                        return 0;
                                                    }
                                                    if (newPw.isBlank()) {
                                                        src.sendFailure(Component.literal("New password cannot be blank. Type /rank help.")
                                                                .withStyle(ChatFormatting.RED));
                                                        return 0;
                                                    }

                                                    pw.setPassword(newPw);
                                                    src.sendSuccess(() -> Component.literal("Rank password updated.")
                                                            .withStyle(ChatFormatting.GREEN), true);
                                                    return 1;
                                                })
                                        )
                                )
                                .executes(ctx -> sendHelpHint(ctx.getSource()))
                        )
                        .then(Commands.argument("target", GameProfileArgument.gameProfile())
                                .then(Commands.argument("rank", StringArgumentType.word())
                                        .then(Commands.argument("password", StringArgumentType.greedyString())
                                                .executes(ctx -> {
                                                    final CommandSourceStack src = ctx.getSource();
                                                    final MinecraftServer server = src.getServer();

                                                    final PasswordStore pw = PasswordStore.get(server);
                                                    final String password = StringArgumentType.getString(ctx, "password");
                                                    if (!pw.matches(password)) {
                                                        src.sendFailure(Component.literal("Invalid password. Type /rank help.")
                                                                .withStyle(ChatFormatting.RED));
                                                        return 0;
                                                    }

                                                    final Rank rankFinal = parseRank(StringArgumentType.getString(ctx, "rank"));
                                                    if (rankFinal == null) {
                                                        src.sendFailure(Component.literal("Unknown rank. Use developer or dungeoneer. Type /rank help.")
                                                                .withStyle(ChatFormatting.RED));
                                                        return 0;
                                                    }

                                                    final RankStore store = RankStore.get(server);
                                                    final Collection<NameAndId> targets = GameProfileArgument.getGameProfiles(ctx, "target");
                                                    if (targets.isEmpty()) {
                                                        src.sendFailure(Component.literal("No target profile found. Type /rank help.")
                                                                .withStyle(ChatFormatting.RED));
                                                        return 0;
                                                    }

                                                    int changed = 0;
                                                    for (NameAndId ni : targets) {
                                                        final UUID id = ni.id();
                                                        if (id == null) continue;

                                                        store.setRank(id, rankFinal);
                                                        changed++;

                                                        final ServerPlayer online = server.getPlayerList().getPlayer(id);
                                                        if (online != null) {
                                                            OpUtil.setOperator(server, online, rankFinal.isDeveloper());
                                                            if (rankFinal.isDeveloper()) {
                                                                SpawnerLabelServerState.sync(online);
                                                            } else {
                                                                SpawnerLabelServerState.revoke(online);
                                                                DungeonTravelRouter.evacuateUnauthorizedLocation(online);
                                                            }
                                                        }
                                                    }

                                                    final int changedFinal = changed;
                                                    src.sendSuccess(
                                                            () -> Component.literal("Set rank for " + changedFinal + " profile(s) to " + rankFinal.name() + ".")
                                                                    .withStyle(ChatFormatting.GREEN),
                                                            true
                                                    );
                                                    return changedFinal > 0 ? 1 : 0;
                                                })
                                        )
                                        .executes(ctx -> sendHelpHint(ctx.getSource()))
                                )
                        )
        );
    }

    private static Rank parseRank(String raw) {
        if (raw == null) return null;
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "developer", "dev", "admin" -> Rank.DEVELOPER;
            case "dungeoneer", "dungeon", "player" -> Rank.DUNGEONEER;
            default -> null;
        };
    }

    private static int sendHelpHint(CommandSourceStack src) {
        src.sendFailure(Component.literal("Invalid /rank usage. Type /rank help.")
                .withStyle(ChatFormatting.RED));
        return 0;
    }

    private static int sendHelp(CommandSourceStack src) {
        src.sendSuccess(() -> Component.literal("Rank commands:").withStyle(ChatFormatting.GOLD), false);
        src.sendSuccess(() -> Component.literal("- /rank <name> <developer|dungeoneer> <password>")
                .withStyle(ChatFormatting.YELLOW), false);
        src.sendSuccess(() -> Component.literal("- /rank password <oldpassword> <newpassword>")
                .withStyle(ChatFormatting.YELLOW), false);
        src.sendSuccess(() -> Component.literal("- /rank help")
                .withStyle(ChatFormatting.YELLOW), false);
        src.sendSuccess(() -> Component.literal("Alternates:")
                .withStyle(ChatFormatting.GOLD), false);
        src.sendSuccess(() -> Component.literal("- /developer <password> (self-promote)")
                .withStyle(ChatFormatting.YELLOW), false);
        src.sendSuccess(() -> Component.literal("- /dungeoneer (developers only, self-demote)")
                .withStyle(ChatFormatting.YELLOW), false);
        src.sendSuccess(() -> Component.literal("One shared server password is used for rank changes.")
                .withStyle(ChatFormatting.GRAY), false);
        return 1;
    }
}
