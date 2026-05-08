package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.SharedConstants;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.resources.model.ModelBakery;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class ModelFeatureRenderer {
    private final PoseStack poseStack = new PoseStack();

    public void render(
        SubmitNodeCollection nodeCollection, MultiBufferSource.BufferSource bufferSource, OutlineBufferSource outlineBufferSource, MultiBufferSource.BufferSource crumblingBufferSource
    ) {
        ModelFeatureRenderer.Storage modelfeaturerenderer$storage = nodeCollection.getModelSubmits();
        this.renderBatch(bufferSource, outlineBufferSource, modelfeaturerenderer$storage.opaqueModelSubmits, crumblingBufferSource);
        modelfeaturerenderer$storage.translucentModelSubmits.sort(Comparator.comparingDouble(p_439003_ -> -p_439003_.position().lengthSquared()));
        this.renderTranslucents(bufferSource, outlineBufferSource, modelfeaturerenderer$storage.translucentModelSubmits, crumblingBufferSource);
    }

    private void renderTranslucents(
        MultiBufferSource.BufferSource bufferSource,
        OutlineBufferSource outlineBufferSource,
        List<SubmitNodeStorage.TranslucentModelSubmit<?>> submits,
        MultiBufferSource.BufferSource crumblingBufferSource
    ) {
        for (SubmitNodeStorage.TranslucentModelSubmit<?> translucentmodelsubmit : submits) {
            this.renderModel(
                translucentmodelsubmit.modelSubmit(),
                translucentmodelsubmit.renderType(),
                bufferSource.getBuffer(translucentmodelsubmit.renderType()),
                outlineBufferSource,
                crumblingBufferSource
            );
        }
    }

    private void renderBatch(
        MultiBufferSource.BufferSource bufferSource,
        OutlineBufferSource outlineBufferSource,
        Map<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> submits,
        MultiBufferSource.BufferSource crumblingBufferSource
    ) {
        Iterable<Entry<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>>> iterable;
        if (SharedConstants.DEBUG_SHUFFLE_MODELS) {
            List<Entry<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>>> list = new ArrayList<>(submits.entrySet());
            Collections.shuffle(list);
            iterable = list;
        } else {
            iterable = submits.entrySet();
        }

        for (Entry<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> entry : iterable) {
            VertexConsumer vertexconsumer = bufferSource.getBuffer(entry.getKey());

            for (SubmitNodeStorage.ModelSubmit<?> modelsubmit : entry.getValue()) {
                this.renderModel(modelsubmit, entry.getKey(), vertexconsumer, outlineBufferSource, crumblingBufferSource);
            }
        }
    }

    private <S> void renderModel(
        SubmitNodeStorage.ModelSubmit<S> submit,
        RenderType renderType,
        VertexConsumer consumer,
        OutlineBufferSource outlineBufferSource,
        MultiBufferSource.BufferSource crumblingBufferSource
    ) {
        this.poseStack.pushPose();
        this.poseStack.last().set(submit.pose());
        Model<? super S> model = submit.model();
        VertexConsumer vertexconsumer = submit.sprite() == null ? consumer : submit.sprite().wrap(consumer);
        model.setupAnim(submit.state());
        model.renderToBuffer(this.poseStack, vertexconsumer, submit.lightCoords(), submit.overlayCoords(), submit.tintedColor());
        if (submit.outlineColor() != 0 && (renderType.outline().isPresent() || renderType.isOutline())) {
            outlineBufferSource.setColor(submit.outlineColor());
            VertexConsumer vertexconsumer1 = outlineBufferSource.getBuffer(renderType);
            model.renderToBuffer(
                this.poseStack,
                submit.sprite() == null ? vertexconsumer1 : submit.sprite().wrap(vertexconsumer1),
                submit.lightCoords(),
                submit.overlayCoords(),
                submit.tintedColor()
            );
        }

        if (submit.crumblingOverlay() != null && renderType.affectsCrumbling()) {
            VertexConsumer vertexconsumer2 = new SheetedDecalTextureGenerator(
                crumblingBufferSource.getBuffer(ModelBakery.DESTROY_TYPES.get(submit.crumblingOverlay().progress())), submit.crumblingOverlay().cameraPose(), 1.0F
            );
            model.renderToBuffer(
                this.poseStack,
                submit.sprite() == null ? vertexconsumer2 : submit.sprite().wrap(vertexconsumer2),
                submit.lightCoords(),
                submit.overlayCoords(),
                submit.tintedColor()
            );
        }

        this.poseStack.popPose();
    }

    @OnlyIn(Dist.CLIENT)
    public record CrumblingOverlay(int progress, PoseStack.Pose cameraPose) {
    }

    @OnlyIn(Dist.CLIENT)
    public static class Storage {
        final Map<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> opaqueModelSubmits = new HashMap<>();
        final List<SubmitNodeStorage.TranslucentModelSubmit<?>> translucentModelSubmits = new ArrayList<>();
        private final Set<RenderType> usedModelSubmitBuckets = new ObjectOpenHashSet<>();

        public void add(RenderType renderType, SubmitNodeStorage.ModelSubmit<?> submit) {
            if (renderType.pipeline().getBlendFunction().isEmpty()) {
                this.opaqueModelSubmits.computeIfAbsent(renderType, p_449371_ -> new ArrayList<>()).add(submit);
            } else {
                Vector3f vector3f = submit.pose().pose().transformPosition(new Vector3f());
                this.translucentModelSubmits.add(new SubmitNodeStorage.TranslucentModelSubmit<>(submit, renderType, vector3f));
            }
        }

        public void clear() {
            this.translucentModelSubmits.clear();

            for (Entry<RenderType, List<SubmitNodeStorage.ModelSubmit<?>>> entry : this.opaqueModelSubmits.entrySet()) {
                List<SubmitNodeStorage.ModelSubmit<?>> list = entry.getValue();
                if (!list.isEmpty()) {
                    this.usedModelSubmitBuckets.add(entry.getKey());
                    list.clear();
                }
            }
        }

        public void endFrame() {
            this.opaqueModelSubmits.keySet().removeIf(p_449694_ -> !this.usedModelSubmitBuckets.contains(p_449694_));
            this.usedModelSubmitBuckets.clear();
        }
    }
}
