package net.minecraft.client.renderer;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ViewArea {
    protected final LevelRenderer levelRenderer;
    protected final Level level;
    protected int sectionGridSizeY;
    protected int sectionGridSizeX;
    protected int sectionGridSizeZ;
    private int viewDistance;
    private SectionPos cameraSectionPos;
    public SectionRenderDispatcher.RenderSection[] sections;

    public ViewArea(SectionRenderDispatcher sectionRenderDispatcher, Level level, int viewDistance, LevelRenderer levelRenderer) {
        this.levelRenderer = levelRenderer;
        this.level = level;
        this.setViewDistance(viewDistance);
        this.createSections(sectionRenderDispatcher);
        this.cameraSectionPos = SectionPos.of(this.viewDistance + 1, 0, this.viewDistance + 1);
    }

    protected void createSections(SectionRenderDispatcher sectionRenderDispatcher) {
        if (!Minecraft.getInstance().isSameThread()) {
            throw new IllegalStateException("createSections called from wrong thread: " + Thread.currentThread().getName());
        } else {
            int i = this.sectionGridSizeX * this.sectionGridSizeY * this.sectionGridSizeZ;
            this.sections = new SectionRenderDispatcher.RenderSection[i];

            for (int j = 0; j < this.sectionGridSizeX; j++) {
                for (int k = 0; k < this.sectionGridSizeY; k++) {
                    for (int l = 0; l < this.sectionGridSizeZ; l++) {
                        int i1 = this.getSectionIndex(j, k, l);
                        this.sections[i1] = sectionRenderDispatcher.new RenderSection(i1, SectionPos.asLong(j, k + this.level.getMinSectionY(), l));
                    }
                }
            }
        }
    }

    public void releaseAllBuffers() {
        for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection : this.sections) {
            sectionrenderdispatcher$rendersection.reset();
        }
    }

    private int getSectionIndex(int x, int y, int z) {
        return (z * this.sectionGridSizeY + y) * this.sectionGridSizeX + x;
    }

    protected void setViewDistance(int renderDistanceChunks) {
        int i = renderDistanceChunks * 2 + 1;
        this.sectionGridSizeX = i;
        this.sectionGridSizeY = this.level.getSectionsCount();
        this.sectionGridSizeZ = i;
        this.viewDistance = renderDistanceChunks;
    }

    public int getViewDistance() {
        return this.viewDistance;
    }

    public LevelHeightAccessor getLevelHeightAccessor() {
        return this.level;
    }

    public void repositionCamera(SectionPos newSectionPos) {
        for (int i = 0; i < this.sectionGridSizeX; i++) {
            int j = newSectionPos.x() - this.viewDistance;
            int k = j + Math.floorMod(i - j, this.sectionGridSizeX);

            for (int l = 0; l < this.sectionGridSizeZ; l++) {
                int i1 = newSectionPos.z() - this.viewDistance;
                int j1 = i1 + Math.floorMod(l - i1, this.sectionGridSizeZ);

                for (int k1 = 0; k1 < this.sectionGridSizeY; k1++) {
                    int l1 = this.level.getMinSectionY() + k1;
                    SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.sections[this.getSectionIndex(i, k1, l)];
                    long i2 = sectionrenderdispatcher$rendersection.getSectionNode();
                    if (i2 != SectionPos.asLong(k, l1, j1)) {
                        sectionrenderdispatcher$rendersection.setSectionNode(SectionPos.asLong(k, l1, j1));
                    }
                }
            }
        }

        this.cameraSectionPos = newSectionPos;
        this.levelRenderer.getSectionOcclusionGraph().invalidate();
    }

    public SectionPos getCameraSectionPos() {
        return this.cameraSectionPos;
    }

    public void setDirty(int sectionX, int sectionY, int sectionZ, boolean reRenderOnMainThread) {
        SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.getRenderSection(sectionX, sectionY, sectionZ);
        if (sectionrenderdispatcher$rendersection != null) {
            sectionrenderdispatcher$rendersection.setDirty(reRenderOnMainThread);
        }
    }

    @Nullable
    protected SectionRenderDispatcher.RenderSection getRenderSectionAt(BlockPos pos) {
        return this.getRenderSection(SectionPos.asLong(pos));
    }

    @Nullable
    protected SectionRenderDispatcher.RenderSection getRenderSection(long sectionPos) {
        int i = SectionPos.x(sectionPos);
        int j = SectionPos.y(sectionPos);
        int k = SectionPos.z(sectionPos);
        return this.getRenderSection(i, j, k);
    }

    @Nullable
    private SectionRenderDispatcher.RenderSection getRenderSection(int x, int y, int z) {
        if (!this.containsSection(x, y, z)) {
            return null;
        } else {
            int i = y - this.level.getMinSectionY();
            int j = Math.floorMod(x, this.sectionGridSizeX);
            int k = Math.floorMod(z, this.sectionGridSizeZ);
            return this.sections[this.getSectionIndex(j, i, k)];
        }
    }

    private boolean containsSection(int x, int y, int z) {
        if (y >= this.level.getMinSectionY() && y <= this.level.getMaxSectionY()) {
            return x < this.cameraSectionPos.x() - this.viewDistance || x > this.cameraSectionPos.x() + this.viewDistance
                ? false
                : z >= this.cameraSectionPos.z() - this.viewDistance && z <= this.cameraSectionPos.z() + this.viewDistance;
        } else {
            return false;
        }
    }
}
