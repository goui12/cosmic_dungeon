package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LivingEntityEmissiveLayer<S extends LivingEntityRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
    private final Function<S, ResourceLocation> textureProvider;
    private final LivingEntityEmissiveLayer.AlphaFunction<S> alphaFunction;
    private final M model;
    private final Function<ResourceLocation, RenderType> bufferProvider;
    private final boolean alwaysVisible;

    public LivingEntityEmissiveLayer(
        RenderLayerParent<S, M> renderer,
        Function<S, ResourceLocation> textureProvider,
        LivingEntityEmissiveLayer.AlphaFunction<S> alphaFunction,
        M model,
        Function<ResourceLocation, RenderType> bufferProvider,
        boolean alwaysVisible
    ) {
        super(renderer);
        this.textureProvider = textureProvider;
        this.alphaFunction = alphaFunction;
        this.model = model;
        this.bufferProvider = bufferProvider;
        this.alwaysVisible = alwaysVisible;
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, S renderState, float yRot, float xRot) {
        if (!renderState.isInvisible || this.alwaysVisible) {
            float f = this.alphaFunction.apply(renderState, renderState.ageInTicks);
            if (!(f <= 1.0E-5F)) {
                int i = ARGB.white(f);
                RenderType rendertype = this.bufferProvider.apply(this.textureProvider.apply(renderState));
                nodeCollector.order(1)
                    .submitModel(
                        this.model,
                        renderState,
                        poseStack,
                        rendertype,
                        packedLight,
                        LivingEntityRenderer.getOverlayCoords(renderState, 0.0F),
                        i,
                        null,
                        renderState.outlineColor,
                        null
                    );
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public interface AlphaFunction<S extends LivingEntityRenderState> {
        float apply(S renderState, float alpha);
    }
}
