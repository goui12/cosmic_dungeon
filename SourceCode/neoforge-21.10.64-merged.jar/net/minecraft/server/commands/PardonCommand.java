package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanList;

public class PardonCommand {
    private static final SimpleCommandExceptionType ERROR_NOT_BANNED = new SimpleCommandExceptionType(Component.translatable("commands.pardon.failed"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("pardon")
                .requires(Commands.hasPermission(3))
                .then(
                    Commands.argument("targets", GameProfileArgument.gameProfile())
                        .suggests(
                            (p_138098_, p_138099_) -> SharedSuggestionProvider.suggest(
                                p_138098_.getSource().getServer().getPlayerList().getBans().getUserList(), p_138099_
                            )
                        )
                        .executes(p_138096_ -> pardonPlayers(p_138096_.getSource(), GameProfileArgument.getGameProfiles(p_138096_, "targets")))
                )
        );
    }

    private static int pardonPlayers(CommandSourceStack source, Collection<NameAndId> gameProfiles) throws CommandSyntaxException {
        UserBanList userbanlist = source.getServer().getPlayerList().getBans();
        int i = 0;

        for (NameAndId nameandid : gameProfiles) {
            if (userbanlist.isBanned(nameandid)) {
                userbanlist.remove(nameandid);
                i++;
                source.sendSuccess(() -> Component.translatable("commands.pardon.success", Component.literal(nameandid.name())), true);
            }
        }

        if (i == 0) {
            throw ERROR_NOT_BANNED.create();
        } else {
            return i;
        }
    }
}
