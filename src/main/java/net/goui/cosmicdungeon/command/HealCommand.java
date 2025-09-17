package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class HealCommand {
    private HealCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("heal")
                        .requires(src -> src.hasPermission(2)) // OP-only
                        // /heal (self)
                        .executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();
                            ServerPlayer player = src.getPlayerOrException();

                            healPlayer(player);
                            src.sendSuccess(() -> Component.literal(
                                    "Healed " + player.getGameProfile().getName() + "."), true);
                            return 1;
                        })
                        // /heal <target>
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> {
                                    CommandSourceStack src = ctx.getSource();
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");

                                    healPlayer(target);
                                    src.sendSuccess(() -> Component.literal(
                                            "Healed " + target.getGameProfile().getName() + "."), true);
                                    return 1;
                                })
                        )
        );
    }

    private static void healPlayer(ServerPlayer player) {
        player.setHealth(player.getMaxHealth());
        player.getFoodData().eat(20, 20.0F); // refill hunger & saturation
        player.removeAllEffects(); // optional: clear potion effects
    }
}
