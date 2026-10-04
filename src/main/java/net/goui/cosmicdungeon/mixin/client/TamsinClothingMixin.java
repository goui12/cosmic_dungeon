package net.goui.cosmicdungeon.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.goui.cosmicdungeon.client.TamsinAppearanceRender;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces the biome/profession/badge passes only for the bound villager's captured state. */
@Mixin(VillagerProfessionLayer.class)
public abstract class TamsinClothingMixin extends RenderLayer<VillagerRenderState, VillagerModel> {
    protected TamsinClothingMixin(RenderLayerParent<VillagerRenderState, VillagerModel> renderer) {
        super(renderer);
    }

    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
            at = @At("HEAD"), cancellable = true)
    private void cosmicdungeon$tamsinClothing(PoseStack pose, SubmitNodeCollector collector,
            int light, LivingEntityRenderState state, float yRot, float xRot, CallbackInfo callback) {
        if (state instanceof VillagerRenderState villager && TamsinAppearanceRender.INSTANCE.isTamsin(villager)) {
            if (!villager.isInvisible)
                renderColoredCutoutModel(getParentModel(), TamsinAppearanceRender.CLOTHING,
                        pose, collector, light, villager, -1, 1);
            callback.cancel();
        }
    }
}
