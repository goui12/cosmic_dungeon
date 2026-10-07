package net.goui.cosmicdungeon.playerclass.bogatyr;

import java.util.*;
import net.goui.cosmicdungeon.effect.ModMobEffects;
import net.goui.cosmicdungeon.playerclass.resource.ClassResourceService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Shared protected-player cadence: additional wolves or owners never multiply Companionship healing. */
@EventBusSubscriber(modid="cosmicdungeon")
public final class BogatyrRescue {
    private static final Map<ServerPlayer,Guard> GUARDS=new WeakHashMap<>();
    private static final class Guard {
        long run,since,seen;final Set<Wolf> sources=Collections.newSetFromMap(new WeakHashMap<>());
        Guard(long run,long now){this.run=run;since=seen=now;}
    }
    private BogatyrRescue(){}
    static void clear(){GUARDS.clear();}
    static void forget(ServerPlayer p){GUARDS.remove(p);p.removeEffect(ModMobEffects.COMPANIONSHIP);}
    static boolean eligible(ServerPlayer owner,ServerPlayer patient,long run){
        if(owner==null||patient==null||patient.level()!=owner.level()||!patient.isAlive()||patient.isSpectator()
                ||!WolfTacticsRules.critical(patient.getHealth())
                ||patient.level().getServer().getPlayerList().getPlayer(patient.getUUID())!=patient)return false;
        var active=ClassResourceService.activeRun(owner).orElse(null);
        return active!=null&&active.runId()==run&&active.containsPlayer(patient.getUUID())&&!active.isCompletionExited(patient.getUUID())
                &&ClassResourceService.activeRun(patient).filter(r->r.runId()==run).isPresent();
    }
    static List<ServerPlayer> patients(ServerPlayer owner,long run){
        var active=ClassResourceService.activeRun(owner).orElse(null);if(active==null||active.runId()!=run)return List.of();
        double radius=net.goui.cosmicdungeon.Config.WOLF_FOLLOW_RANGE.get();
        return active.orderedPlayers().stream().map(owner.level().getServer().getPlayerList()::getPlayer)
                .filter(p->eligible(owner,p,run)&&owner.distanceToSqr(p)<=radius*radius)
                .sorted(Comparator.comparingDouble(ServerPlayer::getHealth).thenComparingDouble(p->owner.distanceToSqr(p))
                        .thenComparing(p->p.getUUID().toString())).toList();
    }
    static void guard(Wolf wolf,ServerPlayer patient,long now){
        if(!BogatyrThreats.available(wolf)||BogatyrModes.mode(wolf)!=WolfMode.SEARCH_AND_RESCUE
                ||wolf.distanceToSqr(patient)>16||!eligible(BogatyrThreats.owner(wolf),patient,BogatyrThreats.run(wolf)))return;
        if(BogatyrWork.probe!=null)BogatyrWork.probe.guards++;
        long run=BogatyrThreats.run(wolf);var g=GUARDS.get(patient);
        if(g==null||g.run!=run||now<g.seen||now-g.seen>1){g=new Guard(run,now);GUARDS.put(patient,g);}
        g.seen=now;g.sources.add(wolf);
    }
    static void tickWolf(Wolf wolf){
        var patient=BogatyrThreats.patient(wolf);
        if(patient!=null)guard(wolf,patient,wolf.level().getServer().overworld().getGameTime());
    }
    private static boolean liveSource(Guard g,ServerPlayer p){
        var iterator=g.sources.iterator();
        while(iterator.hasNext()){
            var wolf=iterator.next();
            if(!wolf.isRemoved()&&wolf.isAddedToLevel()&&wolf.level()==p.level()&&BogatyrThreats.patient(wolf)==p
                    &&wolf.distanceToSqr(p)<=16)return true;
            iterator.remove();
        }
        return false;
    }
    static void pulse(MinecraftServer server,long now){
        var iterator=GUARDS.entrySet().iterator();
        while(iterator.hasNext()){
            var entry=iterator.next();var p=entry.getKey();var g=entry.getValue();
            if(server.getPlayerList().getPlayer(p.getUUID())!=p||!p.isAlive()||!WolfTacticsRules.critical(p.getHealth())
                    ||now!=g.seen||!liveSource(g,p)||ClassResourceService.activeRun(p).filter(r->r.runId()==g.run).isEmpty()){
                p.removeEffect(ModMobEffects.COMPANIONSHIP);iterator.remove();continue;
            }
            // The effect never heals by itself: refresh/amplifier/number of guards cannot accelerate the clock.
            var effect=p.getEffect(ModMobEffects.COMPANIONSHIP);
            if(effect==null||effect.getDuration()<20)p.addEffect(new MobEffectInstance(ModMobEffects.COMPANIONSHIP,40,0,true,false,true));
            if(WolfTacticsRules.healDue(now,g.since)){
                g.since=now;p.heal(2F);if(BogatyrWork.probe!=null)BogatyrWork.probe.heals++;
            }
        }
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){pulse(event.getServer(),event.getServer().overworld().getGameTime());}
    static Vec3 interpose(ServerPlayer patient,Mob attacker){
        if(attacker==null)return patient.position();
        var direction=attacker.position().subtract(patient.position());double length=direction.horizontalDistance();
        if(length<.01)return patient.position();
        double distance=Math.min(2,length*.5);
        return patient.position().add(direction.x/length*distance,0,direction.z/length*distance);
    }
    static final class Escort extends Goal {
        private final Wolf wolf;private long nextPath;
        Escort(Wolf wolf){this.wolf=wolf;setFlags(EnumSet.of(Flag.MOVE));}
        private Vec3 destination(){
            var patient=BogatyrThreats.patient(wolf);if(patient==null)return null;
            return interpose(patient,wolf.getTarget() instanceof Mob mob?mob:null);
        }
        @Override public boolean canUse(){
            var point=destination();return point!=null&&wolf.distanceToSqr(point)>2.25;
        }
        @Override public boolean canContinueToUse(){return canUse();}
        @Override public boolean requiresUpdateEveryTick(){return true;}
        @Override public void start(){nextPath=0;tick();}
        @Override public void tick(){
            var patient=BogatyrThreats.patient(wolf);if(patient==null){wolf.getNavigation().stop();return;}
            guard(wolf,patient,wolf.level().getServer().overworld().getGameTime());
            long now=wolf.level().getGameTime();if(now<nextPath)return;
            nextPath=now+20+Math.floorMod(wolf.getUUID().hashCode(),5);
            var point=destination();
            if(point!=null&&wolf.level().hasChunkAt(net.minecraft.core.BlockPos.containing(point)))
                wolf.getNavigation().moveTo(point.x,point.y,point.z,1);
        }
        @Override public void stop(){wolf.getNavigation().stop();}
    }
}
