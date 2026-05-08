package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.Set;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.ConduitRenderer;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class ConduitSpecialRenderer implements NoDataSpecialModelRenderer {
    private final MaterialSet materials;
    private final ModelPart model;

    public ConduitSpecialRenderer(MaterialSet materials, ModelPart model) {
        this.materials = materials;
        this.model = model;
    }

    @Override
    public void submit(
        ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, int packedOverlay, boolean hasFoil, int outlineColor
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        nodeCollector.submitModelPart(
            this.model,
            poseStack,
            ConduitRenderer.SHELL_TEXTURE.renderType(RenderType::entitySolid),
            packedLight,
            packedOverlay,
            this.materials.get(ConduitRenderer.SHELL_TEXTURE),
            false,
            false,
            -1,
            null,
            outlineColor
        );
        poseStack.popPose();
    }

    @Override
    public void getExtents(Set<Vector3f> output) {
        PoseStack posestack = new PoseStack();
        posestack.translate(0.5F, 0.5F, 0.5F);
        this.model.getExtentsForGui(posestack, output);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<ConduitSpecialRenderer.Unbaked> MAP_CODEC = MapCodec.unit(new ConduitSpecialRenderer.Unbaked());

        @Override
        public MapCodec<ConduitSpecialRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext p_433995_) {
            return new ConduitSpecialRenderer(p_433995_.materials(), p_433995_.entityModelSet().bakeLayer(ModelLayers.CONDUIT_SHELL));
        }
    }
}
