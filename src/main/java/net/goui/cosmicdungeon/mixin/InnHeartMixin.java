package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.npc.inn.InnData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.level.block.entity.CreakingHeartBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The protected First Heart must not create a hostile protector, even outside the Inn cuboid. */
@Mixin(CreakingHeartBlockEntity.class)
public abstract class InnHeartMixin {
    @Inject(method="spawnProtector",at=@At("HEAD"),cancellable=true)
    private static void cosmicdungeon$firstHeart(ServerLevel level,CreakingHeartBlockEntity heart,CallbackInfoReturnable<Creaking> ci){
        if(InnData.get(level.getServer()).contains(level,heart.getBlockPos()))ci.setReturnValue(null);
    }
}
