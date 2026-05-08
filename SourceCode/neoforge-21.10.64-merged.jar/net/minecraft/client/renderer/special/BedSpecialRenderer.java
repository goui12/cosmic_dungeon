package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Set;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BedRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class BedSpecialRenderer implements NoDataSpecialModelRenderer {
    private final BedRenderer bedRenderer;
    private final Material material;

    public BedSpecialRenderer(BedRenderer bedRenderer, Material material) {
        this.bedRenderer = bedRenderer;
        this.material = material;
    }

    @Override
    public void submit(
        ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, int packedOverlay, boolean hasFoil, int outlineColor
    ) {
        this.bedRenderer.submitSpecial(poseStack, nodeCollector, packedLight, packedOverlay, this.material, outlineColor);
    }

    @Override
    public void getExtents(Set<Vector3f> output) {
        this.bedRenderer.getExtents(output);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked(ResourceLocation texture) implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<BedSpecialRenderer.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_388012_ -> p_388012_.group(ResourceLocation.CODEC.fieldOf("texture").forGetter(BedSpecialRenderer.Unbaked::texture))
                .apply(p_388012_, BedSpecialRenderer.Unbaked::new)
        );

        public Unbaked(DyeColor p_386855_) {
            this(Sheets.colorToResourceMaterial(p_386855_));
        }

        @Override
        public MapCodec<BedSpecialRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new BedSpecialRenderer(new BedRenderer(context), Sheets.BED_MAPPER.apply(this.texture));
        }
    }
}
