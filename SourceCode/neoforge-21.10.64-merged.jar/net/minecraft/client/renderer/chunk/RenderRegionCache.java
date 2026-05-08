package net.minecraft.client.renderer.chunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RenderRegionCache {
    private final Long2ObjectMap<SectionCopy> sectionCopyCache = new Long2ObjectOpenHashMap<>();

    public RenderSectionRegion createRegion(Level level, long chunkPos) {
        int i = SectionPos.x(chunkPos);
        int j = SectionPos.y(chunkPos);
        int k = SectionPos.z(chunkPos);
        int l = i - 1;
        int i1 = j - 1;
        int j1 = k - 1;
        int k1 = i + 1;
        int l1 = j + 1;
        int i2 = k + 1;
        SectionCopy[] asectioncopy = new SectionCopy[27];

        for (int j2 = j1; j2 <= i2; j2++) {
            for (int k2 = i1; k2 <= l1; k2++) {
                for (int l2 = l; l2 <= k1; l2++) {
                    int i3 = RenderSectionRegion.index(l, i1, j1, l2, k2, j2);
                    asectioncopy[i3] = this.getSectionDataCopy(level, l2, k2, j2);
                }
            }
        }

        var modelDataManager = level.getModelDataManager().snapshotSectionRegion(l, i1, j1, k1, l1, i2);
        return new RenderSectionRegion(level, l, i1, j1, asectioncopy, modelDataManager);
    }

    private SectionCopy getSectionDataCopy(Level level, int x, int y, int z) {
        return this.sectionCopyCache.computeIfAbsent(SectionPos.asLong(x, y, z), p_426915_ -> {
            LevelChunk levelchunk = level.getChunk(x, z);
            return new SectionCopy(levelchunk, levelchunk.getSectionIndexFromSectionY(y));
        });
    }
}
