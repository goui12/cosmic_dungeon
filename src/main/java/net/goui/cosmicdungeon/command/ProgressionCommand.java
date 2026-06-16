package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.progression.ProgressionService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class ProgressionCommand {
    private ProgressionCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("progression")
                .then(Commands.literal("get")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> get(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("d1")
                        .then(Commands.literal("complete")
                                .requires(AccessPolicy::requireDeveloperOrConsole)
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("lesserBlooms", IntegerArgumentType.integer(0, 6))
                                                .executes(ctx -> d1Complete(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "lesserBlooms")))))))
                .then(Commands.literal("lesser")
                        .then(Commands.literal("add")
                                .requires(AccessPolicy::requireDeveloperOrConsole)
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(ctx -> lesserAdd(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "amount"))))))
                        .then(Commands.literal("set")
                                .requires(AccessPolicy::requireDeveloperOrConsole)
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(ctx -> lesserSet(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "amount")))))))
                .then(Commands.literal("cavern")
                        .then(Commands.literal("add")
                                .requires(AccessPolicy::requireDeveloperOrConsole)
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(ctx -> cavernAdd(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "amount"))))))
                        .then(Commands.literal("set")
                                .requires(AccessPolicy::requireDeveloperOrConsole)
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(ctx -> cavernSet(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "amount")))))))
                .then(Commands.literal("village")
                        .requires(AccessPolicy::requireDeveloperOrConsole)
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(ctx -> villageSet(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), BoolArgumentType.getBool(ctx, "value"))))))
        );
    }

    private static int get(CommandSourceStack src, ServerPlayer target) {
        if (!canRead(src, target)) return 0;
        src.sendSuccess(() -> Component.literal("Progression for " + target.getName().getString() + ":"), false);
        src.sendSuccess(() -> Component.literal(" - D1 best Lesser Blooms: " + ProgressionService.getD1LesserBloomsBest(target)), false);
        src.sendSuccess(() -> Component.literal(" - D1 NPC tier: " + ProgressionService.getD1NpcUnlockTier(target)), false);
        src.sendSuccess(() -> Component.literal(" - Lesser Blooms: " + ProgressionService.getLesserBlooms(target)), false);
        src.sendSuccess(() -> Component.literal(" - Village access: " + ProgressionService.hasVillageAccess(target)), false);
        src.sendSuccess(() -> Component.literal(" - Cavern Residue: " + ProgressionService.getCavernResidue(target)), false);
        src.sendSuccess(() -> Component.literal(" - D2 NPC tier: " + ProgressionService.getD2NpcUnlockTier(target)), false);
        return 1;
    }

    private static int d1Complete(CommandSourceStack src, ServerPlayer target, int lesserBlooms) {
        ProgressionService.markD1Completed(target, lesserBlooms);
        src.sendSuccess(() -> Component.literal("Marked D1 completion for " + target.getName().getString() + " with " + lesserBlooms + " Lesser Blooms."), true);
        return 1;
    }

    private static int lesserAdd(CommandSourceStack src, ServerPlayer target, int amount) {
        ProgressionService.addLesserBlooms(target, amount);
        src.sendSuccess(() -> Component.literal("Added " + amount + " Lesser Blooms to " + target.getName().getString() + ". D1 NPC tier=" + ProgressionService.getD1NpcUnlockTier(target)), true);
        return 1;
    }

    private static int lesserSet(CommandSourceStack src, ServerPlayer target, int amount) {
        ProgressionService.setLesserBlooms(target, amount);
        src.sendSuccess(() -> Component.literal("Set Lesser Blooms for " + target.getName().getString() + " to " + ProgressionService.getLesserBlooms(target) + ". D1 NPC tier=" + ProgressionService.getD1NpcUnlockTier(target)), true);
        return 1;
    }

    private static int cavernAdd(CommandSourceStack src, ServerPlayer target, int amount) {
        ProgressionService.addCavernResidue(target, amount);
        src.sendSuccess(() -> Component.literal("Added " + amount + " Cavern Residue to " + target.getName().getString() + "."), true);
        return 1;
    }

    private static int cavernSet(CommandSourceStack src, ServerPlayer target, int amount) {
        ProgressionService.setCavernResidue(target, amount);
        src.sendSuccess(() -> Component.literal("Set Cavern Residue for " + target.getName().getString() + " to " + ProgressionService.getCavernResidue(target) + "."), true);
        return 1;
    }

    private static int villageSet(CommandSourceStack src, ServerPlayer target, boolean value) {
        ProgressionService.setVillageAccess(target, value);
        src.sendSuccess(() -> Component.literal("Set village access for " + target.getName().getString() + " to " + value + "."), true);
        return 1;
    }

    private static boolean canRead(CommandSourceStack src, ServerPlayer target) {
        ServerPlayer caller = src.getPlayer();
        if (caller == null) return true;
        if (caller.getUUID().equals(target.getUUID())) return true;
        return AccessPolicy.requireDeveloperOrConsole(src);
    }
}
