package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class SpectateCommand {
    private static final SimpleCommandExceptionType ERROR_SELF = new SimpleCommandExceptionType(Component.translatable("commands.spectate.self"));
    private static final DynamicCommandExceptionType ERROR_NOT_SPECTATOR = new DynamicCommandExceptionType(
        p_304298_ -> Component.translatableEscape("commands.spectate.not_spectator", p_304298_)
    );
    private static final DynamicCommandExceptionType ERROR_CANNOT_SPECTATE = new DynamicCommandExceptionType(
        p_445293_ -> Component.translatableEscape("commands.spectate.cannot_spectate", p_445293_)
    );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("spectate")
                .requires(Commands.hasPermission(2))
                .executes(p_138692_ -> spectate(p_138692_.getSource(), null, p_138692_.getSource().getPlayerOrException()))
                .then(
                    Commands.argument("target", EntityArgument.entity())
                        .executes(
                            p_138690_ -> spectate(
                                p_138690_.getSource(), EntityArgument.getEntity(p_138690_, "target"), p_138690_.getSource().getPlayerOrException()
                            )
                        )
                        .then(
                            Commands.argument("player", EntityArgument.player())
                                .executes(
                                    p_138680_ -> spectate(
                                        p_138680_.getSource(), EntityArgument.getEntity(p_138680_, "target"), EntityArgument.getPlayer(p_138680_, "player")
                                    )
                                )
                        )
                )
        );
    }

    private static int spectate(CommandSourceStack source, @Nullable Entity target, ServerPlayer player) throws CommandSyntaxException {
        if (player == target) {
            throw ERROR_SELF.create();
        } else if (!player.isSpectator()) {
            throw ERROR_NOT_SPECTATOR.create(player.getDisplayName());
        } else if (target != null && target.getType().clientTrackingRange() == 0) {
            throw ERROR_CANNOT_SPECTATE.create(target.getDisplayName());
        } else {
            player.setCamera(target);
            if (target != null) {
                source.sendSuccess(() -> Component.translatable("commands.spectate.success.started", target.getDisplayName()), false);
            } else {
                source.sendSuccess(() -> Component.translatable("commands.spectate.success.stopped"), false);
            }

            return 1;
        }
    }
}
