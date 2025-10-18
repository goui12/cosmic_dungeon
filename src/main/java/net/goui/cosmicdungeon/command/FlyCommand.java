package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class FlyCommand {
    private FlyCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("fly")
                        .requires(src -> src.hasPermission(2)) // OP-only
                        .executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();
                            ServerPlayer player = src.getPlayerOrException();

                            boolean newState = !player.getAbilities().mayfly;
                            player.getAbilities().mayfly = newState;
                            player.getAbilities().flying = newState;
                            player.onUpdateAbilities();

                            String msg = newState ? "Flight enabled." : "Flight disabled.";
                            src.sendSuccess(() -> Component.literal(msg), true);
                            return 1;
                        })
        );
    }
}
