package net.goui.cosmicdungeon.mixin;
import net.goui.cosmicdungeon.dungeon.DungeonStartupTiming;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Inactive outside an explicit synchronous dungeon-start measurement; no world scans or extra loads. */
@Mixin(ServerChunkCache.class)
public abstract class DungeonStartupChunkMixin {
    @Inject(method="getChunk",at=@At("HEAD"))
    private void cosmicdungeon$start(int x,int z,ChunkStatus status,boolean require,CallbackInfoReturnable<ChunkAccess> ci){
        DungeonStartupTiming.chunkStarted();
    }
    @Inject(method="getChunk",at=@At("RETURN"))
    private void cosmicdungeon$finish(int x,int z,ChunkStatus status,boolean require,CallbackInfoReturnable<ChunkAccess> ci){
        DungeonStartupTiming.chunkFinished();
    }
}
