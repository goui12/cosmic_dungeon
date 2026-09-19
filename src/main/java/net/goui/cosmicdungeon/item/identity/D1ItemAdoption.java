package net.goui.cosmicdungeon.item.identity;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.economy.pricing.ItemTransferRules;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import java.util.*;

/** Explicit one-slot legacy adoption. Does not scan/load chunks, rewrite chests, infer names,
 * regenerate equipment, or migrate spawner data. Only a previewed complete stack can change. */
@EventBusSubscriber(modid = CosmicDungeonMod.MOD_ID)
public final class D1ItemAdoption {
    private record Pending(String dimension, BlockPos pos, Container container, int slot,
                           ItemAuthoringPlan<ItemStack> edit) {}
    private static final Map<UUID,Pending> PENDING = new HashMap<>();
    private D1ItemAdoption() {}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("d1").then(Commands.literal("item")
                .requires(AccessPolicy::requireDeveloperOrConsole)
                .then(Commands.literal("preview").then(Commands.argument("identity",StringArgumentType.word())
                        .suggests((c,b)->SharedSuggestionProvider.suggest(D1LootCatalog.ids(),b))
                        .executes(c->previewHeld(c.getSource(),StringArgumentType.getString(c,"identity")))))
                .then(Commands.literal("container").then(Commands.argument("position",BlockPosArgument.blockPos())
                        .then(Commands.argument("slot",IntegerArgumentType.integer(0,255))
                                .then(Commands.argument("identity",StringArgumentType.word())
                                        .suggests((c,b)->SharedSuggestionProvider.suggest(D1LootCatalog.ids(),b))
                                        .executes(c->previewContainer(c.getSource(),BlockPosArgument.getLoadedBlockPos(c,"position"),
                                                IntegerArgumentType.getInteger(c,"slot"),StringArgumentType.getString(c,"identity")))))))
                .then(Commands.literal("apply").then(Commands.argument("token",StringArgumentType.word())
                        .executes(c->change(c.getSource(),StringArgumentType.getString(c,"token"),false))))
                .then(Commands.literal("undo").then(Commands.argument("token",StringArgumentType.word())
                        .executes(c->change(c.getSource(),StringArgumentType.getString(c,"token"),true))))
                .then(Commands.literal("survey").executes(c->survey(c.getSource())))));
    }
    private static ServerPlayer developer(CommandSourceStack source) {
        var p=source.getPlayer();
        if(p==null || !AccessPolicy.isDeveloper(p) || p.containerMenu!=p.inventoryMenu) {
            source.sendFailure(Component.literal("Use an in-game developer with other interfaces closed.")); return null;
        }
        return p;
    }
    public static int previewHeld(CommandSourceStack source,String identity) {
        var p=developer(source);return p==null?0:preview(source,p,null,p.getInventory(),p.getInventory().getSelectedSlot(),identity);
    }
    private static int previewContainer(CommandSourceStack source,BlockPos pos,int slot,String identity) {
        var p=developer(source);if(p==null)return 0;
        if(!inRange(p,pos)||!(p.level().getBlockEntity(pos) instanceof Container container)) {
            source.sendFailure(Component.literal("Select a nearby, already-loaded item container."));return 0;
        }
        return preview(source,p,pos.immutable(),container,slot,identity);
    }
    private static boolean inRange(ServerPlayer p,BlockPos pos) {
        double range=Config.ITEM_AUTHORING_RANGE.get();
        return p.level().hasChunkAt(pos)&&p.distanceToSqr(Vec3.atCenterOf(pos))<=range*range;
    }
    private static long now(ServerPlayer p) { return p.level().getServer().overworld().getGameTime(); }
    private static int preview(CommandSourceStack source,ServerPlayer p,BlockPos pos,Container c,int slot,String identity) {
        if(slot<0||slot>=c.getContainerSize())return 0;
        if(c instanceof net.minecraft.world.RandomizableContainer random && random.getLootTable()!=null) {
            source.sendFailure(Component.literal("Unopened loot table: preview will not generate or replace its contents."));return 0;
        }
        var before=c.getItem(slot);
        var facts=ItemTransferRules.facts(before);
        if(before.isEmpty()||facts.protectedItem()||facts.noTrade()||facts.noSale()||facts.repairMarked()) {
            source.sendFailure(Component.literal("Empty/protected/repair items cannot be reclassified."));return 0;
        }
        try {
            var named=identity.isEmpty()?null:D1LootCatalog.find(identity);
            if(!identity.isEmpty()&&named==null)throw new IllegalArgumentException("Unknown identity");
            var provenance=new ItemProvenance(ItemProvenance.LOOT,identity,named==null?ItemProvenanceService.itemKey(before):named.baseItem());
            var after=ItemProvenanceService.adoptCopy(before,provenance);
            String token=UUID.randomUUID().toString();
            var edit=new ItemAuthoringPlan<>(token,now(p)+Config.ITEM_AUTHORING_SECONDS.get()*20L,before.copy(),after);
            PENDING.put(p.getUUID(),new Pending(p.level().dimension().location().toString(),pos,c,slot,edit));
            source.sendSuccess(()->Component.literal("PREVIEW ONLY: "+before.getCount()+" x "+ItemProvenanceService.itemKey(before)
                    +" at "+(pos==null?"held slot "+slot:pos.toShortString()+" slot "+slot)
                    +" -> "+provenance.encode()+". Counts and existing components unchanged. Apply: /d1 item apply "+token
                    +". Exact undo available until preview expiry: /d1 item undo "+token),false);
            return 1;
        } catch(IllegalArgumentException invalid) {source.sendFailure(Component.literal(invalid.getMessage()+". Unchanged."));return 0;}
    }
    private static int change(CommandSourceStack source,String token,boolean undo) {
        var p=developer(source);if(p==null)return 0;
        var pending=PENDING.get(p.getUUID());if(pending==null)return 0;
        boolean target=pending.dimension().equals(p.level().dimension().location().toString())
                &&(pending.pos()==null?pending.container()==p.getInventory()&&pending.slot()==p.getInventory().getSelectedSlot()
                :inRange(p,pending.pos())&&p.level().getBlockEntity(pending.pos())==pending.container());
        target &= pending.slot()>=0 && pending.slot()<pending.container().getContainerSize();
        if(pending.container() instanceof net.minecraft.world.RandomizableContainer random && random.getLootTable()!=null)target=false;
        var actual=target?pending.container().getItem(pending.slot()):ItemStack.EMPTY;
        if(!pending.edit().accepts(token,now(p),target,actual,undo,ItemStack::matches)) {
            if(pending.edit().expired(now(p)))PENDING.remove(p.getUUID());
            source.sendFailure(Component.literal("Expired, changed, wrong-location or already-used action. Item unchanged."));return 0;
        }
        pending.container().setItem(pending.slot(),pending.edit().image(undo).copy());
        pending.container().setChanged();pending.edit().committed(undo);
        if(undo)PENDING.remove(p.getUUID());
        p.inventoryMenu.broadcastChanges();
        source.sendSuccess(()->Component.literal(undo?"Exact original stack restored.":"Preview applied; one existing stack classified."),true);
        return 1;
    }
    private static int survey(CommandSourceStack source) {
        var p=developer(source);if(p==null)return 0;
        int unclassified=0;
        for(int slot=0;slot<p.getInventory().getContainerSize();slot++) {
            var s=p.getInventory().getItem(slot);
            if(s.isEmpty()||!ItemProvenanceService.requiresProvenance(s))continue;
            var f=ItemTransferRules.facts(s);
            String status=f.protectedItem()?"protected":f.provenanceValid()?"classified":f.provenancePresent()?"invalid/unknown":"unclassified";
            if(status.equals("unclassified"))unclassified++;
            int index=slot;
            source.sendSuccess(()->Component.literal("Slot "+index+": "+s.getCount()+" x "+ItemProvenanceService.itemKey(s)+" ["+status+"]"),false);
        }
        return unclassified;
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){PENDING.remove(e.getEntity().getUUID());}
    @SubscribeEvent public static void stopped(ServerStoppedEvent e){PENDING.clear();}
    // TODO(M72, reviewed TEST migration): map trusted authored locations/drop definitions,
    // including unloaded chunks and preset files. These commands never infer identity from names.
    // Preview/apply/undo cover loaded held/container stacks only, not complete legacy migration.
    // Capture a world backup before production adoption; temporary undo expires or clears at logout.
}
