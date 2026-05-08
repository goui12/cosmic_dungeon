package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.SquidModel;
import net.minecraft.client.renderer.entity.state.SquidRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Squid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SquidRenderer<T extends Squid> extends AgeableMobRenderer<T, SquidRenderState, SquidModel> {
    private static final ResourceLocation SQUID_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/squid/squid.png");

    public SquidRenderer(EntityRendererProvider.Context context, SquidModel adultModel, SquidModel babyModel) {
        super(context, adultModel, babyModel, 0.7F);
    }

    public ResourceLocation getTextureLocation(SquidRenderState renderState) {
        return SQUID_LOCATION;
    }

    public SquidRenderState createRenderState() {
        return new SquidRenderState();
    }

    public void extractRenderState(T entity, SquidRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        reusedState.tentacleAngle = Mth.lerp(partialTick, entity.oldTentacleAngle, entity.tentacleAngle);
        reusedState.xBodyRot = Mth.lerp(partialTick, entity.xBodyRotO, entity.xBodyRot);
        reusedState.zBodyRot = Mth.lerp(partialTick, entity.zBodyRotO, entity.zBodyRot);
    }

    protected void setupRotations(SquidRenderState renderState, PoseStack poseStack, float bodyRot, float scale) {
        poseStack.translate(0.0F, renderState.isBaby ? 0.25F : 0.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(renderState.xBodyRot));
        poseStack.mulPose(Axis.YP.rotationDegrees(renderState.zBodyRot));
        poseStack.translate(0.0F, renderState.isBaby ? -0.6F : -1.2F, 0.0F);
    }
}
