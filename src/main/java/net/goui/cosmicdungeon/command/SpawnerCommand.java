// file: src/main/java/net/goui/cosmicdungeon/command/SpawnerCommand.java
package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.block.custom.CosmicMobSpawnerBlock;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerBlockEntity;
import net.goui.cosmicdungeon.network.ModNetwork;
import net.goui.cosmicdungeon.network.payload.SpawnerLabelPayload;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class SpawnerCommand {
    private SpawnerCommand() {}

    private static final String PREF_ROOT = "cosmicdungeon_prefs";
    private static final String KEY_SPAWNER_LABELS = "spawner_labels";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("spawner")

                        // ===================== LABEL (DEV-ONLY, BUT OP IS ALLOWED) =====================
                        .then(Commands.literal("label")
                                // IMPORTANT: allow OP/host in singleplayer, while still supporting your developer rank
                                .requires(src -> src.hasPermission(2) || AccessPolicy.requireDeveloperOrConsole(src))
                                .then(Commands.literal("show").executes(ctx -> setLabels(ctx.getSource(), true)))
                                .then(Commands.literal("hide").executes(ctx -> setLabels(ctx.getSource(), false)))
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> setLabels(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))
                                )
                        )

                        // ===================== BOSS (ONE-SHOT SELF-DESTRUCT) =====================
                        .then(Commands.literal("boss")
                                .requires(src -> src.hasPermission(2) || AccessPolicy.requireDeveloperOrConsole(src))
                                .then(Commands.literal("on").executes(ctx -> setBoss(ctx.getSource(), true)))
                                .then(Commands.literal("off").executes(ctx -> setBoss(ctx.getSource(), false)))
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> setBoss(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))
                                )
                        )

                        .then(Commands.literal("set")
                                .requires(src -> src.hasPermission(0))
                                .then(Commands.argument("mob", StringArgumentType.greedyString())
                                        .suggests(SpawnerCommand::suggestEntityTypes)
                                        .executes(ctx -> {
                                            final CommandSourceStack src = ctx.getSource();
                                            final ServerPlayer player = src.getPlayerOrException();
                                            final Level level = player.level();

                                            final CosmicSpawnerBlockEntity be = getTargetSpawnerBE(src, player, level);
                                            if (be == null) return 0;

                                            final BlockPos pos = be.getBlockPos();
                                            final String raw = StringArgumentType.getString(ctx, "mob").trim();

                                            if (raw.equalsIgnoreCase("none")) {
                                                be.clearSpawnerEntity(level);
                                                src.sendSuccess(() -> Component.literal("Spawner at " + pos.toShortString() + " set to none."), false);
                                                return 1;
                                            }

                                            final ResourceLocation rl;
                                            if (raw.contains(":")) {
                                                rl = ResourceLocation.tryParse(raw);
                                            } else {
                                                rl = ResourceLocation.fromNamespaceAndPath("minecraft", raw);
                                            }

                                            if (rl == null) {
                                                src.sendFailure(Component.literal("Invalid entity id: " + raw + " (expected namespace:path, e.g. cosmicdungeon:crystal_creeper)"));
                                                return 0;
                                            }

                                            var typeOpt = BuiltInRegistries.ENTITY_TYPE.getOptional(rl);
                                            if (typeOpt.isEmpty()) {
                                                src.sendFailure(Component.literal("Unknown entity type: " + rl));
                                                return 0;
                                            }

                                            be.applySpawnerEntity(level, typeOpt.get());

                                            src.sendSuccess(() ->
                                                    Component.literal("Spawner at " + pos.toShortString() + " set to " + rl), false);
                                            return 1;
                                        })
                                )
                        )

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


                        .then(Commands.literal("preset")
                                .requires(src -> src.hasPermission(0))
                                .then(Commands.argument("profile", StringArgumentType.word())
                                        .suggests((ctx,b)->SharedSuggestionProvider.suggest(List.of("trash","elite","boss"), b))
                                        .executes(ctx -> applyPreset(ctx.getSource(), StringArgumentType.getString(ctx, "profile")))
                                )
                        )

                        .then(Commands.literal("validate")
                                .requires(src -> src.hasPermission(0))
                                .executes(ctx -> validateSpawner(ctx.getSource()))
                        )

                        .then(Commands.literal("stats")
                                .requires(src -> src.hasPermission(0))
                                .executes(ctx -> {
                                    var src = ctx.getSource();
                                    var player = src.getPlayerOrException();
                                    var level = player.level();

                                    var be = getTargetSpawnerBE(src, player, level);
                                    if (be == null) return 0;

                                    BlockPos pos = be.getBlockPos();

                                    src.sendSuccess(() -> Component.literal("Cosmic Spawner @ " + pos.toShortString()), false);
                                    src.sendSuccess(() -> Component.literal("Mob: " + be.getSpawnerEntityId()), false);
                                    src.sendSuccess(() -> Component.literal("Delay: " + be.getSpawnerDelayTicks() + " ticks"), false);
                                    src.sendSuccess(() -> Component.literal("DelayRange: " + be.getSpawnerMinSpawnDelay() + ".." + be.getSpawnerMaxSpawnDelay() + " ticks"), false);
                                    src.sendSuccess(() -> Component.literal("Count: " + be.getSpawnerSpawnCount()), false);
                                    src.sendSuccess(() -> Component.literal("Range: " + be.getSpawnerSpawnRange() + " blocks"), false);
                                    src.sendSuccess(() -> Component.literal("Players: " + be.getSpawnerRequiredPlayerRange() + " blocks"), false);
                                    src.sendSuccess(() -> Component.literal("Cap: " + be.getSpawnerMaxNearbyEntities()), false);

                                    boolean pref = getLabelsEnabled(player);
                                    src.sendSuccess(() -> Component.literal("Labels: " + (pref ? "show" : "hide")), false);

                                    src.sendSuccess(() -> Component.literal("BossOneShot: " + (be.isBossOneShot() ? "true" : "false")), false);
                                    src.sendSuccess(() -> Component.literal("BossHasSpawned: " + (be.hasBossSpawned() ? "true" : "false")), false);

                                    return 1;
                                })
                        )
        );
    }


    private static int applyPreset(CommandSourceStack src, String profileRaw) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = src.getPlayerOrException();
        var level = player.level();
        var be = getTargetSpawnerBE(src, player, level);
        if (be == null) return 0;
        String profile = profileRaw.toLowerCase();
        switch (profile) {
            case "trash" -> { be.setSpawnerDelayRange(40, 100); be.setSpawnerSpawnCount(4); be.setSpawnerMaxNearbyEntities(24); be.setSpawnerSpawnRange(6); }
            case "elite" -> { be.setSpawnerDelayRange(120, 220); be.setSpawnerSpawnCount(2); be.setSpawnerMaxNearbyEntities(8); be.setSpawnerSpawnRange(4); }
            case "boss" -> { be.setSpawnerDelayRange(200, 200); be.setSpawnerSpawnCount(1); be.setSpawnerMaxNearbyEntities(2); be.setSpawnerSpawnRange(2); be.setBossOneShot(true); }
            default -> { src.sendFailure(Component.literal("Unknown profile: " + profileRaw)); return 0; }
        }
        src.sendSuccess(() -> Component.literal("Applied spawner preset: " + profile), false);
        return 1;
    }

    private static int validateSpawner(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = src.getPlayerOrException();
        var level = player.level();
        var be = getTargetSpawnerBE(src, player, level);
        if (be == null) return 0;
        int issues = 0;
        ResourceLocation rl = ResourceLocation.tryParse(be.getSpawnerEntityId());
        if (rl == null || BuiltInRegistries.ENTITY_TYPE.getOptional(rl).isEmpty()) {
            src.sendFailure(Component.literal("[Invalid Entity ID] " + be.getSpawnerEntityId())); issues++;
        }
        if (be.getSpawnerMinSpawnDelay() > be.getSpawnerMaxSpawnDelay()) {
            src.sendFailure(Component.literal("[Delay Range] minDelay is greater than maxDelay.")); issues++;
        }
        if (be.getSpawnerMaxNearbyEntities() > 0 && be.getSpawnerSpawnCount() > be.getSpawnerMaxNearbyEntities()) {
            src.sendFailure(Component.literal("[Cap Mismatch] spawn count exceeds nearby cap.")); issues++;
        }
        if (be.getSpawnerRequiredPlayerRange() < be.getSpawnerSpawnRange()) {
            src.sendFailure(Component.literal("[Range Mismatch] player range is smaller than spawn range.")); issues++;
        }
        if (issues == 0) src.sendSuccess(() -> Component.literal("Spawner validation passed."), false);
        return issues == 0 ? 1 : 0;
    }

    private static int setBoss(CommandSourceStack src, boolean enabled) {
        final ServerPlayer sp;
        try {
            sp = src.getPlayerOrException();
        } catch (Exception ex) {
            src.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }

        final Level level = sp.level();
        final CosmicSpawnerBlockEntity be = getTargetSpawnerBE(src, sp, level);
        if (be == null) return 0;

        be.setBossOneShot(enabled);

        src.sendSuccess(() -> Component.literal("Spawner boss one-shot: " + (enabled ? "ENABLED" : "DISABLED") + "."), false);
        return 1;
    }

    private static int setLabels(CommandSourceStack src, boolean enabled) {
        ServerPlayer sp;
        try {
            sp = src.getPlayerOrException();
        } catch (Exception ex) {
            src.sendFailure(Component.literal("This command must be run by a player (client toggle)."));
            return 0;
        }

        setLabelsEnabled(sp, enabled);
        ModNetwork.sendTo(sp, new SpawnerLabelPayload(enabled));

        src.sendSuccess(() -> Component.literal("Spawner labels: " + (enabled ? "SHOW" : "HIDE") + " (client HUD)."), false);
        return 1;
    }

    private static boolean getLabelsEnabled(ServerPlayer sp) {
        if (sp == null) return false;
        CompoundTag pd = sp.getPersistentData();
        CompoundTag prefs = pd.getCompoundOrEmpty(PREF_ROOT);
        return prefs.getBooleanOr(KEY_SPAWNER_LABELS, false);
    }

    private static void setLabelsEnabled(ServerPlayer sp, boolean enabled) {
        if (sp == null) return;

        CompoundTag pd = sp.getPersistentData();
        CompoundTag prefs = pd.getCompoundOrEmpty(PREF_ROOT).copy();
        prefs.putBoolean(KEY_SPAWNER_LABELS, enabled);
        pd.put(PREF_ROOT, prefs);
    }

    private static CompletableFuture<Suggestions> suggestEntityTypes(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        final String remaining = builder.getRemaining().toLowerCase();

        builder.suggest("none");

        final boolean wantsFullIds = remaining.contains(":");

        List<String> ids = new ArrayList<>(BuiltInRegistries.ENTITY_TYPE.keySet().size());
        for (ResourceLocation rl : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            if (wantsFullIds) {
                ids.add(rl.toString());
            } else {
                if ("minecraft".equals(rl.getNamespace())) {
                    ids.add(rl.getPath());
                } else {
                    ids.add(rl.toString());
                }
            }
        }

        ids.sort(String::compareTo);
        return SharedSuggestionProvider.suggest(ids, builder);
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