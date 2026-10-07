package net.goui.cosmicdungeon.playerclass.bogatyr;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.warden.Warden;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;

/** Redirects the native target authority; goal targets and brain targets are distinct stores. */
final class BogatyrAggro {
    private BogatyrAggro(){}
    static boolean redirect(Mob attacker,Wolf guard){
        if(attacker==null||guard==null||!attacker.isAlive()||!guard.isAlive()
                ||attacker.level()!=guard.level()||CompanionAllies.friendly(attacker)
                ||attacker.isAlliedTo(guard)||!attacker.canAttack(guard))return false;
        var brain=attacker.getBrain();
        boolean brainTarget=brain.checkMemory(MemoryModuleType.ATTACK_TARGET,MemoryStatus.REGISTERED);
        if(attacker.getTarget()==guard||brainTarget&&brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null)==guard)return true;
        if(!brainTarget){
            attacker.setTarget(guard);return attacker.getTarget()==guard;
        }
        var change=CommonHooks.onLivingChangeTarget(attacker,guard,LivingChangeTargetEvent.LivingTargetType.BEHAVIOR_TARGET);
        if(change.isCanceled()||change.getNewAboutToBeSetTarget()!=guard)return false;
        if(attacker instanceof Warden warden)warden.setAttackTarget(guard);
        else {
            brain.setMemory(MemoryModuleType.ATTACK_TARGET,guard);
            brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        }
        return brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null)==guard;
    }
}
