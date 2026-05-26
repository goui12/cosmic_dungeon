package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.faction.FactionDefinition;
import net.goui.cosmicdungeon.faction.FactionDefinitions;
import net.goui.cosmicdungeon.faction.FactionService;
import net.goui.cosmicdungeon.faction.FactionTier;
import net.goui.cosmicdungeon.faction.PlayerFactionData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class FactionCommand {
    private FactionCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("faction")
                .then(Commands.literal("get")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("faction", ResourceLocationArgument.id()).suggests(FactionCommand::suggestFactions)
                                        .executes(ctx -> get(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), ResourceLocationArgument.getId(ctx, "faction"))))))
                .then(Commands.literal("set")
                        .requires(AccessPolicy::requireDeveloperOrConsole)
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("faction", ResourceLocationArgument.id()).suggests(FactionCommand::suggestFactions)
                                        .then(Commands.argument("value", IntegerArgumentType.integer())
                                                .executes(ctx -> set(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), ResourceLocationArgument.getId(ctx, "faction"), IntegerArgumentType.getInteger(ctx, "value")))))))
                .then(Commands.literal("add")
                        .requires(AccessPolicy::requireDeveloperOrConsole)
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("faction", ResourceLocationArgument.id()).suggests(FactionCommand::suggestFactions)
                                        .then(Commands.argument("delta", IntegerArgumentType.integer())
                                                .executes(ctx -> add(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), ResourceLocationArgument.getId(ctx, "faction"), IntegerArgumentType.getInteger(ctx, "delta")))))))
                .then(Commands.literal("list")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> list(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
        );
    }

    private static int get(CommandSourceStack src, ServerPlayer target, ResourceLocation factionId) {
        if (!canRead(src, target)) return 0;
        FactionDefinition definition = FactionDefinitions.get(factionId);
        if (definition == null) {
            src.sendFailure(Component.literal("Unknown faction: " + factionId));
            return 0;
        }

        int value = FactionService.getValue(target, factionId);
        FactionTier tier = definition.tierFor(value);
        src.sendSuccess(() -> Component.literal(target.getName().getString() + " faction " + factionId + ": " + value + " (" + tier + ")"), false);
        return 1;
    }

    private static int set(CommandSourceStack src, ServerPlayer target, ResourceLocation factionId, int value) {
        FactionDefinition definition = FactionDefinitions.get(factionId);
        if (definition == null) {
            src.sendFailure(Component.literal("Unknown faction: " + factionId));
            return 0;
        }
        FactionService.setValue(target, factionId, value);
        int clamped = FactionService.getValue(target, factionId);
        src.sendSuccess(() -> Component.literal("Set " + target.getName().getString() + " " + factionId + " to " + clamped + "."), true);
        return 1;
    }

    private static int add(CommandSourceStack src, ServerPlayer target, ResourceLocation factionId, int delta) {
        FactionDefinition definition = FactionDefinitions.get(factionId);
        if (definition == null) {
            src.sendFailure(Component.literal("Unknown faction: " + factionId));
            return 0;
        }
        int value = FactionService.adjust(target, factionId, delta, "command");
        FactionTier tier = definition.tierFor(value);
        src.sendSuccess(() -> Component.literal("Adjusted " + target.getName().getString() + " " + factionId + " by " + delta + " -> " + value + " (" + tier + ")"), true);
        return 1;
    }

    private static int list(CommandSourceStack src, ServerPlayer target) {
        if (!canRead(src, target)) return 0;

        src.sendSuccess(() -> Component.literal("Factions for " + target.getName().getString() + ":"), false);
        Map<String, Integer> stored = PlayerFactionData.get(target.level().getServer()).getStoredValues(target.getUUID());

        FactionDefinitions.ids().stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .forEach(id -> {
                    FactionDefinition definition = FactionDefinitions.get(id);
                    int value = FactionService.getValue(target, id);
                    FactionTier tier = definition.tierFor(value);
                    boolean explicit = stored.containsKey(id.toString());
                    src.sendSuccess(() -> Component.literal(" - " + id + ": " + value + " (" + tier + ")" + (explicit ? "" : " [default]")), false);
                });
        return 1;
    }

    private static boolean canRead(CommandSourceStack src, ServerPlayer target) {
        ServerPlayer caller = src.getPlayer();
        if (caller == null) return true;
        if (caller.getUUID().equals(target.getUUID())) return true;
        return AccessPolicy.requireDeveloperOrConsole(src);
    }

    private static CompletableFuture<Suggestions> suggestFactions(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggestResource(FactionDefinitions.ids().stream(), builder);
    }
}
