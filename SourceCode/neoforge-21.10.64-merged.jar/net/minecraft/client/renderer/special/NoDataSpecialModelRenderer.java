package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface NoDataSpecialModelRenderer extends SpecialModelRenderer<Void> {
    @Nullable
    default Void extractArgument(ItemStack stack) {
        return null;
    }

    default void submit(
        @Nullable Void argument,
        ItemDisplayContext displayContext,
        PoseStack poseStack,
        SubmitNodeCollector nodeCollector,
        int packedLight,
        int packedOverlay,
        boolean hasFoil,
        int outlineColor
    ) {
        this.submit(displayContext, poseStack, nodeCollector, packedLight, packedOverlay, hasFoil, outlineColor);
    }

    void submit(
        ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, int packedOverlay, boolean hasFoil, int outlineColor
    );
}
