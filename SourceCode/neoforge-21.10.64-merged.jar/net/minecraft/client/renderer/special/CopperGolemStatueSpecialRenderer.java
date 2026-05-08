package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.model.CopperGolemStatueModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.coppergolem.CopperGolemOxidationLevels;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class CopperGolemStatueSpecialRenderer implements NoDataSpecialModelRenderer {
    private final CopperGolemStatueModel model;
    private final ResourceLocation texture;
    static final Map<CopperGolemStatueBlock.Pose, ModelLayerLocation> MODELS = Map.of(
        CopperGolemStatueBlock.Pose.STANDING,
        ModelLayers.COPPER_GOLEM,
        CopperGolemStatueBlock.Pose.SITTING,
        ModelLayers.COPPER_GOLEM_SITTING,
        CopperGolemStatueBlock.Pose.STAR,
        ModelLayers.COPPER_GOLEM_STAR,
        CopperGolemStatueBlock.Pose.RUNNING,
        ModelLayers.COPPER_GOLEM_RUNNING
    );

    public CopperGolemStatueSpecialRenderer(CopperGolemStatueModel model, ResourceLocation texture) {
        this.model = model;
        this.texture = texture;
    }

    @Override
    public void submit(
        ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, int packedOverlay, boolean hasFoil, int outlineColor
    ) {
        this.positionModel(poseStack);
        nodeCollector.submitModel(
            this.model, Direction.SOUTH, poseStack, RenderType.entityCutoutNoCull(this.texture), packedLight, packedOverlay, -1, null, outlineColor, null
        );
    }

    @Override
    public void getExtents(Set<Vector3f> output) {
        PoseStack posestack = new PoseStack();
        this.positionModel(posestack);
        this.model.root().getExtentsForGui(posestack, output);
    }

    private void positionModel(PoseStack poseStack) {
        poseStack.translate(0.5F, 1.5F, 0.5F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked(ResourceLocation texture, CopperGolemStatueBlock.Pose pose) implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<CopperGolemStatueSpecialRenderer.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_445238_ -> p_445238_.group(
                    ResourceLocation.CODEC.fieldOf("texture").forGetter(CopperGolemStatueSpecialRenderer.Unbaked::texture),
                    CopperGolemStatueBlock.Pose.CODEC.fieldOf("pose").forGetter(CopperGolemStatueSpecialRenderer.Unbaked::pose)
                )
                .apply(p_445238_, CopperGolemStatueSpecialRenderer.Unbaked::new)
        );

        public Unbaked(WeatheringCopper.WeatherState p_447117_, CopperGolemStatueBlock.Pose p_445375_) {
            this(CopperGolemOxidationLevels.getOxidationLevel(p_447117_).texture(), p_445375_);
        }

        @Override
        public MapCodec<CopperGolemStatueSpecialRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            CopperGolemStatueModel coppergolemstatuemodel = new CopperGolemStatueModel(
                context.entityModelSet().bakeLayer(CopperGolemStatueSpecialRenderer.MODELS.get(this.pose))
            );
            return new CopperGolemStatueSpecialRenderer(coppergolemstatuemodel, this.texture);
        }
    }
}
