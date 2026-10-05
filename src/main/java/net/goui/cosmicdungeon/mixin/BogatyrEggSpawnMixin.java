package net.goui.cosmicdungeon.mixin;

import java.util.function.Consumer;
import net.goui.cosmicdungeon.playerclass.bogatyr.BogatyrWolfEvents;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native egg spawning retains placement, authored NBT and consumption; tame before world insertion. */
@Mixin(EntityType.class)
public abstract class BogatyrEggSpawnMixin {
    @Inject(method = "createDefaultStackConfig", at = @At("RETURN"), cancellable = true)
    private static void cosmicdungeon$tameEgg(Level level, ItemStack stack, LivingEntity owner,
            CallbackInfoReturnable<Consumer<Entity>> cir) {
        if (BogatyrWolfEvents.isBogatyrWolfEgg(owner, stack))
            cir.setReturnValue(cir.getReturnValue().andThen(entity -> BogatyrWolfEvents.tameEgg(owner, entity)));
    }
}
