package net.goui.cosmicdungeon.playerclass.bogatyr;

import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.playerclass.resource.ClassResourceService;
import net.minecraft.server.level.*;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.util.*;

@EventBusSubscriber(modid="cosmicdungeon")
public final class BogatyrThreats {
    private record Targets(String dimension,long run,WolfMode mode,long tick,List<UUID> ids){}
    private record Attacks(long run,Map<UUID,Long> until){}
    private static final Map<Wolf,java.lang.ref.WeakReference<ProtectOwner>> INSTALLED=new WeakHashMap<>();
    private static final Map<UUID,Targets> CACHE=new HashMap<>();
    private static final Map<UUID,Attacks> ATTACKS=new HashMap<>();
    private static long budgetTick=Long.MIN_VALUE;
    private static int scans;
    private BogatyrThreats(){}
    public static void clear(){INSTALLED.clear();CACHE.clear();ATTACKS.clear();BogatyrModes.clear();budgetTick=Long.MIN_VALUE;scans=0;}
    public static void install(Wolf wolf){
        if(wolf.level() instanceof ServerLevel&&!INSTALLED.containsKey(wolf)){
            var goal=new ProtectOwner(wolf);INSTALLED.put(wolf,new java.lang.ref.WeakReference<>(goal));
            wolf.targetSelector.addGoal(0,goal);wolf.goalSelector.addGoal(0,new BogatyrModes.StandGround(wolf));
            BogatyrModes.apply(wolf,false);
        }
    }
    static void invalidate(UUID owner){CACHE.remove(owner);ATTACKS.remove(owner);}
    static void reset(Wolf wolf){var reference=INSTALLED.get(wolf);var goal=reference==null?null:reference.get();if(goal!=null)goal.reset();}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){
        invalidate(event.getEntity().getUUID());BogatyrRecovery.clearPlayer(event.getEntity().getUUID());
    }
    private static long run(Wolf wolf){return wolf.getPersistentData().getLongOr(BogatyrWolfEvents.RUN,0);}
    private static ServerPlayer owner(Wolf wolf){
        var id=BogatyrCompanions.owner(wolf);
        var p=id==null?null:((ServerLevel)wolf.level()).getServer().getPlayerList().getPlayer(id);
        return p!=null&&p.level()==wolf.level()&&p.isAlive()&&!p.isSpectator()?p:null;
    }
    private static boolean available(Wolf wolf){
        return BogatyrWolfEvents.managed(wolf)&&wolf.isTame()&&wolf.isAlive()&&!BogatyrModes.standing(wolf)
                &&!wolf.isOrderedToSit()&&!wolf.isInSittingPose()&&!BogatyrRecovery.held(wolf);
    }
    private static boolean recent(LivingEntity victim,LivingEntity target){
        return victim.getLastHurtByMob()==target&&victim.tickCount-victim.getLastHurtByMobTimestamp()<=200;
    }
    private static boolean threatens(Mob target,ServerPlayer owner,long run,long now){
        var victim=target.getTarget();
        if(victim==owner||victim instanceof Wolf pet&&BogatyrWolfEvents.managed(pet)
                &&owner.getUUID().equals(BogatyrCompanions.owner(pet))&&run(pet)==run)return true;
        var attacks=ATTACKS.get(owner.getUUID());
        return recent(owner,target)||attacks!=null&&attacks.run()==run&&attacks.until().getOrDefault(target.getUUID(),Long.MIN_VALUE)>=now;
    }
    static boolean eligible(Wolf wolf,ServerPlayer owner,Mob target){
        if(target==null||!available(wolf)||owner==null||CompanionAllies.friendly(target)||!target.isAlive()
                ||target.level()!=owner.level()||!wolf.canAttack(target)||wolf.isAlliedTo(target)||owner.isAlliedTo(target))return false;
        double radius=Config.WOLF_FOLLOW_RANGE.get();
        if(owner.distanceToSqr(target)>radius*radius)return false;
        return BogatyrModes.mode(wolf)==WolfMode.AGGRESSIVE?target instanceof Enemy
                :recent(wolf,target)||threatens(target,owner,run(wolf),owner.level().getGameTime());
    }
    private static List<UUID> candidates(ServerPlayer owner,long run,WolfMode mode){
        var level=owner.level();long now=level.getGameTime();var cached=CACHE.get(owner.getUUID());
        String dimension=level.dimension().location().toString();
        boolean matching=cached!=null&&cached.dimension().equals(dimension)&&cached.run()==run&&cached.mode()==mode;
        if(matching&&!WolfBehaviourRules.refresh(now,cached.tick(),Config.WOLF_THREAT_POLL_TICKS.get()))return cached.ids();
        if(budgetTick!=now){budgetTick=now;scans=0;}
        if(scans>=Config.WOLF_THREAT_SCANS.get())return matching?cached.ids():List.of();
        scans++;var found=new ArrayList<Mob>();int[] visited={0};double radius=Config.WOLF_FOLLOW_RANGE.get();
        level.getEntities().get(EntityTypeTest.forClass(Mob.class),owner.getBoundingBox().inflate(radius),mob->{
            visited[0]++;
            if(mob.isAlive()&&!CompanionAllies.friendly(mob)&&owner.distanceToSqr(mob)<=radius*radius
                    &&(mode==WolfMode.AGGRESSIVE?mob instanceof Enemy:threatens(mob,owner,run,now)))found.add(mob);
            return visited[0]>=Config.WOLF_THREAT_CANDIDATES.get()
                    ?AbortableIterationConsumer.Continuation.ABORT:AbortableIterationConsumer.Continuation.CONTINUE;
        });
        found.sort(Comparator.comparingDouble((Mob mob)->owner.distanceToSqr(mob)).thenComparing(mob->mob.getUUID().toString()));
        var ids=found.stream().map(Mob::getUUID).toList();
        CACHE.put(owner.getUUID(),new Targets(dimension,run,mode,now,ids));return ids;
    }
    /** Native prey/owner-offense/retaliation goals must obey the same mode and ally boundary. */
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void changingTarget(LivingChangeTargetEvent event){
        if(!(event.getEntity() instanceof Wolf wolf)||!(wolf.level() instanceof ServerLevel)||!BogatyrWolfEvents.owned(wolf))return;
        var requested=event.getNewAboutToBeSetTarget();
        if(CompanionAllies.friendly(requested)){event.setNewAboutToBeSetTarget(null);return;}
        if(!BogatyrWolfEvents.managed(wolf)||requested==null)return;
        var reference=INSTALLED.get(wolf);var goal=reference==null?null:reference.get();
        if(!(requested instanceof Mob mob)||!eligible(wolf,owner(wolf),mob)
                ||BogatyrModes.mode(wolf)==WolfMode.AGGRESSIVE&&(goal==null||goal.choice!=requested))
            event.setNewAboutToBeSetTarget(null);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void preventFriendlyDamage(LivingIncomingDamageEvent event){
        if(event.getSource().getEntity() instanceof Wolf wolf&&BogatyrWolfEvents.owned(wolf)
                &&(CompanionAllies.friendly(event.getEntity())||BogatyrModes.standing(wolf))){
            event.setCanceled(true);wolf.setTarget(null);wolf.stopBeingAngry();
        }
    }
    /** Actual damage remembers one bounded pack-wide retaliation set; no spatial query on the damage path. */
    @SubscribeEvent public static void attacked(LivingDamageEvent.Post event){
        if(!(event.getEntity().level() instanceof ServerLevel level)||event.getNewDamage()<=0
                ||!(event.getSource().getEntity() instanceof Mob attacker)||CompanionAllies.friendly(attacker))return;
        UUID owner;long run;
        if(event.getEntity() instanceof Wolf wolf&&BogatyrWolfEvents.managed(wolf)){
            if(BogatyrModes.standing(wolf)){BogatyrModes.hold(wolf);return;}
            owner=BogatyrCompanions.owner(wolf);run=run(wolf);
        }else if(event.getEntity() instanceof ServerPlayer player){
            var active=ClassResourceService.activeRun(player).orElse(null);if(active==null)return;
            owner=player.getUUID();run=active.runId();
        }else return;
        if(owner==null||BogatyrCompanionData.get(level.getServer()).mode(owner,run).mode()==WolfMode.STAND_GROUND)return;
        long now=level.getGameTime();var attacks=ATTACKS.get(owner);
        if(attacks==null||attacks.run()!=run){attacks=new Attacks(run,new LinkedHashMap<>());ATTACKS.put(owner,attacks);}
        var history=attacks.until();history.values().removeIf(until->until<now);
        if(!history.containsKey(attacker.getUUID())&&history.size()>=Config.WOLF_THREAT_CANDIDATES.get())
            history.remove(history.keySet().iterator().next());
        history.put(attacker.getUUID(),now+200);
    }
    static final class ProtectOwner extends Goal {
        private final Wolf wolf;private Mob choice,applied;private long nextChoice;
        ProtectOwner(Wolf wolf){this.wolf=wolf;setFlags(EnumSet.of(Flag.TARGET));reset();}
        void reset(){choice=null;applied=null;long now=wolf.level().getGameTime();
            nextChoice=now+Math.floorMod(wolf.getUUID().hashCode(),Config.WOLF_THREAT_POLL_TICKS.get());}
        private Mob choose(){
            if(!available(wolf))return null;var owner=owner(wolf);if(owner==null)return null;
            long now=owner.level().getGameTime();
            if(now<nextChoice)return choice!=null&&eligible(wolf,owner,choice)?choice:null;
            nextChoice=now+Config.WOLF_THREAT_POLL_TICKS.get();choice=null;
            var ids=new LinkedHashSet<UUID>(candidates(owner,run(wolf),BogatyrModes.mode(wolf)));
            if(BogatyrModes.mode(wolf)==WolfMode.DEFENSIVE){
                var attacks=ATTACKS.get(owner.getUUID());
                if(attacks!=null&&attacks.run()==run(wolf))ids.addAll(attacks.until().keySet());
                if(wolf.getLastHurtByMob()!=null)ids.add(wolf.getLastHurtByMob().getUUID());
                if(owner.getLastHurtByMob()!=null)ids.add(owner.getLastHurtByMob().getUUID());
            }
            // Owner candidates are shared; per-wolf visibility/attack checks run only on staggered decisions.
            var found=new ArrayList<Mob>();
            for(var id:ids)if(owner.level().getEntity(id) instanceof Mob mob&&eligible(wolf,owner,mob))found.add(mob);
            found.sort(Comparator.comparingDouble((Mob mob)->owner.distanceToSqr(mob)).thenComparing(mob->mob.getUUID().toString()));
            for(var mob:found)if(wolf.getSensing().hasLineOfSight(mob)){choice=mob;break;}
            return choice;
        }
        @Override public boolean canUse(){return choose()!=null;}
        @Override public boolean canContinueToUse(){return choose()!=null;}
        @Override public void start(){applied=choice;wolf.setTarget(applied);}
        @Override public void tick(){var next=choose();if(next!=null&&wolf.getTarget()!=next){applied=next;wolf.setTarget(next);}}
        @Override public void stop(){if(wolf.getTarget()==applied)wolf.setTarget(null);applied=null;}
    }
}
