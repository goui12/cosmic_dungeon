package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.auth.AccessPolicy;
import net.goui.cosmicdungeon.item.identity.ProtectedItemLifecycle;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class ProtectedItemDeathMixin {
    @Redirect(method = "destroyVanishingCursedItems", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;has(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/component/DataComponentType;)Z"))
    private boolean cosmicdungeon$keepProtectedCurse(ItemStack stack, DataComponentType<?> component) {
        return !((Object)this instanceof ServerPlayer player && ProtectedItemLifecycle.retain(player, stack))
                && EnchantmentHelper.has(stack, component);
    }
    @Redirect(method = "dropEquipment", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;dropAll()V"))
    private void cosmicdungeon$deathInventory(Inventory inventory) {
        if ((Object)this instanceof ServerPlayer player && !AccessPolicy.isDeveloper(player))
            ProtectedItemLifecycle.dropUnprotected(player);
        else inventory.dropAll();
    }
}
