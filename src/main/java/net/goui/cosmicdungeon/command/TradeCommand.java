package net.goui.cosmicdungeon.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

public final class TradeCommand {
    private TradeCommand() {}
    public static void register(CommandDispatcher<CommandSourceStack> d){
        d.register(Commands.literal("trade")
            .then(Commands.argument("player", EntityArgument.player()).executes(ctx->request(ctx.getSource(), EntityArgument.getPlayer(ctx,"player"))))
            .then(Commands.literal("accept").then(Commands.argument("player", EntityArgument.player()).executes(ctx->accept(ctx.getSource(), EntityArgument.getPlayer(ctx,"player")))))
            .then(Commands.literal("deny").then(Commands.argument("player", EntityArgument.player()).executes(ctx->deny(ctx.getSource(), EntityArgument.getPlayer(ctx,"player")))))
            .then(Commands.literal("cancel").executes(ctx->cancel(ctx.getSource()))));
    }
    private static int request(CommandSourceStack src, ServerPlayer t){ try{ var sp=src.getPlayerOrException(); net.goui.cosmicdungeon.trade.TradeSessionData.invite(sp,t); return 1;}catch(Exception e){return 0;}}
    private static int accept(CommandSourceStack src, ServerPlayer inviter){ try{ var sp=src.getPlayerOrException(); return net.goui.cosmicdungeon.trade.TradeSessionData.acceptInvite(sp,inviter)?1:0;}catch(Exception e){return 0;}}
    private static int deny(CommandSourceStack src, ServerPlayer requester){ try{ var sp=src.getPlayerOrException(); return net.goui.cosmicdungeon.trade.TradeSessionData.denyInvite(sp,requester)?1:0;}catch(Exception e){return 0;}}
    private static int cancel(CommandSourceStack src){ try{ var sp=src.getPlayerOrException(); net.goui.cosmicdungeon.trade.TradeSessionData.cancel(sp,"Cancelled"); return 1;}catch(Exception e){return 0;}}
}
