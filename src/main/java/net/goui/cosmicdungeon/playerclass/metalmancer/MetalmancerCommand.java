// file: net/goui/cosmicdungeon/playerclass/metalmancer/MetalmancerCommand.java
package net.goui.cosmicdungeon.playerclass.metalmancer;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.api.ClassKeys;
import net.goui.cosmicdungeon.playerclass.api.ClassNet;
import net.goui.cosmicdungeon.playerclass.api.ClassNbtUtil;
import net.goui.cosmicdungeon.playerclass.ore.SatchelApi;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class MetalmancerCommand {
    private MetalmancerCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(
                Commands.literal("metalmancer")
                        .requires(src -> src.hasPermission(0))
                        // /metalmancer  -> toggle class on/off
                        .executes(ctx -> {
                            ServerPlayer sp = ctx.getSource().getPlayerOrException();

                            // Check current class via the central class system
                            String current = ClassData.getClassId(sp);
                            boolean enable = !ClassKeys.CLASS_ID_METALMANCER.equals(current);

                            // Delegate all behavior to ClassNet
                            if (enable) {
                                ClassNet.enableMetalmancer(sp);   // sets class_id, seeds extra, syncs
                            } else {
                                ClassNet.disableMetalmancer(sp);  // sets none, clears extra, syncs
                            }

                            ctx.getSource().sendSuccess(
                                    () -> Component.literal(
                                            enable
                                                    ? "Class: Metalmancer enabled"
                                                    : "Class: Metalmancer disabled"
                                    ),
                                    true
                            );
                            return 1;
                        })
                        // /metalmancer ore <amount>  -> dev helper to set ore in satchel
                        .then(Commands.literal("ore")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(ctx -> {
                                            ServerPlayer sp = ctx.getSource().getPlayerOrException();
                                            int requested = IntegerArgumentType.getInteger(ctx, "amount");

                                            // Optional: require you to actually be a Metalmancer
                                            if (!ClassNbtUtil.isMetalmancer(sp)) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("You must be a Metalmancer to use /metalmancer ore.")
                                                );
                                                return 0;
                                            }

                                            // Set ore, clamped to satchel capacity
                                            SatchelApi.set(sp, requested);

                                            int current = SatchelApi.get(sp);
                                            int cap     = SatchelApi.capacity(sp);

                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal(
                                                            "Satchel ore set to " + current + " / " + cap
                                                    ),
                                                    true
                                            );
                                            return current;
                                        })))
        );
    }
}
