package net.goui.cosmicdungeon.economy;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Explicit bounded inspection only; no custody, grant, consumption, world scan or migration. */
public final class LegacyCurrencyReview {
    // Diagnostic work/output bounds, not gameplay modifiers.
    private static final int MAX_STACKS=256,MAX_DEPTH=8,MAX_ROWS=32;
    private LegacyCurrencyReview(){}
    private static final class Review {
        int remaining=MAX_STACKS;boolean incomplete;
        final List<String> rows=new ArrayList<>();
    }
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("currency").then(Commands.literal("legacy")
                .requires(WealthReviewCommands::allowed)
                .then(Commands.literal("receipts").executes(c->receipts(c.getSource())))
                .then(Commands.literal("inspect").then(Commands.argument("player",EntityArgument.player())
                        .executes(c->inspect(c.getSource(),EntityArgument.getPlayer(c,"player")))))
                .then(Commands.literal("entity").then(Commands.argument("item",EntityArgument.entity())
                        .executes(c->entity(c.getSource(),EntityArgument.getEntity(c,"item")))))));
    }
    private static int receipts(CommandSourceStack source) {
        WealthReviewCommands.actor(source);
        var summary=PlayerCurrencyData.get(source.getServer()).receiptRetentionSummary();
        source.sendSuccess(()->Component.literal("Retained account evidence: "+summary
                +". Retired mob details compact into exact intervals; other replay receipts are retained."),false);
        return 1;
    }
    private static int inspect(CommandSourceStack source,ServerPlayer player) {
        WealthReviewCommands.actor(source);
        var review=new Review();
        inspectContainer(player.getInventory(),"inventory",review);
        inspectContainer(player.getEnderChestInventory(),"ender_chest",review);
        source.sendSuccess(()->Component.literal("Legacy review for "+player.getUUID()
                +"; nominal values are not approved credit. Items and balances unchanged."),false);
        for(var row:review.rows)source.sendSuccess(()->Component.literal(row),false);
        source.sendSuccess(()->Component.literal((review.incomplete?"Inspection limited; more nested data requires review."
                :"Inspected current inventory/Ender Chest within bounds.")+" Offline/unloaded storage and old account credits are not covered."),false);
        return review.rows.size();
    }
    private static void inspectContainer(Container container,String path,Review review) {
        for(int slot=0;slot<container.getContainerSize();slot++) {
            if(review.remaining<=0 || review.rows.size()>=MAX_ROWS){review.incomplete=true;break;}
            inspectStack(container.getItem(slot),path+"["+slot+"]",0,review);
        }
    }
    private static void inspectStack(ItemStack stack,String path,int depth,Review review) {
        if(stack.isEmpty())return;
        if(depth>MAX_DEPTH || review.remaining--<=0 || review.rows.size()>=MAX_ROWS){review.incomplete=true;return;}
        String item=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        var kind=LegacyCurrencyPolicy.classify(item,DeathCurrencyService.marked(stack));
        if(kind!=LegacyCurrencyPolicy.Kind.ORDINARY)review.rows.add(describe(path,item,stack.getCount(),kind));
        var bundle=stack.get(DataComponents.BUNDLE_CONTENTS);
        if(bundle!=null) {
            int index=0;for(var child:bundle.items()) {
                if(review.remaining<=0 || review.rows.size()>=MAX_ROWS){review.incomplete=true;break;}
                inspectStack(child,path+"/bundle["+(index++)+"]",depth+1,review);
            }
        }
        var nested=stack.get(DataComponents.CONTAINER);
        if(nested!=null) {
            int index=0;for(var child:nested.nonEmptyItems()) {
                if(review.remaining<=0 || review.rows.size()>=MAX_ROWS){review.incomplete=true;break;}
                inspectStack(child,path+"/nonempty_container_entry["+(index++)+"]",depth+1,review);
            }
        }
    }
    private static String describe(String path,String item,int count,LegacyCurrencyPolicy.Kind kind) {
        String nominal="managed death amount belongs to its account record";
        if(kind==LegacyCurrencyPolicy.Kind.LEGACY_REVIEW)
            nominal="nominal "+LegacyCurrencyPolicy.nominalTrace(item,count)+" Trace; credit entitlement UNVERIFIED";
        return path+": "+count+" x "+item+" ["+kind+"; "+nominal+"]";
    }
    private static int entity(CommandSourceStack source,Entity entity) {
        WealthReviewCommands.actor(source);
        if(!(entity instanceof ItemEntity item)) {
            source.sendFailure(Component.literal("Select one already-loaded item entity."));return 0;
        }
        var stack=item.getItem();String key=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        var kind=LegacyCurrencyPolicy.classify(key,DeathCurrencyService.logical(item));
        if(kind==LegacyCurrencyPolicy.Kind.ORDINARY) {
            source.sendSuccess(()->Component.literal("Not a legacy denomination or managed death marker."),false);return 0;
        }
        source.sendSuccess(()->Component.literal(describe("entity "+item.getUUID(),key,stack.getCount(),kind)
                +" in "+item.level().dimension().location()+" at "+item.blockPosition()
                +". No mutation. Capture full native entity data and a consistent backup before recovery."),false);
        return 1;
    }
}
