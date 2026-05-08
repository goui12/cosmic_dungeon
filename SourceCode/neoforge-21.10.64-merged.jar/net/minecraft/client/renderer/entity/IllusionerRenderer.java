package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Arrays;
import net.minecraft.client.model.IllagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.IllusionerRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Illusioner;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class IllusionerRenderer extends IllagerRenderer<Illusioner, IllusionerRenderState> {
    private static final ResourceLocation ILLUSIONER = ResourceLocation.withDefaultNamespace("textures/entity/illager/illusioner.png");

    public IllusionerRenderer(EntityRendererProvider.Context p_174186_) {
        super(p_174186_, new IllagerModel<>(p_174186_.bakeLayer(ModelLayers.ILLUSIONER)), 0.5F);
        this.addLayer(
            new ItemInHandLayer<IllusionerRenderState, IllagerModel<IllusionerRenderState>>(this) {
                public void submit(
                    PoseStack p_433680_, SubmitNodeCollector p_435258_, int p_432819_, IllusionerRenderState p_435923_, float p_433353_, float p_434240_
                ) {
                    if (p_435923_.isCastingSpell || p_435923_.isAggressive) {
                        super.submit(p_433680_, p_435258_, p_432819_, p_435923_, p_433353_, p_434240_);
                    }
                }
            }
        );
        this.model.getHat().visible = true;
    }

    public ResourceLocation getTextureLocation(IllusionerRenderState p_361469_) {
        return ILLUSIONER;
    }

    public IllusionerRenderState createRenderState() {
        return new IllusionerRenderState();
    }

    public void extractRenderState(Illusioner p_363486_, IllusionerRenderState p_361201_, float p_361809_) {
        super.extractRenderState(p_363486_, p_361201_, p_361809_);
        Vec3[] avec3 = p_363486_.getIllusionOffsets(p_361809_);
        p_361201_.illusionOffsets = Arrays.copyOf(avec3, avec3.length);
        p_361201_.isCastingSpell = p_363486_.isCastingSpell();
    }

    public void submit(IllusionerRenderState p_360892_, PoseStack p_114932_, SubmitNodeCollector p_435684_, CameraRenderState p_451531_) {
        if (p_360892_.isInvisible) {
            Vec3[] avec3 = p_360892_.illusionOffsets;

            for (int i = 0; i < avec3.length; i++) {
                p_114932_.pushPose();
                p_114932_.translate(
                    avec3[i].x + Mth.cos(i + p_360892_.ageInTicks * 0.5F) * 0.025,
                    avec3[i].y + Mth.cos(i + p_360892_.ageInTicks * 0.75F) * 0.0125,
                    avec3[i].z + Mth.cos(i + p_360892_.ageInTicks * 0.7F) * 0.025
                );
                super.submit(p_360892_, p_114932_, p_435684_, p_451531_);
                p_114932_.popPose();
            }
        } else {
            super.submit(p_360892_, p_114932_, p_435684_, p_451531_);
        }
    }

    protected boolean isBodyVisible(IllusionerRenderState p_363096_) {
        return true;
    }

    protected AABB getBoundingBoxForCulling(Illusioner p_364185_) {
        return super.getBoundingBoxForCulling(p_364185_).inflate(3.0, 0.0, 3.0);
    }
}
