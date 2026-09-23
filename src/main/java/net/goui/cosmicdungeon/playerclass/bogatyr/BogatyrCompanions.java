package net.goui.cosmicdungeon.playerclass.bogatyr;

import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.dungeon.DungeonRunRegistryData;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import java.util.*;

@EventBusSubscriber(modid="cosmicdungeon")
public final class BogatyrCompanions {
    private BogatyrCompanions(){}
    public static UUID owner(Wolf wolf){
        return wolf.getOwnerReference()==null?null:wolf.getOwnerReference().getUUID();
    }
    /** O(1) entity lookup; ticks only update changed chunks. No nearby-entity or inventory scan. */
    public static void track(Wolf wolf,boolean exactPosition){
        if(!(wolf.level() instanceof ServerLevel level)||!wolf.isTame()
                ||!BogatyrWolfEvents.managed(wolf)||owner(wolf)==null||wolf.isRemoved()||!wolf.isAddedToLevel())return;
        if(!BogatyrIdentity.observe(wolf))return;
        var data=BogatyrCompanionData.get(level.getServer());
        UUID identity=BogatyrIdentity.id(wolf);
        var old=data.find(identity).orElse(null);
        var pos=wolf.blockPosition();
        String dimension=level.dimension().location().toString();
        long run=wolf.getPersistentData().getLongOr(BogatyrWolfEvents.RUN,0);
        if(!exactPosition&&old!=null&&old.located()&&old.owner().equals(owner(wolf))&&old.run()==run
                &&old.dimension().equals(dimension)&&old.entityUuid().equals(wolf.getUUID())
                &&(net.minecraft.core.BlockPos.of(old.position()).getX()>>4)==(pos.getX()>>4)
                &&(net.minecraft.core.BlockPos.of(old.position()).getZ()>>4)==(pos.getZ()>>4))return;
        data.remember(new BogatyrCompanionData.Companion(identity,owner(wolf),run,dimension,pos.asLong(),true,wolf.getUUID()));
        BogatyrIdentity.recorded(wolf);
    }
    public static void added(Wolf wolf){
        if(!(wolf.level() instanceof ServerLevel level)||!wolf.isAddedToLevel()
                ||level.getEntity(wolf.getUUID())!=wolf)return;
        if(BogatyrWolfEvents.managed(wolf)&&!BogatyrIdentity.observe(wolf))return;
        BogatyrWolfEvents.added(wolf);track(wolf,true);BogatyrRecovery.onObserved(wolf);
    }
    public static void removing(Wolf wolf,Entity.RemovalReason reason){
        if(!(wolf.level() instanceof ServerLevel level)||!BogatyrWolfEvents.managed(wolf))return;
        // Called before setRemoved: unload/dimension travel retains the real last location.
        track(wolf,true);
        if(BogatyrIdentity.held(wolf))return;
        if(reason==Entity.RemovalReason.CHANGED_DIMENSION)BogatyrIdentity.departing(wolf);
        if(reason==Entity.RemovalReason.KILLED&&owner(wolf)!=null){
            BogatyrCompanionData.get(level.getServer()).died(BogatyrIdentity.id(wolf),owner(wolf));
            D1RunData.get(level.getServer()).removeUnique(wolf.getPersistentData().getLongOr(BogatyrWolfEvents.RUN,0),
                    "wolves:"+owner(wolf),BogatyrIdentity.id(wolf).toString());
        }
        // DISCARD is not proof of death. Keep a hold for explicit recovery/inspection.
    }
    @SubscribeEvent public static void tick(EntityTickEvent.Post event){
        if(event.getEntity() instanceof Wolf wolf){BogatyrRecovery.onObserved(wolf);BogatyrDuration.tick(wolf);track(wolf,false);}
    }
    public static void preserveRun(MinecraftServer server,DungeonRunRegistryData.RunRecord run){
        if(!run.dungeonId().equals("dungeon_1")||run.dungeonDimensionIds().isEmpty())return;
        var data=BogatyrCompanionData.get(server);
        for(UUID owner:run.orderedPlayers())
            for(String id:D1RunData.get(server).values(run.runId(),"wolves:"+owner))
                data.preserveLegacy(UUID.fromString(id),owner,run.runId(),run.dungeonDimensionIds().getFirst());
    }
    public static boolean preserveBeforeCleanup(MinecraftServer server,DungeonRunRegistryData.RunRecord run){
        preserveRun(server,run);
        // Verify the independent directory on disk BEFORE its legacy source roster can be cleared.
        return !run.dungeonId().equals("dungeon_1")||BogatyrCompanionData.get(server).flushVerified(server);
    }
    public static int packSize(MinecraftServer server,UUID owner,long runId){
        DungeonRunRegistryData.get(server).getRun(runId).ifPresent(run->preserveRun(server,run));
        return BogatyrCompanionData.get(server).activeCount(owner);
    }
    /** Preflight every target before ANY purge/filesystem replacement. Never load chunks here. */
    public static Optional<String> resetBlocker(ServerLevel level){
        var server=level.getServer();
        boolean associatedRun=false;
        for(var run:DungeonRunRegistryData.get(server).listAllRuns())
            if(run.containsDimension(level.dimension())&&run.dungeonId().equals("dungeon_1")){
                preserveRun(server,run);associatedRun=true;
            }
        var data=BogatyrCompanionData.get(server);
        // Even a wolf already moved outside must be durable before its old source roster is erased.
        if(associatedRun&&!data.flushVerified(server))
            return Optional.of("Companion directory could not be verified on disk; retaining this instance.");
        if(data.dimensionHeld(level.dimension().location().toString()))
            return Optional.of("Companion identity conflict requires review before this instance can reset.");
        if(!data.pendingInDimension(level.dimension().location().toString()).isEmpty())
            return Optional.of("A companion delivery must be reconciled before resetting this instance.");
        var entries=data.inDimension(level.dimension().location().toString());
        if(entries.stream().allMatch(c->data.readyForSourceReset(c.wolf())))return Optional.empty();
        return Optional.of("Companion recovery must finish before resetting "+level.dimension().location()
                +": "+entries.size()+" saved wolf identities remain. Use /d1 wolves inspect.");
    }
    private static int roster(net.minecraft.commands.CommandSourceStack source,int page)throws com.mojang.brigadier.exceptions.CommandSyntaxException{
        var player=source.getPlayerOrException();var data=BogatyrCompanionData.get(source.getServer());
        var entries=data.forOwner(player.getUUID());int active=data.activeCount(player.getUUID());
        int pages=Math.max(1,(entries.size()+31)/32);
        if(page>pages){source.sendFailure(Component.literal("Your companion roster has "+pages+" page(s)."));return 0;}
        source.sendSuccess(()->Component.literal("Companions: "+active+" active / "+Config.WOLF_CAP.get()
                +"; "+(entries.size()-active)+" stored; "+entries.size()+" total. Page "+page+" / "+pages+"."),false);
        for(var entry:entries.stream().skip((page-1L)*32).limit(32).toList()){
            var archive=data.archive(entry.wolf()).orElse(null);
            String status=archive==null||archive.phase().equals(WolfArchive.DONE)
                    ?(entry.located()?"active":"location needs review")
                    :switch(archive.phase()){
                        case WolfArchive.STORED->"stored";
                        case WolfArchive.PREPARED->"waiting for dungeon reset";
                        default->"return pending";
                    };
            source.sendSuccess(()->Component.literal(entry.wolf()+" | "+status+" | "+entry.dimension()),false);
        }
        return entries.size();
    }
    public static void register(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher){
        BogatyrReview.register(dispatcher);
        dispatcher.register(Commands.literal("d1").then(Commands.literal("wolves")
                .then(Commands.literal("recover").executes(ctx->BogatyrRecovery.recover(ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("bond",net.minecraft.commands.arguments.UuidArgument.uuid())
                                .executes(ctx->BogatyrRecovery.recover(ctx.getSource().getPlayerOrException(),
                                        net.minecraft.commands.arguments.UuidArgument.getUuid(ctx,"bond")))))
                .then(Commands.literal("call").executes(ctx->BogatyrRecovery.call(ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("bond",net.minecraft.commands.arguments.UuidArgument.uuid())
                                .executes(ctx->BogatyrRecovery.call(ctx.getSource().getPlayerOrException(),
                                        net.minecraft.commands.arguments.UuidArgument.getUuid(ctx,"bond")))))
                .executes(ctx->roster(ctx.getSource(),1))
                .then(Commands.argument("page",com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                        .executes(ctx->roster(ctx.getSource(),com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx,"page"))))
                .then(Commands.literal("inspect").requires(AccessPolicy::requireDeveloperOrConsole).executes(ctx->{
                    var entries=BogatyrCompanionData.get(ctx.getSource().getServer())
                            .inDimension(ctx.getSource().getLevel().dimension().location().toString());
                    ctx.getSource().sendSuccess(()->Component.literal(entries.size()+" companion identities in this dimension."),false);
                    for(var entry:entries.stream().limit(32).toList())ctx.getSource().sendSuccess(()->Component.literal(
                            entry.wolf()+" entity="+entry.entityUuid()+" owner="+entry.owner()+" run="+entry.run()+" located="+entry.located()
                                    +" pos="+net.minecraft.core.BlockPos.of(entry.position()).toShortString()
                                    +" phase="+BogatyrCompanionData.get(ctx.getSource().getServer()).archive(entry.wolf()).map(WolfArchive::phase).orElse("live")),false);
                    return entries.size();
                }))));
    }
    // TODO(M44 recovery review): inspect uncertain legacy/identity/delivery holds before explicit recovery.
    // Wolf Internal 10-3IgopUqHKyPHuZDKlpa64JMYXmq-_8GgKtQFhLX3c (2026-04-25) makes tamed
    // wolves permanent. Stored wolves can return in the main world or an active owner D1 Bogatyr run.
    // Existing main-world pets now transfer through reserved/removing source receipts before delivery.
    // Q&A D27's zero-permanent duration and owner-only armor drop on expiry are implemented.
    // D2+ scapula recruitment/totem auras remain deferred; bounded source-ranked threats are implemented.
    // Verified live-source review exists; damaged/missing/cloned saves and licensed failure tests still require explicit investigation.
}
