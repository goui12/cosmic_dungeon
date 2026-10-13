package net.goui.cosmicdungeon.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.goui.cosmicdungeon.dungeon.DungeonStartupTiming;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;

/** Inactive outside an explicit synchronous dungeon-start measurement; no world scans or extra loads. */
@Mixin(ServerChunkCache.class)
public abstract class DungeonStartupChunkMixin {
    @WrapMethod(method="getChunk")
    private ChunkAccess cosmicdungeon$chunk(int x,int z,ChunkStatus status,boolean require,Operation<ChunkAccess> original){
        if(!DungeonStartupTiming.active())return original.call(x,z,status,require);
        return DungeonStartupTiming.chunk(()->original.call(x,z,status,require));
    }
}
