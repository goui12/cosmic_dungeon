package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

public final class FlySpeedCommand {
    private FlySpeedCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("flyspeed")
                        .requires(src -> src.hasPermission(2)) // OP-only
                        .then(Commands.argument("speed", FloatArgumentType.floatArg(0.0F, 10.0F))
                                // nice QoL suggestions in chat
                                .suggests((ctx, builder) ->
                                        SharedSuggestionProvider.suggest(
                                                Arrays.asList("0.5", "1", "2", "3", "5", "7", "10"),
                                                builder))
                                .executes(ctx -> {
                                    CommandSourceStack src = ctx.getSource();
                                    ServerPlayer player = src.getPlayerOrException();
                                    float speed = FloatArgumentType.getFloat(ctx, "speed");

                                    // Allow in Creative OR if /fly has enabled mayfly in Survival/Adventure
                                    if (!player.getAbilities().mayfly) {
                                        src.sendFailure(Component.literal(
                                                "You don’t have flight enabled. Use /fly first."));
                                        return 0;
                                    }

                                    // Map 0..10 to internal 0..1 range (vanilla default ~= 0.1; 1 → 0.1F)
                                    // Tweak the divisor if you want a different feel.
                                    player.getAbilities().setFlyingSpeed(speed / 10.0F);
                                    player.onUpdateAbilities();

                                    src.sendSuccess(() ->
                                            Component.literal("Set flight speed to " + speed), true);
                                    return 1;
                                }))
        );
    }
}
