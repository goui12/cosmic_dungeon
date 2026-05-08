package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameModeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;

public class GameModeCommand {
    public static final int PERMISSION_LEVEL = 2;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("gamemode")
                .requires(Commands.hasPermission(2))
                .then(
                    Commands.argument("gamemode", GameModeArgument.gameMode())
                        .executes(
                            p_258228_ -> setMode(
                                p_258228_,
                                Collections.singleton(p_258228_.getSource().getPlayerOrException()),
                                GameModeArgument.getGameMode(p_258228_, "gamemode")
                            )
                        )
                        .then(
                            Commands.argument("target", EntityArgument.players())
                                .executes(
                                    p_258229_ -> setMode(
                                        p_258229_, EntityArgument.getPlayers(p_258229_, "target"), GameModeArgument.getGameMode(p_258229_, "gamemode")
                                    )
                                )
                        )
                )
        );
    }

    private static void logGamemodeChange(CommandSourceStack source, ServerPlayer player, GameType gameType) {
        Component component = Component.translatable("gameMode." + gameType.getName());
        if (source.getEntity() == player) {
            source.sendSuccess(() -> Component.translatable("commands.gamemode.success.self", component), true);
        } else {
            if (source.getLevel().getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK)) {
                player.sendSystemMessage(Component.translatable("gameMode.changed", component));
            }

            source.sendSuccess(() -> Component.translatable("commands.gamemode.success.other", player.getDisplayName(), component), true);
        }
    }

    private static int setMode(CommandContext<CommandSourceStack> source, Collection<ServerPlayer> players, GameType gameType) {
        int i = 0;

        for (ServerPlayer serverplayer : players) {
            if (setGameMode(source.getSource(), serverplayer, gameType)) {
                i++;
            }
        }

        return i;
    }

    public static void setGameMode(ServerPlayer player, GameType gameMode) {
        setGameMode(player.createCommandSourceStack(), player, gameMode);
    }

    private static boolean setGameMode(CommandSourceStack source, ServerPlayer player, GameType gameMode) {
        if (player.setGameMode(gameMode)) {
            logGamemodeChange(source, player, gameMode);
            return true;
        } else {
            return false;
        }
    }
}
