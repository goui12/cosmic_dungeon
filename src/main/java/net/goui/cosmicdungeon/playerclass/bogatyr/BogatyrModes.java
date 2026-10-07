package net.goui.cosmicdungeon.playerclass.bogatyr;

import java.util.*;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.resource.ClassResourceService;
import net.goui.cosmicdungeon.transaction.InventoryTransactionGuard;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Mode commands change the current run's real pack, without inventory/resource effects or chunk loading. */
@EventBusSubscriber(modid="cosmicdungeon")
public final class BogatyrModes {
    private static final Map<Wolf,WolfMode> APPLIED=new WeakHashMap<>();
    private BogatyrModes(){}
    public static void clear(){APPLIED.clear();}
    private static WolfMode savedMode(Wolf wolf){
        if(!(wolf.level() instanceof ServerLevel level)||!BogatyrWolfEvents.managed(wolf))return WolfMode.DEFENSIVE;
        return BogatyrCompanionData.get(level.getServer()).mode(BogatyrCompanions.owner(wolf),
                wolf.getPersistentData().getLongOr(BogatyrWolfEvents.RUN,0)).mode();
    }
    public static WolfMode mode(Wolf wolf){var value=APPLIED.get(wolf);return value==null?savedMode(wolf):value;}
    public static boolean standing(Wolf wolf){
        return wolf.level() instanceof ServerLevel&&BogatyrWolfEvents.managed(wolf)&&mode(wolf)==WolfMode.STAND_GROUND;
    }
    static boolean select(ServerPlayer player,long run,WolfMode mode){
        var active=ClassResourceService.activeRun(player).orElse(null);
        if(active==null||active.runId()!=run||!"bogatyr".equals(ClassData.getClassId(player))
                ||!player.isAlive()||player.isDeadOrDying()
                ||player.level().getServer().getPlayerList().getPlayer(player.getUUID())!=player
                ||!InventoryTransactionGuard.beforeCurrentInventoryAction(player))return false;
        var data=BogatyrCompanionData.get(player.level().getServer());var old=data.mode(player.getUUID(),run);
        if(!old.writable())return false;
        if(old.mode()!=mode){
            if(!data.setMode(player.getUUID(),run,mode))return false;
            // Free, idempotent preference uses normal native SavedData saves; no disk I/O on command/tick paths.
        }
        BogatyrThreats.invalidate(player.getUUID());
        for(var wolf:BogatyrCommands.loaded(player,run))apply(wolf,true);
        return true;
    }
    static void apply(Wolf wolf,boolean command){
        if(!(wolf.level() instanceof ServerLevel)||!BogatyrWolfEvents.managed(wolf))return;
        var mode=savedMode(wolf);var old=APPLIED.put(wolf,mode);
        if(command||old!=mode){
            wolf.targetSelector.getAvailableGoals().stream().filter(net.minecraft.world.entity.ai.goal.WrappedGoal::isRunning).forEach(net.minecraft.world.entity.ai.goal.WrappedGoal::stop);
            wolf.goalSelector.getAvailableGoals().stream().filter(net.minecraft.world.entity.ai.goal.WrappedGoal::isRunning).filter(g->g.getFlags().contains(Goal.Flag.MOVE)||g.getFlags().contains(Goal.Flag.JUMP))
                    .forEach(net.minecraft.world.entity.ai.goal.WrappedGoal::stop);
            wolf.getNavigation().stop();wolf.setTarget(null);wolf.stopBeingAngry();
            wolf.setLastHurtByMob(null);wolf.setLastHurtMob(null);
            BogatyrThreats.reset(wolf);wolf.getNavigation().updatePathfinderMaxVisitedNodes();
            if(mode!=WolfMode.STAND_GROUND&&(command||old==WolfMode.STAND_GROUND)){
                wolf.setOrderedToSit(false);wolf.setInSittingPose(false);
            }
        }
        if(mode==WolfMode.STAND_GROUND)hold(wolf);
        else {BogatyrBoundary.check(wolf);BogatyrRescue.tickWolf(wolf);}
    }
    public static void hold(Wolf wolf){
        wolf.getNavigation().stop();wolf.setTarget(null);wolf.stopBeingAngry();
        wolf.setLastHurtByMob(null);wolf.setLastHurtMob(null);
        wolf.setOrderedToSit(true);wolf.setInSittingPose(true);
        wolf.setJumping(false);wolf.setSpeed(0);wolf.xxa=0;wolf.yya=0;wolf.zza=0;
        wolf.getMoveControl().setWantedPosition(wolf.getX(),wolf.getY(),wolf.getZ(),0);
    }
    @SubscribeEvent public static void beforeTick(EntityTickEvent.Pre event){
        if(event.getEntity() instanceof Wolf wolf)apply(wolf,false);
    }
    static final class StandGround extends Goal {
        private final Wolf wolf;
        StandGround(Wolf wolf){this.wolf=wolf;setFlags(EnumSet.of(Flag.MOVE,Flag.JUMP));}
        @Override public boolean canUse(){return standing(wolf);}
        @Override public boolean canContinueToUse(){return standing(wolf);}
        @Override public boolean requiresUpdateEveryTick(){return true;}
        @Override public void start(){hold(wolf);}
        @Override public void tick(){hold(wolf);}
    }
}
