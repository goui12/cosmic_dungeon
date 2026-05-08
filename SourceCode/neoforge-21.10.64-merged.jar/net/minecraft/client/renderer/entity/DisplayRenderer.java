package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.BlockDisplayEntityRenderState;
import net.minecraft.client.renderer.entity.state.DisplayEntityRenderState;
import net.minecraft.client.renderer.entity.state.ItemDisplayEntityRenderState;
import net.minecraft.client.renderer.entity.state.TextDisplayEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Display;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public abstract class DisplayRenderer<T extends Display, S, ST extends DisplayEntityRenderState> extends EntityRenderer<T, ST> {
    private final EntityRenderDispatcher entityRenderDispatcher;

    protected DisplayRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.entityRenderDispatcher = context.getEntityRenderDispatcher();
    }

    protected AABB getBoundingBoxForCulling(T minecraft) {
        return minecraft.getBoundingBoxForCulling();
    }

    protected boolean affectedByCulling(T display) {
        return display.affectedByCulling();
    }

    private static int getBrightnessOverride(Display display) {
        Display.RenderState display$renderstate = display.renderState();
        return display$renderstate != null ? display$renderstate.brightnessOverride() : -1;
    }

    protected int getSkyLightLevel(T entity, BlockPos pos) {
        int i = getBrightnessOverride(entity);
        return i != -1 ? LightTexture.sky(i) : super.getSkyLightLevel(entity, pos);
    }

    protected int getBlockLightLevel(T entity, BlockPos pos) {
        int i = getBrightnessOverride(entity);
        return i != -1 ? LightTexture.block(i) : super.getBlockLightLevel(entity, pos);
    }

    protected float getShadowRadius(ST renderState) {
        Display.RenderState display$renderstate = renderState.renderState;
        return display$renderstate == null ? 0.0F : display$renderstate.shadowRadius().get(renderState.interpolationProgress);
    }

    protected float getShadowStrength(ST renderState) {
        Display.RenderState display$renderstate = renderState.renderState;
        return display$renderstate == null ? 0.0F : display$renderstate.shadowStrength().get(renderState.interpolationProgress);
    }

    public void submit(ST renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        Display.RenderState display$renderstate = renderState.renderState;
        if (display$renderstate != null && renderState.hasSubState()) {
            float f = renderState.interpolationProgress;
            super.submit(renderState, poseStack, nodeCollector, cameraRenderState);
            poseStack.pushPose();
            poseStack.mulPose(this.calculateOrientation(display$renderstate, renderState, new Quaternionf()));
            Transformation transformation = display$renderstate.transformation().get(f);
            poseStack.mulPose(transformation.getMatrix());
            this.submitInner(renderState, poseStack, nodeCollector, renderState.lightCoords, f);
            poseStack.popPose();
        }
    }

    private Quaternionf calculateOrientation(Display.RenderState renderState, ST entityRenderState, Quaternionf quaternion) {
        return switch (renderState.billboardConstraints()) {
            case FIXED -> quaternion.rotationYXZ((float) (-Math.PI / 180.0) * entityRenderState.entityYRot, (float) (Math.PI / 180.0) * entityRenderState.entityXRot, 0.0F);
            case HORIZONTAL -> quaternion.rotationYXZ(
                (float) (-Math.PI / 180.0) * entityRenderState.entityYRot, (float) (Math.PI / 180.0) * transformXRot(entityRenderState.cameraXRot), 0.0F
            );
            case VERTICAL -> quaternion.rotationYXZ(
                (float) (-Math.PI / 180.0) * transformYRot(entityRenderState.cameraYRot), (float) (Math.PI / 180.0) * entityRenderState.entityXRot, 0.0F
            );
            case CENTER -> quaternion.rotationYXZ(
                (float) (-Math.PI / 180.0) * transformYRot(entityRenderState.cameraYRot), (float) (Math.PI / 180.0) * transformXRot(entityRenderState.cameraXRot), 0.0F
            );
        };
    }

    private static float transformYRot(float yRot) {
        return yRot - 180.0F;
    }

    private static float transformXRot(float xRot) {
        return -xRot;
    }

    private static <T extends Display> float entityYRot(T entity, float partialTick) {
        return entity.getYRot(partialTick);
    }

    private static <T extends Display> float entityXRot(T entity, float partialTick) {
        return entity.getXRot(partialTick);
    }

    protected abstract void submitInner(ST renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, float partialTick);

    public void extractRenderState(T entity, ST reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        reusedState.renderState = entity.renderState();
        reusedState.interpolationProgress = entity.calculateInterpolationProgress(partialTick);
        reusedState.entityYRot = entityYRot(entity, partialTick);
        reusedState.entityXRot = entityXRot(entity, partialTick);
        Camera camera = this.entityRenderDispatcher.camera;
        reusedState.cameraXRot = camera.getXRot();
        reusedState.cameraYRot = camera.getYRot();
    }

    @OnlyIn(Dist.CLIENT)
    public static class BlockDisplayRenderer
        extends DisplayRenderer<Display.BlockDisplay, Display.BlockDisplay.BlockRenderState, BlockDisplayEntityRenderState> {
        protected BlockDisplayRenderer(EntityRendererProvider.Context p_270283_) {
            super(p_270283_);
        }

        public BlockDisplayEntityRenderState createRenderState() {
            return new BlockDisplayEntityRenderState();
        }

        public void extractRenderState(Display.BlockDisplay p_362697_, BlockDisplayEntityRenderState p_363759_, float p_360854_) {
            super.extractRenderState(p_362697_, p_363759_, p_360854_);
            p_363759_.blockRenderState = p_362697_.blockRenderState();
        }

        public void submitInner(BlockDisplayEntityRenderState p_432901_, PoseStack p_434089_, SubmitNodeCollector p_433174_, int p_435266_, float p_435422_) {
            p_433174_.submitBlock(p_434089_, p_432901_.blockRenderState.blockState(), p_435266_, OverlayTexture.NO_OVERLAY, p_432901_.outlineColor);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class ItemDisplayRenderer extends DisplayRenderer<Display.ItemDisplay, Display.ItemDisplay.ItemRenderState, ItemDisplayEntityRenderState> {
        private final ItemModelResolver itemModelResolver;

        protected ItemDisplayRenderer(EntityRendererProvider.Context p_270110_) {
            super(p_270110_);
            this.itemModelResolver = p_270110_.getItemModelResolver();
        }

        public ItemDisplayEntityRenderState createRenderState() {
            return new ItemDisplayEntityRenderState();
        }

        public void extractRenderState(Display.ItemDisplay p_360671_, ItemDisplayEntityRenderState p_361611_, float p_361257_) {
            super.extractRenderState(p_360671_, p_361611_, p_361257_);
            Display.ItemDisplay.ItemRenderState display$itemdisplay$itemrenderstate = p_360671_.itemRenderState();
            if (display$itemdisplay$itemrenderstate != null) {
                this.itemModelResolver
                    .updateForNonLiving(
                        p_361611_.item, display$itemdisplay$itemrenderstate.itemStack(), display$itemdisplay$itemrenderstate.itemTransform(), p_360671_
                    );
            } else {
                p_361611_.item.clear();
            }
        }

        public void submitInner(ItemDisplayEntityRenderState p_433571_, PoseStack p_432839_, SubmitNodeCollector p_433402_, int p_434368_, float p_433057_) {
            if (!p_433571_.item.isEmpty()) {
                p_432839_.mulPose(Axis.YP.rotation((float) Math.PI));
                p_433571_.item.submit(p_432839_, p_433402_, p_434368_, OverlayTexture.NO_OVERLAY, p_433571_.outlineColor);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class TextDisplayRenderer extends DisplayRenderer<Display.TextDisplay, Display.TextDisplay.TextRenderState, TextDisplayEntityRenderState> {
        private final Font font;

        protected TextDisplayRenderer(EntityRendererProvider.Context context) {
            super(context);
            this.font = context.getFont();
        }

        public TextDisplayEntityRenderState createRenderState() {
            return new TextDisplayEntityRenderState();
        }

        public void extractRenderState(Display.TextDisplay entity, TextDisplayEntityRenderState reusedState, float partialTick) {
            super.extractRenderState(entity, reusedState, partialTick);
            reusedState.textRenderState = entity.textRenderState();
            reusedState.cachedInfo = entity.cacheDisplay(this::splitLines);
        }

        private Display.TextDisplay.CachedInfo splitLines(Component text, int maxWidth) {
            List<FormattedCharSequence> list = this.font.split(text, maxWidth);
            List<Display.TextDisplay.CachedLine> list1 = new ArrayList<>(list.size());
            int i = 0;

            for (FormattedCharSequence formattedcharsequence : list) {
                int j = this.font.width(formattedcharsequence);
                i = Math.max(i, j);
                list1.add(new Display.TextDisplay.CachedLine(formattedcharsequence, j));
            }

            return new Display.TextDisplay.CachedInfo(list1, i);
        }

        public void submitInner(TextDisplayEntityRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, float partialTick) {
            Display.TextDisplay.TextRenderState display$textdisplay$textrenderstate = renderState.textRenderState;
            byte b0 = display$textdisplay$textrenderstate.flags();
            boolean flag = (b0 & 2) != 0;
            boolean flag1 = (b0 & 4) != 0;
            boolean flag2 = (b0 & 1) != 0;
            Display.TextDisplay.Align display$textdisplay$align = Display.TextDisplay.getAlign(b0);
            byte b1 = (byte)display$textdisplay$textrenderstate.textOpacity().get(partialTick);
            int i;
            if (flag1) {
                float f = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
                i = (int)(f * 255.0F) << 24;
            } else {
                i = display$textdisplay$textrenderstate.backgroundColor().get(partialTick);
            }

            float f2 = 0.0F;
            Matrix4f matrix4f = poseStack.last().pose();
            matrix4f.rotate((float) Math.PI, 0.0F, 1.0F, 0.0F);
            matrix4f.scale(-0.025F, -0.025F, -0.025F);
            Display.TextDisplay.CachedInfo display$textdisplay$cachedinfo = renderState.cachedInfo;
            int j = 1;
            int k = 9 + 1;
            int l = display$textdisplay$cachedinfo.width();
            int i1 = display$textdisplay$cachedinfo.lines().size() * k - 1;
            matrix4f.translate(1.0F - l / 2.0F, -i1, 0.0F);
            if (i != 0) {
                nodeCollector.submitCustomGeometry(
                    poseStack, flag ? RenderType.textBackgroundSeeThrough() : RenderType.textBackground(), (p_434840_, p_435597_) -> {
                        p_435597_.addVertex(p_434840_, -1.0F, -1.0F, 0.0F).setColor(i).setLight(packedLight);
                        p_435597_.addVertex(p_434840_, -1.0F, (float)i1, 0.0F).setColor(i).setLight(packedLight);
                        p_435597_.addVertex(p_434840_, (float)l, (float)i1, 0.0F).setColor(i).setLight(packedLight);
                        p_435597_.addVertex(p_434840_, (float)l, -1.0F, 0.0F).setColor(i).setLight(packedLight);
                    }
                );
            }

            OrderedSubmitNodeCollector orderedsubmitnodecollector = nodeCollector.order(i != 0 ? 1 : 0);

            for (Display.TextDisplay.CachedLine display$textdisplay$cachedline : display$textdisplay$cachedinfo.lines()) {
                float f1 = switch (display$textdisplay$align) {
                    case LEFT -> 0.0F;
                    case RIGHT -> l - display$textdisplay$cachedline.width();
                    case CENTER -> l / 2.0F - display$textdisplay$cachedline.width() / 2.0F;
                };
                orderedsubmitnodecollector.submitText(
                    poseStack,
                    f1,
                    f2,
                    display$textdisplay$cachedline.contents(),
                    flag2,
                    flag ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.POLYGON_OFFSET,
                    packedLight,
                    b1 << 24 | 16777215,
                    0,
                    0
                );
                f2 += k;
            }
        }
    }
}
