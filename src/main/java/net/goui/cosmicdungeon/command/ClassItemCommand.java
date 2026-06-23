package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.playerclass.api.ClassItemUtil;
import net.goui.cosmicdungeon.playerclass.api.ClassKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class ClassItemCommand {
    private ClassItemCommand() {}

    private static final String USAGE = "Usage:\n"
            + "  /classitem attune <class_name> <dungeon_number> <tier_number> <trace_value>\n"
            + "  /classitem clear\n"
            + "Example:\n"
            + "  /classitem attune Judicator d1 4 500";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("classitem")
                .requires(AccessPolicy::requireDeveloperOrConsole)
                .executes(ctx -> showHelp(ctx.getSource()))
                .then(Commands.literal("attune")
                        .then(attuneArguments()))
                .then(Commands.literal("clear")
                        .executes(ctx -> clear(ctx.getSource())))
                .then(Commands.argument("unknown", StringArgumentType.greedyString())
                        .executes(ctx -> unknown(ctx.getSource(), StringArgumentType.getString(ctx, "unknown")))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> attuneArguments() {
        return Commands.argument("class_name", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(ClassKeys.playableClassIds(), builder))
                .then(Commands.argument("dungeon_number", StringArgumentType.word())
                        .then(Commands.argument("tier_number", IntegerArgumentType.integer(1, 10))
                                .then(Commands.argument("trace_value", LongArgumentType.longArg(0L))
                                        .executes(ClassItemCommand::attune))));
    }

    private static int showHelp(CommandSourceStack src) {
        src.sendSuccess(() -> Component.literal(USAGE).withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int unknown(CommandSourceStack src, String unknown) {
        String subcommand = unknown == null ? "" : unknown.trim().split("\\s+", 2)[0];
        src.sendFailure(Component.literal("Unknown /classitem subcommand: " + subcommand + "\n" + USAGE));
        return 0;
    }

    private static int attune(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player = src.getPlayer();
        if (player == null) {
            src.sendFailure(Component.literal("Only an in-game developer can attune a held item."));
            return 0;
        }

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            src.sendFailure(Component.literal("Hold an item in your main hand before using /classitem attune."));
            return 0;
        }

        String classRaw = StringArgumentType.getString(ctx, "class_name");
        String classId = ClassItemUtil.normalizeClassId(classRaw);
        if (!ClassItemUtil.isPlayableClass(classId)) {
            src.sendFailure(Component.literal("Invalid class item attunement: " + classRaw + ". Use one of: "
                    + String.join(", ", ClassKeys.playableClassIds()) + "."));
            return 0;
        }

        Integer dungeon = parseDungeon(StringArgumentType.getString(ctx, "dungeon_number"));
        if (dungeon == null) {
            src.sendFailure(Component.literal("Invalid dungeon number. Use a positive value like d1 or 1."));
            return 0;
        }

        int tier = IntegerArgumentType.getInteger(ctx, "tier_number");
        long trace = LongArgumentType.getLong(ctx, "trace_value");

        ClassItemUtil.attune(stack, classId, dungeon, tier, trace);
        String itemName = stack.getHoverName().getString();
        String displayClass = ClassItemUtil.displayNameForClass(classId);
        src.sendSuccess(() -> Component.literal("Attuned " + itemName + " with the Spirit of the " + displayClass
                + ". D" + dungeon + " T" + tier + ", " + trace + " Trace."), true);
        return 1;
    }

    private static int clear(CommandSourceStack src) {
        ServerPlayer player = src.getPlayer();
        if (player == null) {
            src.sendFailure(Component.literal("Only an in-game developer can clear held item attunement."));
            return 0;
        }

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            src.sendFailure(Component.literal("Hold an item in your main hand before using /classitem clear."));
            return 0;
        }

        if (!ClassItemUtil.hasAnyAttunementMetadata(stack)) {
            src.sendFailure(Component.literal("This item is not class-attuned."));
            return 0;
        }

        ClassItemUtil.clearAttunement(stack);
        src.sendSuccess(() -> Component.literal("Cleared CosmicDungeon class item attunement from "
                + stack.getHoverName().getString() + "."), true);
        return 1;
    }

    private static Integer parseDungeon(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim();
        if (value.length() > 1 && (value.charAt(0) == 'd' || value.charAt(0) == 'D')) {
            value = value.substring(1);
        }
        if (value.isBlank() || !value.chars().allMatch(Character::isDigit)) return null;
        try {
            int dungeon = Integer.parseInt(value);
            return dungeon > 0 ? dungeon : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
