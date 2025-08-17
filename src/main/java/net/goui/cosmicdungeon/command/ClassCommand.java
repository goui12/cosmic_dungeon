// net.goui.cosmicdungeon.command.ClassCommand.java
package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.goui.cosmicdungeon.classsystem.ClassData;
import net.goui.cosmicdungeon.classsystem.PlayerClass;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class ClassCommand {
    private ClassCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("class")
                .then(Commands.literal("get")
                        .executes(ctx -> {
                            var src = ctx.getSource();
                            ServerPlayer p = src.getPlayerOrException();
                            var cls = ClassData.get(p);
                            src.sendSuccess(() -> Component.literal("Your class: " + (cls == null ? "None" : cls.name())), false);
                            return 1;
                        })
                        .then(Commands.argument("target", EntityArgument.player())
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> {
                                    var src = ctx.getSource();
                                    ServerPlayer t = EntityArgument.getPlayer(ctx, "target");
                                    var cls = ClassData.get(t);
                                    src.sendSuccess(() -> Component.literal(t.getGameProfile().getName() + ": " + (cls == null ? "None" : cls.name())), false);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("set")
                        .requires(src -> src.hasPermission(2)) // OP-only to assign
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("cls", StringArgumentType.word())
                                        .suggests((c,b)-> {
                                            List<String> list = java.util.Arrays.stream(PlayerClass.values()).map(Enum::name).map(String::toLowerCase).toList();
                                            return net.minecraft.commands.SharedSuggestionProvider.suggest(list, b);
                                        })
                                        .executes(ctx -> {
                                            var src = ctx.getSource();
                                            ServerPlayer t = EntityArgument.getPlayer(ctx, "target");
                                            String in = StringArgumentType.getString(ctx, "cls");
                                            var pc = PlayerClass.from(in);
                                            if (pc == null) {
                                                src.sendFailure(Component.literal("Unknown class: " + in));
                                                return 0;
                                            }
                                            ClassData.set(t, pc);
                                            src.sendSuccess(() -> Component.literal("Set " + t.getGameProfile().getName() + " to " + pc.name()), true);
                                            return 1;
                                        })
                                )
                        )
                )
        );
    }
}
