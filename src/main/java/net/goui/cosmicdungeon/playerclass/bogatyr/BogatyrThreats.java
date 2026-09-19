package net.goui.cosmicdungeon.playerclass.bogatyr;

import net.goui.cosmicdungeon.Config;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.util.*;

@EventBusSubscriber(modid="cosmicdungeon")
public final class BogatyrThreats {
    private record Targets(String dimension,long tick,List<UUID> ids){}
    private static final Set<Wolf> INSTALLED=Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<UUID,Targets> CACHE=new HashMap<>();
    private static long budgetTick=Long.MIN_VALUE;
    private static int scans;
    private BogatyrThreats(){}
    public static void clear(){INSTALLED.clear();CACHE.clear();budgetTick=Long.MIN_VALUE;scans=0;}
    public static void install(Wolf wolf){
        if(wolf.level() instanceof ServerLevel&&INSTALLED.add(wolf))wolf.targetSelector.addGoal(0,new ProtectOwner(wolf));
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){CACHE.remove(event.getEntity().getUUID());BogatyrRecovery.clearPlayer(event.getEntity().getUUID());}
    private static ServerPlayer owner(Wolf wolf){
        return wolf.getOwner() instanceof ServerPlayer player&&player.level()==wolf.level()&&player.isAlive()
                &&!player.isSpectator()?player:null;
    }
    private static boolean available(Wolf wolf){
        return BogatyrWolfEvents.managed(wolf)&&wolf.isTame()&&wolf.isAlive()
                &&!wolf.isOrderedToSit()&&!wolf.isInSittingPose()&&!BogatyrRecovery.held(wolf);
    }
    private static boolean eligible(Wolf wolf,ServerPlayer owner,Mob target){
        double radius=Config.WOLF_FOLLOW_RANGE.get();
        return target.isAlive()&&target.getTarget()==owner&&target.level()==owner.level()
                &&owner.distanceToSqr(target)<=radius*radius
                &&wolf.canAttack(target)&&wolf.wantsToAttack(target,owner)
                &&!wolf.isAlliedTo(target)&&!owner.isAlliedTo(target);
    }
    private static List<UUID> candidates(ServerPlayer owner){
        var level=owner.level();long now=level.getServer().overworld().getGameTime();
        var cached=CACHE.get(owner.getUUID());
        String dimension=level.dimension().location().toString();
        if(cached!=null&&cached.dimension().equals(dimension)
                &&!WolfBehaviourRules.refresh(now,cached.tick(),Config.WOLF_THREAT_POLL_TICKS.get()))return cached.ids();
        if(budgetTick!=now){budgetTick=now;scans=0;}
        if(scans>=Config.WOLF_THREAT_SCANS.get())return cached!=null&&cached.dimension().equals(dimension)?cached.ids():List.of();
        scans++;
        var found=new ArrayList<Mob>();int[] visited={0};
        double radius=Config.WOLF_FOLLOW_RANGE.get();
        level.getEntities().get(EntityTypeTest.forClass(Mob.class),owner.getBoundingBox().inflate(radius),mob->{
            visited[0]++;
            if(mob.isAlive()&&mob.getTarget()==owner&&owner.distanceToSqr(mob)<=radius*radius)found.add(mob);
            return visited[0]>=Config.WOLF_THREAT_CANDIDATES.get()
                    ?AbortableIterationConsumer.Continuation.ABORT:AbortableIterationConsumer.Continuation.CONTINUE;
        });
        found.sort(Comparator.comparingInt((Mob mob)->WolfBehaviourRules.threatRank(mob instanceof AbstractSkeleton,mob instanceof RangedAttackMob))
                .thenComparingDouble(owner::distanceToSqr).thenComparing(mob->mob.getUUID().toString()));
        var ids=found.stream().map(Mob::getUUID).toList();
        CACHE.put(owner.getUUID(),new Targets(dimension,now,ids));return ids;
    }
    /** Sitting and archive holds are passive even if a vanilla target goal proposes an enemy. */
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void changingTarget(LivingChangeTargetEvent event){
        if(!(event.getEntity() instanceof Wolf wolf)||!(wolf.level() instanceof ServerLevel)
                ||!BogatyrWolfEvents.managed(wolf))return;
        var requested=event.getNewAboutToBeSetTarget();
        if(!available(wolf)){event.setNewAboutToBeSetTarget(null);return;}
        var owner=owner(wolf);
        if(requested instanceof AbstractSkeleton&&wolf.getLastHurtByMob()!=requested
                &&(owner==null||(((Mob)requested).getTarget()!=owner
                &&owner.getLastHurtByMob()!=requested&&owner.getLastHurtMob()!=requested))){
            event.setNewAboutToBeSetTarget(null);
        }
    }
    private static final class ProtectOwner extends Goal {
        private final Wolf wolf;
        private Mob choice, applied;
        private long lastChoice=Long.MIN_VALUE;
        ProtectOwner(Wolf wolf){this.wolf=wolf;setFlags(EnumSet.of(Flag.TARGET));}
        private Mob choose(){
            if(!available(wolf))return null;
            var owner=owner(wolf);if(owner==null)return null;
            long now=owner.level().getServer().overworld().getGameTime();
            if(!WolfBehaviourRules.refresh(now,lastChoice,Config.WOLF_THREAT_POLL_TICKS.get()))
                return choice!=null&&eligible(wolf,owner,choice)?choice:null;
            lastChoice=now;choice=null;
            for(UUID id:candidates(owner)){
                var entity=owner.level().getEntity(id);
                if(entity instanceof Mob mob&&eligible(wolf,owner,mob)&&wolf.getSensing().hasLineOfSight(mob)){
                    choice=mob;break;
                }
            }
            return choice;
        }
        @Override public boolean canUse(){return choose()!=null;}
        @Override public boolean canContinueToUse(){return choose()!=null;}
        @Override public void start(){applied=choice;wolf.setTarget(applied);}
        @Override public void tick(){var next=choose();if(next!=null&&wolf.getTarget()!=next){applied=next;wolf.setTarget(next);}}
        @Override public void stop(){if(wolf.getTarget()==applied)wolf.setTarget(null);choice=null;applied=null;}
    }
}
