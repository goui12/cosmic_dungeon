package net.goui.cosmicdungeon.mixin;
import net.goui.cosmicdungeon.playerclass.skill.SkillPotions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(PotionContents.class)
public abstract class SkillDrinkPotionMixin {
    @Redirect(method="onConsume",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/alchemy/PotionContents;applyToLivingEntity(Lnet/minecraft/world/entity/LivingEntity;F)V"))
    private void cosmicdungeon$drink(PotionContents contents,LivingEntity target,float scale,
                                     Level level,LivingEntity entity,ItemStack stack,Consumable consumable) {
        SkillPotions.drink(contents,target,scale,stack);
    }
}
