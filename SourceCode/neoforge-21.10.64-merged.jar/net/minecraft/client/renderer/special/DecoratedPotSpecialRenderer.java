package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.DecoratedPotRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.PotDecorations;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class DecoratedPotSpecialRenderer implements SpecialModelRenderer<PotDecorations> {
    private final DecoratedPotRenderer decoratedPotRenderer;

    public DecoratedPotSpecialRenderer(DecoratedPotRenderer decoratedPotRenderer) {
        this.decoratedPotRenderer = decoratedPotRenderer;
    }

    @Nullable
    public PotDecorations extractArgument(ItemStack stack) {
        return stack.get(DataComponents.POT_DECORATIONS);
    }

    public void submit(
        @Nullable PotDecorations argument,
        ItemDisplayContext displayContext,
        PoseStack poseStack,
        SubmitNodeCollector nodeCollector,
        int packedLight,
        int packedOverlay,
        boolean hasFoil,
        int outlineColor
    ) {
        this.decoratedPotRenderer.submit(poseStack, nodeCollector, packedLight, packedOverlay, Objects.requireNonNullElse(argument, PotDecorations.EMPTY), outlineColor);
    }

    @Override
    public void getExtents(Set<Vector3f> output) {
        this.decoratedPotRenderer.getExtents(output);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<DecoratedPotSpecialRenderer.Unbaked> MAP_CODEC = MapCodec.unit(new DecoratedPotSpecialRenderer.Unbaked());

        @Override
        public MapCodec<DecoratedPotSpecialRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext p_435515_) {
            return new DecoratedPotSpecialRenderer(new DecoratedPotRenderer(p_435515_));
        }
    }
}
