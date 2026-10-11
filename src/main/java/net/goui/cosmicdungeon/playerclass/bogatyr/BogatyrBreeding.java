package net.goui.cosmicdungeon.playerclass.bogatyr;
import java.util.*;
import net.goui.cosmicdungeon.Config;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.wolf.Wolf;
/** Transient permission to mate in Stay. The durable original anchor is never overwritten. */
public final class BogatyrBreeding {
    private record Attempt(long deadline,long epoch){}
    private static final Map<Wolf,Attempt> ATTEMPTS=new WeakHashMap<>();
    private BogatyrBreeding(){}
    public static void clear(){ATTEMPTS.clear();}
    public static void begin(Wolf wolf){
        if(!BogatyrWolfEvents.managed(wolf)||!(wolf.level() instanceof net.minecraft.server.level.ServerLevel))return;
        BogatyrModes.apply(wolf,false);
        if(!BogatyrModes.standing(wolf)||!BogatyrModes.careAllowed(wolf)||!wolf.isInLove())return;
        ATTEMPTS.putIfAbsent(wolf,new Attempt(wolf.level().getGameTime()+Math.max(600,Config.WOLF_BREED_TICKS.get()+100),BogatyrModes.epoch(wolf)));
        wolf.goalSelector.getAvailableGoals().stream().filter(g->g.isRunning()&&g.getFlags().contains(Goal.Flag.MOVE)).forEach(WrappedGoal::stop);
        wolf.getNavigation().stop();wolf.setOrderedToSit(false);wolf.setInSittingPose(false);
    }
    public static boolean active(Wolf wolf){
        var value=ATTEMPTS.get(wolf);
        return value!=null&&wolf.isAlive()&&!wolf.isRemoved()&&wolf.isInLove()&&wolf.getAge()==0
                &&WolfCareRules.healthy(wolf.getHealth(),wolf.getMaxHealth())&&!wolf.isPanicking()
                &&wolf.level().getGameTime()<value.deadline()&&BogatyrModes.epoch(wolf)==value.epoch()
                &&BogatyrModes.standing(wolf)&&BogatyrModes.careAllowed(wolf);
    }
    static void tick(Wolf wolf){tick(wolf,wolf.level().getGameTime());}
    static void tick(Wolf wolf,long now){var a=ATTEMPTS.get(wolf);if(a!=null&&(now>=a.deadline()||!active(wolf)))cancel(wolf);}
    static void cancel(Wolf wolf){
        if(ATTEMPTS.remove(wolf)==null)return;
        wolf.resetLove();wolf.getNavigation().stop();
        wolf.goalSelector.getAvailableGoals().stream().filter(g->g.isRunning()&&g.getFlags().contains(Goal.Flag.MOVE)).forEach(WrappedGoal::stop);
        wolf.setTarget(null);wolf.setLastHurtByMob(null);wolf.setLastHurtMob(null);wolf.stopBeingAngry();
    }
    /** Reload cannot resume a half-observed breeding attempt; native health/cooldowns and the saved anchor remain. */
    static void loaded(Wolf wolf){
        ATTEMPTS.remove(wolf);
        if(BogatyrModes.standing(wolf)&&BogatyrModes.careAllowed(wolf))wolf.resetLove();
    }
    /** Bounded tests call the same expiry predicate as native ticking, without changing world time. */
    static long deadline(Wolf wolf){var a=ATTEMPTS.get(wolf);return a==null?0:a.deadline();}
}
