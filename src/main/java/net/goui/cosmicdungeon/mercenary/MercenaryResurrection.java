package net.goui.cosmicdungeon.mercenary;

import java.util.UUID;
import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.network.PartyPayloads;
import net.goui.cosmicdungeon.transaction.InventoryTransactionGuard;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Server-thread resurrection; no living-hirer combat shortcut and no copied inventory or replacement mercenary. */
@EventBusSubscriber(modid=CosmicDungeonMod.MOD_ID)
public final class MercenaryResurrection {
    private static final String PROTECTION="cosmicdungeon_resurrection_protected_until";
    private static final String REQUEST="cosmicdungeon_resurrection_request_until";
    private record Respawn(ServerPlayer original,TeleportTransition transition){}
    // Exists only within a synchronous native respawn call; cannot leak into later ordinary respawns.
    private static final ThreadLocal<Respawn> RESPAWN=new ThreadLocal<>();
    private MercenaryResurrection(){}
    private static long now(MinecraftServer server){return server.overworld().getGameTime();}
    static boolean member(ServerPlayer player,DungeonRunRegistryData.RunRecord run){
        if(player==null||run==null||!run.containsPlayer(player.getUUID())||run.isCompletionExited(player.getUUID())
                ||!run.containsDimension(player.level().dimension())||player.isSpectator()||AccessPolicy.isDeveloper(player))return false;
        var server=player.level().getServer();
        if(server.getPlayerList().getPlayer(player.getUUID())!=player||player.connection==null||player.hasDisconnected())return false;
        var recovery=PendingDungeonRecoveryData.get(server);var handoff=recovery.handoff(player.getUUID());
        return recovery.completed(player.getUUID())<run.runId()
                &&!(handoff!=null&&handoff.run()==run.runId()&&handoff.kind().equals("cleanup"));
    }
    public static DungeonRunRegistryData.RunRecord activeRun(ServerPlayer player){
        var server=player.level().getServer();
        return DungeonRunRegistryData.get(server).findRunForPlayer(player.getUUID())
                .filter(r->r.stateEnum()==DungeonRunState.ACTIVE&&r.dungeonId().equals("dungeon_1")
                        &&!D1RunData.get(server).sealed(r.runId())&&member(player,r)).orElse(null);
    }
    /**
     * Creates the canonical latest-death token before native drops. Repeated die calls before
     * respawn reuse it so a second invocation cannot replace the physical-drop provenance.
     */
    public static MercenaryResurrectionState.Death prepareDeath(ServerPlayer player){
        player.getPersistentData().remove(PROTECTION);
        if(!player.isDeadOrDying())return null;
        var run=activeRun(player);if(run==null)return null;
        var data=D1RunData.get(player.level().getServer());
        var current=MercenaryResurrectionState.death(data,run.runId(),player.getUUID());
        if(current!=null)return current;
        var death=new MercenaryResurrectionState.Death(UUID.randomUUID(),
                player.level().dimension().location().toString(),player.position(),player.getYRot(),player.getXRot());
        MercenaryResurrectionState.remember(data,run.runId(),player.getUUID(),death);
        return death;
    }
    /** Final-tail fallback for keep-inventory/spectator-native paths; normally pre-drop already prepared it. */
    public static void died(ServerPlayer player){ prepareDeath(player); }
    static MercenaryEntity available(ServerPlayer player,DungeonRunRegistryData.RunRecord run,MercenaryContract contract){
        if(!player.isDeadOrDying()||activeRun(player)!=run||!MercenarySkill.POSITIVE_POTIONS.supports(contract)
                ||MercenarySkill.level(MercenarySkills.successes(D1RunData.get(player.level().getServer()),run.runId(),
                        contract,MercenarySkill.POSITIVE_POTIONS))<MercenaryResurrectionState.UNLOCK_LEVEL)return null;
        var server=player.level().getServer();
        var hirer=server.getPlayerList().getPlayer(contract.hirer());
        // A dead but connected hirer is intentionally admitted; combat AI retains its living-hirer gate.
        if(!member(hirer,run)||hirer.level()!=player.level())return null;
        if(!(player.level().getEntity(contract.id()) instanceof MercenaryEntity merc)||merc.runId()!=run.runId()
                ||!merc.isAlive()||merc.dormant()||!contract.equals(merc.contract())
                ||!MercenaryLifecycle.admitted(run,contract,merc.getUUID(),player.level().dimension().location().toString()))return null;
        var data=D1RunData.get(server);
        return MercenaryResurrectionState.readyAt(data,run.runId(),contract.id())<=now(server)?merc:null;
    }
    public static PartyPayloads.Resurrection snapshot(MinecraftServer server,DungeonRunRegistryData.RunRecord run,
                                                     MercenaryContract contract,UUID viewer){
        var data=D1RunData.get(server);
        if(!MercenarySkill.POSITIVE_POTIONS.supports(contract)
                ||MercenarySkill.level(MercenarySkills.successes(data,run.runId(),contract,MercenarySkill.POSITIVE_POTIONS))
                    <MercenaryResurrectionState.UNLOCK_LEVEL)return PartyPayloads.Resurrection.LOCKED;
        int seconds=MercenaryResurrectionState.seconds(MercenaryResurrectionState.readyAt(data,run.runId(),contract.id()),now(server));
        var player=server.getPlayerList().getPlayer(viewer);
        var death=player!=null&&player.isDeadOrDying()?MercenaryResurrectionState.death(data,run.runId(),viewer):null;
        boolean offer=death!=null&&death.dimension().equals(player.level().dimension().location().toString())
                &&available(player,run,contract)!=null;
        return new PartyPayloads.Resurrection(run.runId(),contract.id().toString(),offer?death.id().toString():"",seconds);
    }
    public static void accept(ServerPlayer player,PartyPayloads.Resurrect request){
        var server=player.level().getServer();long tick=now(server);
        long last=player.getPersistentData().getLongOr(REQUEST,0);
        if(last>tick&&last-tick<=20)return;
        player.getPersistentData().putLong(REQUEST,tick+10);
        var run=activeRun(player);
        if(run==null||run.runId()!=request.run()||!player.isDeadOrDying()||RESPAWN.get()!=null)return;
        var contract=run.mercenaries().stream().filter(c->c.id().equals(request.mercenary())).findFirst().orElse(null);
        if(contract==null||available(player,run,contract)==null)return;
        var data=D1RunData.get(server);
        var death=MercenaryResurrectionState.death(data,run.runId(),player.getUUID());
        if(death==null||!death.id().equals(request.death())||!death.dimension().equals(player.level().dimension().location().toString()))return;
        if(!InventoryTransactionGuard.beforeInventoryChange(player)){
            player.sendSystemMessage(Component.literal("Finish inventory recovery before accepting resurrection."));return;
        }
        if(!MercenaryResurrectionState.consume(data,run.runId(),player.getUUID(),request.death(),contract.id(),tick))return;
        if(!data.flushVerified()){
            player.sendSystemMessage(Component.literal("Resurrection could not be saved safely. Use normal respawn."));return;
        }
        var transition=new TeleportTransition(player.level(),death.position(),Vec3.ZERO,death.yaw(),death.pitch(),TeleportTransition.DO_NOTHING);
        RESPAWN.set(new Respawn(player,transition));
        try{
            var replacement=server.getPlayerList().respawn(player,false,Entity.RemovalReason.KILLED);
            replacement.connection.player=replacement;
            replacement.connection.resetPosition();
            if(replacement.isAlive()&&replacement.level()==transition.newLevel()){
                replacement.getPersistentData().putLong(PROTECTION,now(server)+MercenaryResurrectionState.PROTECTION_TICKS);
                replacement.sendSystemMessage(Component.literal(contract.name()+" resurrected you. Invulnerable for 5 seconds."));
            }
        }finally{RESPAWN.remove();}
    }
    /** HEAD override prevents bed/anchor consumption while retaining the original respawn configuration. */
    public static TeleportTransition destination(ServerPlayer player){
        var active=RESPAWN.get();return active!=null&&active.original()==player?active.transition():null;
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void cloned(PlayerEvent.Clone event){
        var active=RESPAWN.get();
        if(active!=null&&event.getOriginal()==active.original()&&event.getEntity() instanceof ServerPlayer player)
            player.getPersistentData().putLong(PROTECTION,now(player.level().getServer())+MercenaryResurrectionState.PROTECTION_TICKS);
    }
    @SubscribeEvent public static void respawned(PlayerEvent.PlayerRespawnEvent event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        var run=DungeonRunRegistryData.get(player.level().getServer()).findRunForPlayer(player.getUUID()).orElse(null);
        if(run!=null)MercenaryResurrectionState.clearDeath(D1RunData.get(player.level().getServer()),run.runId(),player.getUUID());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void protection(EntityInvulnerabilityCheckEvent event){
        if(event.getEntity() instanceof ServerPlayer player&&player.isAlive()
                &&MercenaryResurrectionState.protectedAt(player.getPersistentData().getLongOr(PROTECTION,0),now(player.level().getServer())))
            event.setInvulnerable(true);
    }
}
