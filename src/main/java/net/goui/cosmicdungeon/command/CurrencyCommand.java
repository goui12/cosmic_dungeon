package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.economy.CurrencyAmount;
import net.goui.cosmicdungeon.economy.CurrencyDenomination;
import net.goui.cosmicdungeon.economy.CurrencyService;
import net.goui.cosmicdungeon.economy.PlayerCurrencyData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

public final class CurrencyCommand {
    private CurrencyCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("currency")
                .executes(ctx -> sendUsage(ctx.getSource()))
                .then(Commands.literal("balance")
                        .executes(ctx -> balanceSelf(ctx.getSource()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(AccessPolicy::requireDeveloperOrConsole)
                                .executes(ctx -> balanceOther(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("add")
                        .requires(AccessPolicy::requireDeveloperOrConsole)
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("denomination", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(CurrencyDenomination.values()).map(CurrencyDenomination::id), builder))
                                        .then(Commands.argument("amount", LongArgumentType.longArg(0L))
                                                .executes(ctx -> add(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "denomination"),
                                                        LongArgumentType.getLong(ctx, "amount")))))))
                .then(Commands.literal("remove")
                        .requires(AccessPolicy::requireDeveloperOrConsole)
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("denomination", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(CurrencyDenomination.values()).map(CurrencyDenomination::id), builder))
                                        .then(Commands.argument("amount", LongArgumentType.longArg(0L))
                                                .executes(ctx -> remove(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "denomination"),
                                                        LongArgumentType.getLong(ctx, "amount")))))))
                .then(Commands.literal("set")
                        .requires(AccessPolicy::requireDeveloperOrConsole)
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("denomination", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(CurrencyDenomination.values()).map(CurrencyDenomination::id), builder))
                                        .then(Commands.argument("amount", LongArgumentType.longArg(0L))
                                                .executes(ctx -> set(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "denomination"),
                                                        LongArgumentType.getLong(ctx, "amount")))))))
                .then(Commands.literal("clear")
                        .requires(AccessPolicy::requireDeveloperOrConsole)
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> clear(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("capacity")
                        .requires(AccessPolicy::requireDeveloperOrConsole)
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("traceAmount", LongArgumentType.longArg(0L))
                                        .executes(ctx -> setCapacity(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
                                                LongArgumentType.getLong(ctx, "traceAmount"))))))
        );
    }

    private static int balanceSelf(CommandSourceStack src) {
        ServerPlayer sp = src.getPlayer();
        if (sp == null) {
            src.sendFailure(Component.literal("Console must use /currency balance <player>."));
            return 0;
        }
        sendBalance(src, sp, sp);
        return 1;
    }

    private static int balanceOther(CommandSourceStack src, ServerPlayer target) {
        sendBalance(src, src.getPlayer(), target);
        return 1;
    }

    private static void sendBalance(CommandSourceStack src, ServerPlayer viewer, ServerPlayer target) {
        long trace = CurrencyService.getBalanceTrace(target);
        long capacity = CurrencyService.getCapacity(target);
        String formatted = CurrencyAmount.ofTrace(trace).formatNormalized();
        String prefix = viewer != null && viewer.getUUID().equals(target.getUUID()) ? "Your" : target.getName().getString() + "'s";
        src.sendSuccess(() -> Component.literal(prefix + " balance: " + formatted + " (" + trace + " Trace / " + capacity + " cap)"), false);
    }

    private static int add(CommandSourceStack src, ServerPlayer target, String denominationRaw, long amount) {
        CurrencyDenomination denomination = CurrencyDenomination.fromId(denominationRaw);
        if (denomination == null) return failInvalidDenomination(src);

        long trace;
        try {
            trace = denomination.toTrace(amount);
        } catch (ArithmeticException ex) {
            src.sendFailure(Component.literal("Amount is too large."));
            return 0;
        }

        boolean ok = CurrencyService.tryDeposit(target, trace);
        if (!ok) {
            src.sendFailure(Component.literal("Deposit failed: would exceed capacity."));
            return 0;
        }

        src.sendSuccess(() -> Component.literal("Added " + amount + " " + denomination.id() + " to " + target.getName().getString() + "."), true);
        sendBalance(src, src.getPlayer(), target);
        return 1;
    }

    private static int remove(CommandSourceStack src, ServerPlayer target, String denominationRaw, long amount) {
        CurrencyDenomination denomination = CurrencyDenomination.fromId(denominationRaw);
        if (denomination == null) return failInvalidDenomination(src);

        long trace;
        try {
            trace = denomination.toTrace(amount);
        } catch (ArithmeticException ex) {
            src.sendFailure(Component.literal("Amount is too large."));
            return 0;
        }

        boolean ok = CurrencyService.tryWithdraw(target, trace);
        if (!ok) {
            src.sendFailure(Component.literal("Withdraw failed: insufficient balance."));
            return 0;
        }

        src.sendSuccess(() -> Component.literal("Removed " + amount + " " + denomination.id() + " from " + target.getName().getString() + "."), true);
        sendBalance(src, src.getPlayer(), target);
        return 1;
    }

    private static int set(CommandSourceStack src, ServerPlayer target, String denominationRaw, long amount) {
        CurrencyDenomination denomination = CurrencyDenomination.fromId(denominationRaw);
        if (denomination == null) return failInvalidDenomination(src);

        long trace;
        try {
            trace = denomination.toTrace(amount);
        } catch (ArithmeticException ex) {
            src.sendFailure(Component.literal("Amount is too large."));
            return 0;
        }

        CurrencyService.setBalanceTrace(target, trace);
        src.sendSuccess(() -> Component.literal("Set balance for " + target.getName().getString() + "."), true);
        sendBalance(src, src.getPlayer(), target);
        return 1;
    }

    private static int clear(CommandSourceStack src, ServerPlayer target) {
        CurrencyService.clear(target);
        src.sendSuccess(() -> Component.literal("Cleared currency data for " + target.getName().getString() + "."), true);
        return 1;
    }

    private static int setCapacity(CommandSourceStack src, ServerPlayer target, long traceAmount) {
        CurrencyService.setCapacity(target, traceAmount);
        src.sendSuccess(() -> Component.literal("Set capacity for " + target.getName().getString() + " to " + traceAmount + " Trace."), true);
        sendBalance(src, src.getPlayer(), target);
        return 1;
    }

    private static int failInvalidDenomination(CommandSourceStack src) {
        src.sendFailure(Component.literal("Invalid denomination. Use trace, mark, seal, crown, or anchor."));
        return 0;
    }

    private static int sendUsage(CommandSourceStack src) {
        src.sendFailure(Component.literal("Usage: /currency balance|add|remove|set|clear|capacity"));
        return 0;
    }
}
