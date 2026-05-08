package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.IllagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.IllagerRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Vindicator;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VindicatorRenderer extends IllagerRenderer<Vindicator, IllagerRenderState> {
    private static final ResourceLocation VINDICATOR = ResourceLocation.withDefaultNamespace("textures/entity/illager/vindicator.png");

    public VindicatorRenderer(EntityRendererProvider.Context p_174439_) {
        super(p_174439_, new IllagerModel<>(p_174439_.bakeLayer(ModelLayers.VINDICATOR)), 0.5F);
        this.addLayer(
            new ItemInHandLayer<IllagerRenderState, IllagerModel<IllagerRenderState>>(this) {
                public void submit(
                    PoseStack p_434569_, SubmitNodeCollector p_434409_, int p_433214_, IllagerRenderState p_433367_, float p_435149_, float p_433947_
                ) {
                    if (p_433367_.isAggressive) {
                        super.submit(p_434569_, p_434409_, p_433214_, p_433367_, p_435149_, p_433947_);
                    }
                }
            }
        );
    }

    public ResourceLocation getTextureLocation(IllagerRenderState p_364813_) {
        return VINDICATOR;
    }

    public IllagerRenderState createRenderState() {
        return new IllagerRenderState();
    }
}
