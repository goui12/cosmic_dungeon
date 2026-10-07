package net.goui.cosmicdungeon.playerclass.bogatyr;

import java.util.*;
import net.goui.cosmicdungeon.dungeon.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.wolf.Wolf;

/** Run tombstones precede cleanup; native active-run entities remain the only source of live wolves. */
public final class BogatyrRunLifecycle {
    private static final Set<MinecraftServer> MIGRATED=Collections.newSetFromMap(new WeakHashMap<>());
    private BogatyrRunLifecycle(){}
    static boolean active(MinecraftServer server,long id,UUID owner){
        var data=BogatyrCompanionData.get(server);
        if(id<=0||data.runRetired(id)||owner==null)return false;
        return DungeonRunRegistryData.get(server).getRun(id).filter(r->r.containsPlayer(owner)&&!r.isCompletionExited(owner)
                &&(r.stateEnum()==DungeonRunState.ACTIVE||r.stateEnum()==DungeonRunState.PAUSED)).isPresent();
    }
    /** Once per server lifetime, retire old ended archives without loading a single entity chunk. */
    public static boolean migrate(MinecraftServer server){
        if(MIGRATED.contains(server))return true;
        var data=BogatyrCompanionData.get(server);boolean changed=false;
        for(var entry:data.entries()){
            var archive=data.archive(entry.wolf()).orElse(null);
            long effective=archive!=null&&archive.targetRun()>0?archive.targetRun():entry.run();
            if(!active(server,effective,entry.owner())||archive!=null&&archive.phase().equals(WolfArchive.STORED)){
                data.retireRecord(entry);changed=true;
            }
        }
        if(changed&&!data.flushVerified(server))return false;
        MIGRATED.add(server);return true;
    }
    /** Runs before identity observation/enrollment, including a wolf loaded after its run was erased. */
    public static boolean admit(Wolf wolf){
        if(!BogatyrWolfEvents.managed(wolf)||!(wolf.level() instanceof ServerLevel level))return true;
        var server=level.getServer();long run=wolf.getPersistentData().getLongOr(BogatyrWolfEvents.RUN,0);
        var owner=BogatyrCompanions.owner(wolf);
        var record=DungeonRunRegistryData.get(server).getRun(run).orElse(null);
        if(!active(server,run,owner)||record==null||!record.containsDimension(level.dimension())){
            var data=BogatyrCompanionData.get(server);
            data.find(BogatyrIdentity.id(wolf)).filter(e->e.run()==run&&e.owner().equals(owner)
                    &&!activeDestination(server,data,e,run)).ifPresent(data::retireRecord);
            wolf.discard();return false;
        }
        return true;
    }
    /** Persist retirement before dropping directory entries or resetting a source dimension. */
    public static boolean retire(MinecraftServer server,DungeonRunRegistryData.RunRecord run){
        if(run==null)return true;
        var data=BogatyrCompanionData.get(server);data.markRunRetired(run.runId());
        if(!data.flushVerified(server))return false;
        for(var entry:data.entries()){
            var archive=data.archive(entry.wolf()).orElse(null);
            if(entry.run()!=run.runId()&&(archive==null||archive.targetRun()!=run.runId()))continue;
            if(activeDestination(server,data,entry,run.runId()))continue;
            // Retire the record and its archive first; late native loads are rejected by the tombstone.
            var addresses=new LinkedHashMap<String,UUID>();addresses.put(entry.dimension(),entry.entityUuid());
            if(archive!=null&&!archive.targetDimension().isEmpty())addresses.put(archive.targetDimension(),entry.wolf());
            data.retireRecord(entry);
            for(var address:addresses.entrySet()){
                var level=BogatyrRecovery.level(server,address.getKey());if(level==null)continue;
                if(level.getEntity(address.getValue()) instanceof Wolf wolf&&BogatyrWolfEvents.managed(wolf)
                        &&(wolf.getPersistentData().getLongOr(BogatyrWolfEvents.RUN,0)==run.runId()
                        ||archive!=null&&archive.transaction().toString().equals(wolf.getPersistentData().getStringOr(WolfArchive.MARKER,""))))
                    wolf.discard();
            }
        }
        return data.flushVerified(server);
    }
    private static boolean activeDestination(MinecraftServer server,BogatyrCompanionData data,
            BogatyrCompanionData.Companion entry,long retiringRun){
        var archive=data.archive(entry.wolf()).orElse(null);
        return archive!=null&&archive.targetRun()>0&&archive.targetRun()!=retiringRun
                &&active(server,archive.targetRun(),entry.owner());
    }
    public static Optional<String> resetBlocker(ServerLevel level){
        var server=level.getServer();if(!migrate(server))return Optional.of("Wolf retirement save needs review before reset.");
        var data=BogatyrCompanionData.get(server);String dimension=level.dimension().location().toString();
        for(var entry:data.inDimension(dimension))if(active(server,entry.run(),entry.owner()))
            return Optional.of("An active Wolfpack still belongs to this dungeon run.");
        for(var entry:data.pendingInDimension(dimension)){
            var archive=data.archive(entry.wolf()).orElseThrow();
            if(active(server,archive.targetRun(),entry.owner()))return Optional.of("An active Wolfpack still belongs to this dungeon run.");
        }
        return Optional.empty();
    }
    public static void clear(){MIGRATED.clear();}
}
