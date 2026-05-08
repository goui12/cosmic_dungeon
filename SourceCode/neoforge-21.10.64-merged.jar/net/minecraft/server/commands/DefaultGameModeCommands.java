package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameModeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;

public class DefaultGameModeCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("defaultgamemode")
                .requires(Commands.hasPermission(2))
                .then(
                    Commands.argument("gamemode", GameModeArgument.gameMode())
                        .executes(p_258227_ -> setMode(p_258227_.getSource(), GameModeArgument.getGameMode(p_258227_, "gamemode")))
                )
        );
    }

    /**
     * Sets the {@link net.minecraft.world.level.GameType} of the player who ran the command.
     */
    private static int setMode(CommandSourceStack commandSource, GameType gamemode) {
        MinecraftServer minecraftserver = commandSource.getServer();
        minecraftserver.setDefaultGameType(gamemode);
        int i = minecraftserver.enforceGameTypeForPlayers(minecraftserver.getForcedGameType());
        commandSource.sendSuccess(() -> Component.translatable("commands.defaultgamemode.success", gamemode.getLongDisplayName()), true);
        return i;
    }
}
