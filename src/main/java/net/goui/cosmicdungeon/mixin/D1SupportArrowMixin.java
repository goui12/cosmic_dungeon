package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.playerclass.d1.D1ArrowAbilities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractArrow.class)
public abstract class D1SupportArrowMixin {
    @Redirect(method = "canHitEntity", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;canHarmPlayer(Lnet/minecraft/world/entity/player/Player;)Z"))
    private boolean cosmicdungeon$allowRestorativeAid(Player shooter, Player target) {
        return shooter.canHarmPlayer(target) || target instanceof ServerPlayer recipient
                && D1ArrowAbilities.canSupport((AbstractArrow) (Object) this, recipient);
    }
}
