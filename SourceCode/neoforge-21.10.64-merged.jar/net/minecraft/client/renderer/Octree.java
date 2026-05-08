package net.minecraft.client.renderer;

import javax.annotation.Nullable;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Octree {
    private final Octree.Branch root;
    final BlockPos cameraSectionCenter;

    public Octree(SectionPos cameraSectionPos, int viewDistance, int sectionGridSizeY, int minY) {
        int i = viewDistance * 2 + 1;
        int j = Mth.smallestEncompassingPowerOfTwo(i);
        int k = viewDistance * 16;
        BlockPos blockpos = cameraSectionPos.origin();
        this.cameraSectionCenter = cameraSectionPos.center();
        int l = blockpos.getX() - k;
        int i1 = l + j * 16 - 1;
        int j1 = j >= sectionGridSizeY ? minY : blockpos.getY() - k;
        int k1 = j1 + j * 16 - 1;
        int l1 = blockpos.getZ() - k;
        int i2 = l1 + j * 16 - 1;
        this.root = new Octree.Branch(new BoundingBox(l, j1, l1, i1, k1, i2));
    }

    public boolean add(SectionRenderDispatcher.RenderSection section) {
        return this.root.add(section);
    }

    public void visitNodes(Octree.OctreeVisitor visitor, Frustum frustum, int nearbyRadius) {
        this.root.visitNodes(visitor, false, frustum, 0, nearbyRadius, true);
    }

    boolean isClose(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int radius) {
        int i = this.cameraSectionCenter.getX();
        int j = this.cameraSectionCenter.getY();
        int k = this.cameraSectionCenter.getZ();
        return i > minX - radius
            && i < maxX + radius
            && j > minY - radius
            && j < maxY + radius
            && k > minZ - radius
            && k < maxZ + radius;
    }

    @OnlyIn(Dist.CLIENT)
    static enum AxisSorting {
        XYZ(4, 2, 1),
        XZY(4, 1, 2),
        YXZ(2, 4, 1),
        YZX(1, 4, 2),
        ZXY(2, 1, 4),
        ZYX(1, 2, 4);

        final int xShift;
        final int yShift;
        final int zShift;

        private AxisSorting(int xShift, int yShift, int zShift) {
            this.xShift = xShift;
            this.yShift = yShift;
            this.zShift = zShift;
        }

        public static Octree.AxisSorting getAxisSorting(int xDiff, int yDiff, int zDiff) {
            if (xDiff > yDiff && xDiff > zDiff) {
                return yDiff > zDiff ? XYZ : XZY;
            } else if (yDiff > xDiff && yDiff > zDiff) {
                return xDiff > zDiff ? YXZ : YZX;
            } else {
                return xDiff > yDiff ? ZXY : ZYX;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    class Branch implements Octree.Node {
        private final Octree.Node[] nodes = new Octree.Node[8];
        private final BoundingBox boundingBox;
        private final int bbCenterX;
        private final int bbCenterY;
        private final int bbCenterZ;
        private final Octree.AxisSorting sorting;
        private final boolean cameraXDiffNegative;
        private final boolean cameraYDiffNegative;
        private final boolean cameraZDiffNegative;

        public Branch(BoundingBox boundingBox) {
            this.boundingBox = boundingBox;
            this.bbCenterX = this.boundingBox.minX() + this.boundingBox.getXSpan() / 2;
            this.bbCenterY = this.boundingBox.minY() + this.boundingBox.getYSpan() / 2;
            this.bbCenterZ = this.boundingBox.minZ() + this.boundingBox.getZSpan() / 2;
            int i = Octree.this.cameraSectionCenter.getX() - this.bbCenterX;
            int j = Octree.this.cameraSectionCenter.getY() - this.bbCenterY;
            int k = Octree.this.cameraSectionCenter.getZ() - this.bbCenterZ;
            this.sorting = Octree.AxisSorting.getAxisSorting(Math.abs(i), Math.abs(j), Math.abs(k));
            this.cameraXDiffNegative = i < 0;
            this.cameraYDiffNegative = j < 0;
            this.cameraZDiffNegative = k < 0;
        }

        public boolean add(SectionRenderDispatcher.RenderSection section) {
            long i = section.getSectionNode();
            boolean flag = SectionPos.sectionToBlockCoord(SectionPos.x(i)) - this.bbCenterX < 0;
            boolean flag1 = SectionPos.sectionToBlockCoord(SectionPos.y(i)) - this.bbCenterY < 0;
            boolean flag2 = SectionPos.sectionToBlockCoord(SectionPos.z(i)) - this.bbCenterZ < 0;
            boolean flag3 = flag != this.cameraXDiffNegative;
            boolean flag4 = flag1 != this.cameraYDiffNegative;
            boolean flag5 = flag2 != this.cameraZDiffNegative;
            int j = getNodeIndex(this.sorting, flag3, flag4, flag5);
            if (this.areChildrenLeaves()) {
                boolean flag6 = this.nodes[j] != null;
                this.nodes[j] = Octree.this.new Leaf(section);
                return !flag6;
            } else if (this.nodes[j] != null) {
                Octree.Branch octree$branch1 = (Octree.Branch)this.nodes[j];
                return octree$branch1.add(section);
            } else {
                BoundingBox boundingbox = this.createChildBoundingBox(flag, flag1, flag2);
                Octree.Branch octree$branch = Octree.this.new Branch(boundingbox);
                this.nodes[j] = octree$branch;
                return octree$branch.add(section);
            }
        }

        private static int getNodeIndex(Octree.AxisSorting sorting, boolean xDiffNegative, boolean yDiffNegative, boolean zDiffNegative) {
            int i = 0;
            if (xDiffNegative) {
                i += sorting.xShift;
            }

            if (yDiffNegative) {
                i += sorting.yShift;
            }

            if (zDiffNegative) {
                i += sorting.zShift;
            }

            return i;
        }

        private boolean areChildrenLeaves() {
            return this.boundingBox.getXSpan() == 32;
        }

        private BoundingBox createChildBoundingBox(boolean xDiffNegative, boolean yDiffNegative, boolean zDiffNegative) {
            int i;
            int j;
            if (xDiffNegative) {
                i = this.boundingBox.minX();
                j = this.bbCenterX - 1;
            } else {
                i = this.bbCenterX;
                j = this.boundingBox.maxX();
            }

            int k;
            int l;
            if (yDiffNegative) {
                k = this.boundingBox.minY();
                l = this.bbCenterY - 1;
            } else {
                k = this.bbCenterY;
                l = this.boundingBox.maxY();
            }

            int i1;
            int j1;
            if (zDiffNegative) {
                i1 = this.boundingBox.minZ();
                j1 = this.bbCenterZ - 1;
            } else {
                i1 = this.bbCenterZ;
                j1 = this.boundingBox.maxZ();
            }

            return new BoundingBox(i, k, i1, j, l, j1);
        }

        @Override
        public void visitNodes(Octree.OctreeVisitor visitor, boolean isLeafNode, Frustum frustum, int recursionDepth, int nearbyRadius, boolean isNearby) {
            boolean flag = isLeafNode;
            if (!isLeafNode) {
                int i = frustum.cubeInFrustum(this.boundingBox);
                isLeafNode = i == -2;
                flag = i == -2 || i == -1;
            }

            if (flag) {
                isNearby = isNearby
                    && Octree.this.isClose(
                        this.boundingBox.minX(),
                        this.boundingBox.minY(),
                        this.boundingBox.minZ(),
                        this.boundingBox.maxX(),
                        this.boundingBox.maxY(),
                        this.boundingBox.maxZ(),
                        nearbyRadius
                    );
                visitor.visit(this, isLeafNode, recursionDepth, isNearby);

                for (Octree.Node octree$node : this.nodes) {
                    if (octree$node != null) {
                        octree$node.visitNodes(visitor, isLeafNode, frustum, recursionDepth + 1, nearbyRadius, isNearby);
                    }
                }
            }
        }

        @Nullable
        @Override
        public SectionRenderDispatcher.RenderSection getSection() {
            return null;
        }

        @Override
        public AABB getAABB() {
            return new AABB(
                this.boundingBox.minX(),
                this.boundingBox.minY(),
                this.boundingBox.minZ(),
                this.boundingBox.maxX() + 1,
                this.boundingBox.maxY() + 1,
                this.boundingBox.maxZ() + 1
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    final class Leaf implements Octree.Node {
        private final SectionRenderDispatcher.RenderSection section;

        Leaf(SectionRenderDispatcher.RenderSection section) {
            this.section = section;
        }

        @Override
        public void visitNodes(Octree.OctreeVisitor visitor, boolean isLeafNode, Frustum frustum, int recursionDepth, int nearbyRadius, boolean isNearby) {
            AABB aabb = this.section.getBoundingBox();
            if (isLeafNode || frustum.isVisible(this.getSection().getBoundingBox())) {
                isNearby = isNearby && Octree.this.isClose(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ, nearbyRadius);
                visitor.visit(this, isLeafNode, recursionDepth, isNearby);
            }
        }

        @Override
        public SectionRenderDispatcher.RenderSection getSection() {
            return this.section;
        }

        @Override
        public AABB getAABB() {
            return this.section.getBoundingBox();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public interface Node {
        void visitNodes(Octree.OctreeVisitor visitor, boolean isLeafNode, Frustum frustum, int recursionDepth, int nearbyRadius, boolean isNearby);

        @Nullable
        SectionRenderDispatcher.RenderSection getSection();

        AABB getAABB();
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface OctreeVisitor {
        void visit(Octree.Node node, boolean isLeafNode, int recursionDepth, boolean isNearby);
    }
}
