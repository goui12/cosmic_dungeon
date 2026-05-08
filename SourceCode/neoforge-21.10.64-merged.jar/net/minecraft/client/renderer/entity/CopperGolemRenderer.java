package net.minecraft.client.renderer.entity;

import java.util.Optional;
import java.util.function.Function;
import net.minecraft.client.model.CopperGolemModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.BlockDecorationLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.CopperGolemRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.coppergolem.CopperGolem;
import net.minecraft.world.entity.animal.coppergolem.CopperGolemOxidationLevels;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CopperGolemRenderer extends MobRenderer<CopperGolem, CopperGolemRenderState, CopperGolemModel> {
    public CopperGolemRenderer(EntityRendererProvider.Context p_435987_) {
        super(p_435987_, new CopperGolemModel(p_435987_.bakeLayer(ModelLayers.COPPER_GOLEM)), 0.5F);
        this.addLayer(
            new LivingEntityEmissiveLayer<>(
                this,
                getEyeTextureLocationProvider(),
                (p_432853_, p_433275_) -> 1.0F,
                new CopperGolemModel(p_435987_.bakeLayer(ModelLayers.COPPER_GOLEM)),
                RenderType::eyes,
                false
            )
        );
        this.addLayer(new ItemInHandLayer<>(this));
        this.addLayer(new BlockDecorationLayer<>(this, p_442328_ -> p_442328_.blockOnAntenna, this.model::applyBlockOnAntennaTransform));
        this.addLayer(new CustomHeadLayer<>(this, p_435987_.getModelSet(), p_435987_.getPlayerSkinRenderCache()));
    }

    public ResourceLocation getTextureLocation(CopperGolemRenderState p_433831_) {
        return CopperGolemOxidationLevels.getOxidationLevel(p_433831_.weathering).texture();
    }

    private static Function<CopperGolemRenderState, ResourceLocation> getEyeTextureLocationProvider() {
        return p_433437_ -> CopperGolemOxidationLevels.getOxidationLevel(p_433437_.weathering).eyeTexture();
    }

    public CopperGolemRenderState createRenderState() {
        return new CopperGolemRenderState();
    }

    public void extractRenderState(CopperGolem p_433175_, CopperGolemRenderState p_435246_, float p_435082_) {
        super.extractRenderState(p_433175_, p_435246_, p_435082_);
        ArmedEntityRenderState.extractArmedEntityRenderState(p_433175_, p_435246_, this.itemModelResolver);
        p_435246_.weathering = p_433175_.getWeatherState();
        p_435246_.copperGolemState = p_433175_.getState();
        p_435246_.idleAnimationState.copyFrom(p_433175_.getIdleAnimationState());
        p_435246_.interactionGetItem.copyFrom(p_433175_.getInteractionGetItemAnimationState());
        p_435246_.interactionGetNoItem.copyFrom(p_433175_.getInteractionGetNoItemAnimationState());
        p_435246_.interactionDropItem.copyFrom(p_433175_.getInteractionDropItemAnimationState());
        p_435246_.interactionDropNoItem.copyFrom(p_433175_.getInteractionDropNoItemAnimationState());
        p_435246_.blockOnAntenna = Optional.of(p_433175_.getItemBySlot(CopperGolem.EQUIPMENT_SLOT_ANTENNA)).flatMap(p_437175_ -> {
            if (p_437175_.getItem() instanceof BlockItem blockitem) {
                BlockItemStateProperties blockitemstateproperties = p_437175_.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
                return Optional.of(blockitemstateproperties.apply(blockitem.getBlock().defaultBlockState()));
            } else {
                return Optional.empty();
            }
        });
    }
}
