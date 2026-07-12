package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.goui.cosmicdungeon.rift.RiftRegistryData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class RiftDestinationCommand {
    private RiftDestinationCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                Commands.literal("rift")
                        .then(Commands.literal("destination")
                                .then(Commands.literal("new")
                                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                                .executes(ctx -> cmdNew(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                                .then(Commands.literal("delete")
                                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                                .suggests(RiftDestinationCommand::suggestDestinations)
                                                .executes(ctx -> cmdDelete(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                                .then(Commands.literal("info")
                                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                                .suggests(RiftDestinationCommand::suggestDestinations)
                                                .executes(ctx -> cmdInfo(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                                .then(Commands.literal("move")
                                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                                .suggests(RiftDestinationCommand::suggestDestinations)
                                                .executes(ctx -> cmdMove(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                        )
        );

        dispatcher.register(
                Commands.literal("rd")
                        .then(Commands.literal("new")
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> cmdNew(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                        .then(Commands.literal("delete")
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .suggests(RiftDestinationCommand::suggestDestinations)
                                        .executes(ctx -> cmdDelete(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                        .then(Commands.literal("info")
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .suggests(RiftDestinationCommand::suggestDestinations)
                                        .executes(ctx -> cmdInfo(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                        .then(Commands.literal("move")
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .suggests(RiftDestinationCommand::suggestDestinations)
                                        .executes(ctx -> cmdMove(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
        );
    }

    private static int cmdNew(CommandSourceStack src, String nameRaw) {
        ServerPlayer p = src.getPlayer();
        if (p == null) {
            src.sendFailure(Component.translatable("permissions.requires.player"));
            return 0;
        }
        if (!(p.level() instanceof ServerLevel level)) return 0;

        String name = cleanName(nameRaw);
        if (name.isBlank()) {
            src.sendFailure(Component.literal("Destination name cannot be empty."));
            return 0;
        }
        if (name.length() > 48) {
            src.sendFailure(Component.literal("Destination name too long (max 48)."));
            return 0;
        }

        RiftRegistryData data = RiftRegistryData.get(level);

        BlockPos pos = p.blockPosition();
        ResourceLocation dim = level.dimension().location();

        if (!data.createDestination(name, dim, pos)) {
            src.sendFailure(Component.literal("Destination already exists: ")
                    .append(Component.literal(name).withStyle(ChatFormatting.YELLOW)));
            return 0;
        }

        src.sendSuccess(() ->
                        Component.literal("Created destination ")
                                .withStyle(ChatFormatting.GREEN)
                                .append(Component.literal(name).withStyle(ChatFormatting.AQUA))
                                .append(Component.literal(" at "))
                                .append(copyable(pos.toShortString(), pos.toShortString())),
                false
        );
        return 1;
    }

    private static int cmdDelete(CommandSourceStack src, String nameRaw) {
        ServerPlayer p = src.getPlayer();
        if (p == null) {
            src.sendFailure(Component.translatable("permissions.requires.player"));
            return 0;
        }
        if (!(p.level() instanceof ServerLevel level)) return 0;

        String name = cleanName(nameRaw);
        RiftRegistryData data = RiftRegistryData.get(level);

        var res = data.deleteDestination(name);
        if (res instanceof RiftRegistryData.DeleteResult.NotFound) {
            src.sendFailure(Component.literal("No such destination: ")
                    .append(Component.literal(name).withStyle(ChatFormatting.YELLOW)));
            return 0;
        }
        if (res instanceof RiftRegistryData.DeleteResult.InUse iu) {
            src.sendFailure(Component.literal("Cannot delete destination ")
                    .append(Component.literal(name).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" — used by " + iu.count() + " rift(s). Try /rd info " + name)));
            return 0;
        }

        src.sendSuccess(() ->
                        Component.literal("Deleted destination ").withStyle(ChatFormatting.GREEN)
                                .append(Component.literal(name).withStyle(ChatFormatting.AQUA)),
                false
        );
        return 1;
    }

    private static int cmdInfo(CommandSourceStack src, String nameRaw) {
        ServerPlayer p = src.getPlayer();
        if (p == null) {
            src.sendFailure(Component.translatable("permissions.requires.player"));
            return 0;
        }
        if (!(p.level() instanceof ServerLevel level)) return 0;

        String name = cleanName(nameRaw);
        RiftRegistryData data = RiftRegistryData.get(level);

        if (!data.destinationExists(name)) {
            src.sendFailure(Component.literal("No such destination: ")
                    .append(Component.literal(name).withStyle(ChatFormatting.YELLOW)));
            return 0;
        }

        List<RiftRegistryData.PortalRecord> portals = data.listPortalsUsingDestination(name);

        src.sendSuccess(() ->
                        Component.literal("Destination ").withStyle(ChatFormatting.DARK_AQUA)
                                .append(Component.literal(name).withStyle(ChatFormatting.AQUA))
                                .append(Component.literal(" is used by " + portals.size() + " rift(s).")),
                false
        );

        for (var pr : portals) {
            BlockPos a = pr.anchorPos();
            MutableComponent line = Component.literal(" - ").withStyle(ChatFormatting.GRAY);

            String displayName = (pr.portalName() == null || pr.portalName().isBlank())
                    ? "(unnamed rift)"
                    : pr.portalName();

            line.append(Component.literal(displayName).withStyle(ChatFormatting.WHITE));
            line.append(Component.literal(" @ ").withStyle(ChatFormatting.DARK_GRAY));
            line.append(copyable(a.toShortString(), a.toShortString()));

            src.sendSuccess(() -> line, false);
        }

        return 1;
    }

    private static int cmdMove(CommandSourceStack src, String nameRaw) {
        ServerPlayer p = src.getPlayer();
        if (p == null) {
            src.sendFailure(Component.translatable("permissions.requires.player"));
            return 0;
        }
        if (!(p.level() instanceof ServerLevel level)) return 0;

        String name = cleanName(nameRaw);
        RiftRegistryData data = RiftRegistryData.get(level);
        BlockPos pos = p.blockPosition();
        ResourceLocation dim = level.dimension().location();

        if (!data.moveDestination(name, dim, pos)) {
            src.sendFailure(Component.literal("No such destination: ")
                    .append(Component.literal(name).withStyle(ChatFormatting.YELLOW)));
            return 0;
        }

        src.sendSuccess(() ->
                        Component.literal("Moved destination ").withStyle(ChatFormatting.GREEN)
                                .append(Component.literal(name).withStyle(ChatFormatting.AQUA))
                                .append(Component.literal(" to "))
                                .append(Component.literal(dim.toString()).withStyle(ChatFormatting.DARK_AQUA))
                                .append(Component.literal(" "))
                                .append(copyable(pos.toShortString(), pos.toShortString())),
                false
        );
        return 1;
    }

    private static String cleanName(String raw) {
        return raw == null ? "" : raw.trim();
    }

    private static CompletableFuture<Suggestions> suggestDestinations(
            CommandContext<CommandSourceStack> ctx,
            SuggestionsBuilder builder
    ) {
        // Suggestions must never throw.
        ServerPlayer p = ctx.getSource().getPlayer();
        if (p == null) return builder.buildFuture();
        if (!(p.level() instanceof ServerLevel level)) return builder.buildFuture();

        RiftRegistryData data = RiftRegistryData.get(level);
        return SharedSuggestionProvider.suggest(data.listDestinationNamesSorted(), builder);
    }

    private static MutableComponent copyable(String visible, String toCopy) {
        return Component.literal(visible).withStyle(
                Style.EMPTY
                        .withColor(ChatFormatting.WHITE)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent.CopyToClipboard(toCopy))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy")))
        );
    }
}
