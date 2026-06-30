package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.goui.cosmicdungeon.region.quest.RegionQuestHandler;
import net.goui.cosmicdungeon.region.quest.RegionQuestRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletableFuture;

public final class RegionQuestCommand {
    private RegionQuestCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("quest")
                .then(Commands.argument("quest", StringArgumentType.word())
                        .suggests(RegionQuestCommand::suggestQuestNames)
                        .then(Commands.literal("status")
                                .executes(ctx -> runForQuest(ctx, RegionQuestHandler::status)))
                        .then(Commands.literal("reset")
                                .executes(ctx -> runForQuest(ctx, RegionQuestHandler::reset)))
                        .then(Commands.literal("setregion")
                                .then(Commands.literal("pos1")
                                        .executes(ctx -> runForQuest(ctx, (handler, source) -> handler.setRegionPos(source, true))))
                                .then(Commands.literal("pos2")
                                        .executes(ctx -> runForQuest(ctx, (handler, source) -> handler.setRegionPos(source, false)))))
                        .then(Commands.literal("complete-debug")
                                .executes(ctx -> runForQuest(ctx, RegionQuestHandler::completeDebug))));
    }

    private static CompletableFuture<Suggestions> suggestQuestNames(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        for (String questName : RegionQuestRegistry.questNames()) {
            builder.suggest(questName);
        }
        return builder.buildFuture();
    }

    private static int runForQuest(CommandContext<CommandSourceStack> ctx, QuestAction action) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        String quest = StringArgumentType.getString(ctx, "quest");
        RegionQuestHandler handler = RegionQuestRegistry.get(quest).orElse(null);
        if (handler == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown region quest: " + quest + ". Known quests: " + RegionQuestRegistry.knownQuestList()).withStyle(ChatFormatting.RED));
            return 0;
        }
        return action.run(handler, ctx.getSource());
    }

    @FunctionalInterface
    private interface QuestAction {
        int run(RegionQuestHandler handler, CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException;
    }
}
