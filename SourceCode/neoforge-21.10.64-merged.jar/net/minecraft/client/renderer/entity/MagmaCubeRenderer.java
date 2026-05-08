package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.LavaSlimeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.MagmaCube;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MagmaCubeRenderer extends MobRenderer<MagmaCube, SlimeRenderState, LavaSlimeModel> {
    private static final ResourceLocation MAGMACUBE_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/slime/magmacube.png");

    public MagmaCubeRenderer(EntityRendererProvider.Context p_174298_) {
        super(p_174298_, new LavaSlimeModel(p_174298_.bakeLayer(ModelLayers.MAGMA_CUBE)), 0.25F);
    }

    protected int getBlockLightLevel(MagmaCube entity, BlockPos pos) {
        return 15;
    }

    public ResourceLocation getTextureLocation(SlimeRenderState renderState) {
        return MAGMACUBE_LOCATION;
    }

    public SlimeRenderState createRenderState() {
        return new SlimeRenderState();
    }

    public void extractRenderState(MagmaCube entity, SlimeRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        reusedState.squish = Mth.lerp(partialTick, entity.oSquish, entity.squish);
        reusedState.size = entity.getSize();
    }

    protected float getShadowRadius(SlimeRenderState renderState) {
        return renderState.size * 0.25F;
    }

    protected void scale(SlimeRenderState renderState, PoseStack poseStack) {
        int i = renderState.size;
        float f = renderState.squish / (i * 0.5F + 1.0F);
        float f1 = 1.0F / (f + 1.0F);
        poseStack.scale(f1 * i, 1.0F / f1 * i, f1 * i);
    }
}
