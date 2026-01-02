package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.goui.cosmicdungeon.block.custom.CosmicMobSpawnerBlock;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class SpawnerCommand {
    private SpawnerCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("spawner")
                        .then(Commands.literal("set")
                                .requires(src -> src.hasPermission(0))
                                .then(Commands.argument("mob", StringArgumentType.string())
                                        .executes(ctx -> {
                                            final CommandSourceStack src = ctx.getSource();
                                            final ServerPlayer player = src.getPlayerOrException();
                                            final Level level = player.level();

                                            final CosmicSpawnerBlockEntity be = getTargetSpawnerBE(src, player, level);
                                            if (be == null) return 0;

                                            final BlockPos pos = be.getBlockPos();
                                            final String raw = StringArgumentType.getString(ctx, "mob").trim();

                                            if (raw.equalsIgnoreCase("none")) {
                                                be.setSpawnerEntityId("none");
                                                be.clearSpawnerEntity(level);
                                                be.markUpdated();
                                                src.sendSuccess(() -> Component.literal("Spawner at " + pos.toShortString() + " set to none."), false);
                                                return 1;
                                            }

                                            ResourceLocation rl = ResourceLocation.tryParse(raw);
                                            if (rl == null) {
                                                src.sendFailure(Component.literal("Invalid mob id: " + raw + " (expected namespace:path)"));
                                                return 0;
                                            }

                                            var typeOpt = BuiltInRegistries.ENTITY_TYPE.getOptional(rl);
                                            if (typeOpt.isEmpty()) {
                                                src.sendFailure(Component.literal("Unknown entity type: " + rl));
                                                return 0;
                                            }

                                            be.setSpawnerEntityId(rl.toString());
                                            be.applySpawnerEntity(level, typeOpt.get());
                                            be.markUpdated();

                                            src.sendSuccess(() ->
                                                    Component.literal("Spawner at " + pos.toShortString() + " set to " + rl), false);
                                            return 1;
                                        })
                                )
                        )

                        // /spawner delay <ticks>
                        .then(Commands.literal("delay")
                                .requires(src -> src.hasPermission(0))
                                .then(Commands.argument("ticks", IntegerArgumentType.integer(0, 72000))
                                        .executes(ctx -> {
                                            var src = ctx.getSource();
                                            var player = src.getPlayerOrException();
                                            var level = player.level();

                                            var be = getTargetSpawnerBE(src, player, level);
                                            if (be == null) return 0;

                                            int ticks = IntegerArgumentType.getInteger(ctx, "ticks");
                                            be.setSpawnerDelayTicks(ticks);
                                            src.sendSuccess(() -> Component.literal("Spawner delay set to " + ticks + " ticks."), false);
                                            return 1;
                                        })
                                )
                        )

                        // /spawner delayrange <minTicks> <maxTicks>
                        .then(Commands.literal("delayrange")
                                .requires(src -> src.hasPermission(0))
                                .then(Commands.argument("minTicks", IntegerArgumentType.integer(0, 72000))
                                        .then(Commands.argument("maxTicks", IntegerArgumentType.integer(0, 72000))
                                                .executes(ctx -> {
                                                    var src = ctx.getSource();
                                                    var player = src.getPlayerOrException();
                                                    var level = player.level();

                                                    var be = getTargetSpawnerBE(src, player, level);
                                                    if (be == null) return 0;

                                                    int min = IntegerArgumentType.getInteger(ctx, "minTicks");
                                                    int max = IntegerArgumentType.getInteger(ctx, "maxTicks");
                                                    be.setSpawnerDelayRange(min, max);

                                                    int lo = Math.min(min, max);
                                                    int hi = Math.max(min, max);
                                                    src.sendSuccess(() -> Component.literal("Spawner delay range set to " + lo + ".." + hi + " ticks."), false);
                                                    return 1;
                                                })
                                        )
                                )
                        )

                        // /spawner range <blocks>
                        .then(Commands.literal("range")
                                .requires(src -> src.hasPermission(0))
                                .then(Commands.argument("blocks", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> {
                                            var src = ctx.getSource();
                                            var player = src.getPlayerOrException();
                                            var level = player.level();

                                            var be = getTargetSpawnerBE(src, player, level);
                                            if (be == null) return 0;

                                            int blocks = IntegerArgumentType.getInteger(ctx, "blocks");
                                            be.setSpawnerSpawnRange(blocks);
                                            src.sendSuccess(() -> Component.literal("Spawner range set to " + blocks + " blocks."), false);
                                            return 1;
                                        })
                                )
                        )

                        // /spawner count <n>
                        .then(Commands.literal("count")
                                .requires(src -> src.hasPermission(0))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> {
                                            var src = ctx.getSource();
                                            var player = src.getPlayerOrException();
                                            var level = player.level();

                                            var be = getTargetSpawnerBE(src, player, level);
                                            if (be == null) return 0;

                                            int count = IntegerArgumentType.getInteger(ctx, "count");
                                            be.setSpawnerSpawnCount(count);
                                            src.sendSuccess(() -> Component.literal("Spawner spawn count set to " + count + "."), false);
                                            return 1;
                                        })
                                )
                        )

                        // /spawner players <blocks>
                        .then(Commands.literal("players")
                                .requires(src -> src.hasPermission(0))
                                .then(Commands.argument("blocks", IntegerArgumentType.integer(1, 128))
                                        .executes(ctx -> {
                                            var src = ctx.getSource();
                                            var player = src.getPlayerOrException();
                                            var level = player.level();

                                            var be = getTargetSpawnerBE(src, player, level);
                                            if (be == null) return 0;

                                            int blocks = IntegerArgumentType.getInteger(ctx, "blocks");
                                            be.setSpawnerRequiredPlayerRange(blocks);
                                            src.sendSuccess(() -> Component.literal("Spawner required player range set to " + blocks + " blocks."), false);
                                            return 1;
                                        })
                                )
                        )

                        // /spawner cap <n>
                        .then(Commands.literal("cap")
                                .requires(src -> src.hasPermission(0))
                                .then(Commands.argument("count", IntegerArgumentType.integer(0, 128))
                                        .executes(ctx -> {
                                            var src = ctx.getSource();
                                            var player = src.getPlayerOrException();
                                            var level = player.level();

                                            var be = getTargetSpawnerBE(src, player, level);
                                            if (be == null) return 0;

                                            int cap = IntegerArgumentType.getInteger(ctx, "count");
                                            be.setSpawnerMaxNearbyEntities(cap);
                                            src.sendSuccess(() -> Component.literal("Spawner max nearby entities cap set to " + cap + "."), false);
                                            return 1;
                                        })
                                )
                        )

                        // /spawner stats
                        .then(Commands.literal("stats")
                                .requires(src -> src.hasPermission(0))
                                .executes(ctx -> {
                                    var src = ctx.getSource();
                                    var player = src.getPlayerOrException();
                                    var level = player.level();

                                    var be = getTargetSpawnerBE(src, player, level);
                                    if (be == null) return 0;

                                    BlockPos pos = be.getBlockPos();

                                    Component header = Component.literal("Cosmic Spawner @ " + pos.toShortString())
                                            .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);

                                    Component mob = Component.literal("Mob: ")
                                            .withStyle(ChatFormatting.GRAY)
                                            .append(Component.literal(be.getSpawnerEntityId()).withStyle(ChatFormatting.WHITE));

                                    Component delay = Component.literal("Delay: ")
                                            .withStyle(ChatFormatting.GRAY)
                                            .append(Component.literal(String.valueOf(be.getSpawnerDelayTicks())).withStyle(ChatFormatting.WHITE))
                                            .append(Component.literal(" ticks").withStyle(ChatFormatting.DARK_GRAY));

                                    Component rangeDelay = Component.literal("DelayRange: ")
                                            .withStyle(ChatFormatting.GRAY)
                                            .append(Component.literal(be.getSpawnerMinSpawnDelay() + ".." + be.getSpawnerMaxSpawnDelay()).withStyle(ChatFormatting.WHITE))
                                            .append(Component.literal(" ticks").withStyle(ChatFormatting.DARK_GRAY));

                                    Component spawnCount = Component.literal("Count: ")
                                            .withStyle(ChatFormatting.GRAY)
                                            .append(Component.literal(String.valueOf(be.getSpawnerSpawnCount())).withStyle(ChatFormatting.WHITE));

                                    Component spawnRange = Component.literal("Range: ")
                                            .withStyle(ChatFormatting.GRAY)
                                            .append(Component.literal(String.valueOf(be.getSpawnerSpawnRange())).withStyle(ChatFormatting.WHITE))
                                            .append(Component.literal(" blocks").withStyle(ChatFormatting.DARK_GRAY));

                                    Component players = Component.literal("Players: ")
                                            .withStyle(ChatFormatting.GRAY)
                                            .append(Component.literal(String.valueOf(be.getSpawnerRequiredPlayerRange())).withStyle(ChatFormatting.WHITE))
                                            .append(Component.literal(" blocks").withStyle(ChatFormatting.DARK_GRAY));

                                    Component cap = Component.literal("Cap: ")
                                            .withStyle(ChatFormatting.GRAY)
                                            .append(Component.literal(String.valueOf(be.getSpawnerMaxNearbyEntities())).withStyle(ChatFormatting.WHITE));

                                    src.sendSuccess(() -> header, false);
                                    src.sendSuccess(() -> mob, false);
                                    src.sendSuccess(() -> delay, false);
                                    src.sendSuccess(() -> rangeDelay, false);
                                    src.sendSuccess(() -> spawnCount, false);
                                    src.sendSuccess(() -> spawnRange, false);
                                    src.sendSuccess(() -> players, false);
                                    src.sendSuccess(() -> cap, false);

                                    return 1;
                                })
                        )
        );
    }

    private static CosmicSpawnerBlockEntity getTargetSpawnerBE(CommandSourceStack src, ServerPlayer player, Level level) {
        final BlockHitResult hit = raycast(player, 5.0D);
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            src.sendFailure(Component.literal("Look at a Cosmic Spawner within 5 blocks."));
            return null;
        }

        final BlockPos pos = hit.getBlockPos();

        if (!(level.getBlockState(pos).getBlock() instanceof CosmicMobSpawnerBlock)) {
            src.sendFailure(Component.literal("Target block is not a Cosmic Spawner."));
            return null;
        }

        if (!(level.getBlockEntity(pos) instanceof CosmicSpawnerBlockEntity be)) {
            src.sendFailure(Component.literal("Cosmic Spawner block entity missing at target."));
            return null;
        }

        return be;
    }

    private static BlockHitResult raycast(ServerPlayer p, double range) {
        ClipContext ctx = new ClipContext(
                p.getEyePosition(),
                p.getEyePosition().add(p.getLookAngle().scale(range)),
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                p
        );
        HitResult hr = p.level().clip(ctx);
        return hr instanceof BlockHitResult bhr ? bhr : null;
    }
}
