package net.goui.cosmicdungeon.playerclass.bogatyr;

import java.util.*;
import net.goui.cosmicdungeon.effect.ModMobEffects;
import net.goui.cosmicdungeon.network.BogatyrPayloads;
import net.goui.cosmicdungeon.transaction.PlayerSaveProof;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.pathfinder.*;
import net.minecraft.world.phys.Vec3;

/** Native combat/path/save boundaries plus opt-in work sampling; no gameplay server is launched locally. */
public final class BogatyrTacticsGameTests {
    private BogatyrTacticsGameTests(){}
    private static Mob mob(BogatyrCommandGameTests.Fixture f,EntityType<? extends Mob> type,int dx,int dz,double health){
        var mob=type.create(f.level,EntitySpawnReason.MOB_SUMMONED);f.check(mob!=null,"Native tactics mob factory");
        mob.setNoAi(true);mob.snapTo(f.origin.x+dx,f.origin.y,f.origin.z+dz,0,0);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);mob.setHealth((float)health);
        f.check(f.level.addFreshEntity(mob),"Native tactics mob accepted");f.extras.add(mob);return mob;
    }
    private static BogatyrThreats.ProtectOwner goal(Wolf wolf){
        return (BogatyrThreats.ProtectOwner)wolf.targetSelector.getAvailableGoals().stream()
                .map(net.minecraft.world.entity.ai.goal.WrappedGoal::getGoal).filter(g->g instanceof BogatyrThreats.ProtectOwner).findFirst().orElseThrow();
    }
    private static Mob decide(Wolf wolf,long now){
        var goal=goal(wolf);var target=goal.think(now);if(target!=null)goal.start();return target;
    }
    private static Path path(BogatyrCommandGameTests.Fixture f,int... offsets){
        var nodes=new ArrayList<Node>();for(int x:offsets)nodes.add(new Node((int)Math.floor(f.origin.x)+x,(int)f.origin.y,(int)Math.floor(f.origin.z)));
        return new Path(nodes,new BlockPos(nodes.getLast().x,nodes.getLast().y,nodes.getLast().z),true);
    }
    public static void strategic(GameTestHelper helper){
        try(var f=new BogatyrCommandGameTests.Fixture(helper,Long.MAX_VALUE-2501)){
            var wolf=f.wolf(0,0,-2,0);var zombie=mob(f,EntityType.ZOMBIE,1,1,200);
            var skeleton=mob(f,EntityType.SKELETON,2,0,20);var pillager=mob(f,EntityType.PILLAGER,3,0,40);
            var blaze=mob(f,EntityType.BLAZE,3,2,80);
            f.check(BogatyrModes.select(f.p(),f.run,WolfMode.STRATEGIC),"Strategic accepted");
            long now=f.level.getGameTime()+20;
            f.check(decide(wolf,now)==blaze,"Native ranged Blaze outranks larger melee and lower-health interface ranged mobs");
            blaze.setHealth(0);f.check(decide(wolf,now+20)==pillager,"Among ranged mobs descending max HP beats skeleton species priority");
            pillager.setHealth(0);f.check(decide(wolf,now+40)==skeleton,"Remaining ranged mob precedes high-health melee");
            skeleton.setHealth(0);f.check(decide(wolf,now+60)==zombie,"Melee target becomes eligible after ranged threats end");
            var data=BogatyrCompanionData.get(f.level.getServer());
            f.check(data.flushVerified(f.level.getServer()),"Advanced mode saved natively");
            var round=BogatyrCompanionData.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE,data.image()).getOrThrow();
            f.check(round.mode(f.p().getUUID(),f.run).mode()==WolfMode.STRATEGIC,"Appended mode preserves native save identity");
            helper.succeed();
        }
    }
    public static void rescue(GameTestHelper helper){
        try(var f=new BogatyrCommandGameTests.Fixture(helper,Long.MAX_VALUE-2502)){
            var wolf=f.wolf(0,0,-2,0);var patient=f.current.get(1);patient.setHealth(4);
            var attacker=mob(f,EntityType.ZOMBIE,2,0,40);var unrelated=mob(f,EntityType.SKELETON,3,2,80);
            attacker.setTarget(patient);unrelated.setTarget(f.p());
            f.check(BogatyrModes.select(f.p(),f.run,WolfMode.SEARCH_AND_RESCUE),"Search and Rescue accepted");
            long now=f.level.getGameTime()+20;
            f.check(decide(wolf,now)==attacker&&attacker.getTarget()==wolf,"Actual attacker of critical ally is selected and diverted");
            f.check(BogatyrThreats.patient(wolf)==patient,"Same-run critical player receives the guard");
            var between=BogatyrRescue.interpose(patient,attacker);
            f.check(between.distanceToSqr(patient.position())<attacker.distanceToSqr(patient)
                    &&between.distanceToSqr(attacker.position())<attacker.distanceToSqr(patient),"Escort chooses a point between attacker and patient");
            f.check(decide(wolf,now+240)==attacker,"A lone guard retains the real redirected attacker after damage memory expires");
            var escort=wolf.goalSelector.getAvailableGoals().stream().map(net.minecraft.world.entity.ai.goal.WrappedGoal::getGoal)
                    .filter(g->g instanceof BogatyrRescue.Escort).findFirst().orElseThrow();
            f.check(escort.canUse(),"Distant guard starts native interposition");escort.start();
            f.check(!wolf.getNavigation().isDone(),"Interposition installs a native loaded path");
            attacker.setHealth(0);var breeze=mob(f,EntityType.BREEZE,2,2,30);
            breeze.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET,patient);
            f.check(decide(wolf,now+280)==breeze&&breeze.getTarget()==wolf,"Native brain target is diverted for Breeze");
            patient.setHealth(7);
            f.check(BogatyrThreats.patient(wolf)==null&&decide(wolf,now+300)==null,"Players above three hearts are no longer rescue targets");
            patient.setHealth(4);patient.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            f.check(BogatyrRescue.patients(f.p(),f.run).isEmpty(),"Spectators cannot receive rescue protection");
            helper.succeed();
        }
    }
    public static void companionship(GameTestHelper helper){
        try(var f=new BogatyrCommandGameTests.Fixture(helper,Long.MAX_VALUE-2503)){
            var wolves=new ArrayList<Wolf>();for(int i=0;i<12;i++)wolves.add(f.wolf(i%2,0,(i%3)-1,1));
            f.p().setHealth(4);
            for(var owner:f.current)f.check(BogatyrModes.select(owner,f.run,WolfMode.SEARCH_AND_RESCUE),"Both pack owners select rescue");
            long base=f.level.getServer().overworld().getGameTime()+20;
            for(var wolf:wolves){decide(wolf,base);f.check(BogatyrThreats.patient(wolf)==f.p(),"Both owners protect the same critical player");}
            for(long now=base;now<base+100;now++){
                for(var wolf:wolves)BogatyrRescue.guard(wolf,f.p(),now);BogatyrRescue.pulse(f.level.getServer(),now);
            }
            f.check(f.p().getHealth()==4&&f.p().hasEffect(ModMobEffects.COMPANIONSHIP),"No early heal; native Companionship indicator is present");
            for(var wolf:wolves)BogatyrRescue.guard(wolf,f.p(),base+100);
            BogatyrRescue.pulse(f.level.getServer(),base+100);
            f.check(f.p().getHealth()==6,"Twelve wolves across two owners heal exactly one heart total");
            for(long now=base+101;now<base+200;now++){
                for(var wolf:wolves)BogatyrRescue.guard(wolf,f.p(),now);BogatyrRescue.pulse(f.level.getServer(),now);
            }
            // All guards can switch after marking the current tick; the pulse must recheck live sources.
            for(var wolf:wolves)BogatyrRescue.guard(wolf,f.p(),base+200);
            for(var owner:f.current)BogatyrModes.select(owner,f.run,WolfMode.STAND_GROUND);
            BogatyrRescue.pulse(f.level.getServer(),base+200);
            f.check(f.p().getHealth()==6&&!f.p().hasEffect(ModMobEffects.COMPANIONSHIP),"No stale heal after every guard leaves rescue at the due tick");
            f.check(PlayerSaveProof.save(f.p()),"Rescue recipient save is native");f.reloadOwner();
            BogatyrRescue.pulse(f.level.getServer(),base+500);
            f.check(f.p().getHealth()==6,"Reconnect/offline time never catches up healing");
            helper.succeed();
        }
    }
    public static void dangerClose(GameTestHelper helper){
        try(var f=new BogatyrCommandGameTests.Fixture(helper,Long.MAX_VALUE-2504)){
            var wolf=f.wolf(0,0,-2,0);var near=mob(f,EntityType.ZOMBIE,1,1,40);
            var threat=mob(f,EntityType.SKELETON,3,2,40);var outer=mob(f,EntityType.ZOMBIE,6,0,40);
            f.check(BogatyrModes.select(f.p(),f.run,WolfMode.DANGER_CLOSE),"Danger Close accepted");
            long now=f.level.getGameTime()+20;
            f.check(decide(wolf,now)==near,"Danger Close initially chooses the nearest in-bound hostile");
            threat.setTarget(f.p());
            f.check(decide(wolf,now+20)==near,"Current valid kill is retained despite a new master threat");
            near.setHealth(0);f.check(decide(wolf,now+40)==threat,"After the kill, current master threats take priority");
            threat.setHealth(0);f.p().setPos(f.origin.x-6,f.origin.y,f.origin.z);
            f.check(f.level.getEntity(outer.getUUID())==outer,"Boundary fixture target is tracked in the known loaded chunk");
            f.check(decide(wolf,now+60)==outer,"The full sixteen-block boundary includes hostiles beyond default follow range");
            f.p().setPos(f.origin);
            f.check(!wolf.getNavigation().moveTo(path(f,-2,17,2),1),"A detour outside the boundary is rejected even with an inside endpoint");
            f.check(wolf.getNavigation().moveTo(path(f,-2,-1,0,1,2),1),"An entirely inside native path is accepted");
            f.p().setPos(f.origin.x-20,f.origin.y,f.origin.z);BogatyrBoundary.check(wolf);
            f.check(wolf.getTarget()==null&&wolf.getNavigation().isDone(),"Moving master immediately drops out-of-bound combat and obsolete path");
            f.check(!BogatyrBoundary.mayAttack(wolf,outer),"An out-of-bound wolf cannot land a stale attack");
            f.check(BogatyrBoundary.allowPath(wolf,path(f,-2,-3,-4)),"A bounded inward partial return path is allowed");
            f.check(!BogatyrBoundary.allowPath(wolf,path(f,-2,-1,0)),"An outward partial path remains forbidden");
            f.p().setPos(f.origin);
            var nativePath=path(f,-2,17,2);
            try{
                var field=net.minecraft.world.entity.ai.navigation.PathNavigation.class.getDeclaredField("path");field.setAccessible(true);field.set(wolf.getNavigation(),nativePath);
                var stamp=net.minecraft.world.entity.ai.navigation.PathNavigation.class.getDeclaredField("timeLastRecompute");stamp.setAccessible(true);stamp.setLong(wolf.getNavigation(),f.level.getGameTime());
                wolf.getNavigation().recomputePath();
                f.check(wolf.getNavigation().isDone(),"Recompute return hook rejects an installed out-of-bound route");
                var length=net.minecraft.world.entity.ai.navigation.PathNavigation.class.getDeclaredMethod("getMaxPathLength");length.setAccessible(true);
                f.check((float)length.invoke(wolf.getNavigation())>=32,"Danger Close path search covers the diameter without unbounded owner-distance growth");
            }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            helper.succeed();
        }
    }
    public static void workScaling(GameTestHelper helper){
        try(var f=new BogatyrCommandGameTests.Fixture(helper,Long.MAX_VALUE-2505)){
            var wolves=new ArrayList<Wolf>();var enemy=mob(f,EntityType.ZOMBIE,3,2,200);
            var patient=f.current.get(1);patient.setHealth(4);enemy.setTarget(patient);
            long clock=f.level.getGameTime()+1000;int scenario=0;
            for(int size:new int[]{1,30,120}){
                while(wolves.size()<size)wolves.add(f.wolf(0,0,-2,0));
                for(var mode:List.of(WolfMode.AGGRESSIVE,WolfMode.STRATEGIC,WolfMode.SEARCH_AND_RESCUE,WolfMode.DANGER_CLOSE)){
                    f.check(BogatyrModes.select(f.p(),f.run,mode),"Cost sample mode selected");enemy.setTarget(patient);
                    long begin=clock+(scenario++)*1000;
                    for(var wolf:wolves)goal(wolf).resetAt(begin);
                    var probe=BogatyrWork.begin();long started=System.nanoTime();int chosen=0;
                    try{
                        for(int tick=0;tick<400;tick++){
                            if(tick%20==0)enemy.setPos(f.origin.x+3,f.origin.y,f.origin.z+(tick%40==0?2:1));
                            for(var wolf:wolves){
                                BogatyrModes.apply(wolf,false);
                                var selected=goal(wolf).think(begin+tick);
                                if(selected!=null){
                                    chosen++;goal(wolf).start();
                                    if(Math.floorMod(tick+wolf.getUUID().hashCode(),20)==0)wolf.getNavigation().moveTo(selected,1);
                                }
                            }
                        }
                    }finally{BogatyrWork.end();}
                    double millis=(System.nanoTime()-started)/1_000_000.0;
                    f.check(chosen>=size*300,"Large friendly pack cannot starve actual target selection: "+mode+"/"+size);
                    f.check(probe.scans<=21&&probe.visited<=21L*net.goui.cosmicdungeon.Config.WOLF_THREAT_CANDIDATES.get(),"Queries remain per owner and accepted candidates bounded: "+mode+"/"+size);
                    f.check(probe.decisions<=size*21L,"Slow decisions remain roughly once per second: "+mode+"/"+size);
                    com.mojang.logging.LogUtils.getLogger().info("WOLF_WORK mode={} wolves={} ticks=400 mean_ms={} scans={} inspected={} accepted={} decisions={} paths={} nodes={} guards={}",
                            mode,size,String.format(java.util.Locale.ROOT,"%.4f",millis/400),probe.scans,probe.inspected,probe.visited,
                            probe.decisions,probe.paths,probe.nodes,probe.guards);
                    BogatyrThreats.invalidate(f.p().getUUID());
                }
            }
            f.check(wolves.size()==120,"Measured large pack is not silently capped");
            helper.succeed();
        }finally{BogatyrWork.end();}
    }
}
