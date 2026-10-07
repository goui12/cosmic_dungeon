package net.goui.cosmicdungeon.playerclass.bogatyr;

import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.playerclass.resource.ClassResourceService;
import net.minecraft.server.level.*;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.util.*;

@EventBusSubscriber(modid="cosmicdungeon")
public final class BogatyrThreats {
    private record Targets(String dimension,long run,WolfMode mode,long tick,List<UUID> ids,List<UUID> patients){}
    private record Attacks(long run,Map<UUID,Long> until){}
    private static final Map<Wolf,java.lang.ref.WeakReference<ProtectOwner>> INSTALLED=new WeakHashMap<>();
    private static final Map<UUID,Targets> CACHE=new HashMap<>();
    private static final Map<UUID,Attacks> ATTACKS=new HashMap<>();
    private static long budgetTick=Long.MIN_VALUE;
    private static int scans;
    private BogatyrThreats(){}
    public static void clear(){INSTALLED.clear();CACHE.clear();ATTACKS.clear();BogatyrModes.clear();BogatyrRescue.clear();budgetTick=Long.MIN_VALUE;scans=0;}
    public static void install(Wolf wolf){
        if(wolf.level() instanceof ServerLevel&&!INSTALLED.containsKey(wolf)){
            var goal=new ProtectOwner(wolf);INSTALLED.put(wolf,new java.lang.ref.WeakReference<>(goal));
            wolf.targetSelector.addGoal(0,goal);wolf.goalSelector.addGoal(0,new BogatyrModes.StandGround(wolf));
            wolf.goalSelector.addGoal(0,new BogatyrBoundary.Return(wolf));wolf.goalSelector.addGoal(2,new BogatyrRescue.Escort(wolf));
            BogatyrModes.apply(wolf,false);
        }
    }
    private static ProtectOwner goal(Wolf wolf){var ref=INSTALLED.get(wolf);return ref==null?null:ref.get();}
    static void invalidate(UUID owner){CACHE.remove(owner);ATTACKS.remove(owner);}
    static void reset(Wolf wolf){var goal=goal(wolf);if(goal!=null)goal.reset();}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){
        invalidate(event.getEntity().getUUID());BogatyrRecovery.clearPlayer(event.getEntity().getUUID());
        if(event.getEntity() instanceof ServerPlayer p)BogatyrRescue.forget(p);
    }
    static long run(Wolf wolf){return wolf.getPersistentData().getLongOr(BogatyrWolfEvents.RUN,0);}
    static ServerPlayer owner(Wolf wolf){
        if(!(wolf.level() instanceof ServerLevel level))return null;
        var id=BogatyrCompanions.owner(wolf);var p=id==null?null:level.getServer().getPlayerList().getPlayer(id);
        return p!=null&&p.level()==wolf.level()&&p.isAlive()&&!p.isSpectator()?p:null;
    }
    static boolean available(Wolf wolf){
        return BogatyrWolfEvents.managed(wolf)&&wolf.isTame()&&wolf.isAlive()&&!BogatyrModes.standing(wolf)
                &&!wolf.isOrderedToSit()&&!wolf.isInSittingPose()&&!BogatyrRecovery.held(wolf);
    }
    private static boolean recent(LivingEntity victim,LivingEntity target){
        return victim.getLastHurtByMob()==target&&victim.tickCount-victim.getLastHurtByMobTimestamp()<=200;
    }
    static boolean attacks(Mob target,ServerPlayer victim,long run,long now){
        var history=ATTACKS.get(victim.getUUID());
        var focus=target.getTarget();var guard=focus instanceof Wolf pet?goal(pet):null;
        boolean guarding=focus instanceof Wolf pet&&run(pet)==run&&BogatyrModes.mode(pet)==WolfMode.SEARCH_AND_RESCUE
                &&guard!=null&&BogatyrThreats.patient(pet)==victim;
        return focus==victim||guarding||recent(victim,target)
                ||history!=null&&history.run()==run&&history.until().getOrDefault(target.getUUID(),Long.MIN_VALUE)>=now;
    }
    private static boolean threatens(Mob target,ServerPlayer owner,long run,long now){
        var victim=target.getTarget();
        return attacks(target,owner,run,now)||victim instanceof Wolf pet&&BogatyrWolfEvents.managed(pet)
                &&owner.getUUID().equals(BogatyrCompanions.owner(pet))&&run(pet)==run;
    }
    static ServerPlayer patient(Wolf wolf){
        if(BogatyrModes.mode(wolf)!=WolfMode.SEARCH_AND_RESCUE||!available(wolf))return null;
        var goal=goal(wolf);var owner=owner(wolf);if(goal==null||goal.patient==null||owner==null)return null;
        var p=owner.level().getServer().getPlayerList().getPlayer(goal.patient);
        return BogatyrRescue.eligible(owner,p,run(wolf))?p:null;
    }
    private static boolean safe(Wolf wolf,ServerPlayer owner,Mob target){
        if(target==null||!available(wolf)||owner==null||CompanionAllies.friendly(target)||!target.isAlive()
                ||target.level()!=owner.level()||!wolf.canAttack(target)||wolf.isAlliedTo(target)||owner.isAlliedTo(target))return false;
        double radius=BogatyrModes.mode(wolf)==WolfMode.DANGER_CLOSE?16:Config.WOLF_FOLLOW_RANGE.get();
        return owner.distanceToSqr(target)<=radius*radius&&BogatyrBoundary.mayAttack(wolf,target);
    }
    static boolean eligible(Wolf wolf,ServerPlayer owner,Mob target){
        if(!safe(wolf,owner,target))return false;
        return switch(BogatyrModes.mode(wolf)){
            case AGGRESSIVE,STRATEGIC,DANGER_CLOSE -> target instanceof Enemy;
            case SEARCH_AND_RESCUE -> {var p=patient(wolf);yield p!=null&&attacks(target,p,run(wolf),owner.level().getGameTime());}
            case DEFENSIVE -> recent(wolf,target)||threatens(target,owner,run(wolf),owner.level().getGameTime());
            case STAND_GROUND -> false;
        };
    }
    private static Targets candidates(ServerPlayer owner,long run,WolfMode mode,long now){
        var level=owner.level();var cached=CACHE.get(owner.getUUID());String dimension=level.dimension().location().toString();
        boolean matching=cached!=null&&cached.dimension().equals(dimension)&&cached.run()==run&&cached.mode()==mode;
        if(matching&&!WolfBehaviourRules.refresh(now,cached.tick(),Config.WOLF_THREAT_POLL_TICKS.get()))return cached;
        if(budgetTick!=now){budgetTick=now;scans=0;}
        if(scans>=Config.WOLF_THREAT_SCANS.get())return matching?cached:new Targets(dimension,run,mode,now,List.of(),List.of());
        scans++;if(BogatyrWork.probe!=null)BogatyrWork.probe.scans++;
        var patients=mode==WolfMode.SEARCH_AND_RESCUE?BogatyrRescue.patients(owner,run):List.<ServerPlayer>of();
        var found=new ArrayList<Mob>();int[] visited={0};
        double radius=mode==WolfMode.DANGER_CLOSE?16:Config.WOLF_FOLLOW_RANGE.get();
        // Filter at the typed query boundary: friendly packs cannot consume the accepted-threat budget.
        var types=new EntityTypeTest<Entity,Mob>(){
            public Mob tryCast(Entity entity){
                if(!(entity instanceof Mob mob))return null;
                if(BogatyrWork.probe!=null)BogatyrWork.probe.inspected++;
                if(!mob.isAlive()||CompanionAllies.friendly(mob))return null;
                boolean include=switch(mode){
                    case DEFENSIVE -> threatens(mob,owner,run,now);
                    case SEARCH_AND_RESCUE -> patients.stream().anyMatch(p->attacks(mob,p,run,now));
                    case STAND_GROUND -> false;
                    default -> mob instanceof Enemy;
                };
                return include?mob:null;
            }
            public Class<? extends Entity> getBaseClass(){return Mob.class;}
        };
        level.getEntities().get(types,owner.getBoundingBox().inflate(radius),mob->{
            visited[0]++;if(BogatyrWork.probe!=null)BogatyrWork.probe.visited++;
            if(owner.distanceToSqr(mob)<=radius*radius)found.add(mob);
            return visited[0]>=Config.WOLF_THREAT_CANDIDATES.get()
                    ?AbortableIterationConsumer.Continuation.ABORT:AbortableIterationConsumer.Continuation.CONTINUE;
        });
        Comparator<Mob> order=Comparator.comparingDouble((Mob m)->owner.distanceToSqr(m)).thenComparing(m->m.getUUID().toString());
        if(mode==WolfMode.STRATEGIC)order=(a,b)->WolfTacticsRules.compareStrategic(BogatyrRanged.ranged(a),a.getMaxHealth(),a.getStringUUID(),
                BogatyrRanged.ranged(b),b.getMaxHealth(),b.getStringUUID());
        else if(mode==WolfMode.DANGER_CLOSE)order=Comparator.comparingInt((Mob m)->attacks(m,owner,run,now)?0:1).thenComparing(order);
        found.sort(order);
        var next=new Targets(dimension,run,mode,now,found.stream().map(Mob::getUUID).toList(),patients.stream().map(ServerPlayer::getUUID).toList());
        CACHE.put(owner.getUUID(),next);return next;
    }
    /** Native prey, offense and retaliation cannot override the current strategy or ally boundary. */
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void changingTarget(LivingChangeTargetEvent event){
        if(!(event.getEntity() instanceof Wolf wolf)||!(wolf.level() instanceof ServerLevel)||!BogatyrWolfEvents.owned(wolf))return;
        var requested=event.getNewAboutToBeSetTarget();
        if(CompanionAllies.friendly(requested)){event.setNewAboutToBeSetTarget(null);return;}
        if(!BogatyrWolfEvents.managed(wolf)||requested==null)return;
        var goal=goal(wolf);
        if(!(requested instanceof Mob mob)||!eligible(wolf,owner(wolf),mob)
                ||BogatyrModes.mode(wolf)!=WolfMode.DEFENSIVE&&(goal==null||goal.choice!=requested))
            event.setNewAboutToBeSetTarget(null);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void preventFriendlyDamage(LivingIncomingDamageEvent event){
        if(event.getSource().getEntity() instanceof Wolf wolf&&BogatyrWolfEvents.owned(wolf)
                &&(CompanionAllies.friendly(event.getEntity())||BogatyrModes.standing(wolf)
                    ||BogatyrWolfEvents.managed(wolf)&&!BogatyrBoundary.mayAttack(wolf,event.getEntity()))){
            event.setCanceled(true);wolf.setTarget(null);wolf.stopBeingAngry();
        }
    }
    private static void remember(UUID owner,long run,Mob attacker,long now){
        var attacks=ATTACKS.get(owner);
        if(attacks==null||attacks.run()!=run){attacks=new Attacks(run,new LinkedHashMap<>());ATTACKS.put(owner,attacks);}
        var history=attacks.until();history.values().removeIf(until->until<now);
        if(!history.containsKey(attacker.getUUID())&&history.size()>=Config.WOLF_THREAT_CANDIDATES.get())
            history.remove(history.keySet().iterator().next());
        history.put(attacker.getUUID(),now+200);
    }
    @SubscribeEvent public static void attacked(LivingDamageEvent.Post event){
        if(!(event.getEntity().level() instanceof ServerLevel level)||event.getNewDamage()<=0
                ||!(event.getSource().getEntity() instanceof Mob attacker)||CompanionAllies.friendly(attacker))return;
        UUID owner;long run;
        if(event.getEntity() instanceof Wolf wolf&&BogatyrWolfEvents.managed(wolf)){
            if(BogatyrModes.standing(wolf)){BogatyrModes.hold(wolf);return;}
            owner=BogatyrCompanions.owner(wolf);run=run(wolf);
        }else if(event.getEntity() instanceof ServerPlayer player){
            var active=ClassResourceService.activeRun(player).orElse(null);if(active==null)return;owner=player.getUUID();run=active.runId();
        }else return;
        if(owner==null||BogatyrCompanionData.get(level.getServer()).mode(owner,run).mode()==WolfMode.STAND_GROUND)return;
        remember(owner,run,attacker,level.getGameTime());
    }
    static final class ProtectOwner extends Goal {
        private final Wolf wolf;private Mob choice,applied;private UUID patient;private long nextChoice;
        ProtectOwner(Wolf wolf){this.wolf=wolf;setFlags(EnumSet.of(Flag.TARGET));reset();}
        void reset(){resetAt(wolf.level().getGameTime());}
        void resetAt(long now){choice=null;applied=null;patient=null;
            nextChoice=now+Math.floorMod(wolf.getUUID().hashCode(),Config.WOLF_THREAT_POLL_TICKS.get());}
        /** Same decision kernel used by native goals and opt-in CI cost sampling; never changes world time. */
        Mob think(long now){
            if(!available(wolf))return null;var owner=owner(wolf);if(owner==null)return null;
            var mode=BogatyrModes.mode(wolf);
            if(mode==WolfMode.DANGER_CLOSE&&wolf.getTarget() instanceof Mob current&&eligible(wolf,owner,current)){
                choice=current;return choice;
            }
            if(now<nextChoice)return choice!=null&&eligible(wolf,owner,choice)?choice:null;
            nextChoice=now+Config.WOLF_THREAT_POLL_TICKS.get();choice=null;
            if(mode!=WolfMode.SEARCH_AND_RESCUE)patient=null;
            if(BogatyrWork.probe!=null)BogatyrWork.probe.decisions++;
            var shared=candidates(owner,run(wolf),mode,now);
            if(mode==WolfMode.SEARCH_AND_RESCUE){
                ServerPlayer first=null;
                for(var id:shared.patients()){
                    var p=owner.level().getServer().getPlayerList().getPlayer(id);
                    if(!BogatyrRescue.eligible(owner,p,run(wolf)))continue;
                    if(first==null)first=p;
                    for(var target:shared.ids())if(owner.level().getEntity(target) instanceof Mob mob
                            &&safe(wolf,owner,mob)&&attacks(mob,p,run(wolf),now)&&wolf.getSensing().hasLineOfSight(mob)){
                        patient=id;choice=mob;return choice;
                    }
                }
                patient=first==null?null:first.getUUID();return null;
            }
            for(var id:shared.ids())if(owner.level().getEntity(id) instanceof Mob mob&&eligible(wolf,owner,mob)
                    &&wolf.getSensing().hasLineOfSight(mob)){choice=mob;break;}
            if(choice==null&&mode==WolfMode.DEFENSIVE){
                var history=ATTACKS.get(owner.getUUID());var urgent=new LinkedHashSet<UUID>();
                if(history!=null&&history.run()==run(wolf))urgent.addAll(history.until().keySet());
                if(wolf.getLastHurtByMob()!=null)urgent.add(wolf.getLastHurtByMob().getUUID());
                if(owner.getLastHurtByMob()!=null)urgent.add(owner.getLastHurtByMob().getUUID());
                for(var id:urgent)if(owner.level().getEntity(id) instanceof Mob mob&&eligible(wolf,owner,mob)
                        &&wolf.getSensing().hasLineOfSight(mob)){choice=mob;break;}
            }
            return choice;
        }
        private Mob choose(){return think(wolf.level().getGameTime());}
        private void apply(){
            applied=choice;if(wolf.getTarget()!=applied)wolf.setTarget(applied);
            var p=BogatyrThreats.patient(wolf);
            if(p!=null&&choice!=null&&choice.getTarget()!=wolf&&attacks(choice,p,run(wolf),wolf.level().getGameTime())){
                if(choice.getTarget() instanceof Wolf guarding&&BogatyrThreats.patient(guarding)==p)return;
                remember(p.getUUID(),run(wolf),choice,wolf.level().getGameTime());BogatyrAggro.redirect(choice,wolf);
            }
        }
        @Override public boolean canUse(){return choose()!=null;}
        @Override public boolean canContinueToUse(){return choose()!=null;}
        @Override public void start(){apply();}
        @Override public void tick(){var next=choose();if(next!=null)apply();}
        @Override public void stop(){if(wolf.getTarget()==applied)wolf.setTarget(null);applied=null;}
    }
}
