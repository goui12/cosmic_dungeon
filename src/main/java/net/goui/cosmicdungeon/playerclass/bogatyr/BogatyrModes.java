package net.goui.cosmicdungeon.playerclass.bogatyr;
import java.util.*;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.resource.ClassResourceService;
import net.goui.cosmicdungeon.transaction.InventoryTransactionGuard;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
/** Native Patrol with a bounded recall-and-hold override. Never forces entity chunks. */
@EventBusSubscriber(modid="cosmicdungeon")
public final class BogatyrModes {
    static final String STAY="cosmicdungeon.bogatyr_stay";
    private static final class Applied {
        private final WolfMode mode;private final long epoch,run;private final UUID owner;
        private boolean targetHeld,targetWasDisabled;
        Applied(WolfMode mode,long epoch,UUID owner,long run){this.mode=mode;this.epoch=epoch;this.owner=owner;this.run=run;}
        WolfMode mode(){return mode;}
        long epoch(){return epoch;}
        boolean matches(WolfMode mode,long epoch,UUID owner,long run){
            return this.mode==mode&&this.epoch==epoch&&this.run==run&&Objects.equals(this.owner,owner);
        }
    }
    private static final Map<Wolf,Applied> APPLIED=new WeakHashMap<>();
    private BogatyrModes(){}
    public static void clear(){
        APPLIED.forEach(BogatyrModes::releaseTargets);APPLIED.clear();BogatyrBreeding.clear();
    }
    private static void suppressTargets(Wolf wolf,Applied applied){
        if(!applied.targetHeld){
            applied.targetWasDisabled=((TargetSelectorFlags)wolf.targetSelector).cosmicdungeon$targetDisabled();
            applied.targetHeld=true;
            if(BogatyrWork.probe!=null)BogatyrWork.probe.targetSuppressions++;
        }
        // Native selector cleanup/update honors this flag, including subsequently added TARGET goals.
        wolf.targetSelector.disableControlFlag(Goal.Flag.TARGET);
    }
    private static void releaseTargets(Wolf wolf,Applied applied){
        if(applied!=null&&applied.targetHeld){
            if(!applied.targetWasDisabled)wolf.targetSelector.enableControlFlag(Goal.Flag.TARGET);
            applied.targetHeld=false;
        }
    }
    /** Restore native targeting when a live timed companion leaves managed enrollment. */
    public static void released(Wolf wolf){releaseTargets(wolf,APPLIED.remove(wolf));}
    private static WolfModeData.State state(Wolf wolf){
        return BogatyrCompanionData.get(((ServerLevel)wolf.level()).getServer())
                .mode(BogatyrCompanions.owner(wolf),BogatyrThreats.run(wolf));
    }
    static long epoch(Wolf wolf){
        return BogatyrCompanionData.get(((ServerLevel)wolf.level()).getServer())
                .modeEpoch(BogatyrCompanions.owner(wolf),BogatyrThreats.run(wolf));
    }
    public static WolfMode mode(Wolf wolf){
        if(!(wolf.level() instanceof ServerLevel)||!BogatyrWolfEvents.managed(wolf))return WolfMode.PATROL;
        if(unsupportedAnchor(wolf))return WolfMode.STAY;
        var value=APPLIED.get(wolf);return value==null?state(wolf).mode():value.mode();
    }
    public static boolean standing(Wolf wolf){
        return wolf.level() instanceof ServerLevel&&BogatyrWolfEvents.managed(wolf)&&mode(wolf)==WolfMode.STAY;
    }
    static boolean select(ServerPlayer player,long run,WolfMode mode){
        if(mode==null||!mode.active())return false;
        var active=ClassResourceService.activeRun(player).orElse(null);
        if(active==null||active.runId()!=run||!"bogatyr".equals(ClassData.getClassId(player))
                ||!player.isAlive()||player.isDeadOrDying()
                ||player.level().getServer().getPlayerList().getPlayer(player.getUUID())!=player
                ||!InventoryTransactionGuard.beforeCurrentInventoryAction(player))return false;
        var data=BogatyrCompanionData.get(player.level().getServer());var old=data.mode(player.getUUID(),run);
        if(!old.writable())return false;
        if(old.mode()==mode)return true; // Radio re-selection never recalls an already settled pack.
        if(!data.setMode(player.getUUID(),run,mode))return false;
        BogatyrThreats.invalidate(player.getUUID());
        for(var wolf:BogatyrCommands.loaded(player,run))apply(wolf,true);
        return true;
    }
    static boolean careAllowed(Wolf wolf){
        return wolf.level() instanceof ServerLevel&&BogatyrWolfEvents.managed(wolf)
                &&!unsupportedAnchor(wolf)&&state(wolf).writable()&&!BogatyrRecovery.held(wolf);
    }
    private static void clearThreat(Wolf wolf){
        wolf.setTarget(null);wolf.stopBeingAngry();wolf.setLastHurtByMob(null);wolf.setLastHurtMob(null);
    }
    private static void forget(Wolf wolf){
        clearThreat(wolf);
        if(BogatyrWork.probe!=null)BogatyrWork.probe.targetTraversals++;
        wolf.targetSelector.getAvailableGoals().forEach(g->{
            if(BogatyrWork.probe!=null)BogatyrWork.probe.targetGoals++;
            if(g.isRunning())g.stop();
            if(g.getGoal() instanceof BogatyrTargetMemory memory){
                if(BogatyrWork.probe!=null)BogatyrWork.probe.memoryResets++;
                memory.cosmicdungeon$forgetOwnerThreat();
            }
        });
    }
    static void apply(Wolf wolf,boolean command){
        if(!(wolf.level() instanceof ServerLevel))return;
        if(!BogatyrWolfEvents.managed(wolf)){released(wolf);return;}
        if(unsupportedAnchor(wolf)){BogatyrBreeding.cancel(wolf);forget(wolf);hold(wolf);return;}
        var state=state(wolf);long epoch=epoch(wolf);var owner=BogatyrCompanions.owner(wolf);long run=BogatyrThreats.run(wolf);
        var next=APPLIED.get(wolf);
        if(next==null||!next.matches(state.mode(),epoch,owner,run)){
            releaseTargets(wolf,next);
            next=new Applied(state.mode(),epoch,owner,run);APPLIED.put(wolf,next);
            BogatyrBreeding.cancel(wolf);
            wolf.goalSelector.getAvailableGoals().stream().filter(g->g.isRunning()
                    &&(g.getFlags().contains(Goal.Flag.MOVE)||g.getFlags().contains(Goal.Flag.JUMP)))
                    .forEach(net.minecraft.world.entity.ai.goal.WrappedGoal::stop);
            wolf.getNavigation().stop();forget(wolf);
            if(next.mode()==WolfMode.PATROL){
                wolf.setOrderedToSit(false);wolf.setInSittingPose(false);
            }else if(state.writable()){
                var saved=wolf.getPersistentData().getCompoundOrEmpty(STAY);
                if(!valid(saved,wolf,next.epoch())){
                    var value=saved.copy();value.putInt("schema",1);value.putLong("epoch",next.epoch());
                    value.putLong("run",BogatyrThreats.run(wolf));value.putString("owner",String.valueOf(BogatyrCompanions.owner(wolf)));
                    value.putBoolean("settled",false);wolf.getPersistentData().put(STAY,value);
                }
                wolf.setOrderedToSit(false);wolf.setInSittingPose(false);
            }
        }
        BogatyrBreeding.tick(wolf);
        if(next.mode()==WolfMode.STAY){
            suppressTargets(wolf,next);clearThreat(wolf);
            if(!state.writable()||BogatyrRecovery.held(wolf)||seated(wolf))hold(wolf);
        }
    }
    private static boolean unsupportedAnchor(Wolf wolf){
        var root=wolf.getPersistentData();if(!root.contains(STAY))return false;
        if(!(root.get(STAY) instanceof CompoundTag value)||value.getIntOr("schema",-1)!=1
                ||!(value.get("epoch") instanceof net.minecraft.nbt.LongTag)||value.getLongOr("epoch",-1)<0
                ||!(value.get("run") instanceof net.minecraft.nbt.LongTag)||value.getLongOr("run",0)<=0
                ||!(value.get("owner") instanceof net.minecraft.nbt.StringTag)
                ||!(value.get("settled") instanceof net.minecraft.nbt.ByteTag))return true;
        try{UUID.fromString(value.getStringOr("owner",""));}catch(IllegalArgumentException error){return true;}
        return value.getBooleanOr("settled",false)&&(!Double.isFinite(value.getDoubleOr("x",Double.NaN))
                ||!Double.isFinite(value.getDoubleOr("y",Double.NaN))||!Double.isFinite(value.getDoubleOr("z",Double.NaN)));
    }
    private static boolean valid(CompoundTag value,Wolf wolf,long epoch){
        return value.getIntOr("schema",-1)==1&&value.getLongOr("epoch",-1)==epoch
                &&value.getLongOr("run",0)==BogatyrThreats.run(wolf)
                &&value.getStringOr("owner","").equals(String.valueOf(BogatyrCompanions.owner(wolf)));
    }
    static Vec3 anchor(Wolf wolf){
        var value=wolf.getPersistentData().getCompoundOrEmpty(STAY);
        if(!valid(value,wolf,epoch(wolf))||!value.getBooleanOr("settled",false))return null;
        double x=value.getDoubleOr("x",Double.NaN),y=value.getDoubleOr("y",Double.NaN),z=value.getDoubleOr("z",Double.NaN);
        return Double.isFinite(x)&&Double.isFinite(y)&&Double.isFinite(z)?new Vec3(x,y,z):null;
    }
    public static boolean seated(Wolf wolf){
        if(!standing(wolf)||BogatyrBreeding.active(wolf))return false;
        if(unsupportedAnchor(wolf)||!state(wolf).writable())return true;
        var anchor=anchor(wolf);return anchor!=null&&wolf.position().distanceToSqr(anchor)<.25;
    }
    static void settle(Wolf wolf){
        var value=wolf.getPersistentData().getCompoundOrEmpty(STAY).copy();
        value.putBoolean("settled",true);value.putDouble("x",wolf.getX());value.putDouble("y",wolf.getY());value.putDouble("z",wolf.getZ());
        wolf.getPersistentData().put(STAY,value);hold(wolf);
    }
    public static void hold(Wolf wolf){
        wolf.getNavigation().stop();wolf.setTarget(null);wolf.stopBeingAngry();wolf.setLastHurtByMob(null);wolf.setLastHurtMob(null);
        wolf.setOrderedToSit(true);wolf.setInSittingPose(true);wolf.setJumping(false);wolf.setSpeed(0);
        wolf.xxa=0;wolf.yya=0;wolf.zza=0;wolf.setDeltaMovement(new Vec3(0,wolf.getDeltaMovement().y,0));
        wolf.getMoveControl().setWantedPosition(wolf.getX(),wolf.getY(),wolf.getZ(),0);
    }
    @SubscribeEvent public static void beforeTick(EntityTickEvent.Pre event){
        if(event.getEntity() instanceof Wolf wolf)apply(wolf,false);
    }
    static final class StandGround extends Goal {
        private final Wolf wolf;private long nextPath;
        StandGround(Wolf wolf){this.wolf=wolf;setFlags(EnumSet.of(Flag.MOVE,Flag.JUMP));}
        @Override public boolean canUse(){return standing(wolf)&&!BogatyrBreeding.active(wolf);}
        @Override public boolean canContinueToUse(){return standing(wolf)&&!BogatyrBreeding.active(wolf);}
        @Override public boolean requiresUpdateEveryTick(){return true;}
        @Override public void start(){nextPath=0;tick();}
        @Override public void tick(){
            if(unsupportedAnchor(wolf)||!state(wolf).writable()||BogatyrRecovery.held(wolf)){hold(wolf);return;}
            var anchor=anchor(wolf);var owner=BogatyrThreats.owner(wolf);
            if(anchor==null&&owner!=null&&wolf.distanceToSqr(owner)<=25&&wolf.onGround()){settle(wolf);return;}
            if(anchor!=null&&wolf.position().distanceToSqr(anchor)<.25){hold(wolf);return;}
            if(anchor==null&&owner==null){hold(wolf);return;}
            wolf.setOrderedToSit(false);wolf.setInSittingPose(false);
            long now=wolf.level().getGameTime();if(now<nextPath)return;
            nextPath=now+20+Math.floorMod(wolf.getUUID().hashCode(),5);
            var point=anchor==null?owner.position():anchor;
            if(wolf.level().hasChunkAt(net.minecraft.core.BlockPos.containing(point)))
                wolf.getNavigation().moveTo(point.x,point.y,point.z,1.2);
        }
        @Override public void stop(){wolf.getNavigation().stop();}
    }
}
