package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class BannerSpecialRenderer implements SpecialModelRenderer<BannerPatternLayers> {
    private final BannerRenderer bannerRenderer;
    private final DyeColor baseColor;

    public BannerSpecialRenderer(DyeColor baseColor, BannerRenderer bannerRenderer) {
        this.bannerRenderer = bannerRenderer;
        this.baseColor = baseColor;
    }

    @Nullable
    public BannerPatternLayers extractArgument(ItemStack stack) {
        return stack.get(DataComponents.BANNER_PATTERNS);
    }

    public void submit(
        @Nullable BannerPatternLayers argument,
        ItemDisplayContext displayContext,
        PoseStack poseStack,
        SubmitNodeCollector nodeCollector,
        int packedLight,
        int packedOverlay,
        boolean hasFoil,
        int outlineColor
    ) {
        this.bannerRenderer
            .submitSpecial(
                poseStack, nodeCollector, packedLight, packedOverlay, this.baseColor, Objects.requireNonNullElse(argument, BannerPatternLayers.EMPTY), outlineColor
            );
    }

    @Override
    public void getExtents(Set<Vector3f> output) {
        this.bannerRenderer.getExtents(output);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked(DyeColor baseColor) implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<BannerSpecialRenderer.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_386477_ -> p_386477_.group(DyeColor.CODEC.fieldOf("color").forGetter(BannerSpecialRenderer.Unbaked::baseColor))
                .apply(p_386477_, BannerSpecialRenderer.Unbaked::new)
        );

        @Override
        public MapCodec<BannerSpecialRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new BannerSpecialRenderer(this.baseColor, new BannerRenderer(context));
        }
    }
}
