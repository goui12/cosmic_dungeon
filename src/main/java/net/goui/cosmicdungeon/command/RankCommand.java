package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.goui.cosmicdungeon.auth.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

import java.util.Collection;
import java.util.UUID;

public final class RankCommand {
    private RankCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("rank")
                        .requires(AccessPolicy::requireDeveloperOrConsole)

                        .then(Commands.argument("target", GameProfileArgument.gameProfile())
                                .then(Commands.argument("rank", StringArgumentType.word())
                                        .then(Commands.argument("password", StringArgumentType.greedyString())
                                                .executes(ctx -> {
                                                    final CommandSourceStack src = ctx.getSource();
                                                    final MinecraftServer server = src.getServer();

                                                    // password gate
                                                    final PasswordStore pw = PasswordStore.get(server);
                                                    final String password = StringArgumentType.getString(ctx, "password");
                                                    if (!pw.matches(password)) {
                                                        src.sendFailure(Component.literal("Invalid password.")
                                                                .withStyle(ChatFormatting.RED));
                                                        return 0;
                                                    }

                                                    final Rank rankFinal = Rank.fromString(StringArgumentType.getString(ctx, "rank"));
                                                    final RankStore store = RankStore.get(server);

                                                    // NeoForge/Mojmap: this is Collection<NameAndId> now
                                                    final Collection<NameAndId> targets = GameProfileArgument.getGameProfiles(ctx, "target");
                                                    if (targets.isEmpty()) {
                                                        src.sendFailure(Component.literal("No target profile found.")
                                                                .withStyle(ChatFormatting.RED));
                                                        return 0;
                                                    }

                                                    int changed = 0;

                                                    for (NameAndId ni : targets) {
                                                        // NameAndId has an id() accessor in these mappings
                                                        final UUID id = ni.id();
                                                        if (id == null) continue;

                                                        store.setRank(id, rankFinal);
                                                        changed++;

                                                        // If online, sync OP state immediately
                                                        final ServerPlayer online = server.getPlayerList().getPlayer(id);
                                                        if (online != null) {
                                                            OpUtil.setOperator(server, online, rankFinal.isDeveloper());
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
                                )
                        )
        );
    }
}
