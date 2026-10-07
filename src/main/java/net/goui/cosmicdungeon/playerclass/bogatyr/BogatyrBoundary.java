package net.goui.cosmicdungeon.playerclass.bogatyr;

import java.util.EnumSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/** Full route validation on installation; constant-cost moving-boundary checks before each navigation tick. */
public final class BogatyrBoundary {
    private BogatyrBoundary(){}
    static boolean active(Wolf wolf){return BogatyrWolfEvents.managed(wolf)&&BogatyrModes.mode(wolf)==WolfMode.DANGER_CLOSE;}
    static boolean inside(ServerPlayer owner,Vec3 point){return WolfTacticsRules.inside(owner.position().distanceToSqr(point));}
    static boolean mayAttack(Wolf wolf,net.minecraft.world.entity.LivingEntity target){
        if(!active(wolf))return true;var owner=BogatyrThreats.owner(wolf);
        return owner!=null&&inside(owner,wolf.position())&&inside(owner,target.position());
    }
    public static boolean allowPath(Wolf wolf,Path path){
        if(BogatyrWork.probe!=null)BogatyrWork.probe.paths++;
        if(!active(wolf)||path==null)return true;
        var owner=BogatyrThreats.owner(wolf);if(owner==null)return false;
        double previous=owner.distanceToSqr(wolf),initial=previous;boolean returning=!WolfTacticsRules.inside(previous);
        for(int i=path.getNextNodeIndex();i<path.getNodeCount();i++){
            if(BogatyrWork.probe!=null)BogatyrWork.probe.nodes++;
            double distance=owner.position().distanceToSqr(path.getEntityPosAtNode(wolf,i));
            if(!Double.isFinite(distance))return false;
            if(returning){
                // The starting node may be rounded slightly; every subsequent outside node must make inward progress.
                if(i>path.getNextNodeIndex()&&distance>previous+.01)return false;
                returning=!WolfTacticsRules.inside(distance);previous=distance;
            }else if(!WolfTacticsRules.inside(distance))return false;
        }
        return !returning||previous<initial-.25;
    }
    public static void check(Wolf wolf){
        if(!active(wolf))return;if(BogatyrWork.probe!=null)BogatyrWork.probe.leashes++;
        var owner=BogatyrThreats.owner(wolf);
        if(owner==null){stop(wolf);wolf.setTarget(null);return;}
        if(wolf.getTarget()!=null&&!mayAttack(wolf,wolf.getTarget()))wolf.setTarget(null);
        var path=wolf.getNavigation().getPath();
        if(path!=null&&!path.isDone()){
            double current=owner.distanceToSqr(wolf),next=owner.position().distanceToSqr(path.getNextEntityPos(wolf));
            int index=path.getNextNodeIndex()+1;
            // Native navigation may advance a node before setting motion; check the next node too.
            double after=index<path.getNodeCount()?owner.position().distanceToSqr(path.getEntityPosAtNode(wolf,index)):next;
            if(WolfTacticsRules.inside(current)?!WolfTacticsRules.inside(next)||!WolfTacticsRules.inside(after)
                    :next>current+2||after>Math.max(current,next)+2)stop(wolf);
        }
        if(!inside(owner,wolf.position())){
            wolf.setTarget(null);
            if(wolf.goalSelector.getAvailableGoals().stream().noneMatch(g->g.isRunning()&&g.getGoal() instanceof Return))stop(wolf);
            wolf.goalSelector.getAvailableGoals().stream().filter(g->g.isRunning()&&!(g.getGoal() instanceof Return)
                    &&(g.getFlags().contains(Goal.Flag.MOVE)||g.getFlags().contains(Goal.Flag.JUMP)))
                    .forEach(net.minecraft.world.entity.ai.goal.WrappedGoal::stop);
        }
    }
    private static void stop(Wolf wolf){
        wolf.getNavigation().stop();wolf.setJumping(false);wolf.setSpeed(0);wolf.xxa=wolf.yya=wolf.zza=0;
        wolf.getMoveControl().setWantedPosition(wolf.getX(),wolf.getY(),wolf.getZ(),0);
    }
    static final class Return extends Goal {
        private final Wolf wolf;private long nextPath;
        Return(Wolf wolf){this.wolf=wolf;setFlags(EnumSet.of(Flag.MOVE,Flag.JUMP));}
        @Override public boolean canUse(){var owner=BogatyrThreats.owner(wolf);return active(wolf)&&BogatyrThreats.available(wolf)&&owner!=null&&!inside(owner,wolf.position());}
        @Override public boolean canContinueToUse(){var owner=BogatyrThreats.owner(wolf);return active(wolf)&&BogatyrThreats.available(wolf)&&owner!=null&&wolf.distanceToSqr(owner)>144;}
        @Override public boolean requiresUpdateEveryTick(){return true;}
        @Override public void start(){nextPath=0;tick();}
        @Override public void tick(){
            wolf.setTarget(null);long now=wolf.level().getGameTime();
            if(now<nextPath)return;nextPath=now+20+Math.floorMod(wolf.getUUID().hashCode(),5);
            var owner=BogatyrThreats.owner(wolf);
            if(owner!=null&&wolf.level().hasChunkAt(owner.blockPosition()))wolf.getNavigation().moveTo(owner,1.2);
        }
        @Override public void stop(){wolf.getNavigation().stop();}
    }
}
