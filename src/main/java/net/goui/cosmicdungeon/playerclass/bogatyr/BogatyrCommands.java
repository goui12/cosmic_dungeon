package net.goui.cosmicdungeon.playerclass.bogatyr;

import java.util.*;
import net.goui.cosmicdungeon.network.BogatyrPayloads.Kind;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.resource.*;
import net.goui.cosmicdungeon.transaction.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/** Effects and payment use a durable pending journal; uncertain saves never refund, replay or create a replacement. */
public final class BogatyrCommands {
    static final String RECEIPT="wolf_command_receipt";
    record Target(UUID entity,BlockPos destination){}
    record Plan(long run,Kind kind,List<Target> targets){
        Plan{targets=List.copyOf(targets);}
        int count(){return targets.size();}
        int cost(){return count()*WolfCommandRules.unitCost(kind);}
    }
    private BogatyrCommands(){}
    public static boolean blocked(ServerPlayer p){return BogatyrCommandData.get(p.level().getServer()).pending(p.getUUID())!=null;}
    public static boolean held(Wolf wolf){
        return wolf.level() instanceof ServerLevel level&&BogatyrCompanions.owner(wolf)!=null
                &&BogatyrCommandData.get(level.getServer()).pending(BogatyrCompanions.owner(wolf))!=null;
    }
    public static boolean readyForCleanup(MinecraftServer server,List<UUID> owners){
        return owners.stream().noneMatch(id->BogatyrCommandData.get(server).pending(id)!=null);
    }
    static CompoundTag root(ServerPlayer p){return p.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG);}
    static List<Wolf> loaded(ServerPlayer p,long run){
        var level=p.level();var result=new ArrayList<Wolf>();
        for(var entry:BogatyrCompanionData.get(level.getServer()).forOwner(p.getUUID())){
            if(entry.run()!=run||!entry.dimension().equals(level.dimension().location().toString()))continue;
            if(level.getEntity(entry.entityUuid()) instanceof Wolf wolf&&wolf.isAlive()&&!wolf.isRemoved()&&wolf.isAddedToLevel()
                    &&wolf.isTame()&&p.getUUID().equals(BogatyrCompanions.owner(wolf))&&BogatyrWolfEvents.managed(wolf)&&BogatyrIdentity.id(wolf).equals(entry.wolf())
                    &&wolf.getPersistentData().getLongOr(BogatyrWolfEvents.RUN,0)==run&&!BogatyrRecovery.held(wolf))result.add(wolf);
        }
        result.sort(Comparator.comparing(Wolf::getUUID));return result;
    }
    /** Collision checks visit loaded chunks only. Distinct cells prevent a regroup destination pile-up. */
    static BlockPos safe(ServerLevel level,BlockPos center,Wolf wolf,Set<BlockPos> used){
        for(int radius=1;radius<=6;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            if(Math.abs(dx)!=radius&&Math.abs(dz)!=radius)continue;
            for(int dy=-2;dy<=4;dy++){
                var pos=center.offset(dx,dy,dz);
                if(used.contains(pos)||!level.hasChunkAt(pos)||!net.goui.cosmicdungeon.rift.SafeTeleportUtil.isStandable(level,pos))continue;
                var box=wolf.getBoundingBox().move(pos.getX()+.5-wolf.getX(),pos.getY()-wolf.getY(),pos.getZ()+.5-wolf.getZ());
                if(level.noCollision(wolf,box))return pos;
            }
        }
        return null;
    }
    static Plan plan(ServerPlayer p,long run,Kind kind,List<Wolf> pack,int amount){
        if(kind==Kind.BREED&&BogatyrCompanionData.get(p.level().getServer()).mode(p.getUUID(),run).mode()==WolfMode.STAND_GROUND)return new Plan(run,kind,List.of());
        var eligible=new ArrayList<Wolf>();
        if(kind==Kind.REGROUP&&pack.stream().anyMatch(w->w.isPassenger()||w.isVehicle()||w.isLeashed()))
            return new Plan(run,kind,List.of());
        if(kind==Kind.SUMMON){
            if(amount<30)return new Plan(run,kind,List.of());
            var candidate=EntityType.WOLF.create(p.level(),EntitySpawnReason.MOB_SUMMONED);
            if(candidate==null)return new Plan(run,kind,List.of());
            var pos=safe(p.level(),p.blockPosition(),candidate,Set.of());
            return new Plan(run,kind,pos==null?List.of():List.of(new Target(new UUID(0,0),pos)));
        }
        for(var wolf:pack){
            if(kind==Kind.BREED&&WolfCommandRules.breed(wolf.getAge(),wolf.isInLove(),wolf.getHealth(),wolf.getMaxHealth())
                    ||kind==Kind.HEAL&&WolfCommandRules.injured(wolf.getHealth(),wolf.getMaxHealth())
                    ||kind==Kind.REGROUP&&!wolf.isPassenger()&&!wolf.isVehicle()&&!wolf.isLeashed())eligible.add(wolf);
        }
        if(kind==Kind.HEAL)eligible.sort(Comparator.comparingDouble(Wolf::getHealth).thenComparing(Wolf::getUUID));
        int count=kind==Kind.REGROUP?eligible.size():WolfCommandRules.count(kind,eligible.size(),amount);
        var targets=new ArrayList<Target>();var used=new HashSet<BlockPos>();
        for(var wolf:eligible.subList(0,count)){
            BlockPos pos=null;
            if(kind==Kind.REGROUP){
                pos=safe(p.level(),p.blockPosition(),wolf,used);if(pos==null)return new Plan(run,kind,List.of());used.add(pos);
            }
            if(kind!=Kind.REGROUP||!wolf.position().equals(new Vec3(pos.getX()+.5,pos.getY(),pos.getZ()+.5)))targets.add(new Target(wolf.getUUID(),pos));
        }
        return new Plan(run,kind,kind==Kind.REGROUP&&targets.size()>amount?List.of():targets);
    }
    /** Legacy command journals still reconcile, but new Kibble effects are retired. */
    static boolean execute(ServerPlayer p,Plan plan){return false;}
    /** Only an acknowledged entity-save outcome can be settled after reconnect; prepared ambiguity stays held. */
    public static boolean reconcile(ServerPlayer p){
        if(p.level().getServer().getPlayerList().getPlayer(p.getUUID())!=p)return false;
        var data=BogatyrCommandData.get(p.level().getServer());var pending=data.pending(p.getUUID());if(pending==null)return true;
        if(!pending.getStringOr("phase","").equals("entities_saved"))return false;
        try{
            String id=pending.getStringOr("id","");int cost=pending.getIntOr("cost",-1);
            if(!root(p).getStringOr(RECEIPT,"").equals(id)){
                var ledger=ClassResourceLedger.forRun(root(p),pending.getLongOr("run",0));
                if(!ledger.image().equals(pending.getCompoundOrEmpty("ledger"))||ledger.amount(ClassResourceKind.KIBBLE)<cost)return false;
                var next=ledger.withAmount(ClassResourceKind.KIBBLE,ledger.amount(ClassResourceKind.KIBBLE)-cost).nextRevision().applyTo(root(p));
                next.putString(RECEIPT,id);p.getPersistentData().put(ClassData.ROOT_TAG,next);
                if(!PlayerSaveProof.save(p))throw new IllegalStateException("Wolf command debit save needs review");
            }else if(!PlayerSaveProof.save(p))throw new IllegalStateException("Wolf command receipt save needs review");
            data.remove(p.getUUID());
            if(!data.flushVerified()){data.put(p.getUUID(),pending);throw new IllegalStateException("Wolf command completion save needs review");}
            ClassResourceService.pulse(p,p.level().getServer().getTickCount(),false);return true;
        }catch(Exception failure){ClassResourceService.hold(p,failure);return false;}
    }
}
