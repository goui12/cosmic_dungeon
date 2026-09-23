package net.goui.cosmicdungeon.mixin;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AbstractCandleBlock.class)
public abstract class D1CandleMixin {
    @Inject(method="setLit",at=@At("TAIL"))
    private static void cosmicdungeon$check(LevelAccessor level,BlockState state,BlockPos pos,boolean lit,CallbackInfo ci){
        if(lit&&level instanceof ServerLevel server)net.goui.cosmicdungeon.achievement.d1.SixfoldVigilTracker.schedule(server,pos);
    }
}
