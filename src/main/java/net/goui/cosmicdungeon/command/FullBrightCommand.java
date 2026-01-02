package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class FullBrightCommand {
    private FullBrightCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("fullbright")
                        .requires(src -> src.hasPermission(2)) // OP-only
                        .executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();
                            ServerPlayer player = src.getPlayerOrException();

                            boolean hasEffect = player.hasEffect(MobEffects.NIGHT_VISION);

                            if (hasEffect) {
                                player.removeEffect(MobEffects.NIGHT_VISION);
                                src.sendSuccess(() -> Component.literal("Fullbright disabled."), true);
                            } else {
                                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 999999, 0, false, false));
                                src.sendSuccess(() -> Component.literal("Fullbright enabled."), true);
                            }

                            return 1;
                        })
        );
    }
}
