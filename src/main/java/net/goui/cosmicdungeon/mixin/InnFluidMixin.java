package net.goui.cosmicdungeon.mixin;

import net.goui.cosmicdungeon.npc.inn.InnData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Includes LavaFluid's override, which can replace the target before invoking its superclass. */
@Mixin({FlowingFluid.class,LavaFluid.class})
public abstract class InnFluidMixin {
    @Inject(method="spreadTo",at=@At("HEAD"),cancellable=true)
    private void cosmicdungeon$protectBeds(LevelAccessor level,BlockPos pos,BlockState state,Direction direction,FluidState fluid,CallbackInfo ci){
        if(level instanceof ServerLevel server&&InnData.get(server.getServer()).contains(server,pos))ci.cancel();
    }
}
