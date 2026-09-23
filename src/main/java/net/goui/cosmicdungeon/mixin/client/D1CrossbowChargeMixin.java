package net.goui.cosmicdungeon.mixin.client;

import net.goui.cosmicdungeon.item.custom.D1FireworkItem;
import net.minecraft.client.renderer.item.properties.select.Charge;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keep the native loaded-rocket crossbow model for custom D1 rockets. Client only. */
@Mixin(Charge.class)
public abstract class D1CrossbowChargeMixin {
    @Redirect(method = "get(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/world/entity/LivingEntity;ILnet/minecraft/world/item/ItemDisplayContext;)Lnet/minecraft/world/item/CrossbowItem$ChargeType;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/ChargedProjectiles;contains(Lnet/minecraft/world/item/Item;)Z"))
    private boolean cosmicdungeon$rocketCharge(ChargedProjectiles projectiles, Item item) {
        return projectiles.contains(item)
                || item == Items.FIREWORK_ROCKET && D1FireworkItem.containsCustomRocket(projectiles);
    }
}
