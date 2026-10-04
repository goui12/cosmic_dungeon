package net.goui.cosmicdungeon.mixin;
import net.goui.cosmicdungeon.playerclass.skill.SkillPotions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrownLingeringPotion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(ThrownLingeringPotion.class)
public abstract class SkillLingeringPotionMixin {
    @Redirect(method="onHitAsPotion",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean cosmicdungeon$inherit(ServerLevel level,Entity cloud) {
        SkillPotions.inherit(cloud,(Entity)(Object)this);return level.addFreshEntity(cloud);
    }
}
