package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CustomFeatureRenderer {
    public void render(SubmitNodeCollection nodeCollection, MultiBufferSource.BufferSource bufferSource) {
        CustomFeatureRenderer.Storage customfeaturerenderer$storage = nodeCollection.getCustomGeometrySubmits();

        for (Entry<RenderType, List<SubmitNodeStorage.CustomGeometrySubmit>> entry : customfeaturerenderer$storage.customGeometrySubmits.entrySet()) {
            VertexConsumer vertexconsumer = bufferSource.getBuffer(entry.getKey());

            for (SubmitNodeStorage.CustomGeometrySubmit submitnodestorage$customgeometrysubmit : entry.getValue()) {
                submitnodestorage$customgeometrysubmit.customGeometryRenderer().render(submitnodestorage$customgeometrysubmit.pose(), vertexconsumer);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Storage {
        final Map<RenderType, List<SubmitNodeStorage.CustomGeometrySubmit>> customGeometrySubmits = new HashMap<>();
        private final Set<RenderType> customGeometrySubmitsUsage = new ObjectOpenHashSet<>();

        public void add(PoseStack poseStack, RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer renderer) {
            List<SubmitNodeStorage.CustomGeometrySubmit> list = this.customGeometrySubmits.computeIfAbsent(renderType, p_449380_ -> new ArrayList<>());
            list.add(new SubmitNodeStorage.CustomGeometrySubmit(poseStack.last().copy(), renderer));
        }

        public void clear() {
            for (Entry<RenderType, List<SubmitNodeStorage.CustomGeometrySubmit>> entry : this.customGeometrySubmits.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    this.customGeometrySubmitsUsage.add(entry.getKey());
                    entry.getValue().clear();
                }
            }
        }

        public void endFrame() {
            this.customGeometrySubmits.keySet().removeIf(p_449305_ -> !this.customGeometrySubmitsUsage.contains(p_449305_));
            this.customGeometrySubmitsUsage.clear();
        }
    }
}
