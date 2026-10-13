package net.goui.cosmicdungeon.mixin;

import java.util.List;
import net.goui.cosmicdungeon.ammunition.AmmoChoices;
import net.goui.cosmicdungeon.ammunition.AmmoSelectionService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ProjectileWeaponItem.class)
public abstract class AmmoDrawMixin {
    @Inject(method="draw",at=@At("RETURN"))
    private static void cosmicdungeon$afterDraw(ItemStack weapon,ItemStack ammo,LivingEntity shooter,
                                               CallbackInfoReturnable<List<ItemStack>> ci){
        if(shooter instanceof ServerPlayer player&&AmmoChoices.weapon(weapon)&&!ci.getReturnValue().isEmpty())
            AmmoSelectionService.afterDraw(player,weapon);
    }
}
