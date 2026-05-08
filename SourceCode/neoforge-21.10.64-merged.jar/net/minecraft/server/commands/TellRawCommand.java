package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

public class TellRawCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(
            Commands.literal("tellraw")
                .requires(Commands.hasPermission(2))
                .then(
                    Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("message", ComponentArgument.textComponent(context)).executes(p_400897_ -> {
                            int i = 0;

                            for (ServerPlayer serverplayer : EntityArgument.getPlayers(p_400897_, "targets")) {
                                serverplayer.sendSystemMessage(ComponentArgument.getResolvedComponent(p_400897_, "message", serverplayer), false);
                                i++;
                            }

                            return i;
                        }))
                )
        );
    }
}
