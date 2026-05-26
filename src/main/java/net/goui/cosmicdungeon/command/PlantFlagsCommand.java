package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import net.goui.cosmicdungeon.achievement.plantflags.PlantFlagData;
import net.goui.cosmicdungeon.achievement.plantflags.PlantFlagService;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class PlantFlagsCommand {
    private PlantFlagsCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("plantflags")
                .then(Commands.literal("status")
                        .executes(ctx -> {
                            String line = PlantFlagService.statusLine(ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(() -> Component.literal("Plant Flags: " + line).withStyle(ChatFormatting.GRAY), false);
                            return 1;
                        }))
                .then(Commands.literal("reset")
                        .requires(AccessPolicy::requireDeveloperOrConsole)
                        .executes(ctx -> {
                            PlantFlagService.clearForRun(ctx.getSource().getServer(), -1L);
                            ctx.getSource().sendSuccess(() -> Component.literal("Plant Flags state reset.").withStyle(ChatFormatting.YELLOW), true);
                            return 1;
                        }))
                .then(Commands.literal("setregion")
                        .requires(AccessPolicy::requireDeveloperOrConsole)
                        .then(Commands.literal("pos1").executes(ctx -> setPos(ctx.getSource(), true)))
                        .then(Commands.literal("pos2").executes(ctx -> setPos(ctx.getSource(), false))))
                .then(Commands.literal("complete-debug")
                        .requires(AccessPolicy::requireDeveloperOrConsole)
                        .executes(ctx -> {
                            boolean ok = PlantFlagService.completeIfReady(ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(() -> Component.literal(ok ? "Plant Flags completed." : "Plant Flags not ready.").withStyle(ChatFormatting.YELLOW), true);
                            return ok ? 1 : 0;
                        }))
        );
    }

    private static int setPos(CommandSourceStack source, boolean isPos1) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = source.getPlayerOrException();
        PlantFlagData data = PlantFlagData.get(source.getServer());
        String dim = sp.level().dimension().location().toString();
        if (isPos1) {
            data.setRegion(dim, sp.blockPosition(), data.regionPos2());
        } else {
            data.setRegion(dim, data.regionPos1(), sp.blockPosition());
        }
        source.sendSuccess(() -> Component.literal("Plant Flags region " + (isPos1 ? "pos1" : "pos2") + " set at " + sp.blockPosition()).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }
}
