package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.dungeon.d1.RunMemberStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(LivingEntity.class)
public abstract class RunHealingMixin {
    @Redirect(method="heal",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V"))
    private void cosmicdungeon$actualHealing(LivingEntity entity,float health) {
        float before=entity.getHealth();
        entity.setHealth(health);
        if(entity instanceof ServerPlayer player)RunMemberStats.healed(player,before,entity.getHealth());
    }
}
