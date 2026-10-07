package net.goui.cosmicdungeon.playerclass.bogatyr;

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
        if(!BogatyrRunLifecycle.active(level.getServer(),wolf.getPersistentData().getLongOr(BogatyrWolfEvents.RUN,0),owner(wolf))||!BogatyrIdentity.observe(wolf))return;
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
        if(BogatyrWolfEvents.managed(wolf)&&(!BogatyrRunLifecycle.admit(wolf)||!BogatyrIdentity.observe(wolf)))return;
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
        if(event.getEntity() instanceof Wolf wolf&&BogatyrRunLifecycle.admit(wolf)){BogatyrRecovery.onObserved(wolf);BogatyrDuration.tick(wolf);track(wolf,false);}
    }
    public static void preserveRun(MinecraftServer server,DungeonRunRegistryData.RunRecord run){
        // Kept as a source-compatible no-op: run-only packs never manufacture directory entries.
    }
    public static boolean preserveBeforeCleanup(MinecraftServer server,DungeonRunRegistryData.RunRecord run){
        return BogatyrRunLifecycle.retire(server,run);
    }
    public static int packSize(MinecraftServer server,UUID owner,long runId){
        return (int)BogatyrCompanionData.get(server).forOwner(owner).stream().filter(e->e.run()==runId).count();
    }
    public static Optional<String> resetBlocker(ServerLevel level){return BogatyrRunLifecycle.resetBlocker(level);}
    public static void register(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher){
        // Legacy delivery/review mutation commands are retired; roster/inspection remains read-only.
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
    // Wolfpacks now end with their dungeon run; paid Regroup only visits living loaded pack members.
    private static int roster(net.minecraft.commands.CommandSourceStack source,int page)throws com.mojang.brigadier.exceptions.CommandSyntaxException{
        var player=source.getPlayerOrException();var run=net.goui.cosmicdungeon.playerclass.resource.ClassResourceService.activeRun(player).orElse(null);
        int count=run==null?0:BogatyrCommands.loaded(player,run.runId()).size();
        source.sendSuccess(()->Component.literal("Wolfpack: "+count+" living loaded wolves in this dungeon. Use Skills for paid care. Packs end with the run."),false);
        return count;
    }
}
