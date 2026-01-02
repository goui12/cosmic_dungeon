// file: src/main/java/net/goui/cosmicdungeon/command/RegionCommand.java
package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.goui.cosmicdungeon.item.ModItems;
import net.goui.cosmicdungeon.region.RegionLookServer;
import net.goui.cosmicdungeon.region.RegionRegistryData;
import net.goui.cosmicdungeon.region.RegionSelectionStore;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class RegionCommand {
    private RegionCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("region")
                        .requires(src -> src.hasPermission(2))

                        // ----- /region wand -----
                        .then(Commands.literal("wand")
                                .requires(src -> src.getEntity() instanceof ServerPlayer)
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();

                                    ItemStack wand = new ItemStack(ModItems.REGION_WAND.get());

                                    boolean added = player.getInventory().add(wand);
                                    if (!added) {
                                        // Inventory full -> drop at feet
                                        player.drop(wand, false);
                                    }

                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("Region wand ")
                                                    .append(Component.literal(added ? "added to your inventory." : "dropped at your feet (inventory full).")
                                                            .withStyle(ChatFormatting.GRAY)),
                                            false
                                    );

                                    return 1;
                                })
                        )

                        // ----- /region new <name> -----
                        .then(Commands.literal("new")
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            ServerLevel level = (ServerLevel) player.level();
                                            String name = StringArgumentType.getString(ctx, "name").trim();

                                            Optional<RegionSelectionStore.Selection> selOpt = RegionSelectionStore.get(player);
                                            if (selOpt.isEmpty()) {
                                                ctx.getSource().sendFailure(Component.literal("No selection. Use the Region Wand to set Pos1/Pos2."));
                                                return 0;
                                            }

                                            var sel = selOpt.get();
                                            Optional<BlockPos> p1 = sel.pos1();
                                            Optional<BlockPos> p2 = sel.pos2();

                                            if (p1.isEmpty() || p2.isEmpty()) {
                                                ctx.getSource().sendFailure(Component.literal("Selection incomplete. Set Pos1 and Pos2 with the Region Wand."));
                                                return 0;
                                            }

                                            String selDim = sel.dimensionId();
                                            if (selDim.isBlank()) {
                                                ctx.getSource().sendFailure(Component.literal("Selection has no dimension. Re-select Pos1/Pos2."));
                                                return 0;
                                            }

                                            RegionRegistryData data = RegionRegistryData.get(level);

                                            boolean ok = data.create(name, selDim, p1.get(), p2.get());
                                            if (!ok) {
                                                if (data.exists(name)) {
                                                    ctx.getSource().sendFailure(Component.literal("Region already exists: " + name));
                                                } else {
                                                    ctx.getSource().sendFailure(Component.literal("Failed to create region: " + name));
                                                }
                                                return 0;
                                            }

                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal("Created region ")
                                                            .append(Component.literal(name).withStyle(ChatFormatting.AQUA))
                                                            .append(Component.literal(" in " + selDim)),
                                                    true
                                            );
                                            return 1;
                                        })
                                ))

                        // ----- /region look all | /region look <name> -----
                        .then(Commands.literal("look")
                                .then(Commands.literal("all")
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            RegionLookServer.toggleAll(player);
                                            return 1;
                                        })
                                )
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .suggests(RegionCommand::suggestRegionNames)
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String name = StringArgumentType.getString(ctx, "name").trim();
                                            RegionLookServer.toggle(player, name);
                                            return 1;
                                        })
                                )
                        )

                        // ----- /region info <name> -----
                        .then(Commands.literal("info")
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            ServerLevel level = (ServerLevel) ctx.getSource().getLevel();
                                            String name = StringArgumentType.getString(ctx, "name").trim();

                                            RegionRegistryData data = RegionRegistryData.get(level);
                                            var opt = data.get(name);
                                            if (opt.isEmpty()) {
                                                ctx.getSource().sendFailure(Component.literal("Unknown region: " + name));
                                                return 0;
                                            }

                                            var r = opt.get();
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal("Region ").append(Component.literal(r.name()).withStyle(ChatFormatting.AQUA)),
                                                    false
                                            );
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal("  Dimension: ").append(Component.literal(r.dimensionId()).withStyle(ChatFormatting.GRAY)),
                                                    false
                                            );
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal("  Min: " + r.minX() + " " + r.minY() + " " + r.minZ()),
                                                    false
                                            );
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal("  Max: " + r.maxX() + " " + r.maxY() + " " + r.maxZ()),
                                                    false
                                            );
                                            return 1;
                                        })
                                ))

                        // ----- /region delete <name> -----
                        .then(Commands.literal("delete")
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            ServerLevel level = (ServerLevel) ctx.getSource().getLevel();
                                            String name = StringArgumentType.getString(ctx, "name").trim();

                                            RegionRegistryData data = RegionRegistryData.get(level);
                                            boolean ok = data.delete(name);

                                            if (!ok) {
                                                ctx.getSource().sendFailure(Component.literal("Unknown region: " + name));
                                                return 0;
                                            }

                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal("Deleted region ").append(Component.literal(name).withStyle(ChatFormatting.AQUA)),
                                                    true
                                            );
                                            return 1;
                                        })
                                ))

                        // ----- /region list -----
                        .then(Commands.literal("list")
                                .executes(ctx -> {
                                    ServerLevel level = (ServerLevel) ctx.getSource().getLevel();
                                    RegionRegistryData data = RegionRegistryData.get(level);

                                    var list = data.listSorted();
                                    if (list.isEmpty()) {
                                        ctx.getSource().sendSuccess(() -> Component.literal("No regions defined."), false);
                                        return 1;
                                    }

                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("Regions (" + list.size() + "):").withStyle(ChatFormatting.GRAY),
                                            false
                                    );

                                    for (var r : list) {
                                        ctx.getSource().sendSuccess(
                                                () -> Component.literal(" - ")
                                                        .append(Component.literal(r.name()).withStyle(ChatFormatting.AQUA))
                                                        .append(Component.literal(" [" + r.dimensionId() + "] ").withStyle(ChatFormatting.GRAY))
                                                        .append(Component.literal("(" + r.minX() + "," + r.minY() + "," + r.minZ()
                                                                        + " -> " + r.maxX() + "," + r.maxY() + "," + r.maxZ() + ")")
                                                                .withStyle(ChatFormatting.DARK_GRAY)),
                                                false
                                        );
                                    }

                                    return 1;
                                }))
        );
    }

    // inside RegionCommand class
    private static CompletableFuture<Suggestions> suggestRegionNames(
            com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx,
            SuggestionsBuilder builder
    ) {
        // Only runs on server-side command tree; safe to pull from level data
        if (!(ctx.getSource().getLevel() instanceof ServerLevel level)) {
            return builder.buildFuture();
        }

        RegionRegistryData data = RegionRegistryData.get(level);
        var list = data.listSorted(); // you already have this

        for (var r : list) {
            builder.suggest(r.name());
        }

        return builder.buildFuture();
    }
}
