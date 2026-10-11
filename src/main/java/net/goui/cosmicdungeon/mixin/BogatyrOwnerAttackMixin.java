package net.goui.cosmicdungeon.mixin;
import net.goui.cosmicdungeon.playerclass.bogatyr.BogatyrTargetMemory;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import org.spongepowered.asm.mixin.*;
/** A single native target preserves normal remapping for each shadowed field. */
@Mixin(OwnerHurtTargetGoal.class)
public abstract class BogatyrOwnerAttackMixin implements BogatyrTargetMemory {
    @Shadow @Final private TamableAnimal tameAnimal;
    @Shadow private int timestamp;
    @Override public void cosmicdungeon$forgetOwnerThreat(){
        var owner=tameAnimal.getOwner();
        timestamp=owner==null?0:owner.getLastHurtMobTimestamp();
    }
}
