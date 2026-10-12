package net.goui.cosmicdungeon.playerclass.bogatyr;

import java.util.*;
import java.util.function.LongSupplier;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * CI-only native workload sampling. Calls production mode/feed code on real enrolled wolves;
 * does not advance world time, simulate full server ticks, or claim live TPS.
 */
public final class BogatyrIntegrationGameTests {
    private static final int SWEEPS=100, SAMPLES=3;
    private static volatile long sink;
    private BogatyrIntegrationGameTests(){}

    private static BogatyrModes.StandGround stay(Wolf wolf){
        return (BogatyrModes.StandGround)wolf.goalSelector.getAvailableGoals().stream()
                .map(WrappedGoal::getGoal).filter(g->g instanceof BogatyrModes.StandGround)
                .findFirst().orElseThrow();
    }
    private static long measure(LongSupplier work){
        long start=System.nanoTime();sink=work.getAsLong();return System.nanoTime()-start;
    }
    private static long median(long[] values){
        var sorted=values.clone();Arrays.sort(sorted);return sorted[sorted.length/2];
    }
    private static long poll(List<OwnerHurtByTargetGoal> goals){
        long result=0;for(var goal:goals)if(goal.canUse())result++;return result;
    }
    private static long nativeWork(List<OwnerHurtByTargetGoal> goals,int sweeps){
        long result=0;for(int tick=0;tick<sweeps;tick++)result+=poll(goals);return result;
    }
    private static long patrolWork(List<Wolf> wolves,List<OwnerHurtByTargetGoal> goals,int sweeps){
        long result=0;for(int tick=0;tick<sweeps;tick++){
            for(var wolf:wolves)BogatyrModes.apply(wolf,false);
            result+=poll(goals);
        }return result;
    }
    private static long stayWork(List<Wolf> wolves,List<BogatyrModes.StandGround> goals,int sweeps){
        long result=0;for(int tick=0;tick<sweeps;tick++)for(int i=0;i<wolves.size();i++){
            var wolf=wolves.get(i);BogatyrModes.apply(wolf,false);goals.get(i).tick();
            if(wolf.isOrderedToSit())result++;
        }return result;
    }
    /** Equivalent pre-optimization settled-Stay cleanup, confined to the native fixture. */
    private static void legacyForget(Wolf wolf){
        wolf.setTarget(null);wolf.stopBeingAngry();wolf.setLastHurtByMob(null);wolf.setLastHurtMob(null);
        var probe=BogatyrWork.probe;if(probe!=null)probe.targetTraversals++;
        for(var goal:wolf.targetSelector.getAvailableGoals()){
            if(probe!=null)probe.targetGoals++;
            if(goal.isRunning())goal.stop();
            if(goal.getGoal() instanceof BogatyrTargetMemory memory){
                memory.cosmicdungeon$forgetOwnerThreat();
                if(probe!=null)probe.memoryResets++;
            }
        }
    }
    private static long legacyStayWork(List<Wolf> wolves,List<BogatyrModes.StandGround> goals,int sweeps){
        long result=0;for(int tick=0;tick<sweeps;tick++)for(int i=0;i<wolves.size();i++){
            var wolf=wolves.get(i);legacyForget(wolf);BogatyrModes.apply(wolf,false);goals.get(i).tick();
            if(wolf.isOrderedToSit())result++;
        }return result;
    }
    private static long sampleStay(BogatyrCommandGameTests.Fixture f,List<Wolf> wolves,
            List<BogatyrModes.StandGround> goals,boolean legacy,BogatyrWork.Probe totals){
        var probe=BogatyrWork.begin();
        try{
            long elapsed=measure(()->legacy?legacyStayWork(wolves,goals,SWEEPS):stayWork(wolves,goals,SWEEPS));
            quiet(f,probe,legacy?"Legacy settled Stay":"Optimized settled Stay");
            totals.targetTraversals+=probe.targetTraversals;totals.targetGoals+=probe.targetGoals;
            totals.memoryResets+=probe.memoryResets;return elapsed;
        }finally{BogatyrWork.end();}
    }
    private static void quiet(BogatyrCommandGameTests.Fixture f,BogatyrWork.Probe p,String phase){
        f.check(p.scans==0&&p.decisions==0&&p.paths==0&&p.nodes==0,
                phase+" has no retired custom target search or path work");
    }

    public static void packWorkloads(GameTestHelper helper){
        try(var f=new BogatyrCommandGameTests.Fixture(helper,Long.MAX_VALUE-3317)){
            var wolves=new ArrayList<Wolf>();
            for(int size:new int[]{1,30,120}){
                while(wolves.size()<size){
                    int i=wolves.size();wolves.add(f.wolf(0,0,i%5-2,(i/5)%5-2));
                }
                f.check(f.pack().size()==size,"Actual loaded owner pack retains all "+size+" wolves");
                f.check(BogatyrModes.select(f.p(),f.run,WolfMode.PATROL),"Patrol workload is authorized");
                var nativeGoals=wolves.stream().map(w->(OwnerHurtByTargetGoal)w.targetSelector
                        .getAvailableGoals().stream().map(WrappedGoal::getGoal)
                        .filter(g->g instanceof OwnerHurtByTargetGoal).findFirst().orElseThrow()).toList();
                var stayGoals=wolves.stream().map(BogatyrIntegrationGameTests::stay).toList();
                f.check(wolves.stream().allMatch(w->w.targetSelector.getAvailableGoals().stream()
                        .noneMatch(g->g.getGoal() instanceof BogatyrThreats.ProtectOwner)),
                        "Retired proactive scan is absent for every measured wolf");
                f.check(nativeWork(nativeGoals,1)==0,"Quiet native-defense baseline has no remembered threat");
                nativeWork(nativeGoals,20);patrolWork(wolves,nativeGoals,20);
                long[] baseline=new long[SAMPLES],patrol=new long[SAMPLES],settled=new long[SAMPLES],legacyStay=new long[SAMPLES];
                var probe=BogatyrWork.begin();
                try{
                    // Alternate order to reduce the bias of always sampling a colder baseline first.
                    for(int sample=0;sample<SAMPLES;sample++){
                        if(sample%2==0){
                            baseline[sample]=measure(()->nativeWork(nativeGoals,SWEEPS));
                            patrol[sample]=measure(()->patrolWork(wolves,nativeGoals,SWEEPS));
                        }else{
                            patrol[sample]=measure(()->patrolWork(wolves,nativeGoals,SWEEPS));
                            baseline[sample]=measure(()->nativeWork(nativeGoals,SWEEPS));
                        }
                    }
                    quiet(f,probe,"Patrol/"+size);
                    f.check(probe.targetTraversals==0&&probe.targetGoals==0&&probe.memoryResets==0,
                            "Unchanged Patrol never enumerates target selectors or resets goal memory");
                }finally{BogatyrWork.end();}
                f.check(wolves.stream().allMatch(w->!w.isOrderedToSit()&&w.getTarget()==null),
                        "Quiet Patrol preserves native standing state");
                f.check(BogatyrModes.select(f.p(),f.run,WolfMode.STAY),"Stay workload is authorized");
                for(var goal:stayGoals)goal.start();
                var anchors=wolves.stream().map(BogatyrModes::anchor).toList();
                f.check(anchors.stream().allMatch(Objects::nonNull),"Every real wolf settles before stationary sample");
                stayWork(wolves,stayGoals,20);legacyStayWork(wolves,stayGoals,20);
                long nativeGoalCount=wolves.stream().mapToLong(w->w.targetSelector.getAvailableGoals().size()).sum();
                long nativeMemoryCount=wolves.stream().mapToLong(w->w.targetSelector.getAvailableGoals().stream()
                        .filter(g->g.getGoal() instanceof BogatyrTargetMemory).count()).sum();
                var before=new BogatyrWork.Probe();var after=new BogatyrWork.Probe();
                // Alternate measured order on the same real pack; setup and warmup are untimed.
                for(int sample=0;sample<SAMPLES;sample++){
                    if(sample%2==0){
                        legacyStay[sample]=sampleStay(f,wolves,stayGoals,true,before);
                        settled[sample]=sampleStay(f,wolves,stayGoals,false,after);
                    }else{
                        settled[sample]=sampleStay(f,wolves,stayGoals,false,after);
                        legacyStay[sample]=sampleStay(f,wolves,stayGoals,true,before);
                    }
                }
                f.check(before.targetTraversals==(long)size*SWEEPS*SAMPLES
                        &&before.targetGoals==nativeGoalCount*SWEEPS*SAMPLES
                        &&before.memoryResets==nativeMemoryCount*SWEEPS*SAMPLES,
                        "Legacy baseline counts actual native selector traversal and every visited goal");
                f.check(after.targetTraversals==0&&after.targetGoals==0&&after.memoryResets==0,
                        "Unchanged settled Stay never enumerates target selectors or resets goal memory");
                com.mojang.logging.LogUtils.getLogger().info(
                        "WOLF_STAY_TRAVERSAL_COST wolves={} samples={} sweeps={} legacy_us={} optimized_us={} legacy_traversals={} optimized_traversals={} legacy_goals={} optimized_goals={} legacy_memory_resets={} optimized_memory_resets={} legacy_ns={} optimized_ns={}",
                        size,SAMPLES,SWEEPS,median(legacyStay)/1000.0/SWEEPS,median(settled)/1000.0/SWEEPS,
                        before.targetTraversals,after.targetTraversals,before.targetGoals,after.targetGoals,
                        before.memoryResets,after.memoryResets,Arrays.toString(legacyStay),Arrays.toString(settled));
                for(int i=0;i<size;i++)f.check(wolves.get(i).isOrderedToSit()
                        &&wolves.get(i).getNavigation().isDone()
                        &&anchors.get(i).equals(BogatyrModes.anchor(wolves.get(i))),
                        "Measured Stay preserves seating/anchor without navigation");

                // Same real inventory and enrolled pack: projections must never mutate either.
                f.p().getInventory().clearContent();
                f.p().getInventory().setItem(0,new ItemStack(Items.BONE,64));
                for(var wolf:wolves){wolf.setHealth(wolf.getMaxHealth());wolf.setAge(0);wolf.resetLove();}
                int expectedBreeding=size==1?0:WolfFeedRules.LIMIT;
                for(int warm=0;warm<5;warm++)BogatyrFeed.plan(f.p(),f.run,wolves);
                long[] feeding=new long[SAMPLES];
                for(int sample=0;sample<SAMPLES;sample++)feeding[sample]=measure(()->{
                    long inputs=0;for(int i=0;i<10;i++)inputs+=BogatyrFeed.plan(f.p(),f.run,wolves).inputs();
                    return inputs;
                });
                var planned=BogatyrFeed.plan(f.p(),f.run,wolves);
                f.check(planned.inputs()==expectedBreeding,"Dense actual pair planning respects ten-input limit at "+size);
                f.check(planned.meals().stream().noneMatch(BogatyrFeed.Meal::healing)
                        &&planned.meals().stream().flatMap(m->m.wolves().stream()).distinct().count()
                            ==planned.meals().size()*2L,
                        "Breeding plans contain distinct pairs, without reusing a wolf");
                f.check(f.p().getInventory().getItem(0).getCount()==64
                        &&wolves.stream().noneMatch(Wolf::isInLove),"Repeated projections never debit or start breeding");

                for(var wolf:wolves)wolf.setHealth(wolf.getMaxHealth()-1);
                var healing=BogatyrFeed.plan(f.p(),f.run,wolves);
                f.check(healing.inputs()==Math.min(size,WolfFeedRules.LIMIT)
                        &&healing.meals().stream().allMatch(BogatyrFeed.Meal::healing),
                        "Injured-first planning is bounded at every actual pack size");
                int spent=BogatyrFeed.execute(f.p(),f.run);
                f.check(spent==healing.inputs()&&f.p().getInventory().getItem(0).getCount()==64-spent,
                        "Native Feed Pack execution debits exactly its bounded successful heals");
                f.check(wolves.stream().filter(w->w.getHealth()==w.getMaxHealth()).count()==spent,
                        "One measured input heals each chosen one-point wound exactly once");

                com.mojang.logging.LogUtils.getLogger().info(
                        "WOLF_INTEGRATION_COST wolves={} samples={} sweeps={} native_defense_us={} patrol_plus_native_us={} settled_stay_us={} feed_plan_us={} heal_inputs={} baseline_ns={} patrol_ns={} stay_ns={} feed_10plans_ns={}",
                        size,SAMPLES,SWEEPS,median(baseline)/1000.0/SWEEPS,median(patrol)/1000.0/SWEEPS,
                        median(settled)/1000.0/SWEEPS,median(feeding)/1000.0/10,spent,
                        Arrays.toString(baseline),Arrays.toString(patrol),Arrays.toString(settled),Arrays.toString(feeding));
                for(var wolf:wolves)wolf.setHealth(wolf.getMaxHealth());
            }
            helper.succeed();
        }finally{BogatyrWork.end();}
    }
}
