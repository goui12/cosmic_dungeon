package net.goui.cosmicdungeon.playerclass.dragoon.repair;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

public final class DragoonRepairCommand {
    private DragoonRepairCommand() {}
    public static void register(CommandDispatcher<CommandSourceStack> d){ d.register(Commands.literal("repair")
            .then(Commands.argument("player", EntityArgument.player()).executes(ctx->request(ctx.getSource(), EntityArgument.getPlayer(ctx,"player"))))
            .then(Commands.literal("accept").then(Commands.argument("player", EntityArgument.player()).executes(ctx->accept(ctx.getSource(), EntityArgument.getPlayer(ctx,"player")))))
            .then(Commands.literal("deny").then(Commands.argument("player", EntityArgument.player()).executes(ctx->deny(ctx.getSource(), EntityArgument.getPlayer(ctx,"player")))))
            .then(Commands.literal("cancel").executes(ctx->cancel(ctx.getSource())))); }
    private static int request(CommandSourceStack src, ServerPlayer t){ try{ DragoonRepairSessionData.invite(src.getPlayerOrException(),t); return 1; }catch(Exception e){ return 0; }}
    private static int accept(CommandSourceStack src, ServerPlayer d){ try{ return DragoonRepairSessionData.acceptInvite(src.getPlayerOrException(),d)?1:0; }catch(Exception e){ return 0; }}
    private static int deny(CommandSourceStack src, ServerPlayer d){ try{ return DragoonRepairSessionData.denyInvite(src.getPlayerOrException(),d)?1:0; }catch(Exception e){ return 0; }}
    private static int cancel(CommandSourceStack src){ try{ DragoonRepairSessionData.cancel(src.getPlayerOrException(),"Cancelled"); return 1; }catch(Exception e){ return 0; }}
}
