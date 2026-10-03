package net.goui.cosmicdungeon.mixin;

import java.util.Optional;
import net.goui.cosmicdungeon.playerclass.bogatyr.BogatyrWolfEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Right-clicking a wolf with an egg uses a separate native offspring path. */
@Mixin(SpawnEggItem.class)
public abstract class BogatyrEggOffspringMixin {
    @Inject(method = "spawnOffspringFromSpawnEgg", at = @At("HEAD"), cancellable = true)
    private void cosmicdungeon$offspringCap(Player player, Mob parent, EntityType<? extends Mob> type,
            ServerLevel level, Vec3 pos, ItemStack stack, CallbackInfoReturnable<Optional<Mob>> cir) {
        if (type == EntityType.WOLF && !BogatyrWolfEvents.maySpawnEgg(player, stack))
            cir.setReturnValue(Optional.empty());
    }

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(
            method = "spawnOffspringFromSpawnEgg", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private void cosmicdungeon$tameOffspring(ServerLevel level, Entity child,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original,
            Player player, Mob parent, EntityType<? extends Mob> type, ServerLevel suppliedLevel,
            Vec3 pos, ItemStack stack) {
        if (BogatyrWolfEvents.isBogatyrWolfEgg(player, stack)) BogatyrWolfEvents.tameEgg(player, child);
        original.call(level, child);
    }
}
