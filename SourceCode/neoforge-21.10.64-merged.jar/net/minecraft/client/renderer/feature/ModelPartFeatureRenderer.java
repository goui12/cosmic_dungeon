package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.ModelBakery;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ModelPartFeatureRenderer {
    private final PoseStack poseStack = new PoseStack();

    public void render(
        SubmitNodeCollection nodeCollection, MultiBufferSource.BufferSource bufferSource, OutlineBufferSource outlineBufferSource, MultiBufferSource.BufferSource crumblingBufferSource
    ) {
        ModelPartFeatureRenderer.Storage modelpartfeaturerenderer$storage = nodeCollection.getModelPartSubmits();

        for (Entry<RenderType, List<SubmitNodeStorage.ModelPartSubmit>> entry : modelpartfeaturerenderer$storage.modelPartSubmits.entrySet()) {
            RenderType rendertype = entry.getKey();
            List<SubmitNodeStorage.ModelPartSubmit> list = entry.getValue();
            VertexConsumer vertexconsumer = bufferSource.getBuffer(rendertype);

            for (SubmitNodeStorage.ModelPartSubmit submitnodestorage$modelpartsubmit : list) {
                VertexConsumer vertexconsumer1;
                if (submitnodestorage$modelpartsubmit.sprite() != null) {
                    if (submitnodestorage$modelpartsubmit.hasFoil()) {
                        vertexconsumer1 = submitnodestorage$modelpartsubmit.sprite()
                            .wrap(ItemRenderer.getFoilBuffer(bufferSource, rendertype, submitnodestorage$modelpartsubmit.sheeted(), true));
                    } else {
                        vertexconsumer1 = submitnodestorage$modelpartsubmit.sprite().wrap(vertexconsumer);
                    }
                } else if (submitnodestorage$modelpartsubmit.hasFoil()) {
                    vertexconsumer1 = ItemRenderer.getFoilBuffer(bufferSource, rendertype, submitnodestorage$modelpartsubmit.sheeted(), true);
                } else {
                    vertexconsumer1 = vertexconsumer;
                }

                this.poseStack.last().set(submitnodestorage$modelpartsubmit.pose());
                submitnodestorage$modelpartsubmit.modelPart()
                    .render(
                        this.poseStack,
                        vertexconsumer1,
                        submitnodestorage$modelpartsubmit.lightCoords(),
                        submitnodestorage$modelpartsubmit.overlayCoords(),
                        submitnodestorage$modelpartsubmit.tintedColor()
                    );
                if (submitnodestorage$modelpartsubmit.outlineColor() != 0 && (rendertype.outline().isPresent() || rendertype.isOutline())) {
                    outlineBufferSource.setColor(submitnodestorage$modelpartsubmit.outlineColor());
                    VertexConsumer vertexconsumer2 = outlineBufferSource.getBuffer(rendertype);
                    submitnodestorage$modelpartsubmit.modelPart()
                        .render(
                            this.poseStack,
                            submitnodestorage$modelpartsubmit.sprite() == null
                                ? vertexconsumer2
                                : submitnodestorage$modelpartsubmit.sprite().wrap(vertexconsumer2),
                            submitnodestorage$modelpartsubmit.lightCoords(),
                            submitnodestorage$modelpartsubmit.overlayCoords(),
                            submitnodestorage$modelpartsubmit.tintedColor()
                        );
                }

                if (submitnodestorage$modelpartsubmit.crumblingOverlay() != null) {
                    VertexConsumer vertexconsumer3 = new SheetedDecalTextureGenerator(
                        crumblingBufferSource.getBuffer(ModelBakery.DESTROY_TYPES.get(submitnodestorage$modelpartsubmit.crumblingOverlay().progress())),
                        submitnodestorage$modelpartsubmit.crumblingOverlay().cameraPose(),
                        1.0F
                    );
                    submitnodestorage$modelpartsubmit.modelPart()
                        .render(
                            this.poseStack,
                            vertexconsumer3,
                            submitnodestorage$modelpartsubmit.lightCoords(),
                            submitnodestorage$modelpartsubmit.overlayCoords(),
                            submitnodestorage$modelpartsubmit.tintedColor()
                        );
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Storage {
        final Map<RenderType, List<SubmitNodeStorage.ModelPartSubmit>> modelPartSubmits = new HashMap<>();
        private final Set<RenderType> modelPartSubmitsUsage = new ObjectOpenHashSet<>();

        public void add(RenderType renderType, SubmitNodeStorage.ModelPartSubmit submit) {
            this.modelPartSubmits.computeIfAbsent(renderType, p_449399_ -> new ArrayList<>()).add(submit);
        }

        public void clear() {
            for (Entry<RenderType, List<SubmitNodeStorage.ModelPartSubmit>> entry : this.modelPartSubmits.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    this.modelPartSubmitsUsage.add(entry.getKey());
                    entry.getValue().clear();
                }
            }
        }

        public void endFrame() {
            this.modelPartSubmits.keySet().removeIf(p_449703_ -> !this.modelPartSubmitsUsage.contains(p_449703_));
            this.modelPartSubmitsUsage.clear();
        }
    }
}
