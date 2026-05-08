package net.minecraft.client.renderer.item;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BundleSelectedItemSpecialRenderer implements ItemModel {
    static final ItemModel INSTANCE = new BundleSelectedItemSpecialRenderer();

    @Override
    public void update(
        ItemStackRenderState p_387618_,
        ItemStack p_386990_,
        ItemModelResolver p_386923_,
        ItemDisplayContext p_387805_,
        @Nullable ClientLevel p_387156_,
        @Nullable ItemOwner p_434149_,
        int p_386711_
    ) {
        p_387618_.appendModelIdentityElement(this);
        ItemStack itemstack = BundleItem.getSelectedItemStack(p_386990_);
        if (!itemstack.isEmpty()) {
            p_386923_.appendItemLayers(p_387618_, itemstack, p_387805_, p_387156_, p_434149_, p_386711_);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked() implements ItemModel.Unbaked {
        public static final MapCodec<BundleSelectedItemSpecialRenderer.Unbaked> MAP_CODEC = MapCodec.unit(new BundleSelectedItemSpecialRenderer.Unbaked());

        @Override
        public MapCodec<BundleSelectedItemSpecialRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext p_387672_) {
            return BundleSelectedItemSpecialRenderer.INSTANCE;
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver p_388792_) {
        }
    }
}
