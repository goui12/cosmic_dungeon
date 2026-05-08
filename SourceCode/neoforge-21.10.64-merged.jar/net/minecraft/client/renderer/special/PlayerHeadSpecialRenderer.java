package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.SkullBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class PlayerHeadSpecialRenderer implements SpecialModelRenderer<PlayerSkinRenderCache.RenderInfo> {
    private final PlayerSkinRenderCache playerSkinRenderCache;
    private final SkullModelBase modelBase;

    PlayerHeadSpecialRenderer(PlayerSkinRenderCache playerSkinRenderCache, SkullModelBase modelBase) {
        this.playerSkinRenderCache = playerSkinRenderCache;
        this.modelBase = modelBase;
    }

    public void submit(
        @Nullable PlayerSkinRenderCache.RenderInfo argument,
        ItemDisplayContext displayContext,
        PoseStack poseStack,
        SubmitNodeCollector nodeCollector,
        int packedLight,
        int packedOverlay,
        boolean hasFoil,
        int outlineColor
    ) {
        RenderType rendertype = argument != null ? argument.renderType() : PlayerSkinRenderCache.DEFAULT_PLAYER_SKIN_RENDER_TYPE;
        SkullBlockRenderer.submitSkull(null, 180.0F, 0.0F, poseStack, nodeCollector, packedLight, this.modelBase, rendertype, outlineColor, null);
    }

    @Override
    public void getExtents(Set<Vector3f> output) {
        PoseStack posestack = new PoseStack();
        posestack.translate(0.5F, 0.0F, 0.5F);
        posestack.scale(-1.0F, -1.0F, 1.0F);
        this.modelBase.root().getExtentsForGui(posestack, output);
    }

    @Nullable
    public PlayerSkinRenderCache.RenderInfo extractArgument(ItemStack stack) {
        ResolvableProfile resolvableprofile = stack.get(DataComponents.PROFILE);
        return resolvableprofile == null ? null : this.playerSkinRenderCache.getOrDefault(resolvableprofile);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<PlayerHeadSpecialRenderer.Unbaked> MAP_CODEC = MapCodec.unit(PlayerHeadSpecialRenderer.Unbaked::new);

        @Override
        public MapCodec<PlayerHeadSpecialRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        @Nullable
        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext p_433416_) {
            SkullModelBase skullmodelbase = SkullBlockRenderer.createModel(p_433416_.entityModelSet(), SkullBlock.Types.PLAYER);
            return skullmodelbase == null ? null : new PlayerHeadSpecialRenderer(p_433416_.playerSkinRenderCache(), skullmodelbase);
        }
    }
}
