package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.auth.Authority;
import net.goui.cosmicdungeon.auth.PasswordStore;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class PasswordCommand {
    private PasswordCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("password")
                        .requires(AccessPolicy::requireDeveloperOrConsole)


                        // /password show  (console only)
                        .then(Commands.literal("show")
                                .requires(src -> src.getPlayer() == null)
                                .executes(ctx -> {
                                    CommandSourceStack src = ctx.getSource();
                                    PasswordStore pw = PasswordStore.get(src.getServer());
                                    src.sendSuccess(() -> Component.literal("Current password: " + pw.getPassword())
                                            .withStyle(ChatFormatting.YELLOW), false);
                                    return 1;
                                })
                        )

                        // /password <old> <new> <confirm>
                        .then(Commands.argument("old", StringArgumentType.string())
                                .then(Commands.argument("new", StringArgumentType.string())
                                        .then(Commands.argument("confirm", StringArgumentType.string())
                                                .executes(ctx -> {
                                                    CommandSourceStack src = ctx.getSource();
                                                    PasswordStore pw = PasswordStore.get(src.getServer());

                                                    String oldPw = StringArgumentType.getString(ctx, "old");
                                                    String newPw = StringArgumentType.getString(ctx, "new");
                                                    String confirm = StringArgumentType.getString(ctx, "confirm");

                                                    if (!pw.matches(oldPw)) {
                                                        src.sendFailure(Component.literal("Old password is incorrect.")
                                                                .withStyle(ChatFormatting.RED));
                                                        return 0;
                                                    }
                                                    if (!newPw.equals(confirm)) {
                                                        src.sendFailure(Component.literal("Confirmation does not match.")
                                                                .withStyle(ChatFormatting.RED));
                                                        return 0;
                                                    }
                                                    if (newPw.isBlank()) {
                                                        src.sendFailure(Component.literal("Password cannot be blank.")
                                                                .withStyle(ChatFormatting.RED));
                                                        return 0;
                                                    }

                                                    pw.setPassword(newPw);
                                                    src.sendSuccess(() -> Component.literal("Password updated.")
                                                            .withStyle(ChatFormatting.GREEN), true);
                                                    return 1;
                                                })
                                        )
                                )
                        )
        );
    }
}
