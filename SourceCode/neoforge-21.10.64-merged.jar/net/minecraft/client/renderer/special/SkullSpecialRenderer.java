package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.SkullBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class SkullSpecialRenderer implements NoDataSpecialModelRenderer {
    private final SkullModelBase model;
    private final float animation;
    private final RenderType renderType;

    public SkullSpecialRenderer(SkullModelBase model, float animation, RenderType renderType) {
        this.model = model;
        this.animation = animation;
        this.renderType = renderType;
    }

    @Override
    public void submit(
        ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, int packedOverlay, boolean hasFoil, int outlineColor
    ) {
        SkullBlockRenderer.submitSkull(null, 180.0F, this.animation, poseStack, nodeCollector, packedLight, this.model, this.renderType, outlineColor, null);
    }

    @Override
    public void getExtents(Set<Vector3f> output) {
        PoseStack posestack = new PoseStack();
        posestack.translate(0.5F, 0.0F, 0.5F);
        posestack.scale(-1.0F, -1.0F, 1.0F);
        SkullModelBase.State skullmodelbase$state = new SkullModelBase.State();
        skullmodelbase$state.animationPos = this.animation;
        skullmodelbase$state.yRot = 180.0F;
        this.model.setupAnim(skullmodelbase$state);
        this.model.root().getExtentsForGui(posestack, output);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked(SkullBlock.Type kind, Optional<ResourceLocation> textureOverride, float animation) implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<SkullSpecialRenderer.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_390096_ -> p_390096_.group(
                    SkullBlock.Type.CODEC.fieldOf("kind").forGetter(SkullSpecialRenderer.Unbaked::kind),
                    ResourceLocation.CODEC.optionalFieldOf("texture").forGetter(SkullSpecialRenderer.Unbaked::textureOverride),
                    Codec.FLOAT.optionalFieldOf("animation", 0.0F).forGetter(SkullSpecialRenderer.Unbaked::animation)
                )
                .apply(p_390096_, SkullSpecialRenderer.Unbaked::new)
        );

        public Unbaked(SkullBlock.Type p_387200_) {
            this(p_387200_, Optional.empty(), 0.0F);
        }

        @Override
        public MapCodec<SkullSpecialRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        @Nullable
        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            SkullModelBase skullmodelbase = SkullBlockRenderer.createModel(context.entityModelSet(), this.kind);
            ResourceLocation resourcelocation = this.textureOverride
                .<ResourceLocation>map(p_389346_ -> p_389346_.withPath(p_389344_ -> "textures/entity/" + p_389344_ + ".png"))
                .orElse(null);
            if (skullmodelbase == null) {
                return null;
            } else {
                RenderType rendertype = SkullBlockRenderer.getSkullRenderType(this.kind, resourcelocation);
                return new SkullSpecialRenderer(skullmodelbase, this.animation, rendertype);
            }
        }
    }
}
