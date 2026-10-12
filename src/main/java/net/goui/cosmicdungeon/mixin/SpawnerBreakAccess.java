package net.goui.cosmicdungeon.mixin;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(ServerPlayerGameMode.class)
public interface SpawnerBreakAccess {
    @Accessor("isDestroyingBlock") boolean cosmicdungeon$breaking();
    @Accessor("destroyPos") BlockPos cosmicdungeon$breakPos();
}
