package net.minecraft.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.longs.Long2FloatLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;
import java.util.List;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ModelBlockRenderer {
    private static final Direction[] DIRECTIONS = Direction.values();
    private final BlockColors blockColors;
    private static final int CACHE_SIZE = 100;
    protected static final ThreadLocal<ModelBlockRenderer.Cache> CACHE = ThreadLocal.withInitial(ModelBlockRenderer.Cache::new);

    public ModelBlockRenderer(BlockColors blockColors) {
        this.blockColors = blockColors;
    }

    @Deprecated // Neo: Buffer lookup parameter
    public void tesselateBlock(
        BlockAndTintGetter level,
        List<BlockModelPart> parts,
        BlockState state,
        BlockPos pos,
        PoseStack poseStack,
        VertexConsumer consumer,
        boolean checkSides,
        int packedOverlay
    ) {
        tesselateBlock(level, parts, state, pos, poseStack, type -> consumer, checkSides, packedOverlay);
    }

    public void tesselateBlock(
        BlockAndTintGetter p_234380_,
        List<BlockModelPart> p_410025_,
        BlockState p_234382_,
        BlockPos p_234383_,
        PoseStack p_234384_,
        java.util.function.Function<net.minecraft.client.renderer.chunk.ChunkSectionLayer, VertexConsumer> bufferLookup,
        boolean p_234386_,
        int p_234389_
    ) {
        if (!p_410025_.isEmpty()) {
            boolean perPartAO = net.neoforged.neoforge.client.config.NeoForgeClientConfig.INSTANCE.handleAmbientOcclusionPerPart.getAsBoolean();
            boolean flag = Minecraft.useAmbientOcclusion() && (perPartAO || switch(p_410025_.getFirst().ambientOcclusion()) {
                case TRUE -> true;
                case DEFAULT -> p_234382_.getLightEmission(p_234380_, p_234383_) == 0;
                case FALSE -> false;
            });
            p_234384_.translate(p_234382_.getOffset(p_234383_));

            try {
                if (flag) {
                    this.tesselateWithAO(p_234380_, p_410025_, p_234382_, p_234383_, p_234384_, bufferLookup, p_234386_, p_234389_);
                } else {
                    this.tesselateWithoutAO(p_234380_, p_410025_, p_234382_, p_234383_, p_234384_, bufferLookup, p_234386_, p_234389_);
                }
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.forThrowable(throwable, "Tesselating block model");
                CrashReportCategory crashreportcategory = crashreport.addCategory("Block model being tesselated");
                CrashReportCategory.populateBlockDetails(crashreportcategory, p_234380_, p_234383_, p_234382_);
                crashreportcategory.setDetail("Using AO", flag);
                throw new ReportedException(crashreport);
            }
        }
    }

    /**
 * @deprecated Neo: use {@link #shouldRenderFace(BlockAndTintGetter, BlockPos,
 *             BlockState, boolean, Direction, BlockPos)} instead
 */
    @Deprecated
    private static boolean shouldRenderFace(BlockAndTintGetter level, BlockState state, boolean checkSides, Direction face, BlockPos pos) {
        if (!checkSides) {
            return true;
        } else {
            BlockState blockstate = level.getBlockState(pos);
            return Block.shouldRenderFace(state, blockstate, face);
        }
    }

    protected static boolean shouldRenderFace(BlockAndTintGetter p_412640_, BlockPos pos, BlockState p_412168_, boolean p_412054_, Direction p_412130_, BlockPos p_412608_) {
        if (!p_412054_) {
            return true;
        } else {
            BlockState blockstate = p_412640_.getBlockState(p_412608_);
            return Block.shouldRenderFace(p_412640_, pos, p_412168_, blockstate, p_412130_);
        }
    }

    @Deprecated // Neo: Buffer lookup parameter
    public void tesselateWithAO(
        BlockAndTintGetter level,
        List<BlockModelPart> parts,
        BlockState state,
        BlockPos pos,
        PoseStack poseStack,
        VertexConsumer consumer,
        boolean checkSides,
        int packedOverlay
    ) {
        this.tesselateWithAO(level, parts, state, pos, poseStack, type -> consumer, checkSides, packedOverlay);
    }

    public void tesselateWithAO(
        BlockAndTintGetter p_234391_,
        List<BlockModelPart> p_410478_,
        BlockState p_234393_,
        BlockPos p_234394_,
        PoseStack p_234395_,
        java.util.function.Function<net.minecraft.client.renderer.chunk.ChunkSectionLayer, VertexConsumer> bufferLookup,
        boolean p_234397_,
        int p_234400_
    ) {
        // Neo: Inject enhanced AO pipeline
        ModelBlockRenderer.AmbientOcclusionRenderStorage modelblockrenderer$ambientocclusionrenderstorage = net.neoforged.neoforge.client.model.ao.EnhancedAoRenderStorage.newInstance();
        boolean perPartAO = net.neoforged.neoforge.client.config.NeoForgeClientConfig.INSTANCE.handleAmbientOcclusionPerPart.getAsBoolean();
        int lightEmission = -1;
        int i = 0;
        int j = 0;

        for (BlockModelPart blockmodelpart : p_410478_) {
            VertexConsumer p_234396_ = bufferLookup.apply(blockmodelpart.getRenderType(p_234393_));
            boolean ao = !perPartAO || switch (blockmodelpart.ambientOcclusion()) {
                case TRUE -> true;
                case DEFAULT -> {
                    if (lightEmission == -1) {
                        lightEmission = p_234393_.getLightEmission(p_234391_, p_234394_);
                    }
                    yield lightEmission == 0;
                }
                case FALSE -> false;
            };
            for (Direction direction : DIRECTIONS) {
                int k = 1 << direction.ordinal();
                boolean flag = (i & k) == 1;
                boolean flag1 = (j & k) == 1;
                if (!flag || flag1) {
                    List<BakedQuad> list = blockmodelpart.getQuads(direction);
                    if (!list.isEmpty()) {
                        if (!flag) {
                            flag1 = shouldRenderFace(
                                p_234391_,
                                p_234394_,
                                p_234393_,
                                p_234397_,
                                direction,
                                modelblockrenderer$ambientocclusionrenderstorage.scratchPos.setWithOffset(p_234394_, direction)
                            );
                            i |= k;
                            if (flag1) {
                                j |= k;
                            }
                        }

                        if (flag1) {
                            if (!ao) {
                                int light = modelblockrenderer$ambientocclusionrenderstorage.cache.getLightColor(p_234393_, p_234391_, modelblockrenderer$ambientocclusionrenderstorage.scratchPos.setWithOffset(p_234394_, direction));
                                this.renderModelFaceFlat(
                                        p_234391_, p_234393_, p_234394_, light, p_234400_, false, p_234395_, p_234396_, list, modelblockrenderer$ambientocclusionrenderstorage
                                );
                            } else
                            this.renderModelFaceAO(
                                p_234391_, p_234393_, p_234394_, p_234395_, p_234396_, list, modelblockrenderer$ambientocclusionrenderstorage, p_234400_
                            );
                        }
                    }
                }
            }

            List<BakedQuad> list1 = blockmodelpart.getQuads(null);
            if (!list1.isEmpty()) {
                if (!ao) {
                    this.renderModelFaceFlat(
                            p_234391_, p_234393_, p_234394_, -1, p_234400_, true, p_234395_, p_234396_, list1, modelblockrenderer$ambientocclusionrenderstorage
                    );
                } else
                this.renderModelFaceAO(
                    p_234391_, p_234393_, p_234394_, p_234395_, p_234396_, list1, modelblockrenderer$ambientocclusionrenderstorage, p_234400_
                );
            }
        }
    }

    @Deprecated // Neo: Buffer lookup parameter
    public void tesselateWithoutAO(
        BlockAndTintGetter level,
        List<BlockModelPart> parts,
        BlockState state,
        BlockPos pos,
        PoseStack poseStack,
        VertexConsumer consumer,
        boolean checkSides,
        int packedOverlay
    ) {
        this.tesselateWithoutAO(level, parts, state, pos, poseStack, type -> consumer, checkSides, packedOverlay);
    }

    public void tesselateWithoutAO(
        BlockAndTintGetter p_234402_,
        List<BlockModelPart> p_410604_,
        BlockState p_234404_,
        BlockPos p_234405_,
        PoseStack p_234406_,
        java.util.function.Function<net.minecraft.client.renderer.chunk.ChunkSectionLayer, VertexConsumer> bufferLookup,
        boolean p_234408_,
        int p_234411_
    ) {
        ModelBlockRenderer.CommonRenderStorage modelblockrenderer$commonrenderstorage = new ModelBlockRenderer.CommonRenderStorage();
        int i = 0;
        int j = 0;

        for (BlockModelPart blockmodelpart : p_410604_) {
            VertexConsumer p_234407_ = bufferLookup.apply(blockmodelpart.getRenderType(p_234404_));
            for (Direction direction : DIRECTIONS) {
                int k = 1 << direction.ordinal();
                boolean flag = (i & k) == 1;
                boolean flag1 = (j & k) == 1;
                if (!flag || flag1) {
                    List<BakedQuad> list = blockmodelpart.getQuads(direction);
                    if (!list.isEmpty()) {
                        BlockPos blockpos = modelblockrenderer$commonrenderstorage.scratchPos.setWithOffset(p_234405_, direction);
                        if (!flag) {
                            flag1 = shouldRenderFace(p_234402_, p_234405_, p_234404_, p_234408_, direction, blockpos);
                            i |= k;
                            if (flag1) {
                                j |= k;
                            }
                        }

                        if (flag1) {
                            int l = modelblockrenderer$commonrenderstorage.cache.getLightColor(p_234404_, p_234402_, blockpos);
                            this.renderModelFaceFlat(
                                p_234402_, p_234404_, p_234405_, l, p_234411_, false, p_234406_, p_234407_, list, modelblockrenderer$commonrenderstorage
                            );
                        }
                    }
                }
            }

            List<BakedQuad> list1 = blockmodelpart.getQuads(null);
            if (!list1.isEmpty()) {
                this.renderModelFaceFlat(
                    p_234402_, p_234404_, p_234405_, -1, p_234411_, true, p_234406_, p_234407_, list1, modelblockrenderer$commonrenderstorage
                );
            }
        }
    }

    private void renderModelFaceAO(
        BlockAndTintGetter level,
        BlockState state,
        BlockPos pos,
        PoseStack poseStack,
        VertexConsumer consumer,
        List<BakedQuad> quads,
        ModelBlockRenderer.AmbientOcclusionRenderStorage renderStorage,
        int packedOverlay
    ) {
        for (BakedQuad bakedquad : quads) {
            if (!bakedquad.hasAmbientOcclusion()) {
                renderModelQuadFlat(level, state, pos, -1, packedOverlay, true, poseStack, consumer, bakedquad, renderStorage);
                continue;
            }
            calculateShape(level, state, pos, bakedquad.vertices(), bakedquad.direction(), renderStorage);
            renderStorage.captureQuad(bakedquad);
            renderStorage.calculate(level, state, pos, bakedquad.direction(), bakedquad.shade());
            this.putQuadData(level, state, pos, consumer, poseStack.last(), bakedquad, renderStorage, packedOverlay);
        }
    }

    private void putQuadData(
        BlockAndTintGetter level,
        BlockState state,
        BlockPos pos,
        VertexConsumer consumer,
        PoseStack.Pose pose,
        BakedQuad quad,
        ModelBlockRenderer.CommonRenderStorage renderStorage,
        int packedOverlay
    ) {
        int i = quad.tintIndex();
        float f;
        float f1;
        float f2;
        if (i != -1) {
            int j;
            if (renderStorage.tintCacheIndex == i) {
                j = renderStorage.tintCacheValue;
            } else {
                j = this.blockColors.getColor(state, level, pos, i);
                renderStorage.tintCacheIndex = i;
                renderStorage.tintCacheValue = j;
            }

            f = ARGB.redFloat(j);
            f1 = ARGB.greenFloat(j);
            f2 = ARGB.blueFloat(j);
        } else {
            f = 1.0F;
            f1 = 1.0F;
            f2 = 1.0F;
        }

        consumer.putBulkData(pose, quad, renderStorage.brightness, f, f1, f2, 1.0F, renderStorage.lightmap, packedOverlay, true);
    }

    private static void calculateShape(
        BlockAndTintGetter level,
        BlockState state,
        BlockPos pos,
        int[] vertices,
        Direction direction,
        ModelBlockRenderer.CommonRenderStorage renderStorage
    ) {
        float f = 32.0F;
        float f1 = 32.0F;
        float f2 = 32.0F;
        float f3 = -32.0F;
        float f4 = -32.0F;
        float f5 = -32.0F;

        for (int i = 0; i < 4; i++) {
            float f6 = Float.intBitsToFloat(vertices[i * 8]);
            float f7 = Float.intBitsToFloat(vertices[i * 8 + 1]);
            float f8 = Float.intBitsToFloat(vertices[i * 8 + 2]);
            f = Math.min(f, f6);
            f1 = Math.min(f1, f7);
            f2 = Math.min(f2, f8);
            f3 = Math.max(f3, f6);
            f4 = Math.max(f4, f7);
            f5 = Math.max(f5, f8);
        }

        if (renderStorage instanceof ModelBlockRenderer.AmbientOcclusionRenderStorage modelblockrenderer$ambientocclusionrenderstorage) {
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.WEST.index] = f;
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.EAST.index] = f3;
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.DOWN.index] = f1;
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.UP.index] = f4;
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.NORTH.index] = f2;
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.SOUTH.index] = f5;
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.FLIP_WEST.index] = 1.0F - f;
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.FLIP_EAST.index] = 1.0F - f3;
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.FLIP_DOWN.index] = 1.0F - f1;
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.FLIP_UP.index] = 1.0F - f4;
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.FLIP_NORTH.index] = 1.0F - f2;
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.FLIP_SOUTH.index] = 1.0F - f5;
        }

        float f9 = 1.0E-4F;
        float f10 = 0.9999F;

        renderStorage.facePartial = switch (direction) {
            case DOWN, UP -> f >= 1.0E-4F || f2 >= 1.0E-4F || f3 <= 0.9999F || f5 <= 0.9999F;
            case NORTH, SOUTH -> f >= 1.0E-4F || f1 >= 1.0E-4F || f3 <= 0.9999F || f4 <= 0.9999F;
            case WEST, EAST -> f1 >= 1.0E-4F || f2 >= 1.0E-4F || f4 <= 0.9999F || f5 <= 0.9999F;
        };

        renderStorage.faceCubic = switch (direction) {
            case DOWN -> f1 == f4 && (f1 < 1.0E-4F || state.isCollisionShapeFullBlock(level, pos));
            case UP -> f1 == f4 && (f4 > 0.9999F || state.isCollisionShapeFullBlock(level, pos));
            case NORTH -> f2 == f5 && (f2 < 1.0E-4F || state.isCollisionShapeFullBlock(level, pos));
            case SOUTH -> f2 == f5 && (f5 > 0.9999F || state.isCollisionShapeFullBlock(level, pos));
            case WEST -> f == f3 && (f < 1.0E-4F || state.isCollisionShapeFullBlock(level, pos));
            case EAST -> f == f3 && (f3 > 0.9999F || state.isCollisionShapeFullBlock(level, pos));
        };
    }

    /**
     * @param repackLight {@code true} if packed light should be re-calculated
     */
    private void renderModelFaceFlat(
        BlockAndTintGetter level,
        BlockState state,
        BlockPos pos,
        int packedLight,
        int packedOverlay,
        boolean repackLight,
        PoseStack poseStack,
        VertexConsumer consumer,
        List<BakedQuad> quads,
        ModelBlockRenderer.CommonRenderStorage renderStorage
    ) {
        for (BakedQuad bakedquad : quads) {
            renderModelQuadFlat(level, state, pos, packedLight, packedOverlay, repackLight, poseStack, consumer, bakedquad, renderStorage);
        }
    }

    private void renderModelQuadFlat(
        BlockAndTintGetter p_111002_,
        BlockState p_111003_,
        BlockPos p_111004_,
        int p_111005_,
        int p_111006_,
        boolean p_111007_,
        PoseStack p_111008_,
        VertexConsumer p_111009_,
        BakedQuad bakedquad,
        ModelBlockRenderer.CommonRenderStorage p_412163_
    ) {
        {
            if (p_111007_) {
                calculateShape(p_111002_, p_111003_, p_111004_, bakedquad.vertices(), bakedquad.direction(), p_412163_);
                BlockPos blockpos = (BlockPos)(p_412163_.faceCubic ? p_412163_.scratchPos.setWithOffset(p_111004_, bakedquad.direction()) : p_111004_);
                p_111005_ = p_412163_.cache.getLightColor(p_111003_, p_111002_, blockpos);
            }

            net.neoforged.neoforge.client.model.ao.EnhancedAoRenderStorage.applyFlatQuadBrightness(p_111002_, bakedquad, p_412163_);
            p_412163_.lightmap[0] = p_111005_;
            p_412163_.lightmap[1] = p_111005_;
            p_412163_.lightmap[2] = p_111005_;
            p_412163_.lightmap[3] = p_111005_;
            this.putQuadData(p_111002_, p_111003_, p_111004_, p_111009_, p_111008_.last(), bakedquad, p_412163_, p_111006_);
        }
    }

    @Deprecated // Neo: Level, position, and buffer source parameters
    public static void renderModel(
        PoseStack.Pose pose,
        VertexConsumer consumer,
        BlockStateModel model,
        float red,
        float green,
        float blue,
        int packedLight,
        int packedOverlay
    ) {
        renderModel(pose, type -> consumer, model, red, green, blue, packedLight, packedOverlay, net.minecraft.world.level.EmptyBlockAndTintGetter.INSTANCE, BlockPos.ZERO, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
    }

    public static void renderModel(
        PoseStack.Pose p_111068_,
        net.minecraft.client.renderer.MultiBufferSource bufferSource,
        BlockStateModel p_405848_,
        float p_111072_,
        float p_111073_,
        float p_111074_,
        int p_111075_,
        int p_111076_,
        net.minecraft.world.level.BlockAndTintGetter level,
        BlockPos pos,
        BlockState state
    ) {
        for (BlockModelPart blockmodelpart : p_405848_.collectParts(level, pos, state, RandomSource.create(42L))) {
            VertexConsumer p_111069_ = bufferSource.getBuffer(net.neoforged.neoforge.client.RenderTypeHelper.getEntityRenderType(blockmodelpart.getRenderType(state)));
            for (Direction direction : DIRECTIONS) {
                renderQuadList(p_111068_, p_111069_, p_111072_, p_111073_, p_111074_, blockmodelpart.getQuads(direction), p_111075_, p_111076_);
            }

            renderQuadList(p_111068_, p_111069_, p_111072_, p_111073_, p_111074_, blockmodelpart.getQuads(null), p_111075_, p_111076_);
        }
    }

    private static void renderQuadList(
        PoseStack.Pose pose,
        VertexConsumer consumer,
        float red,
        float green,
        float blue,
        List<BakedQuad> quads,
        int packedLight,
        int packedOverlay
    ) {
        for (BakedQuad bakedquad : quads) {
            float f;
            float f1;
            float f2;
            if (bakedquad.isTinted()) {
                f = Mth.clamp(red, 0.0F, 1.0F);
                f1 = Mth.clamp(green, 0.0F, 1.0F);
                f2 = Mth.clamp(blue, 0.0F, 1.0F);
            } else {
                f = 1.0F;
                f1 = 1.0F;
                f2 = 1.0F;
            }

            consumer.putBulkData(pose, bakedquad, f, f1, f2, 1.0F, packedLight, packedOverlay);
        }
    }

    public static void enableCaching() {
        CACHE.get().enable();
    }

    public static void clearCache() {
        CACHE.get().disable();
    }

    @OnlyIn(Dist.CLIENT)
    public static enum AdjacencyInfo {
        DOWN(
            new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH},
            0.5F,
            true,
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.SOUTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.NORTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.NORTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.SOUTH
            }
        ),
        UP(
            new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH},
            1.0F,
            true,
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.SOUTH,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.SOUTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.NORTH,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.NORTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.NORTH,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.NORTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.SOUTH,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.SOUTH
            }
        ),
        NORTH(
            new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST},
            0.8F,
            true,
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.FLIP_WEST
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.FLIP_EAST
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_EAST
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_WEST
            }
        ),
        SOUTH(
            new Direction[]{Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP},
            0.8F,
            true,
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.WEST
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.WEST
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.EAST
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.EAST
            }
        ),
        WEST(
            new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH},
            0.6F,
            true,
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.SOUTH,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.SOUTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.NORTH,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.NORTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.NORTH,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.NORTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.SOUTH,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.SOUTH
            }
        ),
        EAST(
            new Direction[]{Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH},
            0.6F,
            true,
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.SOUTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.NORTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.NORTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.SOUTH
            }
        );

        public final Direction[] corners;
        final boolean doNonCubicWeight;
        final ModelBlockRenderer.SizeInfo[] vert0Weights;
        final ModelBlockRenderer.SizeInfo[] vert1Weights;
        final ModelBlockRenderer.SizeInfo[] vert2Weights;
        final ModelBlockRenderer.SizeInfo[] vert3Weights;
        private static final ModelBlockRenderer.AdjacencyInfo[] BY_FACING = Util.make(new ModelBlockRenderer.AdjacencyInfo[6], p_111134_ -> {
            p_111134_[Direction.DOWN.get3DDataValue()] = DOWN;
            p_111134_[Direction.UP.get3DDataValue()] = UP;
            p_111134_[Direction.NORTH.get3DDataValue()] = NORTH;
            p_111134_[Direction.SOUTH.get3DDataValue()] = SOUTH;
            p_111134_[Direction.WEST.get3DDataValue()] = WEST;
            p_111134_[Direction.EAST.get3DDataValue()] = EAST;
        });

        /**
         * @param shadeBrightness the shade brightness for this direction
         */
        private AdjacencyInfo(
            Direction[] corners,
            float shadeBrightness,
            boolean doNonCubicWeight,
            ModelBlockRenderer.SizeInfo[] vert0Weights,
            ModelBlockRenderer.SizeInfo[] vert1Weights,
            ModelBlockRenderer.SizeInfo[] vert2Weights,
            ModelBlockRenderer.SizeInfo[] vert3Weights
        ) {
            this.corners = corners;
            this.doNonCubicWeight = doNonCubicWeight;
            this.vert0Weights = vert0Weights;
            this.vert1Weights = vert1Weights;
            this.vert2Weights = vert2Weights;
            this.vert3Weights = vert3Weights;
        }

        public static ModelBlockRenderer.AdjacencyInfo fromFacing(Direction facing) {
            return BY_FACING[facing.get3DDataValue()];
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class AmbientOcclusionRenderStorage extends ModelBlockRenderer.CommonRenderStorage {
        protected final float[] faceShape = new float[ModelBlockRenderer.SizeInfo.COUNT];

        public AmbientOcclusionRenderStorage() {
        }

        // Neo: Call this before calling calculate if the render storage is an EnhancedAoRenderStorage!
        public void captureQuad(BakedQuad quad) {}

        public void calculate(BlockAndTintGetter level, BlockState state, BlockPos pos, Direction direction, boolean shade) {
            BlockPos blockpos = this.faceCubic ? pos.relative(direction) : pos;
            ModelBlockRenderer.AdjacencyInfo modelblockrenderer$adjacencyinfo = ModelBlockRenderer.AdjacencyInfo.fromFacing(direction);
            BlockPos.MutableBlockPos blockpos$mutableblockpos = this.scratchPos;
            blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[0]);
            BlockState blockstate = level.getBlockState(blockpos$mutableblockpos);
            int i = this.cache.getLightColor(blockstate, level, blockpos$mutableblockpos);
            float f = this.cache.getShadeBrightness(blockstate, level, blockpos$mutableblockpos);
            blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[1]);
            BlockState blockstate1 = level.getBlockState(blockpos$mutableblockpos);
            int j = this.cache.getLightColor(blockstate1, level, blockpos$mutableblockpos);
            float f1 = this.cache.getShadeBrightness(blockstate1, level, blockpos$mutableblockpos);
            blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[2]);
            BlockState blockstate2 = level.getBlockState(blockpos$mutableblockpos);
            int k = this.cache.getLightColor(blockstate2, level, blockpos$mutableblockpos);
            float f2 = this.cache.getShadeBrightness(blockstate2, level, blockpos$mutableblockpos);
            blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[3]);
            BlockState blockstate3 = level.getBlockState(blockpos$mutableblockpos);
            int l = this.cache.getLightColor(blockstate3, level, blockpos$mutableblockpos);
            float f3 = this.cache.getShadeBrightness(blockstate3, level, blockpos$mutableblockpos);
            BlockState blockstate4 = level.getBlockState(
                blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[0]).move(direction)
            );
            boolean flag = !blockstate4.isViewBlocking(level, blockpos$mutableblockpos) || blockstate4.getLightBlock() == 0;
            BlockState blockstate5 = level.getBlockState(
                blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[1]).move(direction)
            );
            boolean flag1 = !blockstate5.isViewBlocking(level, blockpos$mutableblockpos) || blockstate5.getLightBlock() == 0;
            BlockState blockstate6 = level.getBlockState(
                blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[2]).move(direction)
            );
            boolean flag2 = !blockstate6.isViewBlocking(level, blockpos$mutableblockpos) || blockstate6.getLightBlock() == 0;
            BlockState blockstate7 = level.getBlockState(
                blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[3]).move(direction)
            );
            boolean flag3 = !blockstate7.isViewBlocking(level, blockpos$mutableblockpos) || blockstate7.getLightBlock() == 0;
            float f4;
            int i1;
            if (!flag2 && !flag) {
                f4 = f;
                i1 = i;
            } else {
                blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[0]).move(modelblockrenderer$adjacencyinfo.corners[2]);
                BlockState blockstate8 = level.getBlockState(blockpos$mutableblockpos);
                f4 = this.cache.getShadeBrightness(blockstate8, level, blockpos$mutableblockpos);
                i1 = this.cache.getLightColor(blockstate8, level, blockpos$mutableblockpos);
            }

            float f5;
            int j1;
            if (!flag3 && !flag) {
                f5 = f;
                j1 = i;
            } else {
                blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[0]).move(modelblockrenderer$adjacencyinfo.corners[3]);
                BlockState blockstate10 = level.getBlockState(blockpos$mutableblockpos);
                f5 = this.cache.getShadeBrightness(blockstate10, level, blockpos$mutableblockpos);
                j1 = this.cache.getLightColor(blockstate10, level, blockpos$mutableblockpos);
            }

            float f6;
            int k1;
            if (!flag2 && !flag1) {
                f6 = f;
                k1 = i;
            } else {
                blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[1]).move(modelblockrenderer$adjacencyinfo.corners[2]);
                BlockState blockstate11 = level.getBlockState(blockpos$mutableblockpos);
                f6 = this.cache.getShadeBrightness(blockstate11, level, blockpos$mutableblockpos);
                k1 = this.cache.getLightColor(blockstate11, level, blockpos$mutableblockpos);
            }

            float f7;
            int l1;
            if (!flag3 && !flag1) {
                f7 = f;
                l1 = i;
            } else {
                blockpos$mutableblockpos.setWithOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[1]).move(modelblockrenderer$adjacencyinfo.corners[3]);
                BlockState blockstate12 = level.getBlockState(blockpos$mutableblockpos);
                f7 = this.cache.getShadeBrightness(blockstate12, level, blockpos$mutableblockpos);
                l1 = this.cache.getLightColor(blockstate12, level, blockpos$mutableblockpos);
            }

            int i3 = this.cache.getLightColor(state, level, pos);
            blockpos$mutableblockpos.setWithOffset(pos, direction);
            BlockState blockstate9 = level.getBlockState(blockpos$mutableblockpos);
            if (this.faceCubic || !blockstate9.isSolidRender()) {
                i3 = this.cache.getLightColor(blockstate9, level, blockpos$mutableblockpos);
            }

            float f8 = this.faceCubic
                ? this.cache.getShadeBrightness(level.getBlockState(blockpos), level, blockpos)
                : this.cache.getShadeBrightness(level.getBlockState(pos), level, pos);
            ModelBlockRenderer.AmbientVertexRemap modelblockrenderer$ambientvertexremap = ModelBlockRenderer.AmbientVertexRemap.fromFacing(direction);
            if (this.facePartial && modelblockrenderer$adjacencyinfo.doNonCubicWeight) {
                float f29 = (f3 + f + f5 + f8) * 0.25F;
                float f31 = (f2 + f + f4 + f8) * 0.25F;
                float f32 = (f2 + f1 + f6 + f8) * 0.25F;
                float f33 = (f3 + f1 + f7 + f8) * 0.25F;
                float f13 = this.faceShape[modelblockrenderer$adjacencyinfo.vert0Weights[0].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert0Weights[1].index];
                float f14 = this.faceShape[modelblockrenderer$adjacencyinfo.vert0Weights[2].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert0Weights[3].index];
                float f15 = this.faceShape[modelblockrenderer$adjacencyinfo.vert0Weights[4].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert0Weights[5].index];
                float f16 = this.faceShape[modelblockrenderer$adjacencyinfo.vert0Weights[6].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert0Weights[7].index];
                float f17 = this.faceShape[modelblockrenderer$adjacencyinfo.vert1Weights[0].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert1Weights[1].index];
                float f18 = this.faceShape[modelblockrenderer$adjacencyinfo.vert1Weights[2].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert1Weights[3].index];
                float f19 = this.faceShape[modelblockrenderer$adjacencyinfo.vert1Weights[4].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert1Weights[5].index];
                float f20 = this.faceShape[modelblockrenderer$adjacencyinfo.vert1Weights[6].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert1Weights[7].index];
                float f21 = this.faceShape[modelblockrenderer$adjacencyinfo.vert2Weights[0].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert2Weights[1].index];
                float f22 = this.faceShape[modelblockrenderer$adjacencyinfo.vert2Weights[2].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert2Weights[3].index];
                float f23 = this.faceShape[modelblockrenderer$adjacencyinfo.vert2Weights[4].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert2Weights[5].index];
                float f24 = this.faceShape[modelblockrenderer$adjacencyinfo.vert2Weights[6].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert2Weights[7].index];
                float f25 = this.faceShape[modelblockrenderer$adjacencyinfo.vert3Weights[0].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert3Weights[1].index];
                float f26 = this.faceShape[modelblockrenderer$adjacencyinfo.vert3Weights[2].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert3Weights[3].index];
                float f27 = this.faceShape[modelblockrenderer$adjacencyinfo.vert3Weights[4].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert3Weights[5].index];
                float f28 = this.faceShape[modelblockrenderer$adjacencyinfo.vert3Weights[6].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert3Weights[7].index];
                this.brightness[modelblockrenderer$ambientvertexremap.vert0] = Math.clamp(f29 * f13 + f31 * f14 + f32 * f15 + f33 * f16, 0.0F, 1.0F);
                this.brightness[modelblockrenderer$ambientvertexremap.vert1] = Math.clamp(f29 * f17 + f31 * f18 + f32 * f19 + f33 * f20, 0.0F, 1.0F);
                this.brightness[modelblockrenderer$ambientvertexremap.vert2] = Math.clamp(f29 * f21 + f31 * f22 + f32 * f23 + f33 * f24, 0.0F, 1.0F);
                this.brightness[modelblockrenderer$ambientvertexremap.vert3] = Math.clamp(f29 * f25 + f31 * f26 + f32 * f27 + f33 * f28, 0.0F, 1.0F);
                int i2 = blend(l, i, j1, i3);
                int j2 = blend(k, i, i1, i3);
                int k2 = blend(k, j, k1, i3);
                int l2 = blend(l, j, l1, i3);
                this.lightmap[modelblockrenderer$ambientvertexremap.vert0] = blend(i2, j2, k2, l2, f13, f14, f15, f16);
                this.lightmap[modelblockrenderer$ambientvertexremap.vert1] = blend(i2, j2, k2, l2, f17, f18, f19, f20);
                this.lightmap[modelblockrenderer$ambientvertexremap.vert2] = blend(i2, j2, k2, l2, f21, f22, f23, f24);
                this.lightmap[modelblockrenderer$ambientvertexremap.vert3] = blend(i2, j2, k2, l2, f25, f26, f27, f28);
            } else {
                float f9 = (f3 + f + f5 + f8) * 0.25F;
                float f10 = (f2 + f + f4 + f8) * 0.25F;
                float f11 = (f2 + f1 + f6 + f8) * 0.25F;
                float f12 = (f3 + f1 + f7 + f8) * 0.25F;
                this.lightmap[modelblockrenderer$ambientvertexremap.vert0] = blend(l, i, j1, i3);
                this.lightmap[modelblockrenderer$ambientvertexremap.vert1] = blend(k, i, i1, i3);
                this.lightmap[modelblockrenderer$ambientvertexremap.vert2] = blend(k, j, k1, i3);
                this.lightmap[modelblockrenderer$ambientvertexremap.vert3] = blend(l, j, l1, i3);
                this.brightness[modelblockrenderer$ambientvertexremap.vert0] = f9;
                this.brightness[modelblockrenderer$ambientvertexremap.vert1] = f10;
                this.brightness[modelblockrenderer$ambientvertexremap.vert2] = f11;
                this.brightness[modelblockrenderer$ambientvertexremap.vert3] = f12;
            }

            float f30 = level.getShade(direction, shade);

            for (int j3 = 0; j3 < this.brightness.length; j3++) {
                this.brightness[j3] = this.brightness[j3] * f30;
            }
        }

        private static int blend(int color1, int color2, int color3, int currentBlockColor) {
            if (color1 == 0) {
                color1 = currentBlockColor;
            }

            if (color2 == 0) {
                color2 = currentBlockColor;
            }

            if (color3 == 0) {
                color3 = currentBlockColor;
            }

            return color1 + color2 + color3 + currentBlockColor >> 2 & 16711935;
        }

        protected static int blend(int color1, int color2, int color3, int blockLight, float color1Weight, float color2Weight, float color3Weight, float blockLightWeight) {
            int i = (int)(
                    (color1 >> 16 & 0xFF) * color1Weight
                        + (color2 >> 16 & 0xFF) * color2Weight
                        + (color3 >> 16 & 0xFF) * color3Weight
                        + (blockLight >> 16 & 0xFF) * blockLightWeight
                )
                & 0xFF;
            int j = (int)((color1 & 0xFF) * color1Weight + (color2 & 0xFF) * color2Weight + (color3 & 0xFF) * color3Weight + (blockLight & 0xFF) * blockLightWeight)
                & 0xFF;
            return i << 16 | j;
        }
    }

    @OnlyIn(Dist.CLIENT)
    static enum AmbientVertexRemap {
        DOWN(0, 1, 2, 3),
        UP(2, 3, 0, 1),
        NORTH(3, 0, 1, 2),
        SOUTH(0, 1, 2, 3),
        WEST(3, 0, 1, 2),
        EAST(1, 2, 3, 0);

        final int vert0;
        final int vert1;
        final int vert2;
        final int vert3;
        private static final ModelBlockRenderer.AmbientVertexRemap[] BY_FACING = Util.make(new ModelBlockRenderer.AmbientVertexRemap[6], p_111204_ -> {
            p_111204_[Direction.DOWN.get3DDataValue()] = DOWN;
            p_111204_[Direction.UP.get3DDataValue()] = UP;
            p_111204_[Direction.NORTH.get3DDataValue()] = NORTH;
            p_111204_[Direction.SOUTH.get3DDataValue()] = SOUTH;
            p_111204_[Direction.WEST.get3DDataValue()] = WEST;
            p_111204_[Direction.EAST.get3DDataValue()] = EAST;
        });

        private AmbientVertexRemap(int vert0, int vert1, int vert2, int vert3) {
            this.vert0 = vert0;
            this.vert1 = vert1;
            this.vert2 = vert2;
            this.vert3 = vert3;
        }

        public static ModelBlockRenderer.AmbientVertexRemap fromFacing(Direction facing) {
            return BY_FACING[facing.get3DDataValue()];
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Cache {
        private boolean enabled;
        private final Long2IntLinkedOpenHashMap colorCache = Util.make(() -> {
            Long2IntLinkedOpenHashMap long2intlinkedopenhashmap = new Long2IntLinkedOpenHashMap(100, 0.25F) {
                @Override
                protected void rehash(int newN) {
                }
            };
            long2intlinkedopenhashmap.defaultReturnValue(Integer.MAX_VALUE);
            return long2intlinkedopenhashmap;
        });
        private final Long2FloatLinkedOpenHashMap brightnessCache = Util.make(() -> {
            Long2FloatLinkedOpenHashMap long2floatlinkedopenhashmap = new Long2FloatLinkedOpenHashMap(100, 0.25F) {
                @Override
                protected void rehash(int newN) {
                }
            };
            long2floatlinkedopenhashmap.defaultReturnValue(Float.NaN);
            return long2floatlinkedopenhashmap;
        });
        private final LevelRenderer.BrightnessGetter cachedBrightnessGetter = (p_412965_, p_412966_) -> {
            long i = p_412966_.asLong();
            int j = this.colorCache.get(i);
            if (j != Integer.MAX_VALUE) {
                return j;
            } else {
                int k = LevelRenderer.BrightnessGetter.DEFAULT.packedBrightness(p_412965_, p_412966_);
                if (this.colorCache.size() == 100) {
                    this.colorCache.removeFirstInt();
                }

                this.colorCache.put(i, k);
                return k;
            }
        };

        private Cache() {
        }

        public void enable() {
            this.enabled = true;
        }

        public void disable() {
            this.enabled = false;
            this.colorCache.clear();
            this.brightnessCache.clear();
        }

        public int getLightColor(BlockState state, BlockAndTintGetter level, BlockPos pos) {
            return LevelRenderer.getLightColor(
                this.enabled ? this.cachedBrightnessGetter : LevelRenderer.BrightnessGetter.DEFAULT, level, state, pos
            );
        }

        public float getShadeBrightness(BlockState state, BlockAndTintGetter level, BlockPos pos) {
            long i = pos.asLong();
            if (this.enabled) {
                float f = this.brightnessCache.get(i);
                if (!Float.isNaN(f)) {
                    return f;
                }
            }

            float f1 = state.getShadeBrightness(level, pos);
            if (this.enabled) {
                if (this.brightnessCache.size() == 100) {
                    this.brightnessCache.removeFirstFloat();
                }

                this.brightnessCache.put(i, f1);
            }

            return f1;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class CommonRenderStorage {
        CommonRenderStorage() {}
        public final BlockPos.MutableBlockPos scratchPos = new BlockPos.MutableBlockPos();
        public boolean faceCubic;
        public boolean facePartial;
        public final float[] brightness = new float[4];
        public final int[] lightmap = new int[4];
        public int tintCacheIndex = -1;
        public int tintCacheValue;
        public final ModelBlockRenderer.Cache cache = ModelBlockRenderer.CACHE.get();
    }

    @OnlyIn(Dist.CLIENT)
    public static enum SizeInfo {
        DOWN(0),
        UP(1),
        NORTH(2),
        SOUTH(3),
        WEST(4),
        EAST(5),
        FLIP_DOWN(6),
        FLIP_UP(7),
        FLIP_NORTH(8),
        FLIP_SOUTH(9),
        FLIP_WEST(10),
        FLIP_EAST(11);

        public static final int COUNT = values().length;
        public final int index;

        private SizeInfo(int index) {
            this.index = index;
        }
    }
}
