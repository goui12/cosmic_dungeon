package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.playerclass.bogatyr.BogatyrCompanions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class BogatyrLifecycleMixin {
    @Inject(method="onAddedToLevel",at=@At("RETURN"))
    private void cosmicdungeon$added(CallbackInfo info){
        if((Object)this instanceof Wolf wolf)BogatyrCompanions.added(wolf);
    }
    @Inject(method="setRemoved",at=@At("HEAD"))
    private void cosmicdungeon$removing(Entity.RemovalReason reason,CallbackInfo info){
        if((Object)this instanceof Wolf wolf && wolf.getRemovalReason()==null)BogatyrCompanions.removing(wolf,reason);
    }
    @Inject(method="saveWithoutId",at=@At("HEAD"))
    private void cosmicdungeon$saving(ValueOutput output,CallbackInfo info){
        if((Object)this instanceof Wolf wolf)BogatyrCompanions.track(wolf,true);
    }
}
