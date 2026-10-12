package net.goui.cosmicdungeon.mixin;
import net.goui.cosmicdungeon.playerclass.bogatyr.BogatyrBreeding;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Both manual ally feeding and Feed Pack enter the same temporary Stay breeding path. */
@Mixin(Animal.class)
public abstract class BogatyrLoveMixin {
    @Inject(method="setInLove",at=@At("RETURN"))
    private void cosmicdungeon$stayMating(Player player,CallbackInfo ci){
        if((Object)this instanceof Wolf wolf)BogatyrBreeding.begin(wolf);
    }
}
