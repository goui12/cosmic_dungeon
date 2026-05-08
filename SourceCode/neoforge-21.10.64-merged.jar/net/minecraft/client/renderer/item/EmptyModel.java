package net.minecraft.client.renderer.item;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EmptyModel implements ItemModel {
    public static final ItemModel INSTANCE = new EmptyModel();

    @Override
    public void update(
        ItemStackRenderState p_390492_,
        ItemStack p_390354_,
        ItemModelResolver p_390507_,
        ItemDisplayContext p_390501_,
        @Nullable ClientLevel p_390403_,
        @Nullable ItemOwner p_435178_,
        int p_390493_
    ) {
        p_390492_.appendModelIdentityElement(this);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked() implements ItemModel.Unbaked {
        public static final MapCodec<EmptyModel.Unbaked> MAP_CODEC = MapCodec.unit(EmptyModel.Unbaked::new);

        @Override
        public void resolveDependencies(ResolvableModel.Resolver p_390435_) {
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext p_390519_) {
            return EmptyModel.INSTANCE;
        }

        @Override
        public MapCodec<EmptyModel.Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
