package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.model.ShieldModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class ShieldSpecialRenderer implements SpecialModelRenderer<DataComponentMap> {
    private final MaterialSet materials;
    private final ShieldModel model;

    public ShieldSpecialRenderer(MaterialSet materials, ShieldModel model) {
        this.materials = materials;
        this.model = model;
    }

    @Nullable
    public DataComponentMap extractArgument(ItemStack stack) {
        return stack.immutableComponents();
    }

    public void submit(
        @Nullable DataComponentMap argument,
        ItemDisplayContext displayContext,
        PoseStack poseStack,
        SubmitNodeCollector nodeCollector,
        int packedLight,
        int packedOverlay,
        boolean hasFoil,
        int outlineColor
    ) {
        BannerPatternLayers bannerpatternlayers = argument != null
            ? argument.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
            : BannerPatternLayers.EMPTY;
        DyeColor dyecolor = argument != null ? argument.get(DataComponents.BASE_COLOR) : null;
        boolean flag = !bannerpatternlayers.layers().isEmpty() || dyecolor != null;
        poseStack.pushPose();
        poseStack.scale(1.0F, -1.0F, -1.0F);
        Material material = flag ? ModelBakery.SHIELD_BASE : ModelBakery.NO_PATTERN_SHIELD;
        nodeCollector.submitModelPart(
            this.model.handle(),
            poseStack,
            this.model.renderType(material.atlasLocation()),
            packedLight,
            packedOverlay,
            this.materials.get(material),
            false,
            false,
            -1,
            null,
            outlineColor
        );
        if (flag) {
            BannerRenderer.submitPatterns(
                this.materials,
                poseStack,
                nodeCollector,
                packedLight,
                packedOverlay,
                this.model,
                Unit.INSTANCE,
                material,
                false,
                Objects.requireNonNullElse(dyecolor, DyeColor.WHITE),
                bannerpatternlayers,
                hasFoil,
                null,
                outlineColor
            );
        } else {
            nodeCollector.submitModelPart(
                this.model.plate(),
                poseStack,
                this.model.renderType(material.atlasLocation()),
                packedLight,
                packedOverlay,
                this.materials.get(material),
                false,
                hasFoil,
                -1,
                null,
                outlineColor
            );
        }

        poseStack.popPose();
    }

    @Override
    public void getExtents(Set<Vector3f> output) {
        PoseStack posestack = new PoseStack();
        posestack.scale(1.0F, -1.0F, -1.0F);
        this.model.root().getExtentsForGui(posestack, output);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final ShieldSpecialRenderer.Unbaked INSTANCE = new ShieldSpecialRenderer.Unbaked();
        public static final MapCodec<ShieldSpecialRenderer.Unbaked> MAP_CODEC = MapCodec.unit(INSTANCE);

        @Override
        public MapCodec<ShieldSpecialRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext p_434068_) {
            return new ShieldSpecialRenderer(p_434068_.materials(), new ShieldModel(p_434068_.entityModelSet().bakeLayer(ModelLayers.SHIELD)));
        }
    }
}
