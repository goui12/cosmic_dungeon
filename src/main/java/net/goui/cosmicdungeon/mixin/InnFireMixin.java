package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.npc.inn.InnData;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireBlock.class)
public abstract class InnFireMixin {
    @Inject(method="checkBurnOut",at=@At("HEAD"),cancellable=true)
    private void cosmicdungeon$noBurn(Level level,BlockPos pos,int chance,RandomSource random,int age,Direction face,CallbackInfo ci){
        if(level instanceof ServerLevel server&&InnData.get(server.getServer()).contains(server,pos))ci.cancel();
    }
    @Redirect(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean cosmicdungeon$noIgnition(ServerLevel level,BlockPos pos,BlockState state,int flags){
        return !InnData.get(level.getServer()).contains(level,pos)&&level.setBlock(pos,state,flags);
    }
}
