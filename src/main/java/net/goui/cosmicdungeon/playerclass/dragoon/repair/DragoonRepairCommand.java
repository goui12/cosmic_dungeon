package net.goui.cosmicdungeon.playerclass.dragoon.repair;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

public final class DragoonRepairCommand {
    private static final Logger LOGGER = LogUtils.getLogger();

    private DragoonRepairCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("repair")
                .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> request(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))
                .then(Commands.literal("accept").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> accept(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("deny").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> deny(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("cancel").executes(ctx -> cancel(ctx.getSource()))));
    }

    private static int request(CommandSourceStack src, ServerPlayer target) {
        return withPlayer(src, "send a repair invite", dragoon -> {
            DragoonRepairSessionData.invite(dragoon, target);
            return 1;
        });
    }

    private static int accept(CommandSourceStack src, ServerPlayer dragoon) {
        return withPlayer(src, "accept a repair invite", target -> DragoonRepairSessionData.acceptInvite(target, dragoon) ? 1 : 0);
    }

    private static int deny(CommandSourceStack src, ServerPlayer dragoon) {
        return withPlayer(src, "deny a repair invite", target -> DragoonRepairSessionData.denyInvite(target, dragoon) ? 1 : 0);
    }

    private static int cancel(CommandSourceStack src) {
        return withPlayer(src, "cancel Repair Affinity", player -> {
            DragoonRepairSessionData.cancel(player, "Cancelled");
            return 1;
        });
    }

    private static int withPlayer(CommandSourceStack src, String action, RepairAction actionCallback) {
        try {
            return actionCallback.run(src.getPlayerOrException());
        } catch (CommandSyntaxException e) {
            src.sendFailure(Component.literal("Only players can " + action + "."));
            return 0;
        } catch (RuntimeException e) {
            LOGGER.error("Failed to {} for command source {}", action, src.getTextName(), e);
            src.sendFailure(Component.literal("Unable to " + action + " right now. Check distance, class, availability, and invite state."));
            return 0;
        }
    }

    @FunctionalInterface
    private interface RepairAction {
        int run(ServerPlayer player) throws CommandSyntaxException;
    }
}
