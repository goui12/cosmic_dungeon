// net.goui.cosmicdungeon.command.ClassCommand.java
package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.goui.cosmicdungeon.classsystem.ClassData;
import net.goui.cosmicdungeon.classsystem.PlayerClass;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.List;

public final class ClassCommand {
    private ClassCommand() {}

    // Matches the key used by ClassData
    private static final String KEY = "cosmicdungeon_class";

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("class")

                //reload for easy testing of class stats
                        .then(Commands.literal("reloadconfig")
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> {
                                    net.goui.cosmicdungeon.classsystem.ClassConfigLoader.reload();
                                    ctx.getSource().sendSuccess(() ->
                                                    net.minecraft.network.chat.Component.literal("[Cosmic Dungeon] ")
                                                            .withStyle(net.minecraft.ChatFormatting.GOLD)
                                                            .append(net.minecraft.network.chat.Component.literal("Reloaded class config.").withStyle(net.minecraft.ChatFormatting.GREEN)),
                                            true);
                                    return 1;
                                })
                        )

                // /class get (self)
                .then(Commands.literal("get")
                        .executes(ctx -> {
                            var src = ctx.getSource();
                            ServerPlayer p = src.getPlayerOrException();
                            var cls = ClassData.get(p);
                            src.sendSuccess(() -> prefix()
                                    .append(Component.literal("Your class: ").withStyle(ChatFormatting.GRAY))
                                    .append(formatClass(cls)), false);
                            return 1;
                        })
                        // /class get <target> (OP)
                        .then(Commands.argument("target", EntityArgument.player())
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> {
                                    var src = ctx.getSource();
                                    ServerPlayer t = EntityArgument.getPlayer(ctx, "target");
                                    var cls = ClassData.get(t);
                                    src.sendSuccess(() -> prefix()
                                            .append(Component.literal(t.getGameProfile().getName() + ": ").withStyle(ChatFormatting.GRAY))
                                            .append(formatClass(cls)), false);
                                    return 1;
                                })
                        )
                )

                // /class set <target> <cls> (OP)
                .then(Commands.literal("set")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("cls", StringArgumentType.word())
                                        .suggests((c, b) -> {
                                            List<String> list = Arrays.stream(PlayerClass.values())
                                                    .map(Enum::name).map(String::toLowerCase).toList();
                                            return SharedSuggestionProvider.suggest(list, b);
                                        })
                                        .executes(ctx -> {
                                            var src = ctx.getSource();
                                            ServerPlayer t = EntityArgument.getPlayer(ctx, "target");
                                            String in = StringArgumentType.getString(ctx, "cls");
                                            var pc = PlayerClass.from(in);
                                            if (pc == null) {
                                                src.sendFailure(prefix()
                                                        .append(Component.literal("Unknown class: ").withStyle(ChatFormatting.RED))
                                                        .append(Component.literal(in).withStyle(ChatFormatting.RED, ChatFormatting.ITALIC)));
                                                return 0;
                                            }
                                            ClassData.set(t, pc);
                                          //  net.goui.cosmicdungeon.classsystem.ClassAttributeApplier.refresh(t, pc);
                                            src.sendSuccess(() -> prefix()
                                                    .append(Component.literal("Set ").withStyle(ChatFormatting.GREEN))
                                                    .append(Component.literal(t.getGameProfile().getName()).withStyle(ChatFormatting.AQUA))
                                                    .append(Component.literal(" to ").withStyle(ChatFormatting.GREEN))
                                                    .append(Component.literal(pc.name()).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)), true);
                                            return 1;
                                        })
                                )
                        )
                )

                // /class remove (self)
                .then(Commands.literal("remove")
                        .executes(ctx -> {
                            var src = ctx.getSource();
                            ServerPlayer p = src.getPlayerOrException();
                            p.getPersistentData().remove(KEY);
                            src.sendSuccess(() -> prefix()
                                    .append(Component.literal("Your class has been removed.").withStyle(ChatFormatting.YELLOW)), false);
                            return 1;
                        })
                        // /class remove <target> (OP)
                        .then(Commands.argument("target", EntityArgument.player())
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> {
                                    var src = ctx.getSource();
                                    ServerPlayer t = EntityArgument.getPlayer(ctx, "target");
                                    t.getPersistentData().remove(KEY);
                                    src.sendSuccess(() -> prefix()
                                            .append(Component.literal("Removed class from ").withStyle(ChatFormatting.YELLOW))
                                            .append(Component.literal(t.getGameProfile().getName()).withStyle(ChatFormatting.AQUA)), true);
                                    return 1;
                                })
                        )
                )
        );
    }

    // ===== Helpers =====

    // Build a GOLD "[Cosmic Dungeon] " prefix as MutableComponent so we can append safely
    private static MutableComponent prefix() {
        return Component.literal("[Cosmic Dungeon] ").withStyle(ChatFormatting.GOLD);
    }

    // Nicely styled class display
    private static MutableComponent formatClass(PlayerClass cls) {
        if (cls == null) {
            return Component.literal("None").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
        }
        return Component.literal(cls.name()).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
    }
}
