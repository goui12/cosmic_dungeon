package net.minecraft.world.level.levelgen;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

/**
 * Modifies terrain noise to be flatter near structures such as villages.
 */
public class Beardifier implements DensityFunctions.BeardifierOrMarker {
    public static final int BEARD_KERNEL_RADIUS = 12;
    private static final int BEARD_KERNEL_SIZE = 24;
    private static final float[] BEARD_KERNEL = Util.make(new float[13824], p_158082_ -> {
        for (int i = 0; i < 24; i++) {
            for (int j = 0; j < 24; j++) {
                for (int k = 0; k < 24; k++) {
                    p_158082_[i * 24 * 24 + j * 24 + k] = (float)computeBeardContribution(j - 12, k - 12, i - 12);
                }
            }
        }
    });
    public static final Beardifier EMPTY = new Beardifier(List.of(), List.of(), null);
    protected final List<Beardifier.Rigid> pieces;
    protected final List<JigsawJunction> junctions;
    @Nullable
    private final BoundingBox affectedBox;

    public static Beardifier forStructuresInChunk(StructureManager structureManager, ChunkPos chunkPos) {
        List<StructureStart> list = structureManager.startsForStructure(chunkPos, p_223941_ -> p_223941_.terrainAdaptation() != TerrainAdjustment.NONE);
        if (list.isEmpty()) {
            return EMPTY;
        } else {
            int i = chunkPos.getMinBlockX();
            int j = chunkPos.getMinBlockZ();
            List<Beardifier.Rigid> list1 = new ArrayList<>();
            List<JigsawJunction> list2 = new ArrayList<>();
            BoundingBox boundingbox = null;

            for (StructureStart structurestart : list) {
                TerrainAdjustment terrainadjustment = structurestart.getStructure().terrainAdaptation();

                for (StructurePiece structurepiece : structurestart.getPieces()) {
                    if (structurepiece.isCloseToChunk(chunkPos, 12)) {
                        if (structurepiece instanceof net.neoforged.neoforge.common.world.PieceBeardifierModifier pieceBeardifierModifier) {
                            if (pieceBeardifierModifier.getTerrainAdjustment() != TerrainAdjustment.NONE) {
                                list1.add(new Beardifier.Rigid(pieceBeardifierModifier.getBeardifierBox(), pieceBeardifierModifier.getTerrainAdjustment(), pieceBeardifierModifier.getGroundLevelDelta()));
                            }
                        } else
                        if (structurepiece instanceof PoolElementStructurePiece poolelementstructurepiece) {
                            StructureTemplatePool.Projection structuretemplatepool$projection = poolelementstructurepiece.getElement().getProjection();
                            if (structuretemplatepool$projection == StructureTemplatePool.Projection.RIGID) {
                                list1.add(
                                    new Beardifier.Rigid(
                                        poolelementstructurepiece.getBoundingBox(), terrainadjustment, poolelementstructurepiece.getGroundLevelDelta()
                                    )
                                );
                                boundingbox = includeBoundingBox(boundingbox, structurepiece.getBoundingBox());
                            }

                            for (JigsawJunction jigsawjunction : poolelementstructurepiece.getJunctions()) {
                                int k = jigsawjunction.getSourceX();
                                int l = jigsawjunction.getSourceZ();
                                if (k > i - 12 && l > j - 12 && k < i + 15 + 12 && l < j + 15 + 12) {
                                    list2.add(jigsawjunction);
                                    BoundingBox boundingbox1 = new BoundingBox(new BlockPos(k, jigsawjunction.getSourceGroundY(), l));
                                    boundingbox = includeBoundingBox(boundingbox, boundingbox1);
                                }
                            }
                        } else {
                            list1.add(new Beardifier.Rigid(structurepiece.getBoundingBox(), terrainadjustment, 0));
                            boundingbox = includeBoundingBox(boundingbox, structurepiece.getBoundingBox());
                        }
                    }
                }
            }

            if (boundingbox == null) {
                return EMPTY;
            } else {
                BoundingBox boundingbox2 = boundingbox.inflatedBy(24);
                return new Beardifier(List.copyOf(list1), List.copyOf(list2), boundingbox2);
            }
        }
    }

    private static BoundingBox includeBoundingBox(@Nullable BoundingBox current, BoundingBox toInclude) {
        return current == null ? toInclude : BoundingBox.encapsulating(current, toInclude);
    }

    @VisibleForTesting
    public Beardifier(List<Beardifier.Rigid> pieces, List<JigsawJunction> junctions, @Nullable BoundingBox affectedBox) {
        this.pieces = pieces;
        this.junctions = junctions;
        this.affectedBox = affectedBox;
    }

    @Override
    public void fillArray(double[] array, DensityFunction.ContextProvider contextProvider) {
        if (this.affectedBox == null) {
            Arrays.fill(array, 0.0);
        } else {
            DensityFunctions.BeardifierOrMarker.super.fillArray(array, contextProvider);
        }
    }

    @Override
    public double compute(DensityFunction.FunctionContext context) {
        if (this.affectedBox == null) {
            return 0.0;
        } else {
            int i = context.blockX();
            int j = context.blockY();
            int k = context.blockZ();
            if (!this.affectedBox.isInside(i, j, k)) {
                return 0.0;
            } else {
                double d0 = 0.0;

                for (Beardifier.Rigid beardifier$rigid : this.pieces) {
                    BoundingBox boundingbox = beardifier$rigid.box();
                    int l = beardifier$rigid.groundLevelDelta();
                    int i1 = Math.max(0, Math.max(boundingbox.minX() - i, i - boundingbox.maxX()));
                    int j1 = Math.max(0, Math.max(boundingbox.minZ() - k, k - boundingbox.maxZ()));
                    int k1 = boundingbox.minY() + l;
                    int l1 = j - k1;

                    int i2 = switch (beardifier$rigid.terrainAdjustment()) {
                        case NONE -> 0;
                        case BURY, BEARD_THIN -> l1;
                        case BEARD_BOX -> Math.max(0, Math.max(k1 - j, j - boundingbox.maxY()));
                        case ENCAPSULATE -> Math.max(0, Math.max(boundingbox.minY() - j, j - boundingbox.maxY()));
                    };

                    d0 += switch (beardifier$rigid.terrainAdjustment()) {
                        case NONE -> 0.0;
                        case BURY -> getBuryContribution(i1, i2 / 2.0, j1);
                        case BEARD_THIN, BEARD_BOX -> getBeardContribution(i1, i2, j1, l1) * 0.8;
                        case ENCAPSULATE -> getBuryContribution(i1 / 2.0, i2 / 2.0, j1 / 2.0) * 0.8;
                    };
                }

                for (JigsawJunction jigsawjunction : this.junctions) {
                    int j2 = i - jigsawjunction.getSourceX();
                    int k2 = j - jigsawjunction.getSourceGroundY();
                    int l2 = k - jigsawjunction.getSourceZ();
                    d0 += getBeardContribution(j2, k2, l2, k2) * 0.4;
                }

                return d0;
            }
        }
    }

    @Override
    public double minValue() {
        return Double.NEGATIVE_INFINITY;
    }

    @Override
    public double maxValue() {
        return Double.POSITIVE_INFINITY;
    }

    protected static double getBuryContribution(double x, double y, double z) {
        double d0 = Mth.length(x, y, z);
        return Mth.clampedMap(d0, 0.0, 6.0, 1.0, 0.0);
    }

    protected static double getBeardContribution(int x, int y, int z, int height) {
        int i = x + 12;
        int j = y + 12;
        int k = z + 12;
        if (isInKernelRange(i) && isInKernelRange(j) && isInKernelRange(k)) {
            double d0 = height + 0.5;
            double d1 = Mth.lengthSquared((double)x, d0, (double)z);
            double d2 = -d0 * Mth.fastInvSqrt(d1 / 2.0) / 2.0;
            return d2 * BEARD_KERNEL[k * 24 * 24 + i * 24 + j];
        } else {
            return 0.0;
        }
    }

    private static boolean isInKernelRange(int value) {
        return value >= 0 && value < 24;
    }

    private static double computeBeardContribution(int x, int y, int z) {
        return computeBeardContribution(x, y + 0.5, z);
    }

    private static double computeBeardContribution(int x, double y, int z) {
        double d0 = Mth.lengthSquared((double)x, y, (double)z);
        return Math.pow(Math.E, -d0 / 16.0);
    }

    @VisibleForTesting
    public record Rigid(BoundingBox box, TerrainAdjustment terrainAdjustment, int groundLevelDelta) {
    }
}
