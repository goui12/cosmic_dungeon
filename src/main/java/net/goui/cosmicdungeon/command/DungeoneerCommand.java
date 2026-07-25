package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import net.goui.cosmicdungeon.auth.*;
import net.goui.cosmicdungeon.dungeon.DungeonAfkService;
import net.goui.cosmicdungeon.dungeon.DungeonLifecycleService;
import net.goui.cosmicdungeon.dungeon.DungeonTravelRouter;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

public final class DungeoneerCommand {
    private DungeoneerCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("dungeoneer")
                        .requires(src -> src.getEntity() instanceof ServerPlayer || AccessPolicy.requireDeveloperOrConsole(src))
                        .then(Commands.literal("kick")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> {
                                            ServerPlayer leader = ctx.getSource().getPlayerOrException();
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                            String error = DungeonLifecycleService.kickRunMember(leader, target);
                                            if (error != null) {
                                                ctx.getSource().sendFailure(Component.literal(error).withStyle(ChatFormatting.RED));
                                                return 0;
                                            }
                                            return 1;
                                        })))

                        .then(Commands.literal("afk-kick")
                                .then(Commands.literal("yes")
                                        .then(Commands.argument("player", com.mojang.brigadier.arguments.StringArgumentType.word())
                                                .executes(ctx -> {
                                                    ServerPlayer leader = ctx.getSource().getPlayerOrException();
                                                    UUID targetId;
                                                    try {
                                                        targetId = UUID.fromString(com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "player"));
                                                    } catch (IllegalArgumentException ex) {
                                                        ctx.getSource().sendFailure(Component.literal("Invalid AFK player id.").withStyle(ChatFormatting.RED));
                                                        return 0;
                                                    }
                                                    ServerPlayer target = leader.level().getServer().getPlayerList().getPlayer(targetId);
                                                    if (target == null) {
                                                        ctx.getSource().sendFailure(Component.literal("That AFK player is no longer online.").withStyle(ChatFormatting.RED));
                                                        return 0;
                                                    }
                                                    if (!DungeonAfkService.isAfk(targetId)) {
                                                        ctx.getSource().sendFailure(Component.literal(target.getName().getString() + " is no longer AFK.").withStyle(ChatFormatting.RED));
                                                        return 0;
                                                    }
                                                    String error = DungeonLifecycleService.kickRunMember(leader, target);
                                                    if (error != null) {
                                                        ctx.getSource().sendFailure(Component.literal(error).withStyle(ChatFormatting.RED));
                                                        return 0;
                                                    }
                                                    return 1;
                                                })))
                                .then(Commands.literal("no")
                                        .then(Commands.argument("player", com.mojang.brigadier.arguments.StringArgumentType.word())
                                                .executes(ctx -> 1))))
                        .then(Commands.literal("rank")
                                .requires(AccessPolicy::requireDeveloperOrConsole)
                                .executes(ctx -> {
                                    CommandSourceStack src = ctx.getSource();
                                    ServerPlayer p = src.getPlayer();
                                    if (p == null) {
                                        src.sendFailure(Component.literal("Console must use /rank for a target."));
                                        return 0;
                                    }

                                    MinecraftServer server = p.level().getServer();
                                    if (server == null) return 0;

                                    RankStore store = RankStore.get(server);
                                    store.setRank(p.getUUID(), Rank.DUNGEONEER);

                                    // Apply deop immediately and revoke developer-only label access.
                                    OpUtil.setOperator(server, p, false);
                                    SpawnerLabelServerState.revoke(p);
                                    DungeonTravelRouter.evacuateUnauthorizedLocation(p);

                                    src.sendSuccess(() -> Component.literal("You are now DUNGEONEER.")
                                            .withStyle(ChatFormatting.GREEN), true);
                                    return 1;
                                }))
                        .executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();
                            if (!AccessPolicy.requireDeveloperOrConsole(src)) {
                                src.sendFailure(Component.literal("You do not have permission to use /dungeoneer."));
                                return 0;
                            }
                            ServerPlayer p = src.getPlayer();
                            if (p == null) {
                                src.sendFailure(Component.literal("Console must use /rank for a target."));
                                return 0;
                            }

                            MinecraftServer server = p.level().getServer();
                            if (server == null) return 0;

                            RankStore store = RankStore.get(server);
                            store.setRank(p.getUUID(), Rank.DUNGEONEER);

                            // Apply deop immediately and revoke developer-only label access.
                            OpUtil.setOperator(server, p, false);
                            SpawnerLabelServerState.revoke(p);
                            DungeonTravelRouter.evacuateUnauthorizedLocation(p);

                            src.sendSuccess(() -> Component.literal("You are now DUNGEONEER.")
                                    .withStyle(ChatFormatting.GREEN), true);
                            return 1;
                        })
        );
    }
}
