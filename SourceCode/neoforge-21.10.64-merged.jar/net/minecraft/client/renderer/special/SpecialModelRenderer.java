package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public interface SpecialModelRenderer<T> {
    void submit(
        @Nullable T argument,
        ItemDisplayContext displayContext,
        PoseStack poseStack,
        SubmitNodeCollector nodeCollector,
        int packedLight,
        int packedOverlay,
        boolean hasFoil,
        int outlineColor
    );

    void getExtents(Set<Vector3f> output);

    @Nullable
    T extractArgument(ItemStack stack);

    @OnlyIn(Dist.CLIENT)
    public interface BakingContext {
        EntityModelSet entityModelSet();

        MaterialSet materials();

        PlayerSkinRenderCache playerSkinRenderCache();

        /**
         * Provides access to the data-driven keyframe animations being loaded by the {@code AnimationLoader}
         */
        default net.neoforged.neoforge.client.entity.animation.json.AnimationLoader.PendingAnimations pendingAnimations() {
            return net.neoforged.neoforge.client.entity.animation.json.AnimationLoader.PendingAnimations.EMPTY;
        }

        @OnlyIn(Dist.CLIENT)
        public record Simple(EntityModelSet entityModelSet, MaterialSet materials, PlayerSkinRenderCache playerSkinRenderCache, net.neoforged.neoforge.client.entity.animation.json.AnimationLoader.PendingAnimations pendingAnimations)
            implements SpecialModelRenderer.BakingContext {

            /**
             * @deprecated Neo: use {@link #Simple(EntityModelSet, MaterialSet, PlayerSkinRenderCache, net.neoforged.neoforge.client.entity.animation.json.AnimationLoader.PendingAnimations)} instead
             */
            @Deprecated
            public Simple(EntityModelSet entityModelSet, MaterialSet materials, PlayerSkinRenderCache playerSkinRenderCache) {
                this(entityModelSet, materials, playerSkinRenderCache, net.neoforged.neoforge.client.entity.animation.json.AnimationLoader.PendingAnimations.EMPTY);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public interface Unbaked {
        @Nullable
        SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context);

        MapCodec<? extends SpecialModelRenderer.Unbaked> type();
    }
}
