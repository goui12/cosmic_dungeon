package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

public final class SurvivalCommand {
    private SurvivalCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("survival")
                        .requires(src -> src.hasPermission(2)) // OP-only
                        // /survival (self)
                        .executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();
                            ServerPlayer player = src.getPlayerOrException();

                            player.setGameMode(GameType.SURVIVAL);
                            src.sendSuccess(() -> Component.literal(
                                    "Set " + player.getGameProfile().getName() + " to Survival."), true);
                            return 1;
                        })
                        // /survival <target>
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> {
                                    CommandSourceStack src = ctx.getSource();
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");

                                    target.setGameMode(GameType.SURVIVAL);
                                    src.sendSuccess(() -> Component.literal(
                                            "Set " + target.getGameProfile().getName() + " to Survival."), true);
                                    return 1;
                                })
                        )
        );
    }
}
