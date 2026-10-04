package net.goui.cosmicdungeon.mixin;

import java.util.function.Consumer;
import net.goui.cosmicdungeon.playerclass.bogatyr.BogatyrWolfEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native egg spawning retains placement, authored NBT and consumption; tame before world insertion. */
@Mixin(EntityType.class)
public abstract class BogatyrEggSpawnMixin {
    @Inject(method = "spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/EntitySpawnReason;ZZ)Lnet/minecraft/world/entity/Entity;",
            at = @At("HEAD"), cancellable = true)
    private void cosmicdungeon$eggCap(ServerLevel level, ItemStack stack, LivingEntity owner,
            BlockPos pos, EntitySpawnReason reason, boolean offset, boolean extraOffset,
            CallbackInfoReturnable<Entity> cir) {
        if ((Object)this == EntityType.WOLF && reason == EntitySpawnReason.SPAWN_ITEM_USE
                && !BogatyrWolfEvents.maySpawnEgg(owner, stack)) cir.setReturnValue(null);
    }

    @Inject(method = "createDefaultStackConfig", at = @At("RETURN"), cancellable = true)
    private static void cosmicdungeon$tameEgg(Level level, ItemStack stack, LivingEntity owner,
            CallbackInfoReturnable<Consumer<Entity>> cir) {
        if (BogatyrWolfEvents.isBogatyrWolfEgg(owner, stack))
            cir.setReturnValue(cir.getReturnValue().andThen(entity -> BogatyrWolfEvents.tameEgg(owner, entity)));
    }
}
