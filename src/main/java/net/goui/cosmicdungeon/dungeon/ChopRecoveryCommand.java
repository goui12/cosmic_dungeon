package net.goui.cosmicdungeon.dungeon;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.component.ModDataComponents;
import net.goui.cosmicdungeon.item.identity.ItemAuthoringPlan;
import net.goui.cosmicdungeon.transaction.InventoryTransactionGuard;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import java.util.*;

/** One developer, one online owner, one reviewed existing stack. No chunk load or bulk migration. */
@EventBusSubscriber(modid=CosmicDungeonMod.MOD_ID)
public final class ChopRecoveryCommand {
    private record Pending(ServerPlayer target,ChopTravelPlan plan,ItemAuthoringPlan<CompoundTag> review) {}
    private static final Map<ServerPlayer,Pending> PENDING=new WeakHashMap<>();
    private ChopRecoveryCommand() {}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("d1").then(Commands.literal("chop")
                .requires(AccessPolicy::requireDeveloperOrConsole)
                .then(Commands.literal("inspect").then(Commands.argument("owner",EntityArgument.player())
                        .executes(c->inspect(c.getSource(),EntityArgument.getPlayer(c,"owner")))))
                .then(Commands.literal("preview").then(Commands.argument("owner",EntityArgument.player())
                        .then(Commands.argument("slot",IntegerArgumentType.integer(0,35))
                                .executes(c->preview(c.getSource(),EntityArgument.getPlayer(c,"owner"),IntegerArgumentType.getInteger(c,"slot"))))))
                .then(Commands.literal("apply").then(Commands.argument("token",StringArgumentType.word())
                        .executes(c->apply(c.getSource(),StringArgumentType.getString(c,"token")))))));
    }
    private static ServerPlayer developer(CommandSourceStack source) {
        var p=source.getPlayer();
        if(p==null||!AccessPolicy.isDeveloper(p)) {
            source.sendFailure(Component.literal("Use an in-game developer for a Chop review."));return null;
        }
        return p;
    }
    private static long now(ServerPlayer p) {return p.level().getServer().overworld().getGameTime();}
    private static int chops(Container inventory) {
        int count=0;for(int i=0;i<inventory.getContainerSize();i++)if(ChopOwnershipService.isChop(inventory.getItem(i)))count++;
        return count;
    }
    private static boolean available(ServerPlayer p) {
        var server=p.level().getServer();
        return server.getPlayerList().getPlayer(p.getUUID())==p&&p.isAlive()&&!p.isPassenger()
                &&p.containerMenu==p.inventoryMenu&&p.inventoryMenu.getCarried().isEmpty()
                &&!InventoryTransactionGuard.blocked(p);
    }
    private static ChopRecoveryReview.Evidence evidence(ServerPlayer p,ItemStack item) {
        var server=p.level().getServer();var runs=DungeonRunRegistryData.get(server);var owners=ChopOwnershipData.get(server);
        var entry=owners.entry(p.getUUID());var target=item.get(ModDataComponents.DUNGEON_RETURN_TARGET.get());
        var token=item.get(ModDataComponents.CHOP_TOKEN.get());
        boolean retained=runs.findRunForPlayer(p.getUUID()).isPresent()
                ||DungeonInstanceSlots.slotOf(p.level().dimension()).isPresent()||DungeonDefinitions.byDimension(p.level().dimension()).isPresent()
                ||entry!=null&&entry.runId()>0&&runs.getRun(entry.runId()).isPresent()
                ||target!=null&&runs.getRun(target.runId()).isPresent();
        return new ChopRecoveryReview.Evidence(p.getUUID(),item.getCount(),chops(p.getInventory()),chops(p.getEnderChestInventory()),
                item.get(ModDataComponents.CHOP_OWNER.get()),token,entry,target==null?null:target.owner(),
                target==null?0:target.runId(),retained,ChopOwnershipService.hasStoredRecovery(p),owners.tokenClaimedByOther(p.getUUID(),token));
    }
    private static CompoundTag snapshot(ServerPlayer p) {
        var image=new CompoundTag();image.put("inventory",ChopTravelRecovery.saveInventory(p));
        var ender=NonNullList.withSize(p.getEnderChestInventory().getContainerSize(),ItemStack.EMPTY);
        for(int i=0;i<ender.size();i++)ender.set(i,p.getEnderChestInventory().getItem(i).copy());
        image.put("ender",ChopTravelRecovery.encode(p,ender));image.put("pose",ChopTravelRecovery.pose(p));
        image.put("ownership",ChopOwnershipData.get(p.level().getServer()).image(p.getUUID()));return image;
    }
    private static int inspect(CommandSourceStack source,ServerPlayer target) {
        if(developer(source)==null)return 0;
        try {
            var entry=ChopOwnershipData.get(target.level().getServer()).entry(target.getUUID());
            source.sendSuccess(()->Component.literal("Chop review for "+target.getUUID()+": ownership="+entry
                    +"; pending inventory recovery="+ChopOwnershipService.hasStoredRecovery(target)
                    +"; transaction/interface ready="+available(target)),false);
            for(int slot=0;slot<target.getInventory().getContainerSize();slot++) {
                var item=target.getInventory().getItem(slot);if(!ChopOwnershipService.isChop(item))continue;
                int index=slot;String reason=ChopRecoveryReview.rejection(evidence(target,item));
                source.sendSuccess(()->Component.literal("Slot "+index+": "+item.getCount()+" x "+item.getItem()
                        +"; owner="+item.get(ModDataComponents.CHOP_OWNER.get())+"; token="+item.get(ModDataComponents.CHOP_TOKEN.get())
                        +"; return="+item.get(ModDataComponents.DUNGEON_RETURN_TARGET.get())
                        +"; old coordinates="+item.get(ModDataComponents.COORDINATES.get())
                        +"; "+(reason.isEmpty()?"eligible for explicit preview":reason)),false);
            }
            source.sendSuccess(()->Component.literal("Ender Chest Chop stacks: "+chops(target.getEnderChestInventory())
                    +". Inspection never changes items; unopened/unloaded storage is not scanned."),false);
            return 1;
        } catch(RuntimeException error) {source.sendFailure(Component.literal("Saved evidence needs review; nothing changed. "+error.getMessage()));return 0;}
    }
    private static int preview(CommandSourceStack source,ServerPlayer target,int slot) {
        var actor=developer(source);if(actor==null)return 0;
        PENDING.remove(actor);
        if(!available(actor)||!available(target)) {source.sendFailure(Component.literal("Both players must close interfaces and settle inventory recovery."));return 0;}
        try {
            var item=target.getInventory().getItem(slot);
            if(!ChopOwnershipService.isChop(item))throw new IllegalArgumentException("Select one existing Chop.");
            String reason=ChopRecoveryReview.rejection(evidence(target,item));
            if(!reason.isEmpty())throw new IllegalArgumentException(reason);
            var token=item.get(ModDataComponents.CHOP_TOKEN.get());if(token==null)token=UUID.randomUUID();
            var next=new ChopOwnershipData.Entry(token.toString(),0,false);
            var before=snapshot(target);var items=ChopTravelRecovery.inventory(target);
            items.set(slot,ChopOwnershipService.rawFrom(item,target.getUUID(),next));
            var after=ChopTravelRecovery.encode(target,items);var ownership=ChopOwnershipData.entryImage(next);
            if(after.equals(before.getCompoundOrEmpty("inventory"))&&ownership.equals(before.getCompoundOrEmpty("ownership")))
                throw new IllegalArgumentException("This Chop is already current.");
            var pose=before.getCompoundOrEmpty("pose");
            var plan=ChopTravelPlan.create(target.getUUID(),0,"adopt",before.getCompoundOrEmpty("inventory"),after,
                    pose,pose,new CompoundTag(),new CompoundTag(),before.getCompoundOrEmpty("ownership"),ownership,null).reviewed(actor.getUUID(),slot);
            ChopTravelRecovery.decode(target,after); // Exact native components must round-trip before offering Apply.
            var review=new ItemAuthoringPlan<>(plan.id().toString(),now(actor)+Config.ITEM_AUTHORING_SECONDS.get()*20L,before,before);
            PENDING.put(actor,new Pending(target,plan,review));
            source.sendSuccess(()->Component.literal("PREVIEW ONLY: owner "+target.getUUID()+", slot "+slot
                    +" -> one owner-bound Raw Chop. Old return coordinates will be removed; other components and slots stay exact."
                    +" No teleport, escrow deletion or inventory grant. Keep the owner's inventory, Ender Chest and position unchanged."
                    +" Apply: /d1 chop apply "+review.token()),false);
            return 1;
        } catch(RuntimeException error) {source.sendFailure(Component.literal("Preview refused; unchanged. "+error.getMessage()));return 0;}
    }
    private static int apply(CommandSourceStack source,String token) {
        var actor=developer(source);if(actor==null)return 0;var pending=PENDING.get(actor);if(pending==null)return 0;
        var target=pending.target();
        try {
            boolean same=actor.level().getServer()==target.level().getServer()&&available(actor)&&available(target);
            if(!same||!pending.review().accepts(token,now(actor),true,snapshot(target),false,CompoundTag::equals))
                throw new IllegalArgumentException("Expired, changed, disconnected or wrong-token review; preview again.");
            int slot=pending.plan().tag("review").getInt("slot").orElseThrow();
            var item=target.getInventory().getItem(slot);String reason=ChopRecoveryReview.rejection(evidence(target,item));
            if(!reason.isEmpty())throw new IllegalArgumentException(reason);
            PENDING.remove(actor); // Retries settle the saved journal; they never submit this preview twice.
            if(!ChopTravelRecovery.execute(target,pending.plan())) {
                source.sendFailure(Component.literal("Recovery did not complete. Preserve the journal; reconnect the owner to settle it."));return 0;
            }
            source.sendSuccess(()->Component.literal("Reviewed Chop recovered as Raw. Decision "+pending.plan().id()
                    +" is receipted with its developer, exact original item and ownership evidence."),true);
            return 1;
        } catch(RuntimeException error) {
            PENDING.remove(actor);source.sendFailure(Component.literal("Apply refused; saved evidence retained. "+error.getMessage()));return 0;
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING.entrySet().removeIf(e->e.getKey()==event.getEntity()||e.getValue().target()==event.getEntity());
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        PENDING.entrySet().removeIf(e->e.getKey().level().getServer()==event.getServer());
    }
    // TODO(M20/M03, Q&A D02/D04/D24): review complete backups for duplicate/overstacked or foreign
    // Chops, orphan escrow, partial owner markers and unloaded/nested storage. Never split/delete
    // stacks, infer a dropped owner, clear custody, or grant another token to bypass an unresolved
    // inventory. The latest owner receipt is bounded evidence, not a permanent migration archive.
}
