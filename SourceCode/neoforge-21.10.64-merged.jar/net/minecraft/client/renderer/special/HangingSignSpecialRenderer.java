package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class HangingSignSpecialRenderer implements NoDataSpecialModelRenderer {
    private final MaterialSet materials;
    private final Model.Simple model;
    private final Material material;

    public HangingSignSpecialRenderer(MaterialSet materials, Model.Simple model, Material material) {
        this.materials = materials;
        this.model = model;
        this.material = material;
    }

    @Override
    public void submit(
        ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, int packedOverlay, boolean hasFoil, int outlineColor
    ) {
        HangingSignRenderer.submitSpecial(this.materials, poseStack, nodeCollector, packedLight, packedOverlay, this.model, this.material);
    }

    @Override
    public void getExtents(Set<Vector3f> output) {
        PoseStack posestack = new PoseStack();
        HangingSignRenderer.translateBase(posestack, 0.0F);
        posestack.scale(1.0F, -1.0F, -1.0F);
        this.model.root().getExtentsForGui(posestack, output);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked(WoodType woodType, Optional<ResourceLocation> texture) implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<HangingSignSpecialRenderer.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_389524_ -> p_389524_.group(
                    WoodType.CODEC.fieldOf("wood_type").forGetter(HangingSignSpecialRenderer.Unbaked::woodType),
                    ResourceLocation.CODEC.optionalFieldOf("texture").forGetter(HangingSignSpecialRenderer.Unbaked::texture)
                )
                .apply(p_389524_, HangingSignSpecialRenderer.Unbaked::new)
        );

        public Unbaked(WoodType p_389680_) {
            this(p_389680_, Optional.empty());
        }

        @Override
        public MapCodec<HangingSignSpecialRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            Model.Simple model$simple = HangingSignRenderer.createSignModel(
                context.entityModelSet(), this.woodType, HangingSignRenderer.AttachmentType.CEILING_MIDDLE
            );
            Material material = this.texture.map(Sheets.HANGING_SIGN_MAPPER::apply).orElseGet(() -> Sheets.getHangingSignMaterial(this.woodType));
            return new HangingSignSpecialRenderer(context.materials(), model$simple, material);
        }
    }
}
