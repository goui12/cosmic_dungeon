package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.item.identity.ItemLifecyclePolicy;
import net.goui.cosmicdungeon.item.identity.ItemMovementRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Mob.class, Dolphin.class, Panda.class, Fox.class, Allay.class, Pillager.class,
        Piglin.class, Villager.class, Raider.class})
public abstract class OwnedItemMobPickupMixin {
    @Inject(method = "pickUpItem", at = @At("HEAD"), cancellable = true)
    private void cosmicdungeon$noOwnerTransfer(ServerLevel level, ItemEntity item, CallbackInfo ci) {
        if (!ItemLifecyclePolicy.mayAutomate(ItemMovementRules.flags(item.getItem()).privateStorage(), item.getTarget()))
            ci.cancel();
    }
}
