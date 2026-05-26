package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import net.goui.cosmicdungeon.achievement.AchievementCounterData;
import net.goui.cosmicdungeon.achievement.CosmicAdvancementUtil;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class AchievementCommand {
    private AchievementCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("achievement")
                .requires(AccessPolicy::requireDeveloperOrConsole)
                .then(Commands.literal("grant")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("achievementId", ResourceLocationArgument.id())
                                        .executes(ctx -> grant(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), ResourceLocationArgument.getId(ctx, "achievementId"))))))
                .then(Commands.literal("counters")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> counters(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))
                        .then(Commands.literal("reset")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> reset(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))));
    }

    private static int grant(CommandSourceStack src, ServerPlayer target, ResourceLocation id) {
        CosmicAdvancementUtil.grant(target, id);
        src.sendSuccess(() -> Component.literal("Granted advancement " + id + " to " + target.getName().getString() + " (criterion: triggered)."), true);
        return 1;
    }

    private static int counters(CommandSourceStack src, ServerPlayer target) {
        var rec = AchievementCounterData.get(target.level().getServer()).get(target.getUUID());
        src.sendSuccess(() -> Component.literal("Achievement counters for " + target.getName().getString() + ":"), false);
        src.sendSuccess(() -> Component.literal(" - bindingIdolReturns: " + rec.bindingIdolReturns()), false);
        src.sendSuccess(() -> Component.literal(" - bindingIdolProvided: " + rec.bindingIdolProvided()), false);
        src.sendSuccess(() -> Component.literal(" - vitalExchangeMask: " + rec.vitalExchangeMask()), false);
        src.sendSuccess(() -> Component.literal(" - d1MusicDiscMask: " + rec.d1MusicDiscMask()), false);
        src.sendSuccess(() -> Component.literal(" - genericCounter1: " + rec.genericCounter1()), false);
        src.sendSuccess(() -> Component.literal(" - genericCounter2: " + rec.genericCounter2()), false);
        return 1;
    }

    private static int reset(CommandSourceStack src, ServerPlayer target) {
        AchievementCounterData.get(target.level().getServer()).reset(target.getUUID());
        src.sendSuccess(() -> Component.literal("Reset achievement counters for " + target.getName().getString() + "."), true);
        return 1;
    }
}
