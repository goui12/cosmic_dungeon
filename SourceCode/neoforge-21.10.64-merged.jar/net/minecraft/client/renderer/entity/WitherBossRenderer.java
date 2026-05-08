package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.WitherBossModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.WitherArmorLayer;
import net.minecraft.client.renderer.entity.state.WitherRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WitherBossRenderer extends MobRenderer<WitherBoss, WitherRenderState, WitherBossModel> {
    private static final ResourceLocation WITHER_INVULNERABLE_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/wither/wither_invulnerable.png");
    private static final ResourceLocation WITHER_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/wither/wither.png");

    public WitherBossRenderer(EntityRendererProvider.Context p_174445_) {
        super(p_174445_, new WitherBossModel(p_174445_.bakeLayer(ModelLayers.WITHER)), 1.0F);
        this.addLayer(new WitherArmorLayer(this, p_174445_.getModelSet()));
    }

    protected int getBlockLightLevel(WitherBoss entity, BlockPos pos) {
        return 15;
    }

    public ResourceLocation getTextureLocation(WitherRenderState renderState) {
        int i = Mth.floor(renderState.invulnerableTicks);
        return i > 0 && (i > 80 || i / 5 % 2 != 1) ? WITHER_INVULNERABLE_LOCATION : WITHER_LOCATION;
    }

    public WitherRenderState createRenderState() {
        return new WitherRenderState();
    }

    protected void scale(WitherRenderState renderState, PoseStack poseStack) {
        float f = 2.0F;
        if (renderState.invulnerableTicks > 0.0F) {
            f -= renderState.invulnerableTicks / 220.0F * 0.5F;
        }

        poseStack.scale(f, f, f);
    }

    public void extractRenderState(WitherBoss entity, WitherRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        int i = entity.getInvulnerableTicks();
        reusedState.invulnerableTicks = i > 0 ? i - partialTick : 0.0F;
        System.arraycopy(entity.getHeadXRots(), 0, reusedState.xHeadRots, 0, reusedState.xHeadRots.length);
        System.arraycopy(entity.getHeadYRots(), 0, reusedState.yHeadRots, 0, reusedState.yHeadRots.length);
        reusedState.isPowered = entity.isPowered();
    }
}
