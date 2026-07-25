package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.dungeon.DungeonDefinition;
import net.goui.cosmicdungeon.dungeon.DungeonDefinitions;
import net.goui.cosmicdungeon.dungeon.DungeonLifecycleService;
import net.goui.cosmicdungeon.dungeon.DungeonRunRegistryData;
import net.goui.cosmicdungeon.dungeon.DungeonWorldSnapshotService;
import net.goui.cosmicdungeon.dungeon.DungeonTravelRouter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class WorldCommand {
    private WorldCommand() {}

    private static final SuggestionProvider<CommandSourceStack> TELEPORT_TARGET_SUGGEST = (ctx, builder) -> {
        Set<String> names = new LinkedHashSet<>();
        names.add("world");
        names.add("overworld");
        names.add("nether");
        names.add("end");
        names.addAll(DungeonDefinitions.suggestedDungeonTargets());
        return SharedSuggestionProvider.suggest(names, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> DUNGEON_TARGET_SUGGEST = (ctx, builder) ->
            SharedSuggestionProvider.suggest(DungeonDefinitions.suggestedDungeonTargets(), builder);

    private static final SuggestionProvider<CommandSourceStack> SNAPSHOT_SUGGEST = (ctx, builder) -> {
        String arg = StringArgumentType.getString(ctx, "target");
        DungeonDefinition def = resolveDungeonTarget(arg);
        if (def == null) return builder.buildFuture();

        List<String> ids = DungeonWorldSnapshotService.listSnapshotIds(ctx.getSource().getServer(), def.id());
        return SharedSuggestionProvider.suggest(ids, builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("world")
                .requires(AccessPolicy::requireDeveloperOrConsole)

                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests(TELEPORT_TARGET_SUGGEST)
                        .executes(ctx -> {
                            String arg = StringArgumentType.getString(ctx, "target");
                            CommandSourceStack source = ctx.getSource();
                            ServerPlayer player = source.getPlayerOrException();

                            ResourceKey<Level> dimKey = resolveTeleportDimensionKey(arg);
                            if (dimKey == null) {
                                source.sendFailure(Component.literal("Unknown dimension or dungeon target: " + arg));
                                return 0;
                            }

                            ServerLevel requested = source.getServer().getLevel(dimKey);
                            if (requested == null) {
                                source.sendFailure(Component.literal("Dimension not loaded: " + dimKey.location()));
                                return 0;
                            }

                            var rd = requested.getLevelData().getRespawnData();
                            DungeonTravelRouter.Result route = DungeonTravelRouter.resolve(player, dimKey, rd.pos());
                            if (route instanceof DungeonTravelRouter.Result.Rejected rejected) {
                                source.sendFailure(Component.literal(rejected.message()));
                                return 0;
                            }
                            DungeonTravelRouter.Result.Allowed allowed = (DungeonTravelRouter.Result.Allowed) route;
                            ServerLevel dest = allowed.level();
                            BlockPos spawn = allowed.pos();
                            BlockPos safe = ensureStandable(dest, spawn);

                            player.teleportTo(dest, safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D, Set.of(), rd.yaw(), rd.pitch(), false);

                            source.sendSuccess(() -> Component.literal(
                                    "Teleported to " + dimKey.location()
                                            + " spawn (" + safe.getX() + " " + safe.getY() + " " + safe.getZ() + ")"
                            ), false);
                            return 1;
                        }))

                .then(Commands.literal("save")
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests(DUNGEON_TARGET_SUGGEST)
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    DungeonDefinition def = resolveDungeonTarget(StringArgumentType.getString(ctx, "target"));
                                    if (def == null) {
                                        source.sendFailure(Component.literal("Unknown dungeon target."));
                                        return 0;
                                    }

                                    DungeonRunRegistryData runs = DungeonRunRegistryData.get(source.getServer());
                                    if (runs.hasActiveOrResettingRun(def.id())) {
                                        source.sendFailure(Component.literal(
                                                "Cannot save a manual snapshot while " + def.id() + " has an active/resetting run."
                                        ));
                                        return 0;
                                    }

                                    var result = DungeonWorldSnapshotService.saveSnapshot(source.getServer(), def.id());
                                    if (result instanceof DungeonWorldSnapshotService.SnapshotResult.Ok ok) {
                                        source.sendSuccess(() -> Component.literal(
                                                "Saved snapshot for " + def.id() + ": " + ok.snapshotId()
                                        ), true);
                                        return 1;
                                    }

                                    DungeonWorldSnapshotService.SnapshotResult.Error err =
                                            (DungeonWorldSnapshotService.SnapshotResult.Error) result;
                                    source.sendFailure(Component.literal(err.message()));
                                    return 0;
                                })))

                .then(Commands.literal("reset")
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests(DUNGEON_TARGET_SUGGEST)
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    DungeonDefinition def = resolveDungeonTarget(StringArgumentType.getString(ctx, "target"));
                                    if (def == null) {
                                        source.sendFailure(Component.literal("Unknown dungeon target."));
                                        return 0;
                                    }

                                    var result = DungeonLifecycleService.manualReset(source.getServer(), def.id(), null);
                                    if (result instanceof DungeonWorldSnapshotService.SnapshotResult.Ok ok) {
                                        source.sendSuccess(() -> Component.literal(
                                                "Reset " + def.id() + " to latest snapshot: " + ok.snapshotId()
                                        ), true);
                                        return 1;
                                    }

                                    DungeonWorldSnapshotService.SnapshotResult.Error err =
                                            (DungeonWorldSnapshotService.SnapshotResult.Error) result;
                                    source.sendFailure(Component.literal(err.message()));
                                    return 0;
                                })
                                .then(Commands.argument("snapshot", StringArgumentType.word())
                                        .suggests(SNAPSHOT_SUGGEST)
                                        .executes(ctx -> {
                                            CommandSourceStack source = ctx.getSource();
                                            DungeonDefinition def = resolveDungeonTarget(StringArgumentType.getString(ctx, "target"));
                                            String snapshot = StringArgumentType.getString(ctx, "snapshot");

                                            if (def == null) {
                                                source.sendFailure(Component.literal("Unknown dungeon target."));
                                                return 0;
                                            }

                                            var result = DungeonLifecycleService.manualReset(source.getServer(), def.id(), snapshot);
                                            if (result instanceof DungeonWorldSnapshotService.SnapshotResult.Ok ok) {
                                                source.sendSuccess(() -> Component.literal(
                                                        "Reset " + def.id() + " to snapshot: " + ok.snapshotId()
                                                ), true);
                                                return 1;
                                            }

                                            DungeonWorldSnapshotService.SnapshotResult.Error err =
                                                    (DungeonWorldSnapshotService.SnapshotResult.Error) result;
                                            source.sendFailure(Component.literal(err.message()));
                                            return 0;
                                        }))))


                .then(Commands.literal("saves")
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests(DUNGEON_TARGET_SUGGEST)
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    DungeonDefinition def = resolveDungeonTarget(StringArgumentType.getString(ctx, "target"));
                                    if (def == null) {
                                        source.sendFailure(Component.literal("Unknown dungeon target."));
                                        return 0;
                                    }

                                    DungeonWorldSnapshotService.sendSnapshotListTo(source, def.id());
                                    return 1;
                                })))

                .then(Commands.literal("runs")
                        .executes(ctx -> {
                            DungeonLifecycleService.sendRunDiagnosticsTo(ctx.getSource(), null);
                            return 1;
                        })
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests(DUNGEON_TARGET_SUGGEST)
                                .executes(ctx -> {
                                    DungeonDefinition def = resolveDungeonTarget(StringArgumentType.getString(ctx, "target"));
                                    if (def == null) {
                                        ctx.getSource().sendFailure(Component.literal("Unknown dungeon target."));
                                        return 0;
                                    }

                                    DungeonLifecycleService.sendRunDiagnosticsTo(ctx.getSource(), def.id());
                                    return 1;
                                })))
        );
    }

    private static BlockPos ensureStandable(ServerLevel level, BlockPos pos) {
        var m = pos.mutable();
        int minY = level.getMinY();
        int maxY = minY + level.getLogicalHeight() - 1;

        for (int y = Math.max(minY + 1, m.getY()); y < maxY - 1; y++) {
            m.setY(y);
            if (isStandable(level, m)) return m.immutable();
        }

        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
        m.set(pos.getX(), Math.max(surfaceY, minY + 1), pos.getZ());

        for (int y = m.getY(); y < Math.min(m.getY() + 8, maxY - 1); y++) {
            m.setY(y);
            if (isStandable(level, m)) return m.immutable();
        }
        return m.immutable();
    }

    private static boolean isStandable(ServerLevel level, BlockPos pos) {
        var below = pos.below();
        var feet = level.getBlockState(pos);
        var head = level.getBlockState(pos.above());

        boolean sturdyBelow = level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
        boolean noFluid = level.getFluidState(pos).isEmpty() && level.getFluidState(pos.above()).isEmpty();
        boolean emptySpace = feet.getCollisionShape(level, pos).isEmpty()
                && head.getCollisionShape(level, pos.above()).isEmpty();

        return sturdyBelow && noFluid && emptySpace;
    }

    private static DungeonDefinition resolveDungeonTarget(String input) {
        return DungeonDefinitions.resolve(input).orElse(null);
    }

    private static ResourceKey<Level> resolveTeleportDimensionKey(String input) {
        if (input == null || input.isBlank()) return null;

        String s = input.toLowerCase();
        if (s.equals("world") || s.equals("overworld") || s.equals("ow")) return Level.OVERWORLD;
        if (s.equals("nether")) return Level.NETHER;
        if (s.equals("end") || s.equals("the_end")) return Level.END;

        DungeonDefinition def = resolveDungeonTarget(s);
        if (def != null) {
            if (def.id().equalsIgnoreCase(s)) {
                return def.primaryDimension();
            }

            ResourceLocation rl = s.contains(":")
                    ? ResourceLocation.tryParse(s)
                    : ResourceLocation.tryBuild("cosmicdungeon", s);

            if (rl != null) {
                for (ResourceKey<Level> key : def.dimensions()) {
                    if (key.location().equals(rl) || key.location().getPath().equalsIgnoreCase(s)) {
                        return key;
                    }
                }
            }

            return def.primaryDimension();
        }

        ResourceLocation id = s.contains(":")
                ? ResourceLocation.tryParse(s)
                : ResourceLocation.tryBuild("cosmicdungeon", s);

        if (id == null) return null;
        return ResourceKey.create(Registries.DIMENSION, id);
    }
}
