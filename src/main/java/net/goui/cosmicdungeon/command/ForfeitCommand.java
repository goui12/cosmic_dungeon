package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.goui.cosmicdungeon.dungeon.DungeonForfeitService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/** Every dungeon member can vote; membership is revalidated by the server service. */
public final class ForfeitCommand {
    private ForfeitCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ff")
                .executes(ctx -> DungeonForfeitService.get(ctx.getSource().getServer())
                        .open(ctx.getSource().getPlayerOrException()))
                .then(choice("yes", true))
                .then(choice("no", false)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> choice(String name, boolean yes) {
        return Commands.literal(name)
                .executes(ctx -> DungeonForfeitService.get(ctx.getSource().getServer())
                        .vote(ctx.getSource().getPlayerOrException(), yes, null))
                .then(Commands.argument("ballot", StringArgumentType.word())
                        .executes(ctx -> DungeonForfeitService.get(ctx.getSource().getServer())
                                .vote(ctx.getSource().getPlayerOrException(), yes,
                                        StringArgumentType.getString(ctx, "ballot"))));
    }
}
