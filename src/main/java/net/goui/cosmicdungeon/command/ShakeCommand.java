// file: src/main/java/net/goui/cosmicdungeon/command/ShakeCommand.java
package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import net.goui.cosmicdungeon.network.ShakeScreenPayload;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import static net.goui.cosmicdungeon.network.ModNetwork.sendTo;

public final class ShakeCommand {
    private ShakeCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("shake")
                        .requires(source -> source.getEntity() instanceof ServerPlayer)
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            sendTo(player, new ShakeScreenPayload());
                            return 1;
                        })
        );
    }
}
