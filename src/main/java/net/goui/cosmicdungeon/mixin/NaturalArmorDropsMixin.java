package net.goui.cosmicdungeon.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.goui.cosmicdungeon.combat.NaturalArmorDrops;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Mob.class)
public abstract class NaturalArmorDropsMixin {
    @WrapOperation(method="populateDefaultEquipmentSlots", at=@At(value="INVOKE",
            target="Lnet/minecraft/world/entity/Mob;setItemSlot(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V"))
    private void cosmicdungeon$naturalArmor(Mob mob, EquipmentSlot slot, ItemStack stack, Operation<Void> original) {
        original.call(mob, slot, stack);
        NaturalArmorDrops.generated(mob, slot);
    }
}
