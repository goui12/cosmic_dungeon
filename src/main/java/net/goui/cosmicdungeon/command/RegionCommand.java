// file: src/main/java/net/goui/cosmicdungeon/command/RegionCommand.java
package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.item.ModItems;
import net.goui.cosmicdungeon.region.RegionLookServer;
import net.goui.cosmicdungeon.region.RegionRegistryData;
import net.goui.cosmicdungeon.region.RegionSelectionStore;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class RegionCommand {
    private RegionCommand() {}

    // Base flags shown on main UI
    private static final String[] KNOWN_FLAGS = new String[] {
            "place", "break", "interact", "explode", "mobgrief", "spread", "burn"
    };

    // Exceptions
    private static final String[] PLACE_EXCEPTIONS = new String[] { "torch", "ladder", "water" };
    private static final String[] BREAK_EXCEPTIONS = new String[] { "torch", "ladder" };

    private static final String KEY_PLACE_EX_PREFIX = "place.ex.";
    private static final String KEY_BREAK_EX_PREFIX = "break.ex.";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("region")
                .requires(src -> {
                    ServerPlayer p = src.getPlayer();
                    if (p == null) return true; // console allowed (canonical)
                    return AccessPolicy.isDeveloper(p);
                });

        // Keep the root clean: attach subtrees.
        root.then(buildWandCommand());
        root.then(buildCreateCommands());   // /region new + /region create
        root.then(buildLookCommand());
        root.then(buildInfoCommand());
        root.then(buildParentCommand());
        root.then(buildDeleteCommand());
        root.then(buildListCommand());
        root.then(buildFlagsCommand());     // the “big one”
        root.then(buildFlagCommand());      // effective region at player

        dispatcher.register(root);
    }

    /* ====================================================================== */
    /*  Subtrees                                                              */
    /* ====================================================================== */

    private static LiteralArgumentBuilder<CommandSourceStack> buildWandCommand() {
        return Commands.literal("wand")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();

                    ItemStack wand = new ItemStack(ModItems.REGION_WAND.get());
                    boolean added = player.getInventory().add(wand);
                    if (!added) player.drop(wand, false);

                    ctx.getSource().sendSuccess(
                            () -> Component.literal("Region wand ")
                                    .append(Component.literal(added
                                                    ? "added to your inventory."
                                                    : "dropped at your feet (inventory full).")
                                            .withStyle(ChatFormatting.GRAY)),
                            false
                    );
                    return 1;
                });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildCreateCommands() {
        LiteralArgumentBuilder<CommandSourceStack> newCmd =
                Commands.literal("new")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> cmdCreateFromSelection(ctx.getSource(), StringArgumentType.getString(ctx, "name"))));

        LiteralArgumentBuilder<CommandSourceStack> createCmd =
                Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> cmdCreateFromSelection(ctx.getSource(), StringArgumentType.getString(ctx, "name"))));

        // We return a dummy literal that never registers; we attach both in register().
        // But Brigadier wants a single builder returned, so we fold them under a container literal.
        // NOTE: This container literal is never invoked directly.
        return Commands.literal("_create_container_")
                .then(newCmd)
                .then(createCmd);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildLookCommand() {
        return Commands.literal("look")
                .then(Commands.literal("all")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            RegionLookServer.toggleAll(player);
                            return 1;
                        }))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(RegionCommand::suggestRegionNames)
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String name = StringArgumentType.getString(ctx, "name").trim();
                            RegionLookServer.toggle(player, name);
                            return 1;
                        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildInfoCommand() {
        return Commands.literal("info")
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .suggests(RegionCommand::suggestRegionNames)
                        .executes(ctx -> {
                            ServerLevel level = (ServerLevel) ctx.getSource().getLevel();
                            String name = StringArgumentType.getString(ctx, "name").trim();

                            RegionRegistryData data = RegionRegistryData.get(level);
                            var opt = data.get(name);
                            if (opt.isEmpty()) {
                                ctx.getSource().sendFailure(Component.literal("Unknown region: " + name).withStyle(ChatFormatting.RED));
                                return 0;
                            }

                            var r = opt.get();
                            BlockPos min = r.min();
                            BlockPos max = r.max();

                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("Region ")
                                            .append(Component.literal(r.name()).withStyle(ChatFormatting.AQUA)),
                                    false
                            );

                            String parent = r.parent() == null ? "(none)" : r.parent();
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("  Parent: ").withStyle(ChatFormatting.GRAY)
                                            .append(Component.literal(parent).withStyle(ChatFormatting.DARK_GRAY)),
                                    false
                            );

                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("  Dimension: ").withStyle(ChatFormatting.GRAY)
                                            .append(Component.literal(r.dimensionId()).withStyle(ChatFormatting.DARK_GRAY)),
                                    false
                            );
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("  Min: ").withStyle(ChatFormatting.GRAY)
                                            .append(Component.literal(min.getX() + " " + min.getY() + " " + min.getZ()).withStyle(ChatFormatting.DARK_GRAY)),
                                    false
                            );
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("  Max: ").withStyle(ChatFormatting.GRAY)
                                            .append(Component.literal(max.getX() + " " + max.getY() + " " + max.getZ()).withStyle(ChatFormatting.DARK_GRAY)),
                                    false
                            );
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("  Flags: ").withStyle(ChatFormatting.GRAY)
                                            .append(Component.literal(String.valueOf(r.flags() == null ? 0 : r.flags().size())).withStyle(ChatFormatting.DARK_GRAY)),
                                    false
                            );
                            return 1;
                        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildParentCommand() {
        return Commands.literal("parent")
                .then(Commands.argument("region", StringArgumentType.word())
                        .suggests(RegionCommand::suggestRegionNames)
                        .then(Commands.argument("newParent", StringArgumentType.word())
                                .suggests(RegionCommand::suggestParentTargets)
                                .executes(ctx -> {
                                    ServerLevel level = ctx.getSource().getLevel();
                                    String region = StringArgumentType.getString(ctx, "region");
                                    String parent = StringArgumentType.getString(ctx, "newParent");

                                    RegionRegistryData data = RegionRegistryData.get(level);

                                    if (!data.exists(region)) {
                                        ctx.getSource().sendFailure(Component.literal("No such region: " + region).withStyle(ChatFormatting.RED));
                                        return 0;
                                    }

                                    String parentName = ("none".equalsIgnoreCase(parent) || "null".equalsIgnoreCase(parent))
                                            ? null
                                            : parent;

                                    boolean ok = data.setParent(region, parentName);
                                    if (!ok) {
                                        ctx.getSource().sendFailure(Component.literal("Failed to set parent (cycle? unknown parent?).").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }

                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("Set parent of ")
                                                    .append(Component.literal(region).withStyle(ChatFormatting.AQUA))
                                                    .append(Component.literal(" to ").withStyle(ChatFormatting.GRAY))
                                                    .append(Component.literal(parentName == null ? "(none)" : parentName).withStyle(ChatFormatting.YELLOW)),
                                            true
                                    );
                                    return 1;
                                })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildDeleteCommand() {
        return Commands.literal("delete")
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .suggests(RegionCommand::suggestRegionNames)
                        .executes(ctx -> {
                            ServerLevel level = (ServerLevel) ctx.getSource().getLevel();
                            String name = StringArgumentType.getString(ctx, "name").trim();

                            RegionRegistryData data = RegionRegistryData.get(level);
                            boolean ok = data.delete(name);
                            if (!ok) {
                                ctx.getSource().sendFailure(Component.literal("Unknown region: " + name).withStyle(ChatFormatting.RED));
                                return 0;
                            }

                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("Deleted region ")
                                            .append(Component.literal(name).withStyle(ChatFormatting.AQUA)),
                                    true
                            );
                            return 1;
                        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildListCommand() {
        return Commands.literal("list")
                .executes(ctx -> {
                    ServerLevel level = (ServerLevel) ctx.getSource().getLevel();
                    RegionRegistryData data = RegionRegistryData.get(level);

                    var list = data.listSorted();
                    if (list.isEmpty()) {
                        ctx.getSource().sendSuccess(() -> Component.literal("No regions defined.").withStyle(ChatFormatting.GRAY), false);
                        return 1;
                    }

                    ctx.getSource().sendSuccess(
                            () -> Component.literal("Regions (" + list.size() + "):").withStyle(ChatFormatting.GRAY),
                            false
                    );

                    for (var r : list) {
                        BlockPos min = r.min();
                        BlockPos max = r.max();
                        String parent = r.parent() == null ? "" : (" parent=" + r.parent());

                        ctx.getSource().sendSuccess(
                                () -> Component.literal(" - ")
                                        .append(Component.literal(r.name()).withStyle(ChatFormatting.AQUA))
                                        .append(Component.literal(" [" + r.dimensionId() + "] ").withStyle(ChatFormatting.GRAY))
                                        .append(Component.literal("("
                                                + min.getX() + "," + min.getY() + "," + min.getZ()
                                                + " -> "
                                                + max.getX() + "," + max.getY() + "," + max.getZ()
                                                + ")").withStyle(ChatFormatting.DARK_GRAY))
                                        .append(Component.literal(parent).withStyle(ChatFormatting.DARK_GRAY)),
                                false
                        );
                    }

                    return 1;
                });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildFlagsCommand() {
        // /region flags <name> ...
        LiteralArgumentBuilder<CommandSourceStack> flags = Commands.literal("flags");

        var nameArg = Commands.argument("name", StringArgumentType.word())
                .suggests(RegionCommand::suggestRegionNames)
                .executes(ctx -> uiFlagListForNamedRegion(ctx.getSource(), StringArgumentType.getString(ctx, "name")));

        // inherit subtree: /region flags <name> inherit <flags|exceptions> <on|off>
        var inherit = Commands.literal("inherit");
        var whichArg = Commands.argument("which", StringArgumentType.word())
                .suggests((c, b) -> { b.suggest("flags"); b.suggest("exceptions"); return b.buildFuture(); });

        var modeArg = Commands.argument("mode", StringArgumentType.word())
                .suggests((c, b) -> { b.suggest("on"); b.suggest("off"); return b.buildFuture(); })
                .executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    ServerLevel level = src.getLevel();

                    String regionName = StringArgumentType.getString(ctx, "name");
                    String which = StringArgumentType.getString(ctx, "which").toLowerCase(Locale.ROOT);
                    String mode = StringArgumentType.getString(ctx, "mode").toLowerCase(Locale.ROOT);

                    RegionRegistryData data = RegionRegistryData.get(level);
                    RegionRegistryData.Region r = data.get(regionName).orElse(null);
                    if (r == null) {
                        src.sendFailure(Component.literal("No such region: " + regionName).withStyle(ChatFormatting.RED));
                        return 0;
                    }

                    String key = "flags".equals(which)
                            ? RegionRegistryData.FLAG_INHERIT_FLAGS
                            : ("exceptions".equals(which) ? RegionRegistryData.FLAG_INHERIT_EXCEPTIONS : null);

                    if (key == null) {
                        src.sendFailure(Component.literal("which must be 'flags' or 'exceptions'").withStyle(ChatFormatting.RED));
                        return 0;
                    }

                    String value = "on".equals(mode) ? "true" : ("off".equals(mode) ? "false" : null);
                    if (value == null) {
                        src.sendFailure(Component.literal("mode must be 'on' or 'off'").withStyle(ChatFormatting.RED));
                        return 0;
                    }

                    data.setFlag(r.name(), key, value);
                    uiFlagListForNamedRegion(src, r.name());
                    return 1;
                });

        whichArg.then(modeArg);
        inherit.then(whichArg);
        nameArg.then(inherit);

        // exceptions subtree: /region flags <name> exceptions <place|break> [<ex> <allow|deny|clear>]
        var exceptions = Commands.literal("exceptions");
        var scopeArg = Commands.argument("scope", StringArgumentType.word())
                .suggests((c, b) -> { b.suggest("place"); b.suggest("break"); return b.buildFuture(); })
                .executes(ctx -> uiExceptionList(
                        ctx.getSource(),
                        StringArgumentType.getString(ctx, "name"),
                        StringArgumentType.getString(ctx, "scope")
                ));

        var exArg = Commands.argument("ex", StringArgumentType.word())
                .suggests(RegionCommand::suggestExceptionKeys);

        var exModeArg = Commands.argument("mode", StringArgumentType.word())
                .suggests(RegionCommand::suggestAllowDenyClear)
                .executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    ServerLevel level = src.getLevel();

                    String regionName = StringArgumentType.getString(ctx, "name");
                    String scope = StringArgumentType.getString(ctx, "scope").toLowerCase(Locale.ROOT);
                    String ex = StringArgumentType.getString(ctx, "ex").toLowerCase(Locale.ROOT);
                    String mode = StringArgumentType.getString(ctx, "mode").toLowerCase(Locale.ROOT);

                    RegionRegistryData data = RegionRegistryData.get(level);
                    RegionRegistryData.Region r = data.get(regionName).orElse(null);
                    if (r == null) {
                        src.sendFailure(Component.literal("No such region: " + regionName).withStyle(ChatFormatting.RED));
                        return 0;
                    }

                    String flagKey = exceptionFlagKey(scope, ex);
                    if (flagKey == null) {
                        src.sendFailure(Component.literal("Unknown exception: " + ex).withStyle(ChatFormatting.RED));
                        return 0;
                    }

                    if ("clear".equals(mode)) {
                        data.setFlag(r.name(), flagKey, null);
                        uiExceptionList(src, r.name(), scope);
                        return 1;
                    }

                    String value;
                    if ("allow".equals(mode)) value = "true";
                    else if ("deny".equals(mode)) value = "false";
                    else {
                        src.sendFailure(Component.literal("Mode must be 'allow', 'deny', or 'clear'.").withStyle(ChatFormatting.RED));
                        return 0;
                    }

                    data.setFlag(r.name(), flagKey, value);
                    uiExceptionList(src, r.name(), scope);
                    return 1;
                });

        exArg.then(exModeArg);
        scopeArg.then(exArg);
        exceptions.then(scopeArg);
        nameArg.then(exceptions);

        // set flag subtree: /region flags <name> <flag> <allow|deny|clear>
        var flagArg = Commands.argument("flag", StringArgumentType.word())
                .suggests(RegionCommand::suggestFlagKeys);

        var flagModeArg = Commands.argument("mode", StringArgumentType.word())
                .suggests(RegionCommand::suggestAllowDenyClear)
                .executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    ServerLevel level = src.getLevel();

                    String regionName = StringArgumentType.getString(ctx, "name");
                    String key = StringArgumentType.getString(ctx, "flag");
                    String mode = StringArgumentType.getString(ctx, "mode").toLowerCase(Locale.ROOT);

                    if (!isKnownFlag(key)) {
                        src.sendFailure(Component.literal("Unknown flag: " + key).withStyle(ChatFormatting.RED));
                        return 0;
                    }

                    RegionRegistryData data = RegionRegistryData.get(level);
                    RegionRegistryData.Region r = data.get(regionName).orElse(null);
                    if (r == null) {
                        src.sendFailure(Component.literal("No such region: " + regionName).withStyle(ChatFormatting.RED));
                        return 0;
                    }

                    if ("clear".equals(mode)) {
                        data.setFlag(r.name(), key, null);
                        uiFlagListForNamedRegion(src, r.name());
                        return 1;
                    }

                    String value;
                    if ("allow".equals(mode)) value = "true";
                    else if ("deny".equals(mode)) value = "false";
                    else {
                        src.sendFailure(Component.literal("Mode must be 'allow', 'deny', or 'clear'.").withStyle(ChatFormatting.RED));
                        return 0;
                    }

                    data.setFlag(r.name(), key, value);
                    uiFlagListForNamedRegion(src, r.name());
                    return 1;
                });

        flagArg.then(flagModeArg);
        nameArg.then(flagArg);

        flags.then(nameArg);
        return flags;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildFlagCommand() {
        // /region flag list
        // /region flag <flag> <allow|deny>  (for effective region at player)
        LiteralArgumentBuilder<CommandSourceStack> flag = Commands.literal("flag");

        flag.then(Commands.literal("list")
                .executes(ctx -> uiFlagListAtPlayer(ctx.getSource())));

        var flagArg = Commands.argument("flag", StringArgumentType.word())
                .suggests(RegionCommand::suggestFlagKeys);

        var modeArg = Commands.argument("mode", StringArgumentType.word())
                .suggests(RegionCommand::suggestAllowDeny)
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    ServerLevel level = ctx.getSource().getLevel();
                    BlockPos pos = p.blockPosition();

                    String key = StringArgumentType.getString(ctx, "flag");
                    String mode = StringArgumentType.getString(ctx, "mode").toLowerCase(Locale.ROOT);

                    if (!isKnownFlag(key)) {
                        ctx.getSource().sendFailure(Component.literal("Unknown flag: " + key).withStyle(ChatFormatting.RED));
                        return 0;
                    }

                    RegionRegistryData data = RegionRegistryData.get(level);

                    List<RegionRegistryData.Region> regions = data.regionsAt(level, pos);
                    if (regions.isEmpty()) {
                        ctx.getSource().sendFailure(Component.literal("You are not inside any region.").withStyle(ChatFormatting.RED));
                        return 0;
                    }

                    RegionRegistryData.Region r = data.effectiveRegionAt(level, pos);

                    String value;
                    if ("allow".equals(mode)) value = "true";
                    else if ("deny".equals(mode)) value = "false";
                    else {
                        ctx.getSource().sendFailure(Component.literal("Mode must be 'allow' or 'deny'.").withStyle(ChatFormatting.RED));
                        return 0;
                    }

                    boolean ok = data.setFlag(r.name(), key, value);
                    if (!ok) {
                        ctx.getSource().sendFailure(Component.literal("Failed to set flag for region '" + r.name() + "'.")
                                .withStyle(ChatFormatting.RED));
                        return 0;
                    }

                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "Set flag '" + key + "'=" + value + " for region '" + r.name() + "'."
                    ).withStyle(ChatFormatting.GREEN), true);

                    uiFlagListAtPlayer(ctx.getSource());
                    return 1;
                });

        flagArg.then(modeArg);
        flag.then(flagArg);
        return flag;
    }

    /* ====================================================================== */
    /*  Create: from wand selection                                            */
    /* ====================================================================== */

    private static int cmdCreateFromSelection(CommandSourceStack src, String rawName) {
        ServerPlayer player = src.getPlayer();
        if (player == null) {
            src.sendFailure(Component.literal("Player-only command.").withStyle(ChatFormatting.RED));
            return 0;
        }

        ServerLevel level = (ServerLevel) player.level();
        String name = rawName == null ? "" : rawName.trim();
        if (name.isBlank()) {
            src.sendFailure(Component.literal("Region name cannot be empty.").withStyle(ChatFormatting.RED));
            return 0;
        }

        Optional<RegionSelectionStore.Selection> selOpt = RegionSelectionStore.get(player);
        if (selOpt.isEmpty()) {
            src.sendFailure(Component.literal("No selection. Use the Region Wand to set Pos1/Pos2.").withStyle(ChatFormatting.RED));
            return 0;
        }

        var sel = selOpt.get();
        Optional<BlockPos> p1 = sel.pos1();
        Optional<BlockPos> p2 = sel.pos2();

        if (p1.isEmpty() || p2.isEmpty()) {
            src.sendFailure(Component.literal("Selection incomplete. Set Pos1 and Pos2 with the Region Wand.").withStyle(ChatFormatting.RED));
            return 0;
        }

        String selDim = sel.dimensionId();
        if (selDim == null || selDim.isBlank()) {
            src.sendFailure(Component.literal("Selection has no dimension. Re-select Pos1/Pos2.").withStyle(ChatFormatting.RED));
            return 0;
        }

        RegionRegistryData data = RegionRegistryData.get(level);

        boolean ok = data.create(name, selDim, p1.get(), p2.get());
        if (!ok) {
            if (data.exists(name)) {
                src.sendFailure(Component.literal("Region already exists: " + name).withStyle(ChatFormatting.RED));
            } else {
                src.sendFailure(Component.literal("Failed to create region: " + name).withStyle(ChatFormatting.RED));
            }
            return 0;
        }

        String parent = data.getParentName(name);
        src.sendSuccess(
                () -> Component.literal("Created region ")
                        .append(Component.literal(name).withStyle(ChatFormatting.AQUA))
                        .append(Component.literal(" in " + selDim).withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(parent == null ? "" : (" (parent: " + parent + ")")).withStyle(ChatFormatting.DARK_GRAY)),
                true
        );
        return 1;
    }

    /* ====================================================================== */
    /*  UI: flags at player                                                    */
    /* ====================================================================== */

    private static int uiFlagListAtPlayer(CommandSourceStack src) {
        ServerPlayer p = src.getPlayer();
        if (p == null) {
            src.sendFailure(Component.literal("This UI is player-only.").withStyle(ChatFormatting.RED));
            return 0;
        }
        if (!(p.level() instanceof ServerLevel level)) return 0;

        BlockPos pos = p.blockPosition();

        RegionRegistryData data = RegionRegistryData.get(level);
        List<RegionRegistryData.Region> regions = data.regionsAt(level, pos);
        if (regions.isEmpty()) {
            src.sendFailure(Component.literal("You are not inside any region.").withStyle(ChatFormatting.RED));
            return 0;
        }

        RegionRegistryData.Region r = data.effectiveRegionAt(level, pos);
        return uiFlagListForRegion(src, data, r, "/region flag list", true);
    }

    /* ====================================================================== */
    /*  UI: flags for a named region                                           */
    /* ====================================================================== */

    private static int uiFlagListForNamedRegion(CommandSourceStack src, String regionNameRaw) {
        ServerPlayer p = src.getPlayer();
        if (p == null) {
            src.sendFailure(Component.literal("This UI is player-only.").withStyle(ChatFormatting.RED));
            return 0;
        }

        ServerLevel level = src.getLevel();
        String regionName = regionNameRaw == null ? "" : regionNameRaw.trim();
        if (regionName.isBlank()) {
            src.sendFailure(Component.literal("Region name cannot be empty.").withStyle(ChatFormatting.RED));
            return 0;
        }

        RegionRegistryData data = RegionRegistryData.get(level);
        RegionRegistryData.Region r = data.get(regionName).orElse(null);
        if (r == null) {
            src.sendFailure(Component.literal("No such region: " + regionName).withStyle(ChatFormatting.RED));
            return 0;
        }

        String refreshCmd = "/region flags " + r.name();
        return uiFlagListForRegion(src, data, r, refreshCmd, false);
    }

    private static int uiFlagListForRegion(CommandSourceStack src, RegionRegistryData data, RegionRegistryData.Region r, String refreshCmd, boolean includeContextLine) {
        ServerPlayer p = src.getPlayer();
        if (p == null) {
            src.sendFailure(Component.literal("This UI is player-only.").withStyle(ChatFormatting.RED));
            return 0;
        }

        boolean inheritFlags = data.inheritFlagsEnabled(r);
        boolean inheritEx = data.inheritExceptionsEnabled(r);

        MutableComponent header = Component.literal("Region flags for ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(r.name()).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" ").withStyle(ChatFormatting.DARK_GRAY))
                .append(clickPill("[REFRESH]", ChatFormatting.AQUA, refreshCmd));

        src.sendSuccess(() -> header, false);

        MutableComponent inheritLine = Component.literal("Inheritance: ").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal("flags=").withStyle(ChatFormatting.DARK_GRAY))
                .append(clickPill(inheritFlags ? "[ON]" : "[OFF]", inheritFlags ? ChatFormatting.GREEN : ChatFormatting.RED,
                        "/region flags " + r.name() + " inherit flags " + (inheritFlags ? "off" : "on")))
                .append(Component.literal("  ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("exceptions=").withStyle(ChatFormatting.DARK_GRAY))
                .append(clickPill(inheritEx ? "[ON]" : "[OFF]", inheritEx ? ChatFormatting.GREEN : ChatFormatting.RED,
                        "/region flags " + r.name() + " inherit exceptions " + (inheritEx ? "off" : "on")));

        src.sendSuccess(() -> inheritLine, false);

        String parent = r.parent() == null ? "(none)" : r.parent();
        src.sendSuccess(() -> Component.literal("Parent: ").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(parent).withStyle(ChatFormatting.GRAY)), false);

        src.sendSuccess(() -> Component.literal("Click ").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal("[ALLOW]").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" / ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("[DENY]").withStyle(ChatFormatting.RED))
                .append(Component.literal(" to set each flag. Missing flags behave as deny by default. Overrides can be cleared.")
                        .withStyle(ChatFormatting.DARK_GRAY)), false);

        if (includeContextLine) {
            BlockPos min = r.min();
            BlockPos max = r.max();
            src.sendSuccess(() -> Component.literal("  ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(r.dimensionId()).withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("  ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal("(" + min.getX() + "," + min.getY() + "," + min.getZ()
                            + " -> " + max.getX() + "," + max.getY() + "," + max.getZ() + ")").withStyle(ChatFormatting.DARK_GRAY)), false);
        }

        for (String key : KNOWN_FLAGS) {
            RegionRegistryData.ResolvedBool resolved = data.resolveFlagBool(r, key, false);

            boolean allowSelected = resolved.value();
            boolean denySelected = !allowSelected;

            String allowCmd = "/region flags " + r.name() + " " + key + " allow";
            String denyCmd  = "/region flags " + r.name() + " " + key + " deny";
            String clrCmd   = "/region flags " + r.name() + " " + key + " clear";

            MutableComponent line = Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(key).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(": ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(clickPill("[ALLOW]", allowSelected ? ChatFormatting.GREEN : ChatFormatting.GRAY, allowCmd))
                    .append(Component.literal(" ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(clickPill("[DENY]", denySelected ? ChatFormatting.RED : ChatFormatting.GRAY, denyCmd));

            if ("place".equals(key) || "break".equals(key)) {
                String exCmd = "/region flags " + r.name() + " exceptions " + key;
                line = line.append(Component.literal(" ").withStyle(ChatFormatting.DARK_GRAY))
                        .append(clickPill("[EXCEPTIONS]", ChatFormatting.DARK_AQUA, exCmd));
            }

            line = line.append(Component.literal(" ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(sourcePill(resolved.source()));

            if (resolved.source() == RegionRegistryData.ValueSource.OVERRIDDEN) {
                line = line.append(Component.literal(" ").withStyle(ChatFormatting.DARK_GRAY))
                        .append(clickPill("[CLR]", ChatFormatting.GOLD, clrCmd));
            }

            p.sendSystemMessage(line);
        }

        return 1;
    }

    /* ====================================================================== */
    /*  UI: exceptions submenu                                                 */
    /* ====================================================================== */

    private static int uiExceptionList(CommandSourceStack src, String regionNameRaw, String scopeRaw) {
        ServerPlayer p = src.getPlayer();
        if (p == null) {
            src.sendFailure(Component.literal("This UI is player-only.").withStyle(ChatFormatting.RED));
            return 0;
        }
        ServerLevel level = src.getLevel();

        String regionName = regionNameRaw == null ? "" : regionNameRaw.trim();
        if (regionName.isBlank()) {
            src.sendFailure(Component.literal("Region name cannot be empty.").withStyle(ChatFormatting.RED));
            return 0;
        }

        String scope = scopeRaw == null ? "" : scopeRaw.trim().toLowerCase(Locale.ROOT);
        if (!("place".equals(scope) || "break".equals(scope))) {
            src.sendFailure(Component.literal("Scope must be 'place' or 'break'.").withStyle(ChatFormatting.RED));
            return 0;
        }

        RegionRegistryData data = RegionRegistryData.get(level);
        RegionRegistryData.Region r = data.get(regionName).orElse(null);
        if (r == null) {
            src.sendFailure(Component.literal("No such region: " + regionName).withStyle(ChatFormatting.RED));
            return 0;
        }

        String backCmd = "/region flags " + r.name();
        String refreshCmd = "/region flags " + r.name() + " exceptions " + scope;

        MutableComponent header = Component.literal("Exceptions for ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(scope).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" in ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(r.name()).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" ").withStyle(ChatFormatting.DARK_GRAY))
                .append(clickPill("[BACK]", ChatFormatting.AQUA, backCmd))
                .append(Component.literal(" ").withStyle(ChatFormatting.DARK_GRAY))
                .append(clickPill("[REFRESH]", ChatFormatting.AQUA, refreshCmd));

        src.sendSuccess(() -> header, false);

        src.sendSuccess(() -> Component.literal("Torch defaults to allow. Others default to deny. Overrides can be cleared to fall back to inherited/default.")
                .withStyle(ChatFormatting.DARK_GRAY), false);

        String[] list = "place".equals(scope) ? PLACE_EXCEPTIONS : BREAK_EXCEPTIONS;

        for (String ex : list) {
            String flagKey = exceptionFlagKey(scope, ex);
            boolean defaultAllow = "torch".equals(ex);

            RegionRegistryData.ResolvedBool resolved = data.resolveExceptionBool(r, flagKey, defaultAllow);

            boolean allowSelected = resolved.value();
            boolean denySelected = !allowSelected;

            String allowCmd = "/region flags " + r.name() + " exceptions " + scope + " " + ex + " allow";
            String denyCmd  = "/region flags " + r.name() + " exceptions " + scope + " " + ex + " deny";
            String clrCmd   = "/region flags " + r.name() + " exceptions " + scope + " " + ex + " clear";

            MutableComponent line = Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(ex).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(": ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(clickPill("[ALLOW]", allowSelected ? ChatFormatting.GREEN : ChatFormatting.GRAY, allowCmd))
                    .append(Component.literal(" ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(clickPill("[DENY]", denySelected ? ChatFormatting.RED : ChatFormatting.GRAY, denyCmd))
                    .append(Component.literal(" ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(sourcePill(resolved.source()));

            if (resolved.source() == RegionRegistryData.ValueSource.OVERRIDDEN) {
                line = line.append(Component.literal(" ").withStyle(ChatFormatting.DARK_GRAY))
                        .append(clickPill("[CLR]", ChatFormatting.GOLD, clrCmd));
            }

            p.sendSystemMessage(line);
        }

        return 1;
    }

    /* ====================================================================== */
    /*  helpers                                                                */
    /* ====================================================================== */

    private static MutableComponent sourcePill(RegionRegistryData.ValueSource src) {
        return switch (src) {
            case DEFAULT -> Component.literal("[DEFAULT]").withStyle(ChatFormatting.DARK_GRAY);
            case INHERITED -> Component.literal("[INHERITED]").withStyle(ChatFormatting.DARK_AQUA);
            case OVERRIDDEN -> Component.literal("[OVERRIDDEN]").withStyle(ChatFormatting.GOLD);
        };
    }

    private static String exceptionFlagKey(String scope, String ex) {
        if (scope == null || ex == null) return null;
        scope = scope.toLowerCase(Locale.ROOT);
        ex = ex.toLowerCase(Locale.ROOT);

        if ("place".equals(scope)) {
            for (String k : PLACE_EXCEPTIONS) if (k.equals(ex)) return KEY_PLACE_EX_PREFIX + ex;
            return null;
        }

        if ("break".equals(scope)) {
            for (String k : BREAK_EXCEPTIONS) if (k.equals(ex)) return KEY_BREAK_EX_PREFIX + ex;
            return null;
        }

        return null;
    }

    private static MutableComponent clickPill(String text, ChatFormatting color, String cmd) {
        return Component.literal(text).withStyle(
                Style.EMPTY.withColor(color)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent.RunCommand(cmd))
        );
    }

    /* ====================================================================== */
    /*  Suggestions                                                            */
    /* ====================================================================== */

    private static CompletableFuture<Suggestions> suggestRegionNames(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        if (!(ctx.getSource().getLevel() instanceof ServerLevel level)) return builder.buildFuture();
        RegionRegistryData data = RegionRegistryData.get(level);
        for (var r : data.listSorted()) builder.suggest(r.name());
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestFlagKeys(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        for (String k : KNOWN_FLAGS) builder.suggest(k);
        builder.suggest("build"); // legacy accepted
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestAllowDeny(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        builder.suggest("allow");
        builder.suggest("deny");
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestAllowDenyClear(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        builder.suggest("allow");
        builder.suggest("deny");
        builder.suggest("clear");
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestExceptionKeys(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        for (String k : PLACE_EXCEPTIONS) builder.suggest(k);
        for (String k : BREAK_EXCEPTIONS) builder.suggest(k);
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestParentTargets(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        builder.suggest("none");
        if (!(ctx.getSource().getLevel() instanceof ServerLevel level)) return builder.buildFuture();
        RegionRegistryData data = RegionRegistryData.get(level);
        for (var r : data.listSorted()) builder.suggest(r.name());
        return builder.buildFuture();
    }

    private static boolean isKnownFlag(String key) {
        if (key == null) return false;
        return switch (key) {
            case "place", "break", "interact", "explode", "mobgrief", "spread", "burn" -> true;
            case "build" -> true; // legacy support
            default -> false;
        };
    }
}
