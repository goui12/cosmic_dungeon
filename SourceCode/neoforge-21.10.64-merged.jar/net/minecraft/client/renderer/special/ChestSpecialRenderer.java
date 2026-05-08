package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Set;
import net.minecraft.client.model.ChestModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class ChestSpecialRenderer implements NoDataSpecialModelRenderer {
    public static final ResourceLocation GIFT_CHEST_TEXTURE = ResourceLocation.withDefaultNamespace("christmas");
    public static final ResourceLocation NORMAL_CHEST_TEXTURE = ResourceLocation.withDefaultNamespace("normal");
    public static final ResourceLocation TRAPPED_CHEST_TEXTURE = ResourceLocation.withDefaultNamespace("trapped");
    public static final ResourceLocation ENDER_CHEST_TEXTURE = ResourceLocation.withDefaultNamespace("ender");
    public static final ResourceLocation COPPER_CHEST_TEXTURE = ResourceLocation.withDefaultNamespace("copper");
    public static final ResourceLocation EXPOSED_COPPER_CHEST_TEXTURE = ResourceLocation.withDefaultNamespace("copper_exposed");
    public static final ResourceLocation WEATHERED_COPPER_CHEST_TEXTURE = ResourceLocation.withDefaultNamespace("copper_weathered");
    public static final ResourceLocation OXIDIZED_COPPER_CHEST_TEXTURE = ResourceLocation.withDefaultNamespace("copper_oxidized");
    private final MaterialSet materials;
    private final ChestModel model;
    private final Material material;
    private final float openness;

    public ChestSpecialRenderer(MaterialSet materials, ChestModel model, Material material, float openness) {
        this.materials = materials;
        this.model = model;
        this.material = material;
        this.openness = openness;
    }

    @Override
    public void submit(
        ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, int packedOverlay, boolean hasFoil, int outlineColor
    ) {
        nodeCollector.submitModel(
            this.model,
            this.openness,
            poseStack,
            this.material.renderType(RenderType::entitySolid),
            packedLight,
            packedOverlay,
            -1,
            this.materials.get(this.material),
            outlineColor,
            null
        );
    }

    @Override
    public void getExtents(Set<Vector3f> output) {
        PoseStack posestack = new PoseStack();
        this.model.setupAnim(this.openness);
        this.model.root().getExtentsForGui(posestack, output);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked(ResourceLocation texture, float openness) implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<ChestSpecialRenderer.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_388545_ -> p_388545_.group(
                    ResourceLocation.CODEC.fieldOf("texture").forGetter(ChestSpecialRenderer.Unbaked::texture),
                    Codec.FLOAT.optionalFieldOf("openness", 0.0F).forGetter(ChestSpecialRenderer.Unbaked::openness)
                )
                .apply(p_388545_, ChestSpecialRenderer.Unbaked::new)
        );

        public Unbaked(ResourceLocation p_387139_) {
            this(p_387139_, 0.0F);
        }

        @Override
        public MapCodec<ChestSpecialRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            ChestModel chestmodel = new ChestModel(context.entityModelSet().bakeLayer(ModelLayers.CHEST));
            Material material = Sheets.CHEST_MAPPER.apply(this.texture);
            return new ChestSpecialRenderer(context.materials(), chestmodel, material, this.openness);
        }
    }
}
