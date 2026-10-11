package net.goui.cosmicdungeon.mixin;
import net.goui.cosmicdungeon.playerclass.bogatyr.*;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.target.*;
import org.spongepowered.asm.mixin.*;
@Mixin({OwnerHurtByTargetGoal.class,OwnerHurtTargetGoal.class})
public abstract class BogatyrOwnerTargetMixin implements BogatyrTargetMemory {
    @Shadow @Final private TamableAnimal tameAnimal;
    @Shadow private int timestamp;
    @Override public void cosmicdungeon$forgetOwnerThreat(){
        var owner=tameAnimal.getOwner();
        timestamp=owner==null?0:(Object)this instanceof OwnerHurtByTargetGoal
                ?owner.getLastHurtByMobTimestamp():owner.getLastHurtMobTimestamp();
    }
}
