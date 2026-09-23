package net.goui.cosmicdungeon.economy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.context.CommandContext;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Revision-checked developer decisions; all evidence and money share the existing account journal. */
public final class WealthReviewCommands {
    private WealthReviewCommands(){}
    static String actor(CommandSourceStack source){
        var player=source.getPlayer();
        if(player!=null&&source.source==player.commandSource()&&AccessPolicy.isDeveloper(player)
                &&source.getServer().getPlayerList().getPlayer(player.getUUID())==player)return "developer:"+player.getUUID();
        if(source.getEntity()==null&&source.source==source.getServer())return "console";
        throw new IllegalArgumentException("Use a connected developer or the direct server console for economic reviews.");
    }
    static boolean allowed(CommandSourceStack source){
        try{actor(source);return true;}catch(IllegalArgumentException denied){return false;}
    }
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        var owner=Commands.argument("player",UuidArgument.uuid()).executes(c->inspect(c,0));
        owner.then(Commands.literal("after").then(Commands.argument("afterThreshold",LongArgumentType.longArg(0))
                .executes(c->inspect(c,LongArgumentType.getLong(c,"afterThreshold")))));
        for(String action:List.of("ack","resolve","reopen")){
            var revision=Commands.argument("revision",LongArgumentType.longArg(0));
            if(action.equals("ack"))revision.executes(c->decide(c,action,""));
            else revision.then(Commands.argument("note",StringArgumentType.greedyString())
                    .executes(c->decide(c,action,StringArgumentType.getString(c,"note"))));
            owner.then(Commands.literal(action).then(Commands.argument("threshold",LongArgumentType.longArg(1)).then(revision)));
        }
        dispatcher.register(Commands.literal("currency").then(Commands.literal("review").requires(WealthReviewCommands::allowed)
                .then(Commands.literal("pending").executes(c->pending(c,0))
                        .then(Commands.argument("afterSequence",LongArgumentType.longArg(0))
                                .executes(c->pending(c,LongArgumentType.getLong(c,"afterSequence")))))
                .then(owner)));
    }
    private static int fail(CommandSourceStack source,Exception error){
        source.sendFailure(Component.literal(error.getMessage()==null?"Economic review requires recovery.":error.getMessage()));return 0;
    }
    private static PlayerCurrencyData verified(CommandSourceStack source){
        actor(source);var data=PlayerCurrencyData.get(source.getServer());
        if(!data.flushVerified())throw new IllegalStateException("Account write/readback failed; evidence retained. Retry after storage recovery.");
        return data;
    }
    private static int inspect(CommandContext<CommandSourceStack> c,long after){
        var source=c.getSource();
        try{
            var data=verified(source);UUID owner=UuidArgument.getUuid(c,"player");
            if(data.observeWealth(owner,System.currentTimeMillis())&&!data.flushVerified())
                throw new IllegalStateException("Observation awaits verified persistence; retry after storage recovery.");
            var rows=data.wealthReviews().owner(owner,after,16);
            if(rows.isEmpty())source.sendSuccess(()->Component.literal("No further wealth reviews for "+owner+" after threshold "+after+"."),false);
            for(var row:rows)source.sendSuccess(()->Component.literal(describe(row)),false);
            if(rows.size()==16)source.sendSuccess(()->Component.literal("Next page: /currency review "+owner+" after "+rows.getLast().threshold()),false);
            return 1;
        }catch(Exception error){return fail(source,error);}
    }
    private static int pending(CommandContext<CommandSourceStack> c,long after){
        var source=c.getSource();
        try{
            var data=verified(source);var rows=data.wealthReviews().pending(after,16);
            source.sendSuccess(()->Component.literal(data.wealthReviews().pendingCount()+" unacknowledged economic reviews; page after "+after+"."),false);
            for(var row:rows)source.sendSuccess(()->Component.literal(describe(row)),false);
            if(!rows.isEmpty())source.sendSuccess(()->Component.literal("Next page: /currency review pending "+rows.getLast().sequence()),false);
            return 1;
        }catch(Exception error){return fail(source,error);}
    }
    private static int decide(CommandContext<CommandSourceStack> c,String action,String note){
        var source=c.getSource();
        try{
            var data=verified(source);UUID owner=UuidArgument.getUuid(c,"player");
            long threshold=LongArgumentType.getLong(c,"threshold"),revision=LongArgumentType.getLong(c,"revision");
            // Revalidate the actual developer connection/console at mutation, not only Brigadier visibility.
            boolean changed=data.decideWealth(owner,threshold,revision,action,actor(source),note,System.currentTimeMillis());
            if(!data.flushVerified())throw new IllegalStateException("Review decision is pending write/readback verification. Retry the identical command; do not assume it was lost.");
            var row=data.wealthReviews().find(owner,threshold).orElseThrow();
            source.sendSuccess(()->Component.literal((changed?"Review saved. ":"Identical review already saved. ")+describe(row)),false);return 1;
        }catch(Exception error){return fail(source,error);}
    }
    public static String describe(WealthReviewState.Notice notice){
        var e=notice.evidence();
        return "Review "+notice.sequence()+" owner="+notice.owner()+" name="+e.getStringOr("name","")
                +" threshold="+notice.threshold()+" revision="+notice.revision()+" "+(notice.finalReview()||notice.threshold()==D1EconomyConfig.WEALTH_MAX.get()?"final ":"")
                +(notice.resolved()?"resolved":notice.acknowledged()?"acknowledged/open":"pending/open")
                +" origin="+notice.origin()+" balance="+e.getLongOr("before",0)+" -> "+e.getLongOr("after",0)
                +" type="+e.getStringOr("type","")+" requested="+e.getLongOr("requested_trace",0)
                +" transaction="+e.getStringOr("transaction","")+" time="+java.time.Instant.ofEpochMilli(e.getLongOr("timestamp",0))
                +(notice.revision()==0?"":" last="+notice.action()+" by="+notice.actor()+" note="+notice.note())
                +". Inspect: /currency review "+notice.owner();
    }
}
