package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.EndGatewayRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TheEndGatewayRenderer extends AbstractEndPortalRenderer<TheEndGatewayBlockEntity, EndGatewayRenderState> {
    private static final ResourceLocation BEAM_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/end_gateway_beam.png");

    public EndGatewayRenderState createRenderState() {
        return new EndGatewayRenderState();
    }

    public void extractRenderState(
        TheEndGatewayBlockEntity p_446930_,
        EndGatewayRenderState p_445622_,
        float p_445588_,
        Vec3 p_445696_,
        @Nullable ModelFeatureRenderer.CrumblingOverlay p_445892_
    ) {
        super.extractRenderState(p_446930_, p_445622_, p_445588_, p_445696_, p_445892_);
        Level level = p_446930_.getLevel();
        if (p_446930_.isSpawning() || p_446930_.isCoolingDown() && level != null) {
            p_445622_.scale = p_446930_.isSpawning() ? p_446930_.getSpawnPercent(p_445588_) : p_446930_.getCooldownPercent(p_445588_);
            double d0 = p_446930_.isSpawning() ? p_446930_.getLevel().getMaxY() : 50.0;
            p_445622_.scale = Mth.sin(p_445622_.scale * (float) Math.PI);
            p_445622_.height = Mth.floor(p_445622_.scale * d0);
            p_445622_.color = p_446930_.isSpawning() ? DyeColor.MAGENTA.getTextureDiffuseColor() : DyeColor.PURPLE.getTextureDiffuseColor();
            p_445622_.animationTime = p_446930_.getLevel() != null ? Math.floorMod(p_446930_.getLevel().getGameTime(), 40) + p_445588_ : 0.0F;
        } else {
            p_445622_.height = 0;
        }
    }

    public void submit(EndGatewayRenderState p_446213_, PoseStack p_439084_, SubmitNodeCollector p_439292_, CameraRenderState p_451275_) {
        if (p_446213_.height > 0) {
            BeaconRenderer.submitBeaconBeam(
                p_439084_,
                p_439292_,
                BEAM_LOCATION,
                p_446213_.scale,
                p_446213_.animationTime,
                -p_446213_.height,
                p_446213_.height * 2,
                p_446213_.color,
                0.15F,
                0.175F
            );
        }

        super.submit(p_446213_, p_439084_, p_439292_, p_451275_);
    }

    @Override
    protected float getOffsetUp() {
        return 1.0F;
    }

    @Override
    protected float getOffsetDown() {
        return 0.0F;
    }

    @Override
    protected RenderType renderType() {
        return RenderType.endGateway();
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(TheEndGatewayBlockEntity blockEntity) {
        return blockEntity.isSpawning() || blockEntity.isCoolingDown() ? net.minecraft.world.phys.AABB.INFINITE : super.getRenderBoundingBox(blockEntity);
    }
}
