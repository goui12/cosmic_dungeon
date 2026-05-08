package net.minecraft.client.renderer.item;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.util.RegistryContextSwapper;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ItemModel {
    void update(
        ItemStackRenderState renderState,
        ItemStack stack,
        ItemModelResolver itemModelResolver,
        ItemDisplayContext displayContext,
        @Nullable ClientLevel level,
        @Nullable ItemOwner owner,
        int seed
    );

    @OnlyIn(Dist.CLIENT)
    public record BakingContext(
        ModelBaker blockModelBaker,
        EntityModelSet entityModelSet,
        MaterialSet materials,
        PlayerSkinRenderCache playerSkinRenderCache,
        ItemModel missingItemModel,
        @Nullable RegistryContextSwapper contextSwapper
        , net.neoforged.neoforge.client.entity.animation.json.AnimationLoader.PendingAnimations pendingAnimations
    ) implements SpecialModelRenderer.BakingContext {
        /**
         * @deprecated Neo: use {@link #BakingContext(ModelBaker, EntityModelSet, MaterialSet, PlayerSkinRenderCache, ItemModel, RegistryContextSwapper, net.neoforged.neoforge.client.entity.animation.json.AnimationLoader.PendingAnimations)} instead
         */
        @Deprecated
        public BakingContext(
                ModelBaker blockModelBaker,
                EntityModelSet entityModelSet,
                MaterialSet materials,
                PlayerSkinRenderCache playerSkinRenderCache,
                ItemModel missingItemModel,
                @Nullable RegistryContextSwapper contextSwapper
        ) {
            this(blockModelBaker, entityModelSet, materials, playerSkinRenderCache, missingItemModel, contextSwapper, net.neoforged.neoforge.client.entity.animation.json.AnimationLoader.PendingAnimations.EMPTY);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public interface Unbaked extends ResolvableModel {
        MapCodec<? extends ItemModel.Unbaked> type();

        ItemModel bake(ItemModel.BakingContext context);
    }
}
